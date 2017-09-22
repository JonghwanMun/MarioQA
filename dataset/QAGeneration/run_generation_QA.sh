#! /bin/bash

# build link of folder for event logs if not exist
if [ ! -d ./event_logs ]; then
	ln -s ../playingMario/event_logs ./
fi
# create folder for generated annotations if not exist
if [ ! -d ./generated_annotation ]; then
	mkdir -p ./generated_annotation
fi

java -cp target/MarioQA-1.0-jar-with-dependencies.jar com.postechCVlab.MarioQA.QApairsGeneration -l data/logFileLists.txt -c data/configuration.json -q data/templates.json -p data/phrases.json -d lev0
