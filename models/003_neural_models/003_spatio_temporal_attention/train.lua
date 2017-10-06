require 'torch'
require 'nn'
require 'nngraph'
require 'math'

-- exotic things
require 'image'

-- local imports
local ST = require 'layers.spatioTemporalAttention'
require 'layers.sentenceEncoder'
require 'misc.DataLoader'
local utils = require 'misc.utils'
local net_utils = require 'misc.net_utils'
require 'misc.optim_updates'

-------------------------------------------------------------------------------
-- Input arguments and options
-------------------------------------------------------------------------------
cmd = torch.CmdLine()
cmd:text()
cmd:text('Train a VideoQA Spatio-Temporal Attention Model')
cmd:text()
cmd:text('Options')

------------------------ Data input settings ------------------------
-- Loader input
cmd:option('-clip_info_file','data/clip_info.json','path to clip information file')
cmd:option('-qa_label_file','data/qa_labels.h5','path to QA labels')

-- Sentence embedding input
cmd:option('-uni_gru_path','data/pretrained_models/skipthought/uni_gru_params.t7','path to skip-thoughts vector GRU model')
cmd:option('-uni_gru_word2vec_path','data/pretrained_models/skipthought/videoqa_uni_gru_word2vec.t7','path to skip-thoughts vector word embedding model')

-- Model Finetuning options
cmd:option('-start_from', '', 'path to a model checkpoint to initialize model weights from. Empty = don\'t')
cmd:option('-ft_continue', 1,'whether maintain the epoch and iteration of checkpoint or not')

-- Model parameter or input dimension settings
cmd:option('-question_feat_dim',2400,'dimension of the skipthought feature from question')
cmd:option('-clip_feat_dim',512,'dimension of the cnn feature from clip')
cmd:option('-drop_prob', 0.5, 'strength of dropout')

-- Optimization: General
cmd:option('-max_epoch', 100, 'max number of iterations to run for (-1 = run forever)')
cmd:option('-batch_size',1,'what is the batch size in number of clips per batch? (there will be x seq_per_img sentences)')
cmd:option('-grad_clip',0.1,'clip gradients at this value')
cmd:option('-optim_alpha',0.9,'alpha for adam')
cmd:option('-optim_beta',0.999,'beta used for adam')
cmd:option('-optim_epsilon',1e-8,'epsilon that goes into denominator for smoothing')

-- Optimization: for the learning rate
cmd:option('-learning_rate',1e-4,'learning rate')
cmd:option('-learning_rate_decay_start', 1, 'at what iteration to start decaying learning rate? (-1 = dont)')
cmd:option('-learning_rate_decay_every', 3, 'every how many iterations thereafter to drop LR?')
cmd:option('-lr_decay_rate', 0.8, 'LR decaying rate')

-- Optimization: for the C3D
cmd:option('-cnn_weight_decay', 5e-4, 'L2 normalization')

-- Evaluation/Checkpointing
cmd:option('-val_clips_use', -1, 'how many clips to use when periodically evaluating the validation loss? (-1 = all)')
cmd:option('-checkpoint_path', './model/', 'folder to save checkpoints into (empty = this folder)')
cmd:option('-losses_log_every', 10, 'How often do we snapshot losses, for inclusion in the progress dump? (0 = disable)')

-- misc
cmd:option('-img_root', 'data/mario_resized_frames/', 'path to clips which are composed of corresponding frames')
cmd:option('-backend', 'nn', 'nn|cudnn')
cmd:option('-debug', false, 'whether use debug mode?')
cmd:option('-id', '', 'an id identifying this run/job. used in cross-val and appended when writing progress files')
cmd:option('-seed', 123, 'random number generator seed to use')
cmd:option('-gpuid', 0, 'which gpu to use. -1 = use CPU')

cmd:text()

-------------------------------------------------------------------------------
-- Basic Torch initializations
-------------------------------------------------------------------------------
local opt = cmd:parse(arg)
torch.manualSeed(opt.seed)
torch.setdefaulttensortype('torch.FloatTensor') -- for CPU
print('options are as follows :')
print(opt)

