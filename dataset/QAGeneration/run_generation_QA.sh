#! /bin/bash

# build link of folder for event logs if not exist
if [ ! -d ./event_logs ]; then
	ln -s ../playingMario/event_logs ./
fi
# create folder for generated annotations if not exist
outFolder=./data/generated_annotation
if [ ! -d ${outFolder} ]; then
	mkdir -p ${outFolder}
fi

java -cp target/MarioQA-1.0-jar-with-dependencies.jar \
	com.postechCVlab.MarioQA.QApairsGeneration \
	-l generation_data/logFileLists.txt \
	-c generation_data/configuration.json \
	-q generation_data/templates.json \
	-p generation_data/phrases.json \
	-o ${outFolder} \
	-d lev0
