require 'nn'
require 'nngraph'
require 'layers.Linear_wo_bias'

local temporalATT_single = {}

-- We assume batch size 1
function temporalATT_single.create(ann_dim, height, width, backend, debug, dropout)
   dropout = dropout or 0.5

   print('\n----------------Temporal Attention parameter------------------------')
   print(string.format('ann_size    : %d',ann_dim))
   print(string.format('height      : %d',height))
   print(string.format('width       : %d',width))
   print(string.format('width       : %s',backend))
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
   -- This layer works with inputs of batch size 1
   -- Also, This layer is first layer of multi-step temporal attention network
   local inputs = {}
   table.insert(inputs, nn.Identity()())
   table.insert(inputs, nn.Identity()())

   -- load input values: clip and question
   local anns = inputs[1]      -- (frames, ann) (ann, frames, height, width)
   local qst  = inputs[2]      -- (frames, ann) this is key for attention

   -- embedding img features
   local emb_anns  = nn.Linear_wo_bias(ann_dim, ann_dim)( nn.View(ann_dim)(anns) ) -- (frames, ann)

   -- embedding qst features
   local emb_qst = nn.Linear(ann_dim, ann_dim)(qst):annotate{name='emb_qst'}       -- (frames, ann)

   -- combining img and qst features
   local emb = nn.Tanh()( nn.CAddTable()({emb_anns, emb_qst}) )                    -- (frames, ann)

   -- computing attention weight (alpha)
   local alphas = nn.SoftMax()(nn.View(1,-1)(nn.Linear(ann_dim,1)(emb)))		   -- (1,frames)

   -- computing context vector
   local ctx = nn.MM(false,false)({alphas,anns})   			                       -- (1, ann)

   if dropout > 0 then
      print('\n-------1-step Temporal Attention Using Dropout-------')
      ctx = nn.Dropout(dropout)(ctx)
   end

   if debug then
      print('1-step Temporal attention model is debug mode')
      table.insert(outputs,anns); table.insert(outputs,transposed_anns); 
	  table.insert(outputs,flat_anns); table.insert(outputs,anns)
      table.insert(outputs,emb_anns)
      table.insert(outputs,emb_qst)
      table.insert(outputs,emb)
      table.insert(outputs,alphas); table.insert(outputs,ctx)
   else
      print('1-step Temporal attention model is not debug mode')
      table.insert(outputs, ctx)
      table.insert(outputs, alphas)
   end

   return nn.gModule(inputs, outputs)
end

return temporalATT_single