if opt.gpuid >= 0 then
   require 'cutorch'
   require 'cunn'
   if opt.backend == 'cudnn' then require 'cudnn' end
   cutorch.manualSeed(opt.seed)
   cutorch.setDevice(opt.gpuid + 1) -- note +1 because lua is 1-indexed
end

-------------------------------------------------------------------------------
-- Create the Data Loader instance
-------------------------------------------------------------------------------
local loader = DataLoader{clip_info_file=opt.clip_info_file, qa_label_file=opt.qa_label_file,
                           fix_num_frame=false, data_path=opt.img_root}

-------------------------------------------------------------------------------
-- Initialize the networks
-------------------------------------------------------------------------------
local net = {}
local kk = 10
local iter = 1
local epoch = 1
local train_acc = 0
local timer = torch.Timer()

if string.len(opt.start_from) > 0 then  -- finetuning the model
   -- load net from file
   print('initializing weights from ' .. opt.start_from)
   local loaded_checkpoint = torch.load(opt.start_from)

   -- continue learning from previous model's iteration and epoch
   if opt.ft_continue > 0 then
      print('Fintuning continue from before model, meaning epoch and iteration is maintained')
      epoch = loaded_checkpoint.epoch +1
      local every_epoch = math.ceil(loader:getNumTrainData() / opt.batch_size)
      if opt.batch_size ~= loaded_checkpoint.opt.batch_size then
         iter = math.ceil(loader:getNumTrainData() / opt.batch_size) * (epoch-1) + 1
      else
         iter = loaded_checkpoint.iter + 1
      end
   end

   net = loaded_checkpoint.net
   net.crit = nn.CrossEntropyCriterion() -- not in checkpoints, create manually

   ----------------------------------------------------------------------------
   -- Unsanitize gradient for each model
   ----------------------------------------------------------------------------

   -- load question encoder
   local qe_modules = net.question_encoder:get(1):getModulesList()
   for k,v in pairs(qe_modules) do net_utils.unsanitize_gradients(v) end
   net_utils.unsanitize_gradients(net.question_encoder:get(2))

   -- load C3D image encoder
   net_utils.unsanitize_gradients(net.c3d_net)

   -- load attention models
   net_utils.unsanitize_gradients(net.att)

   -- load classification and criterion layer
   net_utils.unsanitize_gradients(net.classify)

   ----------------------------- end if ----------------------------------
else -- create net from scratch

   -- attatch question encoder
   print('Question encoder is initialized from skip-thought vector model')
   local uparams = torch.load(opt.uni_gru_path)
   local utables = torch.load(opt.uni_gru_word2vec_path)
   local qeOpt = {}
   qeOpt.backend = 'nn'   -- cudnn may not work
   qeOpt.vocab_size = loader:getVocabSize()
   qeOpt.seq_length = loader:getQstLength()
   print('Option of Question encoder is as follows :')
   print(qeOpt)

   net.question_encoder = nn.Sequential()
   net.question_encoder:add( nn.sentenceEncoder(uparams, utables, qeOpt) )
   net.question_encoder:add( nn.Linear(opt.question_feat_dim, opt.clip_feat_dim) )
   net.question_encoder:add( nn.ReLU() )

   -- attatch C3D image encoder
   net.c3d_net= net_utils.smallC3D({backend=opt.backend})

   -- attatch attention models
   net.att = ST.create(opt.clip_feat_dim, 7, 10, 'nn', false)

   -- attatch classification layer
   net.classify = nn.Sequential()
   net.classify:add( nn.CMulTable() )
   net.classify:add( nn.Linear(opt.clip_feat_dim, loader:getNumAnswer()) )

   -- attatch criterion layer
   net.crit = nn.CrossEntropyCriterion()
end
--------------------------------------------------------------------------------------------------
-- Should keep following order
-- 1. ship to GPU       -> memory reallocation
-- 2. getParameters()   -> memory reallocation, so should be done before next two steps.
-- 3. sanitizing network to save with lower memory
-- 4. create clones

-- ship everything to GPU, maybe
if opt.gpuid >= 0 then
   for k,v in pairs(net) do v:cuda() end
end

