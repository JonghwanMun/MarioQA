Models tested on MarioQA dataset
===============

### 1. Dependencies (This project is tested on linux 14.04 64bit with gpu of Titan X and python 2.7.12 with anaconda 4.1.9)
####  Dependencies for torch (need for model training and evaluation)
   * torch ['https://github.com/torch/distro']
   * nn (luarocks install nn)
   * cutorch (luarocks install cutorch)
   * cunn (luarocks install cunn)
   * cudnn ['https://github.com/soumith/cudnn.torch'] (If you don't want to use cudnn, set flag of backend in train.m and eval.m as 'nn'.)
   * hdf5 (luarocks install hdf5)
   * image (luarocks install image)
   * npy4th (luarocks install npy4th) ['https://github.com/htwaijry/npy4th']

####  Dependencies for python (need for data pre-processing)
   * json
   * cPickle
   * nltk
   * numpy
   * ipython notebook (need for visualization of QA annotations)
   * h5py (conda install h5py or pip install h5py)
   * moviepy (pip install moviepy) ['http://zulko.github.io/moviepy/']
   * theano ['http://deeplearning.net/software/theano/index.html'] (need to ship the parameters of skip-thought model from python to lua.)


### 1. Setup instruction
We need two following processes in `001_data_constuction`.
   + [Pre-processing annotations](001_data_construction/001_preprocessing_data/README.md)
   + [Shipping parameters of skip-thought model](001_data_construction/002_skipthought_porting/README.md) to be loaded in lua

Perform each process in proper folder by following the README file.

### 2. Training and evaluating models
#### Model training
   Move to folder that you want to train the model, and run following lines.

   ```
   mv 003_neural_models/003_spatio_temporal_attention/
   bash gen_simulinks.sh
   bash run_train.sh
   ```

#### Model evaluation
   Move to folder that you want to evaluate the model, and run following lines.

   ```
   mv 003_neural_models/003_spatio_temporal_attention/
   bash gen_simulinks.sh
   bash run_download_pretrained_model.sh
   bash run_eval.sh
