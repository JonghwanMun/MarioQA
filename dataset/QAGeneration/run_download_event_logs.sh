#! /bin/bash

# build link of folder for event logs if not exist
if [ ! -d ./event_logs ]; then
	mkdir -p event_logs 
fi

cd event_logs
wget http://cvlab.postech.ac.kr/research/MarioQA/data/eventLogFiles.tar.gz
tar xzvf eventLogFiles.tar.gz
rm eventLogFiles.tar.gz
cd ..