-- flatten and prepare all model parameters to a single vector.
local qe_params, grad_qe_params = net.question_encoder:getParameters()
local att_params, grad_att_params = net.att:getParameters()
local c3d_params, grad_c3d_params = net.c3d_net:getParameters()
local cls_params, grad_cls_params = net.classify:getParameters()

print('\n============================================================')
print('total number of parameters in QE     : ', qe_params:nElement())
print('total number of parameters in C3D    : ', c3d_params:nElement())
print('total number of parameters in ATT    : ', att_params:nElement())
print('total number of parameters in CLS    : ', cls_params:nElement())

--------------------------------------------------------------------------------------------------
-- construct thin module clones that share parameters with the actual
-- modules. These thin module will have no intermediates and will be used
-- for checkpointing to write significantly smaller checkpoint files

-- sanitize question embedding layer
local thin_qe = nn.Sequential()
local thin_se = net.question_encoder:get(1):clone()
thin_se.core:share(net.question_encoder:get(1).core, 'weight', 'bias')
thin_se.lookup_table:share(net.question_encoder:get(1).lookup_table, 'weight', 'bias')
local se_modules = thin_se:getModulesList()
for k,v in pairs(se_modules) do net_utils.sanitize_gradients(v) end
local thin_linear = net.question_encoder:get(2):clone('weight','bias')
net_utils.sanitize_gradients(thin_linear)

thin_qe:add( thin_se )
thin_qe:add( thin_linear)
thin_qe:add( nn.ReLU() )

-- sanitize C3D image encoder
local thin_c3d = net.c3d_net:clone('weight', 'bias')
net_utils.sanitize_gradients(thin_c3d)

-- sanitize attention layer
local thin_att = net.att:clone('weight', 'bias')
net_utils.sanitize_gradients(thin_att)

-- sanitize classifying layer
local thin_cls = net.classify:clone('weight', 'bias')
net_utils.sanitize_gradients(thin_cls)

--------------------------------------------------------------------------------------------------
-- create clones and ensure parameter sharing. we have to do this
-- all the way here at the end because calls such as :cuda() and
-- :getParameters() reshuffle memory around.
net.question_encoder:get(1):createClones()

--------------------------------------------------------------------------------------------------
collectgarbage()

-------------------------------------------------------------------------------
-- Validation evaluation
-------------------------------------------------------------------------------
local function eval_split(split, evalopt)
   local verbose = utils.getopt(evalopt, 'verbose', true)
   local val_clips_use = utils.getopt(evalopt, 'val_clips_use', -1)
   net.question_encoder:evaluate()
   net.c3d_net:evaluate()
   net.att:evaluate()
   net.classify:evaluate()
   
   loader:resetIterator(split) -- rewind iteator back to first datapoint in the split
   local n = 0
   local loss_sum = 0
   local loss_evals = 0
   local test_acc = 0
   local prediction = {}
   
   print('\n--------------------- Evaluation for test split -----------------------')
   while true do
   
      -----------------------------------------------------------------------------
      -- Load minibatch data 
      -----------------------------------------------------------------------------
      local data = loader:getBatch{batch_size = 1, split = split}
      n = n + data.answers:size(1)
   
      -----------------------------------------------------------------------------
      -- Forward network
      -----------------------------------------------------------------------------
      local qst_feat = net.question_encoder:forward({data.questions, data.question_length})
      local clip_feat = net.c3d_net:forward(data.clips[1])
      local cf_size = clip_feat:size()
      local att_feat, alphas = unpack( net.att:forward({clip_feat, qst_feat:repeatTensor(cf_size[2]*cf_size[3]*cf_size[4], 1)}) )
      local pred = net.classify:forward({att_feat,qst_feat})
      local loss = net.crit:forward(pred, data.answers[{ {1} }])
   
      loss_sum = loss_sum + loss
      loss_evals = loss_evals + 1
      local max_score, ans = torch.max(pred, 2)
      test_acc = test_acc + torch.eq(ans[1]:cuda(), data.answers[{{1}}]):sum()
   
      -- save the prediction results
      table.insert(prediction, ans[1][1])
   
      -----------------------------------------------------------------------------
      -- if we wrapped around the split or used up val imgs budget then bail
      local ix0 = data.bounds.it_pos_now
      local ix1 = math.min(data.bounds.it_max, val_clips_use)
      if verbose then
         local qst_label = torch.squeeze( data.questions[{ {},1 }] )
         local qst = ''
         for i=1, qst_label:size(1) do
            if qst_label[i] ~= 0 then
               qst = qst .. loader:getVocabQuestion()[ tostring(qst_label[i]) ] .. ' '
            end
         end
         print(string.format('question    : (%s)', qst))
         print(string.format('pred answer : (%s)', loader:getVocabAnswer()[ tostring(ans[1][1]-1)]))
         print(string.format('gt answer   : (%s)', loader:getVocabAnswer()[ tostring(data.answers[1]-1)]))
         print(string.format('evaluating validation performance... %d/%d (%f)', ix0-1, ix1, loss))
      end
   
      if loss_evals % 10 == 0 then collectgarbage() end
      if data.bounds.wrapped then break end -- the split ran out of data, lets break out
      if val_clips_use >= 0 and n >= val_clips_use then break end -- we've used enough images
      print('-----------------------------------------------------------------------------------')
   end
   
   test_acc = test_acc / n
   print(prediction)
   
   return loss_sum / loss_evals, test_acc, prediction
