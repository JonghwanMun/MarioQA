require 'image'
require 'math'
local utils = require 'misc.utils'
local net_utils = {}

function net_utils.compute_norm_param_grad(layer_params, layer_grad)
  local norm_grad = 0
  local norm_param = 0
  for k, v in pairs(layer_params) do
  	local norm_loc = v:norm()
  	norm_param = norm_param + norm_loc * norm_loc
  end
  for k, v in pairs(layer_grad) do
  	local norm_loc = v:norm()
  	norm_grad = norm_grad + norm_loc * norm_loc
  end
  return torch.sqrt(norm_param), torch.sqrt(norm_grad)
end

-- initialization of linear layer 
function net_utils.w_init_xavier_caffe(net, dim_in, dim_out)
  local std = math.sqrt( 1/dim_in )
  net.weight:normal(0, std)
  net.bias:zero()
end

function net_utils.w_init_xavier(net, dim_in, dim_out)
  local std = math.sqrt( 2/(dim_in+dim_out) )
  net.weight:normal(0, std)
  net.bias:zero()
end

function net_utils.w_init_kaiming(net, dim_in, dim_out)
  local std = math.sqrt( 4/(dim_in+dim_out) )
  net.weight:normal(0, std)
  net.bias:zero()
end

function net_utils.smallC3D(opt)
  local backend = utils.getopt(opt, 'backend', 'cudnn')

  if backend == 'cudnn' then
    require 'cudnn'
    backend = cudnn
  else
    backend = nn
  end

  -- construction of C3D network
  local c3d_net = nn.Sequential()
  -- conv 1
  c3d_net:add(backend.VolumetricConvolution(3,64,3,3,3,1,1,1,1,1,1))    -- 01
  c3d_net:add(backend.ReLU())                                           -- 02
  c3d_net:add(backend.VolumetricMaxPooling(1,2,2,1,2,2))                -- 03

  -- conv 2
  c3d_net:add(backend.VolumetricConvolution(64,128,3,3,3,1,1,1,1,1,1))  -- 04
  c3d_net:add(backend.ReLU())                                           -- 05
  c3d_net:add(backend.VolumetricMaxPooling(1,2,2,1,2,2))                -- 06

  -- conv 3
  c3d_net:add(backend.VolumetricConvolution(128,256,3,3,3,1,1,1,1,1,1)) -- 07
  c3d_net:add(backend.ReLU())                                           -- 08
  c3d_net:add(backend.VolumetricMaxPooling(2,2,2,2,2,2))                -- 09

  -- conv 4
  c3d_net:add(backend.VolumetricConvolution(256,512,3,3,3,1,1,1,1,1,1)) -- 10
  c3d_net:add(backend.ReLU())                                           -- 11
  c3d_net:add(backend.VolumetricMaxPooling(2,2,2,2,2,2))                -- 12

  c3d_net:add(backend.VolumetricConvolution(512,512,3,3,3,1,1,1,1,1,1)) -- 13
  c3d_net:add(backend.ReLU())                                           -- 14

  return c3d_net
end

---------------------------------------------------------------------------------------------
-- layer that expands features out so we can forward multiple sentences per image
---------------------------------------------------------------------------------------------
local layer, parent = torch.class('nn.FeatExpander', 'nn.Module')
function layer:__init(n)  -- n : #seq per img, usually 5
  parent.__init(self)
  self.n = n
end
function layer:updateOutput(input)
  if self.n == 1 then self.output = input; return self.output end -- act as a noop for efficiency
  -- simply expands out the features. Performs a copy information
  assert(input:nDimension() == 2 or input:nDimension() == 3 or input:nDimension() == 4)
  local d2 = input:size(2)
  if input:nDimension() == 2 then 
    self.output:resize(input:size(1)*self.n, d2)
    for k=1,input:size(1) do
      local j = (k-1)*self.n+1
      self.output[{ {j,j+self.n-1} }] = input[{ {k,k}, {} }]:expand(self.n, d2) -- copy over
    end
  elseif input:nDimension() == 3 then
    local d3 = input:size(3)
    self.output:resize(input:size(1)*self.n, d2, d3)
    for k=1,input:size(1) do
      local j = (k-1)*self.n+1
      self.output[{ {j,j+self.n-1} }] = input[{ {k,k},{},{} }]:expand(self.n, d2, d3) -- copy over
    end
  else
    local d3, d4 = input:size(3), input:size(4)
    self.output:resize(input:size(1)*self.n, d2, d3, d4)
    for k=1,input:size(1) do
      local j = (k-1)*self.n+1
      self.output[{ {j,j+self.n-1} }] = input[{ {k,k},{},{},{} }]:expand(self.n, d2, d3, d4) -- copy over
    end

  end
  return self.output
