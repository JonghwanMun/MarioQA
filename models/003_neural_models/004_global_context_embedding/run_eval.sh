#! /bin/bash

if [ ! -d ./att_map_test ]; then
   mkdir -p ./att_map_test
fi

id=23
tdt=case3

dt=HardTemp
stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2${dt} -val_clips_use -1 -gpuid 0 -clip_info_file data/clip_info_${tdt}2${dt}.json -qa_label_file data/qa_labels_${tdt}2${dt}.h5 2>&1 | tee log_eval_${tdt}2${dt}_m${id}.log

dt=EasyTemp
stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2${dt} -val_clips_use -1 -gpuid 0 -clip_info_file data/clip_info_${tdt}2${dt}.json -qa_label_file data/qa_labels_${tdt}2${dt}.h5 2>&1 | tee log_eval_${tdt}2${dt}_m${id}.log
#stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2${dt} -val_clips_use -1 -gpuid 0 -clip_info_file data/clip_info_EasyTemp2${dt}.json -qa_label_file data/qa_labels_EasyTemp2${dt}.h5 2>&1 | tee log_eval_${tdt}2${dt}_m${id}.log

dt=NoTemp
stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2${dt} -val_clips_use -1 -gpuid 0 -clip_info_file data/clip_info_${tdt}2${dt}.json -qa_label_file data/qa_labels_${tdt}2${dt}.h5 2>&1 | tee log_eval_${tdt}2${dt}_m${id}.log
#stdbuf -oL th eval.lua -start_from model_${tdt}/model_id${id}.t7 -output_prefix _eval_out_${tdt}2${dt} -val_clips_use -1 -gpuid 3 -clip_info_file data/clip_info_${dt}.json -qa_label_file data/qa_labels_${dt}.h5 2>&1 | tee log_eval_${tdt}2${dt}_m${id}.log
