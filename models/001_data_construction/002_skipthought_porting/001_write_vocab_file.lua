local utils = require 'misc.utils'

cmd = torch.CmdLine()
cmd:text()
cmd:text('create coco vocab.txt file')
cmd:text()
cmd:option('-vocab_imginfo_file','data/clip_info.json','path to vocab & img info file')
cmd:option('-saveto','./videoqa_vocab.txt','path to save result vocabulary file')

-- parse input params
opt = cmd:parse(arg or {})

print('load coco vocab and img info file')
local data = utils.read_json(opt.vocab_imginfo_file)
local ix_to_word = data.ix_to_word

print('save vocab file to', opt.saveto) 
local f  = io.open(opt.saveto, 'wt')
for k,v in pairs(ix_to_word) do
	f:write(v .. '\n')
end
f:close()
print('done')
