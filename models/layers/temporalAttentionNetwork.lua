local utils = require 'misc.utils'
local net_utils = require 'misc.net_utils'
local tAtt_single = require 'layers.temporalATT_single'
local tAtt_multi = require 'layers.temporalATT_multi'

-------------------------------------------------------------------------------
-- Temporal Attention Model
-- This is based on skip-thought vector
-------------------------------------------------------------------------------
local layer, parent = torch.class('nn.temporalAttentionNetwork','nn.Module')

function layer:__init(opt)
   parent.__init(self)
 
   -- options for GRU core network
   print('\n----------Temporal Attention initialized as follows:')
   self.backend  = utils.getopt(opt, 'backend', 'nn')
   self.num_step = utils.getopt(opt, 'num_step', 1)
   self.emb_dim  = utils.getopt(opt, 'emb_dim', 512)
   self.height   = utils.getopt(opt, 'height', 7)
   self.width    = utils.getopt(opt, 'width', 10)
   self.debug    = utils.getopt(opt, 'debug', false)
 
   -- create the temporal attention network
   self.spat_att_net = nn.Sequential()
   self.spat_att_net:add( nn.Transpose({1,2}, {2,3}, {3,4}) )
   self.spat_att_net:add( nn.View(-1, self.emb_dim):setNumInputDims(3) )
   self.spat_att_net:add( nn.Mean(2) )
 
   self.core_t_att = {}
   self.core_t_att[1] = tAtt_single.create(self.emb_dim, self.height, 
			self.width, self.backend, self.debug)
 
   if self.num_step > 1 then
      for t=2,self.num_step do
         print(string.format('==> Create %d-step temporal attention', t))
         self.core_t_att[t] = tAtt_multi.create(self.emb_dim, self.height, 
					self.width, self.backend, self.debug)
         collectgarbage()
      end
   end
 
   print(string.format('Temporal Attention backend    : %s ',self.backend))
   print(string.format('Temporal Attention Num Steps  : %d ',self.num_step))
   print(string.format('Temporal Attention Emb Dim    : %d ',self.emb_dim))
   print(string.format('Temporal Attention Height     : %d ',self.height))
   print(string.format('Temporal Attention Width      : %d ',self.width))
   print(string.format('Temporal Attention Debug?     : %s ',self.debug))
   print('')
end

function layer:getModulesList()
   return {self.core_t_att}
end

function layer:parameters()
   local params = {}
   local grad_params = {}
 
   -- we only have two internal modules, return their params
   for t=1,self.num_step do 
      local p,g = self.core_t_att[t]:parameters()
      for k,v in pairs(p) do table.insert(params, v) end
      for k,v in pairs(g) do table.insert(grad_params, v) end
   end
 
   return params, grad_params
end

function layer:getWeight()
   for idxNode, node in ipairs(self.core_t_att[1].forwardnodes) do
      if node.data.annotations.name == 'emb_qst' then
         return node.data.module.weight
      end
   end
end

function layer:training()
	for k,v in pairs(self.core_t_att) do v:training() end
end

function layer:evaluate()
	for k,v in pairs(self.core_t_att) do v:evaluate() end
end

--[[
input is a tuple of:
1. Clip feature
   torch.LongTensor of size AxFxHxW
   where A = feature dimension, F = # frames, H = height, W = weight
2. Question feature
   torch.LongTensor of size 1xA 
--]]
function layer:updateOutput(input)

   local clip_feat = input[1]
   local qst_feat = input[2]

   self.output:resize(1, self.emb_dim):zero()
   self.ctx = {}
   self.alpha = {}
   self.repeated_qst_feat = {}
   self.repeated_ctx = {}
   self.cf_size = clip_feat:size()

   -- forwarding sinlge-step temporal attention network
   self.spat_att_feat = self.spat_att_net:forward(clip_feat)
   self.repeated_qst_feat[1] = qst_feat:repeatTensor(self.cf_size[2], 1)
   self.ctx[1], self.alpha[1] = unpack( self.core_t_att[1]:forward({
			self.spat_att_feat, self.repeated_qst_feat[1]}) )

   -- forwarding multi-step temporal attention network
   if self.num_step > 1 then
      for t=2,self.num_step do
         self.repeated_qst_feat[t] = qst_feat:repeatTensor(self.cf_size[2], 1)
         self.repeated_ctx[t-1] = self.ctx[t-1]:repeatTensor(self.cf_size[2], 1)
         self.ctx[t], self.alpha[t] = unpack( self.core_t_att[t]:forward(
					{self.spat_att_feat, self.repeated_qst_feat[t], self.repeated_ctx[t-1]}) )
      end
   end

   self.output = self.ctx[self.num_step]

   return self.output
end

--[[
gradOutput is an (batch_size, rnn_size) Tensor.
--]]
function layer:updateGradInput(input, gradOutput)

   local clip_feat = input[1]
   local qst_feat = input[2]

   -- initialize gradient input
   self.gradInput = {}
   self.gradInput[1] = clip_feat:clone():zero()
   self.gradInput[2] = qst_feat:clone():zero()

   self.dctx = {}
   self.dspat_att_feat = {}
   self.dqst_feat = {}
   self.drepeated_ctx = {}
   self.dspat_att_feat_aug = self.spat_att_feat:clone():zero()

   -- backward for multiple steps after 1-step
   self.dalpha = self.alpha[1]:clone():zero()
   self.dctx[self.num_step] = gradOutput
   if self.num_step > 1 then
      for t=self.num_step,2,-1 do
         self.dspat_att_feat[t-1], self.dqst_feat[t-1], self.dctx[t-1] = 
					unpack( self.core_t_att[t]:backward(
							{self.spat_att_feat, self.repeated_qst_feat[t], self.repeated_ctx[t-1]}, 
							{self.dctx[t], self.dalpha}) )

         -- averaging gradient for repeated ctx and qst_feat
         self.dctx[t-1] = self.dctx[t-1]:mean(1)
         self.dqst_feat[t-1] = self.dqst_feat[t-1]:mean(1)
      end
   end

   -- backward for first step
   self.dspat_att_feat[0], self.dqst_feat[0] = unpack(
			self.core_t_att[1]:backward(
					{self.spat_att_feat, self.repeated_qst_feat[1]}, 
					{self.dctx[1], self.dalpha}))
   self.dqst_feat[0] = self.dqst_feat[0]:mean(1)

   for t=0,self.num_step-1 do
      self.dspat_att_feat_aug:add(self.dspat_att_feat[t])
      self.gradInput[2]:add(self.dqst_feat[t])
   end

   self.gradInput[1] = self.spat_att_net:backward(clip_feat, self.dspat_att_feat_aug)

   return self.gradInput
end
