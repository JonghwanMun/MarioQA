local npy4th = require 'npy4th'

cmd = torch.CmdLine()
cmd:text()
cmd:text('save params for uni-GRU, bi-GRU, word embedding in torch file')
cmd:text()
cmd:option('-porting_data_dir', './data/pretrained_models/skipthought/', 'data dir containing numpy files for skipthought params')
cmd:option('-save_dir', './data/pretrained_models/skipthought/', 'directory to save torch files')
cmd:option('-utable_path', 'videoqa_utable.npy', 'file path to uni-skipthought word embedding layer (numpy version)')
cmd:option('-uni_word2vec_path', 'videoqa_uni_gru_word2vec.t7', 'file path to save uni-skipthought word embedding layer (torch version)')

-- parse input params
opt = cmd:parse(arg or {})
print(opt)

print('')
print('read numpy data')
print('')
local porting_data_path = opt.porting_data_dir
print('reading uni-word embedding table from ', porting_data_path .. opt.utable_path)
local videoqa_utable = npy4th.loadnpy(porting_data_path .. opt.utable_path)
print(videoqa_utable:size())
print('done')
print('')

-- create saving directory
print(string.format('create save_dir: %s', opt.save_dir))
os.execute(string.format('mkdir -p %s', opt.save_dir))

-- save uparams
print('saving uni-GRU word embedding tables for videoqa')
torch.save(string.format('%s%s', opt.save_dir, opt.uni_word2vec_path), videoqa_utable)
print('done')
print('')
