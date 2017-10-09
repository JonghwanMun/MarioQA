#! /bin/bash

cd data
wget cvlab.postech.ac.kr/~jonghwan/research/MarioQA/preprocessed_annotation.tar.gz
tar zxvf preprocessed_annotation.tar.gz
rm preprocessed_annotation.tar.gz
cd ..
echo "Download the preprocessed annotations"
