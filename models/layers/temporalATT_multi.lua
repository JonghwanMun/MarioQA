require 'nn'
require 'nngraph'
require 'layers.Linear_wo_bias'

local temporalATT_multi = {}

-- We assume batch size 1
function temporalATT_multi.create(emb_dim, height, width, backend, debug, dropout)
   dropout = dropout or 0.5

   print('\n----------------Temporal Attention parameter------------------------')
   print(string.format('emb_size    : %d',emb_dim))
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

   -- there will be 3 inputs - img_feat(ann), qst_feat(key), attended_feat in previous layer
   -- This layer works with inputs of batch size 1
   -- Also, This layer is after second layer of multi-step temporal attention network
   local inputs = {}
   table.insert(inputs, nn.Identity()())
   table.insert(inputs, nn.Identity()())
   table.insert(inputs, nn.Identity()())

   -- load input values: clip and question
   local anns     = inputs[1]  -- (ann, frames, height, width)
   local qst      = inputs[2]  -- (frames, ann), this will be key for attention
   local prev_att = inputs[3]  -- (frames, ann), this will be key for attention

   -- Compute newly key for attention weight
   local key = nn.CAddTable()({qst, prev_att})

   -- embedding img features
   local emb_anns = nn.Linear_wo_bias(emb_dim, emb_dim)(anns)	-- (frames, ann)

   -- embedding key features
   local emb_key = nn.Linear(emb_dim, emb_dim)(key)             -- (frames, ann)

   -- combining img and key features
   local emb = nn.Tanh()( nn.CAddTable()({emb_anns, emb_key}) ) -- (frames, ann)

   -- computing attention weight (alpha)
   local alphas = nn.SoftMax()(nn.View(1,-1)(nn.Linear(emb_dim,1)(emb))) -- (frames, 1) -> (1, frames)

   -- computing context vector
   local ctx  = nn.MM(false,false)({alphas,anns})   			-- (1, ann)
 
   if dropout > 0 then
      print('\n-------Multi-step Temporal Attention Using Dropout--------')
      ctx = nn.Dropout(dropout)(ctx)
   end

   if debug then
      print('Multi-step Temporal attention model is debug mode')
      table.insert(outputs,anns); table.insert(outputs,transposed_anns); 
	  table.insert(outputs,flat_anns); table.insert(outputs,max_anns)
      table.insert(outputs,emb_anns)
      table.insert(outputs,emb_key)
      table.insert(outputs,emb)
      table.insert(outputs,alphas); table.insert(outputs,ctx)
   else
      print('Multi-step Temporal attention model is not debug mode')
      table.insert(outputs, ctx)
      table.insert(outputs, alphas)
   end

   return nn.gModule(inputs, outputs)
end

return temporalATT_multi
