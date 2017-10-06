local npy4th = require 'npy4th'

cmd = torch.CmdLine()
cmd:text()
cmd:text('save params for uni-GRU, bi-GRU, word embedding in torch file')
cmd:text()
cmd:option('-porting_data_dir', './data/pretrained_models/skipthought/', 'data dir containing numpy files for skipthought params')
cmd:option('-save_dir', './data/pretrained_models/skipthought/', 'directory to save torch files')
cmd:option('-uni_gru_params_path', 'uni_gru_params.t7', 'path to save torch file of uni gru')
cmd:option('-bi_gru_params_path', 'bi_gru_params.t7', 'path to save torch file of bi gru')
cmd:option('-bi_gru_r_params_path', 'bi_gru_r_params.t7', 'path to save torch file of bi_r gru')
cmd:option('-prefix', '', 'prefix for data type')

-- parse input params
opt = cmd:parse(arg or {})
								--
print('')
print('read numpy data')
print('')

local porting_data_path = opt.porting_data_dir
print('reading uni-GRU params ..')
local uparams = {}
uparams.U = npy4th.loadnpy(porting_data_path .. string.format('uparams_encoder_U_%s.npy', opt.prefix))
uparams.Ux = npy4th.loadnpy(porting_data_path .. string.format('uparams_encoder_Ux_%s.npy', opt.prefix))
uparams.W = npy4th.loadnpy(porting_data_path .. string.format('uparams_encoder_W_%s.npy', opt.prefix))
uparams.b = npy4th.loadnpy(porting_data_path .. string.format('uparams_encoder_b_%s.npy', opt.prefix))
uparams.Wx = npy4th.loadnpy(porting_data_path .. string.format('uparams_encoder_Wx_%s.npy', opt.prefix))
uparams.bx = npy4th.loadnpy(porting_data_path .. string.format('uparams_encoder_bx_%s.npy', opt.prefix))
print('done')
print('')

-- save uparams
print('saving uni-GRU params .. ')
torch.save(string.format('%s%s', opt.save_dir, opt.uni_gru_params_path), uparams)
print('done')
print('')
