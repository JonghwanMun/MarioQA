#! /bin/bash

width=160
height=120

originFolder=./data/gameplay/
if [ ! -d ${originFolder} ]; then
   ln -s ../000_playing_mario_gen_anno/gameplay/ ${originFolder}
fi

clipFolder=./data/mario_resized_frames/
if [ ! -d ${clipFolder} ]; then
	mkdir -p ${clipFolder}		# doubly check if the folder exists
fi

# preprocessing that resizes the frames
stdbuf -oL th preprocessing_frame.lua \
	-data_path ${originFolder} \
	-out_path ${clipFolder} \
	-width ${width} -height ${height} 2>&1 \
	| tee log_mario_frame_preprocessing_resize${width}x${height}.log
