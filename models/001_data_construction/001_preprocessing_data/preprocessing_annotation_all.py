import os
import json
import argparse
from random import shuffle, seed
import string

# non-standard dependencies:
import h5py
import numpy as np
from scipy.misc import imread, imresize

def convert_to_onehot(vector, num_classes=None) :
  assert isinstance(vector, np.ndarray)
  assert len(vector) > 0

  if num_classes is None :
    num_classes = np.max(vector) + 1
  else :
    assert num_classes > 0
    assert num_classes >= np.max(vector)

  result = np.zeros(shape=(len(vector), num_classes))
  result[np.arange(len(vector)), vector] = 1

  return result.astype('uint32')

def prepro_question(clips):
  
  # preprocess all the captions
  # each clip contains information of video_path, begin_frame, end_frame, 
  # question, answer, clip_length, question_type 
  print 'example processed tokens:'
  for i,clip in enumerate(clips):
    txt = str(clip['question']).lower().translate(None, '?').strip().split() 
    clip['processed_tokens'] = txt
    if i < 10 : print txt

def build_vocab(clips, params):
  count_thr = params['word_count_threshold']

  # count up the number of words
  counts = {}
  for clip in clips:
    txt = clip['processed_tokens']
    for w in txt:
      counts[w] = counts.get(w, 0) + 1
  cw = sorted([(count,w) for w,count in counts.iteritems()], reverse=True)
  print 'top 20 words and their counts:'
  print '\n'.join(map(str,cw[:20]))

  # print some stats
  total_words = sum(counts.itervalues())
  print 'total words:', total_words
  bad_words = [w for w,n in counts.iteritems() if n <= count_thr]
  vocab = [w for w,n in counts.iteritems() if n > count_thr]
  bad_count = sum(counts[w] for w in bad_words)
  print 'number of bad words: %d/%d = %.2f%%' \
          % (len(bad_words), len(counts), len(bad_words)*100.0/len(counts))
  print 'number of words in vocab would be %d' % len(vocab)
  print 'number of UNKs: %d/%d = %.2f%%' \
          % (bad_count, total_words, bad_count*100.0/total_words)

  # lets look at the distribution of lengths as well
  sent_lengths = {}
  for clip in clips:
    txt = clip['processed_tokens']
    nw = len(txt)
    sent_lengths[nw] = sent_lengths.get(nw, 0) + 1
  max_len = max(sent_lengths.keys())
  if max_len <= params['max_length'] :
      params['cur_max_length'] = max_len

  print 'max length sentence in raw data: ', max_len
  print 'sentence length distribution (count, number of words):'
  sum_len = sum(sent_lengths.values())
  for i in xrange(max_len+1):
    print '%2d: %10d   %f%%' \
            % (i, sent_lengths.get(i,0), sent_lengths.get(i,0)*100.0/sum_len)

  # lets now produce the final annotations
  if bad_count > 0:
    # additional special UNK token we will use below to map infrequent words to
    print 'inserting the special UNK token'
    vocab.append('UNK')
  
  # substitute the infrequent words to UNK
  for clip in clips:
    txt = clip['processed_tokens']
    question = [w if counts.get(w,0) > count_thr else 'UNK' for w in txt]
    clip['final_question'] = question

  return vocab

def assign_splits(clips, params):
  
  dt = ['NT', 'ET', 'HT']

  if 'split_file' in params.keys():
    split_mapping = {}
    split_file = open(params['split_file'], 'r')
    while True:
      line = split_file.readline()
      if not line: break

      infos = line.split(':')
      split_mapping[infos[0].strip()] = infos[1].strip()
    for ic in range(3) :
      cur_clips = clips[ic]
      for clip in cur_clips:
        clip['split'] = split_mapping[clip['qa_id']]

      print 'assigned %d to val, %d to test among %d clips for %s.' \
              % (num_val, num_test, len(cur_clips), dt[ic])
      json.dump(cur_clips, open('data/origin_clip_info_%s.json'%dt[ic], 'w'))
      print 'wrote to data/origin_clip_info_%s.json' % dt[ic]
  else :
    for ic in range(3) :
      cur_clips = clips[ic]
      num_val = len(cur_clips) * params['num_val']
      num_test = len(cur_clips) * params['num_test']
    
      for i,clip in enumerate(cur_clips):
        if i < num_val:
          clip['split'] = 'val'
        elif i < num_val + num_test: 
          clip['split'] = 'test'
        else:
          clip['split'] = 'train'

      print 'assigned %d to val, %d to test among %d clips for %s.' \
              % (num_val, num_test, len(cur_clips), dt[ic])
      json.dump(cur_clips, open(params['output_json'] % dt[ic] , 'w'))
      print 'wrote to data/origin_clip_info_%s.json' % dt[ic]

  #return [clips[0], clips[0]+clips[1], clips[0]+clips[1]+clips[2]]
  return clips