end

-------------------------------------------------------------------------------
-- Loss function
-------------------------------------------------------------------------------
local function lossFun()
   net.question_encoder:training()
   net.c3d_net:training()
   net.att:training()
   net.classify:training()
   grad_qe_params:zero()
   grad_c3d_params:zero()
   grad_att_params:zero()
   grad_cls_params:zero()

   local batch_loss = 0

   -----------------------------------------------------------------------------
   -- Load minibatch data
   -----------------------------------------------------------------------------
	--cutorch.synchronize()
   local st = timer:time().real
   local data = loader:getBatch{batch_size = opt.batch_size, split = 'train'}
   local data_load_time = timer:time().real - st
   local forward_time, backward_time = 0, 0

   -- Here, gradients are accumulated
   for bi=1,opt.batch_size do
      -----------------------------------------------------------------------------
      -- Forward pass
      -----------------------------------------------------------------------------
      local sft = timer:time().real
      local qst_feat  = net.question_encoder:forward({data.questions[{ {},{bi} }], data.question_length[{{bi}}]})
      local clip_feat = net.c3d_net:forward(data.clips[bi])
      local cf_size   = clip_feat:size()
      local repeated_qst_feat = qst_feat:repeatTensor(cf_size[2]*cf_size[3]*cf_size[4], 1)
      local att_feat, alphas = unpack( net.att:forward({clip_feat, repeated_qst_feat}) )
      local pred      = net.classify:forward({att_feat,qst_feat})
      local loss      = net.crit:forward(pred, data.answers[{{bi}}])

		----------------------------------------------------------------------------
      batch_loss      = batch_loss + loss
      forward_time    = forward_time + timer:time().real - sft

      local max_score, pred_ans = torch.max(pred, 2)
      train_acc = train_acc + torch.eq(pred_ans[1]:cuda(), data.answers[{{bi}}]):sum()

      -- print current example per 50 iterations
      if iter%50 == 0 then
         local qst_label = torch.squeeze( data.questions[{ {},bi }] )
         local qst = ''
         for i=1, qst_label:size(1) do
            if qst_label[i] ~= 0 then
               qst = qst .. loader:getVocabQuestion()[ tostring(qst_label[i]) ] .. ' '
            end
         end
         print(string.format('==>question    : (%s)', qst))
         print(string.format('==>pred answer : (%s)', loader:getVocabAnswer()[ tostring(pred_ans[1][1]-1)]))
         print(string.format('==>gt answer   : (%s)', loader:getVocabAnswer()[ tostring(data.answers[bi]-1)]))
      end

      -----------------------------------------------------------------------------
      -- Backward pass
      -----------------------------------------------------------------------------
      local sbt = timer:time().real

      local dpred     = net.crit:backward(pred, data.answers[{ {bi} }])
      local datt_feat, dqst_feat1  = unpack( net.classify:backward({att_feat,qst_feat}, dpred) )
      local dclip_feat, dqst_feat2 = unpack( net.att:backward({clip_feat,repeated_qst_feat}, {datt_feat, alphas:clone():zero()}) )
      local dclip     = net.c3d_net:backward(data.clips[bi], dclip_feat)
      local dqst      = net.question_encoder:backward({data.questions[{ {},{bi} }], data.question_length[{ {bi} }]}, dqst_feat1+dqst_feat2:view(-1,qst_feat:size(2)):sum(1))

      backward_time   = backward_time + timer:time().real - sbt

      if loss >= 10 then print(string.format('Oops!! Loss is bigger than 10 (%.3f) (%s)',loss,data.infos[bi]['clip_path'])) end
   end

   -- divide gradient by batch size
   grad_cls_params:mul(1/opt.batch_size)
   grad_att_params:mul(1/opt.batch_size)
   grad_c3d_params:mul(1/opt.batch_size)
   grad_qe_params:mul(1/opt.batch_size)

   -- clip gradients
   grad_cls_params:clamp(-opt.grad_clip, opt.grad_clip)
   grad_att_params:clamp(-opt.grad_clip, opt.grad_clip)
   grad_c3d_params:clamp(-opt.grad_clip, opt.grad_clip)
   grad_qe_params:clamp(-opt.grad_clip, opt.grad_clip)

   if opt.cnn_weight_decay > 0 then
      -- apply L2 regularization
      grad_c3d_params:add(opt.cnn_weight_decay, c3d_params)
   end

   -----------------------------------------------------------------------------
   print(string.format('Elapsed time : data_load (%.4fs) | forward (%.4fs) | backward (%.4fs)', data_load_time, forward_time, backward_time))

   -- and lets get out!
   local losses = { total_loss = batch_loss/opt.batch_size}
   return losses
