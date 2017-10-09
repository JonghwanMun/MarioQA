import os

import theano
import theano.tensor as tensor

import cPickle as pkl
import numpy
import copy
import nltk
import argparse

from collections import OrderedDict, defaultdict
from scipy.linalg import norm
from nltk.tokenize import word_tokenize

print 'save uni, bi GRU encoder parameter for torch porting'
print ''

## import skipthoughts path
import sys
skipthoughts_path ='./skip-thoughts'
print 'skipthoughts_path: %s' % skipthoughts_path
sys.path.append(skipthoughts_path)
import skipthoughts


if __name__ == "__main__":
  parser = argparse.ArgumentParser()

  parser.add_argument('--porting_data_path', default='data/pretrained_models/skipthought/', help='root path for porting data')
  parser.add_argument('--prefix', default='', help='prefix for data type')

  args = parser.parse_args()
  params = vars(args) # convert to ordinary dict

  ## load model options
  print 'Loading model parameters ...'
  path_to_models = './data/pretrained_models/skipthought/paper_version/'
  path_to_umodel = path_to_models + 'uni_skip.npz'
  print '   path_to_models: %s' % path_to_models
  print '   path_to_umodel: %s' % path_to_umodel
  print ''
  
  print 'read uni model options'
  with open('%s.pkl'%path_to_umodel, 'rb') as f:
    uoptions = pkl.load(f)
  print 'done'
  print ''
  
  print 'init and load uparams'
  uparams = skipthoughts.init_params(uoptions)
  uparams = skipthoughts.load_params(path_to_umodel, uparams)
  print(uparams.keys())
  print 'done'
  print ''
  
  print 'start saving'
  porting_data_path = params['porting_data_path'] 
  print 'porting_data_path: %s' % porting_data_path
  print ''
  print 'save_uparams'
  for k in uparams.keys():
  	 save_path = porting_data_path + 'uparams_%s_%s.npy' % (k, params['prefix'])
  	 print '   saving [%s] ..' % save_path,
  	 numpy.save(save_path, uparams[k]) 
  	 print '  done'
  print ''
