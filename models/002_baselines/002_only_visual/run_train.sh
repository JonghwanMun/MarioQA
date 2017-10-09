#! /bin/bash

dt=case3
if [ ! -d ./model_${dt} ]; then
   mkdir -p ./model_${dt}
fi

stdbuf -oL th train.lua \
	-clip_info_file data/clip_info_${dt}.json \
	-qa_label_file data/qa_labels_${dt}.h5 \
	-batch_size 16 -gpuid 0 \
	-checkpoint_path model_${dt} 2>&1 | tee log_train_${dt}.log