end

function layer:updateGradInput(input, gradOutput)
  if self.n == 1 then self.gradInput = gradOutput; return self.gradInput end -- act as noop for efficiency
  -- add up the gradients for each block of expanded features
  self.gradInput:resizeAs(input)
  for k=1,input:size(1) do
    local j = (k-1)*self.n+1
    self.gradInput[k] = torch.sum(gradOutput[{ {j,j+self.n-1} }], 1)
  end
  return self.gradInput
end
---------------------------------------------------------------------------------------------

function net_utils.list_nngraph_modules(g)
  local omg = {}
  for i,node in ipairs(g.forwardnodes) do
      local m = node.data.module
      if m then
        table.insert(omg, m)
      end
   end
   return omg
end
function net_utils.listModules(net)
  -- torch, our relationship is a complicated love/hate thing. And right here it's the latter
  local t = torch.type(net)
  local moduleList
  if t == 'nn.gModule' then
    moduleList = net_utils.list_nngraph_modules(net)
  else
    moduleList = net:listModules()
  end
  return moduleList
end

function net_utils.sanitize_gradients(net)
  local moduleList = net_utils.listModules(net)
  for k,m in ipairs(moduleList) do
    if m.weight and m.gradWeight then
      --print('sanitizing gradWeight in of size ' .. m.gradWeight:nElement())
      --print(m.weight:size())
      m.gradWeight = nil
    end
    if m.bias and m.gradBias then
      --print('sanitizing gradWeight in of size ' .. m.gradBias:nElement())
      --print(m.bias:size())
      m.gradBias = nil
    end
  end
end

function net_utils.unsanitize_gradients(net)
  local moduleList = net_utils.listModules(net)
  for k,m in ipairs(moduleList) do
    if m.weight and (not m.gradWeight) then
      m.gradWeight = m.weight:clone():zero()
      --print('unsanitized gradWeight in of size ' .. m.gradWeight:nElement())
      --print(m.weight:size())
    end
    if m.bias and (not m.gradBias) then
      m.gradBias = m.bias:clone():zero()
      --print('unsanitized gradWeight in of size ' .. m.gradBias:nElement())
      --print(m.bias:size())
    end
  end
end

--[[
take a LongTensor of size DxN with elements 1..vocab_size+1 
(where last dimension is END token), and decode it into table of raw text sentences.
each column is a sequence. ix_to_word gives the mapping to strings, as a table
--]]
function net_utils.decode_sequence(ix_to_word, seq)
  if seq:dim() == 1 then seq:resize(seq:size(1), 1) end
  local D,N = seq:size(1), seq:size(2)
  local out = {}
  local out_len = torch.LongTensor(N)
  for i=1,N do
    local sent_finish = false
    local txt = ''
    for j=1,D do
      local ix = seq[{j,i}]
      local word = ix_to_word[tostring(ix)]
      if not word or word == 0 then 
        out_len[i] = j
        sent_finish = true
        break 
      end -- END token, likely. Or null token
      if j >= 2 then txt = txt .. ' ' end
      txt = txt .. word
    end
    table.insert(out, txt)
    if sent_finish == false then out_len[i] = D end
  end
  return out, out_len
end

function net_utils.decode_question(ix_to_word, qst, q_len)
   str_qst = ''

   for iq=1,q_len do
      if qst[1][iq] ~= 0 then
         if iq == 1 then
            str_qst = ix_to_word[tostring(qst[1][iq])]
         else 
            str_qst = str_qst .. ' ' .. ix_to_word[tostring(qst[1][iq])]
         end
      end
   end
   return str_qst
end

function net_utils.decode_answer(ix_to_ans, ans)
	if type(ans) ~= 'number' then
		ans = ans[1]:nonzero():squeeze()
	end
   return ix_to_ans[ tostring(ans-1) ]
end

function net_utils.clone_list(lst)
  -- takes list of tensors, clone all
  local new = {}
  for k,v in pairs(lst) do
    new[k] = v:clone()
  end
  return new
end

return net_utils
