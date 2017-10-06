require 'nn'
require 'nngraph'
require 'layers.Linear_wo_bias'

local spatioTemporalAttention = {}

-- We assume batch size 1
function spatioTemporalAttention.create(ann_dim, height, width, backend, debug, dropout)
  dropout = dropout or 0.5

  print('\n----------------Spatio-Temporal Attention parameter------------------------')
  print(string.format('ann_size    : %d',ann_dim))
  print(string.format('height      : %d',height))
  print(string.format('width       : %d',width))
  print(string.format('backend     : %s',backend))
  print(string.format('debug       : %s',debug))
  print(string.format('dropout     : %f',dropout))
  print('')

	if backend == 'cudnn' then
		require 'cudnn'
		backend = cudnn
	else
		backend = nn
	end

  -- there will be 2 outputs - attented_feat, alphas
  local outputs = {}

  -- there will be 2 inputs - img_feat(ann), cap_feat(key)
  local inputs = {}
  table.insert(inputs, nn.Identity()())
  table.insert(inputs, nn.Identity()())

  -- load input values: clip and question
  local anns = inputs[1]      -- (ann, frames, height, width)
  local qst  = inputs[2]      -- (frames*height*width, ann) this is key for attention

	-- flatting in terms of spatial dimensions
  local transposed_anns = nn.Transpose({1,2}, {2,3}, {3,4})(anns)           -- (frames, height, width, ann)
  local st_flat_anns = nn.View(ann_dim)(transposed_anns)							 -- (frames*height*width, ann)

  -- embedding img features
  local emb_anns  = nn.Linear_wo_bias(ann_dim, ann_dim)(st_flat_anns) 	    -- (frames*height*width, ann)
  
  -- embedding qst features
  local emb_qst = nn.Linear(ann_dim, ann_dim)(qst)                          -- (frames*height*width, ann)

  -- combining img and qst features
  local emb = nn.Tanh()( nn.CAddTable()({emb_anns, emb_qst}) )              -- (frames*height*width, ann)

  -- computing attention weight (alpha)
  local alphas = nn.SoftMax()(nn.View(1,-1)(nn.Linear(ann_dim,1)(emb)))		 -- (frames*height*width, 1) -> (1, frames*height*width)

  -- computing context vector
  local ctx = nn.MM(false,false)({alphas,st_flat_anns})   			          -- (1, ann)
  --      ctx = nn.View(-1, ann_dim)(ctx)                                   -- (frames, ann)
  if dropout > 0 then
     print('\n----------------Spatio-Temporal Attention Using Dropout------------------------')
     ctx = nn.Dropout(dropout)(ctx)
  end

  if debug then
		print('Spatio-temporal attention model is debug mode')
		table.insert(outputs,anns); table.insert(outputs,transposed_anns); table.insert(outputs,st_flat_anns)
		table.insert(outputs,emb_anns)
		table.insert(outputs,emb_qst)
		table.insert(outputs,emb)
      table.insert(outputs,alphas); table.insert(outputs,ctx)
  else
    print('Spatio-temporal attention model is not debug mode')
    table.insert(outputs, ctx)
    table.insert(outputs, alphas)
  end

  return nn.gModule(inputs, outputs)
end

return spatioTemporalAttention
