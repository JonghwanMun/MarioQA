#! /bin/bash

dt=case3

# create folder where checkpoints are saved, if not exist
if [ ! -d ./model_${dt} ]; then
   mkdir -p ./model_${dt}
fi

stdbuf -oL th train.lua \
	-clip_info_file data/clip_info_${dt}.json \
	-qa_label_file data/qa_labels_${dt}.h5 \
	-uni_gru_path data/pretrained_models/skipthought/uni_gru_params_${dt}.t7 \
	-uni_gru_word2vec_path data/pretrained_models/skipthought/videoqa_uni_gru_word2vec_${dt}.t7 \
	-batch_size 16 -gpuid 0 \
	-checkpoint_path model_${dt} \
	2>&1 | tee log_train_${dt}.log
