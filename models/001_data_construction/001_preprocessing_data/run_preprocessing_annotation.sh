#! /bin/bash

annFolder=data/generated_annotation
if [ ! -d ${annFolder} ]; then
	# if you want to use MarioQA dataset, download the annotations
	mkdir -p ${annFolder}		# doubly check if the folder exists
	wget cvlab.postech.ac.kr/~jonghwan/research/MarioQA/filtered_annotations.tar.gz
	mv filtered_annotations.tar.gz ${annFolder}/
	cd ${annFolder}
	tar zxvf filtered_annotations.tar.gz
	rm filtered_annotations.tar.gz
	cd ..
fi

stdbuf -oL python preprocessing_annotation.py 2>&1 | \
	tee log_preprocessing_annotations.log