def encode_questions(clips, params, wtoi):

  max_length = params['cur_max_length']
  M = len(clips)

  answers = {}
  Q = np.zeros((M, max_length), dtype='uint32')
  question_length = np.zeros(M, dtype='uint32')

  for i,clip in enumerate(clips):

    ans = clip['answer']
    answers[ans] = answers.get(ans, 0) + 1

    Qi = np.zeros((1, max_length), dtype='uint32')
    s = clip['final_question']
    question_length[i] = min(max_length, len(s)) # record the length of this sequence
    for k,w in enumerate(s):  # k is index of words in caption
      if k < max_length:
        Qi[0,k] = wtoi[w]

    # note: word indices are 1-indexed, and questions are padded with zeros
    Q[i, :] = Qi[:]
  
  # construct answers as label format 
  answer_labels = np.zeros(M, dtype='uint32')
  itoa = {i:w for i,w in enumerate(answers)}
  atoi = {w:i for i,w in enumerate(answers)}

  for i,clip in enumerate(clips):
    answer_labels[i] = atoi[clip['answer']]

  A = convert_to_onehot(answer_labels)
  assert Q.shape[0] == M, 'lengths don\'t match? that\'s weird'
  assert np.all(question_length > 0), 'error: some caption had no words?'

  print 'encoded questions to array of size ', `Q.shape`
  return A, Q, question_length, itoa, atoi

def main(params):

  seed(123) # make reproducible
  dt = ['NT', 'ET', 'HT']  # data type

  clips = [None] * 3
  for idt in range(3) :
    clips[idt] = json.load(open(params['input_json']%dt[idt], 'r'))  # raw_annotation.json
    shuffle(clips[idt]) # shuffle the order
    prepro_question(clips[idt]) # tokenization and preprocessing

  # assign the splits
  clips = assign_splits(clips, params)
  cases = [clips[0], clips[0]+clips[1], clips[0]+clips[1]+clips[2]]
  
  """ obtain vocabulary
  Note: we construct vocabulary using case 3 (NoTemp + EasyTemp + HardTemp)
  for convenient evaluation between each others.
  """
  vocab = build_vocab(cases[2], params)
  itow = {i+1:w for i,w in enumerate(vocab)} # a 1-indexed vocab translation table
  wtoi = {w:i+1 for i,w in enumerate(vocab)} # inverse table
  wtoi['<EOS>'] = 0
  itow['0'] = '<EOS>'

  for idt in range(3) :
    # encode captions in large arrays, ready to ship to hdf5 file
    A, Q, question_length, itoa, atoi = encode_questions(cases[idt], params, wtoi)
  
    # create output h5 file
    f = h5py.File(params['output_h5'] % ('case'+str(idt+1)), "w")
    f.create_dataset("answers", dtype='uint32', data=A)
    f.create_dataset("questions", dtype='uint32', data=Q)
    f.create_dataset("question_length", dtype='uint32', data=question_length)
  
    f.close()
    print 'wrote ', params['output_h5'] % ('case'+str(idt+1))
  
    # create output json file
    out = {}
    out['ix_to_ans'] = itoa
    out['ans_to_ix'] = atoi
    out['ix_to_word'] = itow
    out['word_to_ix'] = wtoi
    out['clips'] = []

    key_list = ['video_path', 'begin_frame', 'end_frame', 'question', 'answer',
                'qa_id', 'clip_length']
    for i,clip in enumerate(cases[idt]):
      jclip = {}
      jclip['split'] = clip['split']
      for k in key_list : jclip[k] = clip[k]
      
      out['clips'].append(jclip)
    
    json.dump(out, open(params['output_json'] % ('case'+str(idt+1)) , 'w'))
    print 'wrote ', params['output_json'] % ('case'+str(idt+1))

if __name__ == "__main__":

  parser = argparse.ArgumentParser()

  # input json
  parser.add_argument('--input_json',
                      default='data/generated_annotations/filtered_annotations_%s.json', 
                      help='input json file to process into hdf5')
  parser.add_argument('--output_json', default='data/clip_info_%s.json', 
          help='output clip information of json file')
  parser.add_argument('--output_h5', default='data/qa_labels_%s.h5', 
          help='output QA labels of h5 file')
  parser.add_argument('--slpit_file', default='data/split.txt', 
          help='pre-defined split')
  
  # options
  parser.add_argument('--max_length', default=50, type=int, 
          help='max length of a question, in number of words. questions longer than this get clipped.')
  parser.add_argument('--word_count_threshold', default=0, type=int, 
          help='only words that occur more than this number of times will be put in vocab')
  parser.add_argument('--num_val', default=0.2, type=float, 
          help='number of images to assign to validation data (for CV etc)')
  parser.add_argument('--num_test', default=0.2, type=float, 
          help='number of test images (to withold until very very end)')

  args = parser.parse_args()
  params = vars(args) # convert to ordinary dict
  print 'parsed input parameters:'
  print json.dumps(params, indent = 2)
  main(params)