end

-------------------------------------------------------------------------------
-- Main loop
-------------------------------------------------------------------------------
local loss0
local best_score
local qe_optim_state, c3d_optim_state, att_optim_state, cls_optim_state = {}, {}, {}, {}
local loss_history = {}
local predictions_history = {}
local every_epoch = math.ceil(loader:getNumTrainData() / opt.batch_size)

print('The number of iterations per epoch : ', every_epoch)
while true do
   print('\n--------------------------------------------------------------------------------')
   print(string.format('epoch %d iter %d', epoch, iter))

   -- eval loss/gradient
   local losses = lossFun()
   if iter % opt.losses_log_every == 0 then table.insert(loss_history, losses.total_loss) end

   -- print parameter and gradient weights per 10 iterations
   if iter % 10 == 0 then
      local qe_param_norm = qe_params:norm()
      local qe_grad_norm = grad_qe_params:norm()
      local c3d_param_norm = c3d_params:norm()
      local c3d_grad_norm = grad_c3d_params:norm()
      local att_param_norm = att_params:norm()
      local att_grad_norm = grad_att_params:norm()
      local cls_param_norm = cls_params:norm()
      local cls_grad_norm = grad_cls_params:norm()

      print('===============================================================')
      print(string.format('QE param  : %.7f \t|     QE grad : %.7f', qe_param_norm, qe_grad_norm))
      print(string.format('C3D param : %.7f \t\t|     C3D grad : %.7f', c3d_param_norm, c3d_grad_norm))
      print(string.format('ATT param : %.7f \t\t|     ATT grad : %.7f', att_param_norm, att_grad_norm))
      print(string.format('CLS param : %.7f \t\t|     CLS grad : %.7f', cls_param_norm, cls_grad_norm))
      print('===============================================================')

   end

   -----------------------------------------------------------------------------
   -- save checkpoint at every epoch (or on final iteration)
   if  iter % every_epoch == 0 or iter == opt.max_epoch*every_epoch then
      -- evaluate the validation performance
      local val_loss, val_acc, val_prediction = eval_split('val', {val_clips_use = opt.val_clips_use})
      table.insert(predictions_history, val_prediction)
      print('==========================================================')
      print('=====> validation loss: ', val_loss)
      print('=====> validation accuracy: ', val_acc)
      print('=====> train      accuracy: ', train_acc/loader:getNumTrainData())
      print('==========================================================')

      -- write a (thin) json report
      local checkpoint_path = path.join(opt.checkpoint_path, 'model_id' .. tostring(epoch))
      local checkpoint = {}
      checkpoint.opt = opt
      checkpoint.iter = iter
      checkpoint.epoch = epoch
      checkpoint.loss_history = loss_history
      checkpoint.prediction_history = predictions_history

      utils.write_json(checkpoint_path .. '.json', checkpoint)
      print('wrote json checkpoint to ' .. checkpoint_path .. '.json')

      -- Save the current network
      local save_net = {}
      save_net.question_encoder = thin_qe
      save_net.c3d_net          = thin_c3d
      save_net.att				  = thin_att
      save_net.classify         = thin_cls
      checkpoint.net = save_net
      -- also include the vocabulary mapping so that we can use the checkpoint 
      -- alone to run on arbitrary images without the data loader
      checkpoint.ix_to_word = loader:getVocabQuestion()
      checkpoint.ix_to_ans = loader:getVocabAnswer()
      torch.save(checkpoint_path .. '.t7', checkpoint)
      print('wrote checkpoint to ' .. checkpoint_path .. '.t7')

      -- write the full model checkpoint as well if we did better than ever
      local current_score = -val_loss
      if best_score == nil or current_score > best_score then
         best_score = current_score
         if iter > 0 then -- dont save on very first iteration
            torch.save(checkpoint_path .. 'best_score.t7', checkpoint)
            print('wrote best score checkpoint to ' .. checkpoint_path .. 'best_score.t7')
         end
      end
      if iter ~= 0 then epoch = epoch + 1 end

      train_acc = 0
   end

   -----------------------------------------------------------------------------
   -- check model is saved correctly
   if  (iter+1) % 10 == 0 or iter == opt.max_epoch*every_epoch then
      if torch.sum(thin_qe:get(2).weight - net.question_encoder:get(2).weight) ~= 0 then
         print('!!!!! Model is not saved correctly, linear')
      end
      if torch.sum(thin_c3d:get(1).weight - net.c3d_net:get(1).weight) ~= 0 then
         print('!!!!! Model is not saved correctly, c3d')
      end
   end

   -----------------------------------------------------------------------------
   -- decay the learning rate for both LM and CNN
   local learning_rate = opt.learning_rate
   if epoch > opt.learning_rate_decay_start and opt.learning_rate_decay_start >= 0 then
      local frac = (epoch - opt.learning_rate_decay_start) / opt.learning_rate_decay_every
      local decay_factor = math.pow(opt.lr_decay_rate, frac)
      learning_rate = learning_rate * decay_factor -- set the decayed rate
   end
   print(string.format('Loss : %.2f\t | lr : %.5f\t', losses.total_loss, learning_rate))

   -----------------------------------------------------------------------------
   -- perform a parameter update using adam
   adam(cls_params, grad_cls_params, learning_rate, opt.optim_alpha, opt.optim_beta, opt.optim_epsilon, cls_optim_state)
   adam(att_params, grad_att_params, learning_rate, opt.optim_alpha, opt.optim_beta, opt.optim_epsilon, att_optim_state)
   adam(c3d_params, grad_c3d_params, learning_rate, opt.optim_alpha, opt.optim_beta, opt.optim_epsilon, c3d_optim_state)
   adam(qe_params, grad_qe_params, learning_rate, opt.optim_alpha, opt.optim_beta, opt.optim_epsilon, qe_optim_state)

   -----------------------------------------------------------------------------
   -- stopping criterions
   iter = iter + 1
   if iter % 10 == 0 then collectgarbage() end -- good idea to do this once in a while, i think
   if loss0 == nil then loss0 = losses.total_loss end
   if losses.total_loss > loss0 * 20 then
      print('loss seems to be exploding, quitting.')
   end
   if opt.max_epoch> 0 and epoch >= opt.max_epoch then break end -- stopping criterion
end
