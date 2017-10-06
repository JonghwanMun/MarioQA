
path=data/pretrained_models/skipthought
if [ ! -d ${path} ]; then
   mkdir -p data/pretrained_models/skipthought
   wget cvlab.postech.ac.kr/~jonghwan/MarioQA/skipthought/paper_version.tar
   tar -xvf paper_version.tar
   rm paper_version.tar
   mv paper_version ./data/pretrained_models/skipthought/
fi

dt=$1

echo '====================================================================='
echo '       Run 001_write_vocab_file'
echo '====================================================================='
th 001_write_vocab_file.lua -vocab_imginfo_file data/clip_info_${dt}.json \
	-saveto ./videoqa_vocab_${dt}.txt

echo '====================================================================='
echo '       Run 002_write_vocab_table.py'
echo '====================================================================='
python 002_write_vocab_table.py --videoqa_vocab_path data/clip_info_${dt}.json \
	--small_utable_path data/pretrained_models/skipthought/videoqa_utable_${dt}.npy \
	--small_btable_path data/pretrained_models/skipthought/videoqa_btable_${dt}.npy

echo '====================================================================='
echo '       Run 003_write_encoder_param.py'
echo '====================================================================='
python 003_write_encoder_param.py --prefix ${dt}

echo '====================================================================='
echo '       Run 004_save_params_in_torch_file.lua'
echo '====================================================================='
th 004_save_params_in_torch_file.lua -utable_path videoqa_utable_${dt}.npy \
	-btable_path videoqa_btable_${dt}.npy \
	-uni_word2vec_path videoqa_uni_gru_word2vec_${dt}.t7 \
	-bi_word2vec_path videoqa_bi_gru_word2vec_${dt}.t7

echo '====================================================================='
echo '       Run 005_save_encoder_params_in_torch_file.lua'
echo '====================================================================='
th 005_save_encoder_params_in_torch_file.lua \
	-uni_gru_params_path uni_gru_params_${dt}.t7 \
	-bi_gru_params_path bi_gru_params_${dt}.t7 \
	-bi_gru_r_params_path bi_gru_params_${dt}.t7 -prefix ${dt}

