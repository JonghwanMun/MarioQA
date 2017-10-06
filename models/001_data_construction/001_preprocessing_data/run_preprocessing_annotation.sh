#! /bin/bash

annFolder=generated_annotations
if [ ! -d ${annFolder} ]; then
	mkdir -p ${annFolder}		# doubly check if the folder exists
	wget cvlab.postech.ac.kr/~jonghwan/MarioQA/filtered_annotations.tar.gz
	mv filtered_annotations.tar.gz ${annFolder}/
	cd ${annFolder}
	tar zxvf filtered_annotations.tar.gz
	rm filtered_annotations.tar.gz
	cd ..
fi

stdbuf -oL python preprocessing_annotation_all.py 2>&1 \
	| tee log_preprocessing_annotation_all.log
