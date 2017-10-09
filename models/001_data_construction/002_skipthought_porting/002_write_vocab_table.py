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
  print 'loading uni-tables for skipthoughts ..'
  utable = numpy.load(path_to_models + 'utable.npy')
  print 'utable loading done'
  
  utable = OrderedDict(zip(words, utable))
  print 'utable rearranging done'
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
  
  small_utable = numpy.zeros((len(videoqa_vocab), utable[utable.keys()[0]].shape[1]), \
                                               dtype=utable[utable.keys()[0]].dtype)
  print 'videoqa vocab size: %d' % len(videoqa_vocab)
  print 'small_utable size: %d x %d ' % (small_utable.shape[0], small_utable.shape[1])
  print ''
  
  print 'copying videoqa vocab vectors to small utable ..'
  for k, v in videoqa_vocab.items() :
    if v == '<EOS>':
      small_utable[int(k)] = utable['<eos>']
    else:
      if d[v] > 0:
        small_utable[int(k)] = utable[v]
      else:
        small_utable[int(k)] = utable['UNK']
  
  print 'save small_utable to file'
  small_utable_path = params['small_utable_path']
  print 'videoqa utable path: %s' % small_utable_path
  print 'saving ..'
  numpy.save(small_utable_path, small_utable)
  print 'done\n'
