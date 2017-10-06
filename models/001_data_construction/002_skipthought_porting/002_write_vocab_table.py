import os

import cPickle as pkl
import numpy
import copy
import nltk
import json
import argparse

from collections import OrderedDict, defaultdict
from scipy.linalg import norm
from nltk.tokenize import word_tokenize

## import skipthoughts path
import sys

if __name__ == "__main__":
  parser = argparse.ArgumentParser()

  parser.add_argument('--videoqa_vocab_path', default='data/clip_info.json', help='path to vocabulary')
  parser.add_argument('--small_utable_path', default='data/pretrained_models/skipthought/videoqa_utable.npy', help='path to uni-skipthought word embedding layer weight for videoQA')
  parser.add_argument('--small_btable_path', default='data/pretrained_models/skipthought/videoqa_btable.npy', help='path to bi-skipthought word embedding layer weight for videoQA')

  args = parser.parse_args()
  params = vars(args) # convert to ordinary dict

  skipthoughts_path = './data/pretrained_models/skipthought/paper_version/'
  print 'skipthoughts_path: %s' % skipthoughts_path
  sys.path.append(skipthoughts_path)
  
  ## load table files
  path_to_models = skipthoughts_path 
  print 'path_to_models: %s' % path_to_models
  
  print 'loading skipthoughts dictionaries ..'
  words = []
  f = open(path_to_models + 'dictionary.txt', 'rb')
  for line in f:
    words.append(line.decode('utf-8').strip())
  f.close()
  print 'done\n'
  
  ## load table
  print 'loading uni and bi tables for skipthoughts ..'
  utable = numpy.load(path_to_models + 'utable.npy')
  print 'utable loading done'
  btable = numpy.load(path_to_models + 'btable.npy')
  print 'btable loading done'
  
  utable = OrderedDict(zip(words, utable))
  print 'utable rearranging done'
  btable = OrderedDict(zip(words, btable))
  print 'btable rearranging done'
  print 'done\n'
  
  # word dictionary check
  d = defaultdict(lambda : 0)
  for w in utable.keys():
    d[w] = 1
  
  videoqa_vocab_path = params['videoqa_vocab_path']
  print 'videoQA vocab path: %s \n' % videoqa_vocab_path
  
  print 'load videoQA vocab ..'
  with open(videoqa_vocab_path, 'r') as f:
      info = json.load(f)
      videoqa_vocab = info['ix_to_word']
  print 'done\n'
  
  small_utable = numpy.zeros((len(videoqa_vocab)+1, utable[utable.keys()[0]].shape[1]), \
                                               dtype=utable[utable.keys()[0]].dtype)
  small_btable = numpy.zeros((len(videoqa_vocab)+1, btable[btable.keys()[0]].shape[1]), \
                                               dtype=btable[btable.keys()[0]].dtype)
  print 'videoqa vocab size: %d' % len(videoqa_vocab)
  print 'small_utable size: %d x %d ' % (small_utable.shape[0], small_utable.shape[1])
  print 'small_btable size: %d x %d ' % (small_btable.shape[0], small_btable.shape[1])
  print ''
  
  print 'copying videoqa vocab vectors to small utable and btable ..'
  for k, v in videoqa_vocab.items() :
    if d[v] > 0:
      small_utable[int(k)-1] = utable[v]
      small_btable[int(k)-1] = btable[v]
    else:
      small_utable[int(k)-1] = utable['UNK']
      small_btable[int(k)-1] = btable['UNK']
  
  print 'last vector is for <eos>'
  small_utable[len(videoqa_vocab)] = utable['<eos>']
  small_btable[len(videoqa_vocab)] = btable['<eos>']
  print 'done\n'
  
  print 'save small_utable and small_btable to file'
  small_utable_path = params['small_utable_path']
  small_btable_path = params['small_btable_path']
  print 'videoqa utable path: %s' % small_utable_path
  print 'videoqa btable path: %s' % small_btable_path
  print 'saving ..'
  numpy.save(small_utable_path, small_utable)
  numpy.save(small_btable_path, small_btable)
  print 'done\n'
