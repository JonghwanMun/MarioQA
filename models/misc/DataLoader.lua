require 'hdf5'
require 'image'
local utils = require 'misc.utils'
local net_utils = require 'misc.net_utils'

local DataLoader = torch.class('DataLoader')

function DataLoader:__init(opt)

	self.vgg_mean = torch.FloatTensor{123.68, 116.779, 103.939}:view(3,1,1)
	self.vgg_mean = self.vgg_mean:repeatTensor(1,120,160)

	print('\n======================== DataLoader Options ===========================')
	-- set variables
	self.fix_num_frame   = utils.getopt(opt, 'fix_num_frame', false)
	self.num_frame       = utils.getopt(opt, 'num_frame', 16)
	self.sampling_stride = utils.getopt(opt, 'sampling_stride', 4)
	self.data_path       = utils.getopt(opt, 'data_path', 'data/mario_resized_frames/')
	self.load_clip       = utils.getopt(opt, 'load_clip', true)
	print(string.format('==> fix number of frames?        %s', self.fix_num_frame))
	print(string.format('==> number of frames per clip:   %d', self.num_frame))
	print(string.format('==> loading clip?                %s', self.load_clip))
	print(string.format('==> data path:                   %s', self.data_path))
	print(string.format('==> stride of sampling frame:    %d', self.sampling_stride))

	-- load the json file which contains information of clips
	print(string.format('==> loading clip info json file: %s', opt.clip_info_file))
	self.clip_info = utils.read_json(opt.clip_info_file)
	self.ix_to_word = self.clip_info.ix_to_word
	self.ix_to_ans  = self.clip_info.ix_to_ans
	self.word_to_ix = self.clip_info.word_to_ix
	self.ans_to_ix = self.clip_info.ans_to_ix
	self.clips = self.clip_info.clips
	self.vocab_size = utils.count_keys(self.ix_to_word)
	print(string.format('==> vocab size is                %d', self.vocab_size))

	-- open the hdf5 file which contains label information of questions and answers
	print(string.format('==> loading qa label h5 file:    %s', opt.qa_label_file))
	self.qa_label_file = hdf5.open(opt.qa_label_file, 'r')
	self.num_answer = self.qa_label_file:read('/answers'):dataspaceSize()[2]
	self.qst_length = self.qa_label_file:read('/questions'):dataspaceSize()[2]
	print(string.format('==> loading num answer:          %d', self.num_answer))
	print(string.format('==> loading max qst length:      %d', self.qst_length))

	-- separate out indexes for each of the provided splits
	self.split_ix = {}
	self.iterators = {}
	for i,clip in pairs(self.clips) do
		local split = clip.split
		if not self.split_ix[split] then
			-- initialize new split
			self.split_ix[split] = {}
			self.iterators[split] = 1
		end
		table.insert(self.split_ix[split], i)
	end
	for k,v in pairs(self.split_ix) do
		print(string.format('==> assigned %d images to split %s', #v, k))
	end

	-- changing from list to torch tensor for shuffling indices every epoch 
	local tmp_train_ix = torch.LongTensor(#self.split_ix['train']):zero()
	for i=1,#self.split_ix['train'] do
		tmp_train_ix[i] = self.split_ix['train'][i]
	end
	self.split_ix['train'] = tmp_train_ix
	print('======================== DataLoader Options ===========================\n')
end

function DataLoader:resetIterator(split)
	self.iterators[split] = 1
end

function DataLoader:getVocabSize()
	return self.vocab_size
end

function DataLoader:getVocabQuestion()
	return self.ix_to_word
end

function DataLoader:setVocabQuestion(ix_to_word)
	self.ix_to_word = ix_to_word
end

function DataLoader:getVocabAnswer()
	return self.ix_to_ans
end

function DataLoader:setVocabAnswer(ix_to_ans)
	self.ix_to_ans = ix_to_ans
end

function DataLoader:getNumAnswer()
	return self.num_answer
end

function DataLoader:getQstLength()
	return self.qst_length
end

function DataLoader:getNumTrainData()
	return self.split_ix['train']:size(1)
end

function DataLoader:shuffleTrainData()
   local num_train = self:getNumTrainData()
   local shuffle_idx = torch.randperm(num_train)
   local shuffle_ix = torch.LongTensor(num_train):zero()

   for i=1,num_train do
      shuffle_ix[i] = self.split_ix['train'][ shuffle_idx[i] ]
   end
   self.split_ix['train'] = shuffle_ix
end

function DataLoader:getMaxIndexOfSplit(split)
	if split == 'train' then 
		return self.split_ix[split]:size(1)
	else
		return  #self.split_ix[split]
	end
end

--[[
  Split is a string identifier (e.g. train|val|test)
  Returns a batch of data:
  - X (N,3,H,W) containing the images
  - y (L,M) containing the captions as columns (which is better for contiguous memory during training)
  - info table of length N, containing additional information
  The data is iterated linearly in order. Iterators for any split can be reset manually with resetIterator()
--]]
function DataLoader:getBatch(opt)
	local split = utils.getopt(opt, 'split') -- lets require that user passes this in, for safety
	local gpu_use = utils.getopt(opt, 'gpu_use', true) -- flag about gpu usage
	local batch_size = utils.getopt(opt, 'batch_size', 5) -- how many images get returned at one time (to go through CNN)

	local split_ix = self.split_ix[split]
	assert(split_ix, 'split ' .. split .. ' not found.')

	-- pick an index of the datapoint to load next
	local ans_batch = torch.LongTensor(batch_size)
	local qst_batch = torch.LongTensor(batch_size, self.qst_length)
	local qst_leng_batch = torch.LongTensor(batch_size)
	local clip_batch = {} -- torch.ByteTensor(batch_size, 16, 3, 160, 120)
	local max_index = self:getMaxIndexOfSplit(split)
	local wrapped = false
	local infos = {}

	for i=1,batch_size do
		local ri = self.iterators[split] -- get next index from iterator
		local ri_next = ri + 1 -- increment iterator
		if ri_next > max_index then ri_next = 1; wrapped = true end -- wrap back around
		self.iterators[split] = ri_next
		ix = split_ix[ri]

		-- Shuffle the data when wrapped
		if split == 'train' and wrapped == true and i == batch_size then
			print('======> train data is wrapped!! we shuffle the clip index')
			self:shuffleTrainData()
			print(self.clips[ self.split_ix['train'][1] ])
		end
		assert(ix ~= nil, 'bug: split ' .. split .. ' was accessed out of bounds with ' .. ri)

		-- fetch the frames 
		local frame_idx = {}
		if self.load_clip then
			local clip
			local begin_frame = self.clips[ix].begin_frame
			local end_frame = self.clips[ix].end_frame
			if self.fix_num_frame then
				local sampling_stride = self.clips[ix].clip_length / self.num_frame
				local fIdx = math.random(begin_frame, begin_frame+math.floor(sampling_stride))
				clip = torch.FloatTensor(self.num_frame, 3, 120, 160)
				for fi=1,self.num_frame do
					table.insert(frame_idx, math.floor(fIdx))
					-- from raw image
					local frame_path = string.gsub(
							string.format(self.data_path .. self.clips[ix].video_path, math.floor(fIdx)),
							'.dat', '.png')
					local frame = image.load(frame_path):float() -- (3,120,160)
					frame:mul(255.0)
					frame:add(-1, self.vgg_mean)

					clip[{fi}] = frame
					fIdx = fIdx + sampling_stride
				end
			else 
				local sampling_stride = self.sampling_stride
				local fIdx = math.random(begin_frame, begin_frame+math.floor(sampling_stride))
				local num_frame = math.ceil(self.clips[ix].clip_length / sampling_stride) - 1
				clip = torch.FloatTensor(num_frame, 3, 120, 160)
				for fi=1,num_frame do
					table.insert(frame_idx, math.floor(fIdx))
					-- from raw image
					local frame_path = string.gsub(
							string.format(self.data_path .. self.clips[ix].video_path, math.floor(fIdx)),
							'.dat', '.png')
					local frame = image.load(frame_path):float() -- (3,120,160)
					frame:mul(255.0)
					frame:add(-1, self.vgg_mean)

					clip[{fi}] = frame
					fIdx = fIdx + sampling_stride
				end
			end
			table.insert(clip_batch, clip:transpose(1,2)) 
		end

		-- fetch the question and answer 
		local tmp_ans = self.qa_label_file:read('/answers'):partial({ix,ix}, {1,self.num_answer})
		ans_batch[{ i }] = tmp_ans[1]:nonzero():squeeze()
		local tmp_qst = self.qa_label_file:read('/questions'):partial({ix,ix}, {1,self.qst_length})
		qst_batch[{ i }] = tmp_qst
		local tmp_q_len = self.qa_label_file:read('/question_length'):partial({ix,ix})[1]
		qst_leng_batch[{ i }] = tmp_q_len

		-- and record associated info as well
		local info_struct = {}
		info_struct.clip_id = self.clips[ix].clip_id
		info_struct.video_path = self.clips[ix].video_path
		info_struct.frame_idx = frame_idx
		if split == 'test' then
			info_struct.begin_frame = self.clips[ix].begin_frame
			info_struct.end_frame   = self.clips[ix].end_frame
			info_struct.clip_length = self.clips[ix].clip_length
			info_struct.question    = self.clips[ix].question
			info_struct.answer      = self.clips[ix].answer
		end
		table.insert(infos, info_struct) 
	end

	-- mapping the answer with vocabulary used in training time
	if split == 'test' then
		-- In test time, we set the ix_to_ans as ix_to_ans used in training time (in eval.lua)
		if self.ans_to_ix_in_train == nil then
			self.ans_to_ix_in_train = {}
			for k, v in pairs(self.ix_to_ans) do
				self.ans_to_ix_in_train[v] = k
			end
			print(self.ans_to_ix_in_train)
		end
		if self.ix_to_ans_in_test == nil then
			self.ix_to_ans_in_test = {}
			for k, v in pairs(self.ans_to_ix) do
				self.ix_to_ans_in_test[v] = k
			end
			print(self.ix_to_ans_in_test)
		end

		for ib=1,batch_size do
			print(ans_batch[ib])
			local ans_string = self.ix_to_ans_in_test[ans_batch[ib]-1]
			print(ans_string)
			local ans_idx = self.ans_to_ix_in_train[ans_string]
			ans_batch[{ib}] = ans_idx + 1
		end
	end

	if gpu_use then
		if self.load_clip then
			for icb=1,batch_size do clip_batch[icb] = clip_batch[icb]:cuda() end
		end
		ans_batch = ans_batch:cuda()
	end

	local data = {}
	if self.load_clip then 
		data.clips  = clip_batch
	else 
		data.clips  = nil
	end
	data.answers   = ans_batch
	data.questions = qst_batch:transpose(1,2):contiguous() -- note: make label sequences go down as columns
	data.question_length = qst_leng_batch
	data.bounds = {it_pos_now = self.iterators[split], it_max = #split_ix, wrapped = wrapped}
	data.infos = infos
	return data
end
