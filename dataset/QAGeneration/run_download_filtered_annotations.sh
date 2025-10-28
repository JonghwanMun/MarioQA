#! /bin/bash

# build link of folder for event logs if not exist
if [ ! -d ./data/generated_annotation ]; then
	mkdir -p ./data/generated_annotation
fi

cd ./data/generated_annotation
wget http://cvlab.postech.ac.kr//research/MarioQA/data/filtered_annotations.tar.gz
tar xzvf filtered_annotations.tar.gz
rm filtered_annotations.tar.gz
cd ../..
