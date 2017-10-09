#! /bin/bash

splitFile=data/split.txt
if [ ! -f ${splitFile} ]; then
	wget cvlab.postech.ac.kr/~jonghwan/research/MarioQA/split.txt
	mv split.txt data/ 
	echo "Download the split.txt file"
fi
