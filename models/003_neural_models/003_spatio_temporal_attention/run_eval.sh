#! /bin/bash

if [ ! -d ./att_map_test ]; then
   mkdir -p ./att_map_test
fi

id=19
tdt=case3

# test on HT
stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2HT -test_clips_use -1 -gpuid 0 -clip_info_file data/clip_info_HT.json -qa_label_file data/qa_labels_HT.h5 2>&1 | tee log_eval_${tdt}2HT_m${id}.log

# test on ET
stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2ET -test_clips_use -1 -gpuid 0 -clip_info_file data/clip_info_ET.json -qa_label_file data/qa_labels_ET.h5 2>&1 | tee log_eval_${tdt}2ET_m${id}.log

# test on NT
stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2NT -test_clips_use -1 -gpuid 0 -clip_info_file data/clip_info_NT.json -qa_label_file data/qa_labels_NT.h5 2>&1 | tee log_eval_${tdt}2NT_m${id}.log
