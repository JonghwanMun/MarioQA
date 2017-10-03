package com.postechCVlab.MarioQA;

import java.io.FileReader;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.postechCVlab.MarioQA.returnObjects.*;

public class EventLogs {

	public static final int NOPIVOT = -1111;
	public static final int NOWORD = 9999;
	public static final int NOTARGET = -2222;
	public static final int DEBUGMODE_LEVEL0 = -1000; // No debug mode
	public static final int DEBUGMODE_LEVEL1 = -1001; // print some debug
														// messages
	public static final int DEBUGMODE_LEVEL2 = -1002; // print all debug
														// messages
	public static final String[] ENEMYLIST = { "RedKoopa", "RedKoopaWing", "GreenKoopa",
			"GreenKoopaWing", "Goomba", "GoombaWing", "Spiky", "SpikyWing", "Flower",
			"BulletBill", };
	public static final String[] ITEMLIST = { "Mushroom", "Fireflower", "Coin", };
	public static final String[] BLOCKLIST = { "Empty Block", "Coin Block", "Mushroom Block",
			"Fireflower Block", };
	// variables for configuration
	private boolean loadConfiguration;
	private int fps;
	private int margin;
	private int maxNumSamplingTrial = 300;
	private List<Integer> durations;

	private String eventLogFilePath;
	private int numEvents;
	private int numFrames;
	private int debugMode;

	private String questionTemplateFilePath;
	private String phraseFilePath;
	private QuestionTemplates qstTemplates;

	// variables for event logs
	private String videoPath;
	private String videoFile;
	private int[] frame;
	private String[] marioState;
	private String[] sceneState;
	private String[] marioLocation;
	private String[] spriteLocation;
	private String[] action;
	private String[] target;
	private String[] means;
	private int[] nearestPrevFrame;
	private int[] nearestPostFrame;

	public EventLogs(String eventLogFile, String confFile, String qstTemplatePath,
			String phrasePath, int debugMode) {

		this.eventLogFilePath = eventLogFile;
		this.questionTemplateFilePath = qstTemplatePath;
		this.phraseFilePath = phrasePath;
		this.debugMode = debugMode;

		/*
		 * Extract video path from event log file. event log file form:
		 * pathToEventLogFile/gid.json video path form : gid/gid_#.dat Here, gid
		 * is gameplay id and # corresponds to frame number.
		 */
		String[] tmpDir = eventLogFile.split("\\.")[0].split("/");
		this.videoFile = tmpDir[tmpDir.length - 1];
		this.videoPath = String.format("%s/%s_%s.dat", this.videoFile, this.videoFile, "%d");
		// this.videoPath = prefix;

		// Load configuration file
		loadConfigurationFile(confFile);

		// Load question template file
		qstTemplates = new QuestionTemplates(qstTemplatePath, phrasePath, debugMode);

		// Load the event log file
		JSONParser parser = new JSONParser();
		JSONArray logList;
		try {
			logList = (JSONArray) parser.parse(new FileReader(eventLogFilePath));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		// initialize variables for the event log
		numEvents = logList.size();
		numFrames = ((Long) ((JSONObject) logList.get(numEvents - 1)).get("frame")).intValue();
		System.out.println(String.format("==> # of events (%d)  | # of frames (%d)", numEvents, numFrames));

		frame = new int[numEvents];
		marioState = new String[numFrames + 1];
		sceneState = new String[numFrames + 1];
		marioLocation = new String[numEvents];
		spriteLocation = new String[numEvents];
		action = new String[numEvents];
		target = new String[numEvents];
		means = new String[numEvents];
		nearestPrevFrame = new int[numEvents];
		nearestPostFrame = new int[numEvents];
		Arrays.fill(nearestPrevFrame, -1);
		Arrays.fill(nearestPostFrame, -1);

		Iterator<JSONObject> iter = (Iterator<JSONObject>) logList.iterator();
		JSONObject curLog;
		int i = 0;
		int prevFrame = 0;
		String curMario;
		String curScene;
		String prevMario = "None";
		String prevScene = "None";

		// iterating the event logs
		while (iter.hasNext()) {
			curLog = iter.next();

			// Extract the event information
			frame[i] = ((Long) curLog.get("frame")).intValue();
			curMario = (String) curLog.get("marioState");
			curScene = (String) curLog.get("sceneState");
			marioLocation[i] = (String) curLog.get("marioLocation");
			spriteLocation[i] = (String) curLog.get("spriteLocation");
			action[i] = (String) curLog.get("action");
			target[i] = (String) curLog.get("target");
			means[i] = (String) curLog.get("means");
			
			// Assign the initial previous states
			if (frame[i] == 1) {
				prevMario = curMario;
				prevScene = curScene;
				marioState[0] = "None";
				sceneState[0] = "None";
			}
			if (this.debugMode == this.DEBUGMODE_LEVEL2) {
				System.out.println(
						String.format("Previous mario (%s)  |  scene (%s)", prevMario, prevScene));
				System.out.println(
						String.format("Current mario (%s)  |  scene (%s)", curMario, curScene));
			}

			/*
			 * Sometimes, two or more actions occur simultaneously. So, in that
			 * case, we do not save the states. If current action is "Scene"
			 * where mario enters new map and there is no change in states, we
			 * use the previous states. If not, we use the current states.
			 */
			if (prevFrame != frame[i]) {
				if (action.equals("Scene") || action.equals("ENDGAME")) {
					for (int ifr = prevFrame + 1; ifr <= frame[i]; ifr++) {
						marioState[ifr] = prevMario;
						sceneState[ifr] = prevScene;
					}
				} else {
					for (int ifr = prevFrame + 1; ifr <= frame[i]; ifr++) {
						marioState[ifr] = curMario;
						sceneState[ifr] = curScene;
					}
				}
			}

			if (this.debugMode == this.DEBUGMODE_LEVEL2)
				PrintLog(i);

			// Update the index, frame, scene and mario information
			if (!((String) curLog.get("action")).equals("ENDGAME")) {
				prevFrame = frame[i];
				prevScene = curScene;
				prevMario = curMario;
				i++;
			}
		}
	}

	public void loadConfigurationFile(String confFile) {
		/* loading the configuration */

		loadConfiguration = true;

		JSONParser parser = new JSONParser();
		JSONObject conf = null;
		try {
			conf = (JSONObject) parser.parse(new FileReader(confFile));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		tmpArray = (JSONArray) conf.get("durations");
		this.durations = new ArrayList<Integer>();
		for (int i = 0; i < tmpArray.size(); i++) {
			this.durations.add(((Long) tmpArray.get(i)).intValue());
		}
		this.fps = ((Long) conf.get("fps")).intValue(); // frame per second
		// the minimum distance between two same events for uniqueness check
		this.margin = ((Long) conf.get("margin")).intValue(); 

		if (this.debugMode == this.DEBUGMODE_LEVEL2) {
			System.out.println("fps: " + this.fps);
			System.out.println("margin: " + this.margin);
			System.out.println("durations: " + this.durations);
		}
	}

	/* 
	 * misc methods.
	 */

	public void PrintLog(int index) {
		System.out.println(String.format("Frame %d | %s | %s | %s | %s | %s | %s | %s ",
				frame[index], marioState[index], sceneState[index], marioLocation[index],
				spriteLocation[index], action[index], target[index], means[index]));
	}

	private boolean isInStringArray(String[] stringArray, String target) {
		/* Check that the target string is in the stringArray */

		for (int i = 0; i < stringArray.length; i++) {
			if (target.equals(stringArray[i]))
				return true;
		}
		return false;
	}

	private int getWordFirstLocation(String sentence, String word) {
		/* Return the first location of given word in sentence */
		int loc = this.NOWORD;
		String[] splitedWords = sentence.split(" ");
		for (int i = 0; i < splitedWords.length; i++) {
			if (splitedWords[i].equals(word)) {
				loc = i;
				break;
			}
		}
		if (this.debugMode == this.DEBUGMODE_LEVEL2) {
			for (int i = 0; i < splitedWords.length; i++)
				System.out.print(splitedWords[i] + "  ");
			System.out.println(loc);
		}
		return loc;
	}

	/* 
	 * Helper methods for generating QAs 
	 */

	private int getIndexOfEvent(int beginFrame, int endFrame, int idx, EventInfo distractingEventInfo) {
		// TODO Auto-generated method stub
		return -1;
	}

	private boolean isEqualEvents(EventInfo targetEventInfo, EventInfo refEventInfo) {
		/* Check whether two events are equal */

		if (targetEventInfo.getAction().equals(refEventInfo.getAction())
				&& targetEventInfo.getTarget().equals(refEventInfo.getTarget())
				&& targetEventInfo.getMeans().equals(refEventInfo.getMeans())) {
			return true;
		}
		return false;
	}

	private boolean isEqualEvents(EventInfo eventInfo, int refIdx) {
		/* Check whether two events are equal where reference event is specified by index */

		if (!eventInfo.getAction().equals(this.action[refIdx])) {
			return false;
		}
		if (!eventInfo.getTarget().equals("None")
				&& !eventInfo.getTarget().equals(this.target[refIdx])) {
			if (eventInfo.getTarget().equals("Enemy")
					&& isInStringArray(this.ENEMYLIST, this.target[refIdx])) {
				return true;
			}
			if (eventInfo.getTarget().equals("Item")
					&& isInStringArray(this.ITEMLIST, this.target[refIdx])) {
				return true;
			}
			return false;
		}
		if (!eventInfo.getMeans().equals("None")
				&& !eventInfo.getMeans().equals(this.means[refIdx])) {
			return false;
		}
		return true;
	}

	private int countingEvent(int beginFrame, int endFrame, EventInfo eventInfo, int centerIdx) {
		/* Counting the event in the search region [beginFrame, endFrame] */

		/*
		 * if the location of target event lies in the search region we add 1
		 * because we search events from the centerIdx in the past and future
		 * for the computational efficiency.
		 */
		int occurrenceOfEvent = 0;
		if (this.frame[centerIdx] > beginFrame && this.frame[centerIdx] < endFrame)
			occurrenceOfEvent++;

		// search for the same event in past
		for (int i = 1; centerIdx - i >= 0; i++) {
			if (this.frame[centerIdx - i] < beginFrame)
				break;
			if (isEqualEvents(eventInfo, centerIdx - i))
				occurrenceOfEvent++;
		}

		// search for the same event in future
		for (int i = 1; centerIdx + i < this.numEvents; i++) {
			if (this.frame[centerIdx + i] > endFrame)
				break;
			if (isEqualEvents(eventInfo, centerIdx + i))
				occurrenceOfEvent++;
		}
		return occurrenceOfEvent;
	}

	private int findNearestPrevFrame(int targetIdx, EventInfo eventInfo) {
		/* Find the nearest previous frame having same event */

		if (this.nearestPrevFrame[targetIdx] == -1) {
			boolean findPrevFrame = false;
			for (int ii = 1; targetIdx - ii >= 0; ii++) {
				if (isEqualEvents(eventInfo, targetIdx - ii)) {
					findPrevFrame = true;
					this.nearestPrevFrame[targetIdx] = this.frame[targetIdx - ii];
					break;
				}
			}
			if (!findPrevFrame)
				this.nearestPrevFrame[targetIdx] = 0;
		}
		return this.nearestPrevFrame[targetIdx];

	}

	private int findNearestPostFrame(int targetIdx, EventInfo eventInfo) {
		/* Find the nearest posterior frame having same event */

		if (this.nearestPostFrame[targetIdx] == -1) {
			boolean findPostFrame = false;
			for (int ii = 1; targetIdx + ii < this.numEvents; ii++) {
				if (isEqualEvents(eventInfo, targetIdx + ii)) {
					findPostFrame = true;
					this.nearestPostFrame[targetIdx] = this.frame[targetIdx + ii];
					break;
				}
			}
			if (!findPostFrame)
				this.nearestPostFrame[targetIdx] = this.numFrames + 1;
		}
		return this.nearestPostFrame[targetIdx];
	}

	private int getRandomClipLength() {
		/* Randomly sampling the clip length specified in configuration */

		int clipLength = this.fps
				* this.durations.get((int) (Math.random() * this.durations.size()));
		return clipLength;
	}

	private String getTargetActionLocation(int targetIdx, int beginFrame, 
			int endFrame, int clipLength) {
		/* Obtain the location answer given clip information and clip length */

		String targetActionLocation;
		if (targetIdx != NOTARGET) {
			int targetFrame = this.frame[targetIdx];
			if (targetFrame < beginFrame + clipLength / 3.0) {
				targetActionLocation = "at the beginning";
			} else if (targetFrame < beginFrame + clipLength * 2.0 / 3.0) {
				targetActionLocation = "None";
			} else {
				targetActionLocation = "at the end";
			}
		} else {
			targetActionLocation = "None";
		}
		return targetActionLocation;
	}

	private String getQuestionSemanticChunk(String questionType, String semanticChunk,
			EventInfo eventInfo) {
		String qstSemanticChunk = questionType + "-" + semanticChunk;
		if (semanticChunk.contains("Arg1")) {
			qstSemanticChunk += ("-" + eventInfo.getTarget());
		}
		if (semanticChunk.contains("Arg2")) {
			qstSemanticChunk += ("-" + eventInfo.getMeans());
		}
		return qstSemanticChunk;
	}

	private String[] getEventCentricAnswerInterrogative(String semanticChunk, int targetIdx,
			String qst, ClipRegion clipRegion, EventInfo eventInfo) {
		/* Methods for obtaining answers and interrogative */

		String ans, inte;
		String[] returnValue = new String[2];

		if (semanticChunk.contains("When")) { // Event-Centric question
			inte = "When";
			ans = clipRegion.getTargetActionLocation();
			if (ans.equals("None")) {
				return null;
			}
		} else if (semanticChunk.contains("Where")) { // Event-Centric question
			inte = "Where";
			if (getWordFirstLocation(qst, "Mario") < 3
					|| this.spriteLocation[targetIdx].equals("None")) {
				ans = this.marioLocation[targetIdx];
			} else {
				ans = this.spriteLocation[targetIdx];
			}
		} else if (semanticChunk.contains("How")) { // Event-Centric question,
													// argument 2
			inte = "How";
			if (eventInfo.getAction().equals("Kill")) {
				ans = qstTemplates.getAnswer(this.means[targetIdx], "answer");
			} else if (eventInfo.getAction().equals("Die")) {
				ans = qstTemplates.getAnswer(this.target[targetIdx], "answer-die");
			} else {
				throw new UnsupportedOperationException(
						"Not implemented semantic chunk for Kill event: " + semanticChunk);
			}
		} else if (semanticChunk.contains("Who")) { // Event-Centric question,
													// argument 1
			inte = "Who";
			ans = qstTemplates.getAnswer(this.target[targetIdx], "answer");
		} else if (semanticChunk.contains("What")) { // Event-Centric question,
														// argument 1
			inte = "What";
			ans = qstTemplates.getAnswer(this.target[targetIdx], "answer");
		} else {
			throw new UnsupportedOperationException(
					"Not implemented semantic chunk for Kill event: " + semanticChunk);
		}

		returnValue[0] = ans;
		returnValue[1] = inte;
		return returnValue;
	}

	private HashMap<String, EventInfo> getCandidateEventInfo(int candIdx) {
		/* 
		 * Obtain possible event infomation from the event index.
		 * This method is designed for hard/easy temporal relation question
		 */

		HashMap<String, EventInfo> eventInfos = new HashMap<String, EventInfo>();
		if (this.action[candIdx].equals("Kill")) {
			eventInfos.put("Kill", new EventInfo(this.action[candIdx], "None", "None"));
			eventInfos.put("Kill-Arg1",
					new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
			eventInfos.put("Kill-Arg2",
					new EventInfo(this.action[candIdx], "None", this.means[candIdx]));
			eventInfos.put("Kill-Arg1-Arg2",
					new EventInfo(this.action[candIdx], this.target[candIdx], this.means[candIdx]));
		} else if (this.action[candIdx].equals("Die")) {
			eventInfos.put("Die", new EventInfo(this.action[candIdx], "None", "None"));
			if (isInStringArray(this.ENEMYLIST, this.target[candIdx])) {
				eventInfos.put("DieEnemy-Arg1",
						new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
			} else if (this.target[candIdx].equals("Falling")) {
				eventInfos.put("DieFalling-Arg1",
						new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
			} else {
				eventInfos.put("DieShell-Arg1",
						new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
			}
		} else if (this.action[candIdx].equals("Jump")) {
			eventInfos.put("Jump", new EventInfo(this.action[candIdx], "None", "None"));
		} else if (this.action[candIdx].equals("Hit")) {
			eventInfos.put("Hit", new EventInfo(this.action[candIdx], "None", "None"));
			eventInfos.put("Hit-Arg1",
					new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
		} else if (this.action[candIdx].equals("Break")) {
			eventInfos.put("Break", new EventInfo(this.action[candIdx], "None", "None"));
		} else if (this.action[candIdx].equals("Appear")) {
			if (isInStringArray(this.ENEMYLIST, this.target[candIdx])) {
				eventInfos.put("AppearEnemy", new EventInfo(this.action[candIdx], "None", "None"));
				eventInfos.put("AppearEnemy-Arg1",
						new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
			}
			if (isInStringArray(this.ITEMLIST, this.target[candIdx])) {
				eventInfos.put("AppearItem", new EventInfo(this.action[candIdx], "None", "None"));
				eventInfos.put("AppearItem-Arg1",
						new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
			}
		} else if (this.action[candIdx].equals("Shoot")) {
			eventInfos.put("Shoot", new EventInfo(this.action[candIdx], "None", "None"));
		} else if (this.action[candIdx].equals("Throw")) {
			eventInfos.put("Throw", new EventInfo(this.action[candIdx], "None", "None"));
		} else if (this.action[candIdx].equals("Kick")) {
			eventInfos.put("Kick", new EventInfo(this.action[candIdx], "None", "None"));
		} else if (this.action[candIdx].equals("Hold")) {
			eventInfos.put("Hold", new EventInfo(this.action[candIdx], "None", "None"));
		} else if (this.action[candIdx].equals("Eat")) {
			eventInfos.put("Eat", new EventInfo(this.action[candIdx], "None", "None"));
			eventInfos.put("Eat-Arg1",
					new EventInfo(this.action[candIdx], this.target[candIdx], "None"));
		}
		return eventInfos;
	}

	private int[] getEventsInCandidateRegion(CandidateRegion candRegion, int targetIdx) {
		/* 
		 * Obtain event list from the candidate region.
		 * This method is designed for finding candidate events as reference event.
		 */

		int[] eventBeginEndIdx = new int[2];

		// find begin index in candidate region
		for (int i = 1; targetIdx - i >= 0; i++) {
			if (this.frame[targetIdx - i] < candRegion.getMinFrame()) {
				eventBeginEndIdx[0] = targetIdx - i + 1;
			}
		}

		// find end index in candidate region
		for (int i = 1; targetIdx + i < this.numEvents; i++) {
			if (this.frame[targetIdx + i] > candRegion.getMaxFrame()) {
				eventBeginEndIdx[1] = targetIdx + i - 1;
			}
		}
		return eventBeginEndIdx;
	}

	private JSONObject constructJSONObjectOfQAInfomation(ClipRegion clipRegion, String qst,
			String ans, EventInfo eventInfo, int clipLength, String questionType,
			String qstSemanticChunk, String temporal_relationship, String inte, int targetIdx) {
		/* Methods for constructing annotation including QA information */

		JSONObject qaInfo = new JSONObject();
		qaInfo.put("video_path", this.videoPath);
		qaInfo.put("begin_frame", clipRegion.getBeginFrame());
		qaInfo.put("end_frame", clipRegion.getEndFrame());
		qaInfo.put("question", qst);
		qaInfo.put("answer", ans);
		qaInfo.put("event", eventInfo.getAction());
		qaInfo.put("clip_length", clipLength);
		qaInfo.put("question_type", questionType);
		qaInfo.put("semantic_chunk", qstSemanticChunk);
		qaInfo.put("temporal_relationship", temporal_relationship);
		qaInfo.put("interrogative", inte);
		if (targetIdx == NOTARGET) {
			qaInfo.put("target_frame", (clipRegion.getBeginFrame()+clipRegion.getEndFrame())/2);
		} else {
			qaInfo.put("target_frame", this.frame[targetIdx]);
		}
		return qaInfo;
	}

	/* 
	 * Methods for computing candidate clip regions 
	 */
	private CandidateRegion findSimpleCandidateRegion(
			int targetIdx, EventInfo eventInfo, int ratio) {
		/*
		 * Obtain candidate region [targetFrame-clipLength/ratio+margin/2,
		 * targetFrame+clipLength/ratio-margin/2]
		 */

		boolean isProper;
		int clipLength = getRandomClipLength();
		int targetFrame = this.frame[targetIdx];

		int minFrame = targetFrame - clipLength / ratio;
		int maxFrame = targetFrame + clipLength / ratio;

		if (ratio == 1) {
			minFrame += this.margin / 2;
			maxFrame -= this.margin / 2;
		}

		if (minFrame < 1)
			minFrame = 1;
		if (maxFrame > this.numFrames)
			maxFrame = this.numFrames;

		/*
		 * Check the obtained candidate region is proper to generate QA. 1. the
		 * candidate regions should be longer than clip length 2. minFrame
		 * should be smaller than the frame of target event 3. maxFrame should
		 * be bigger than the frame of target event
		 */
		isProper = (maxFrame - minFrame + 1) >= clipLength;
		if (minFrame > targetFrame - this.margin / 2)
			isProper = false;
		if (maxFrame < targetFrame + this.margin / 2)
			isProper = false;

		if (this.debugMode == this.DEBUGMODE_LEVEL2) {
			System.out.println(String.format("%s    %d", isProper, clipLength));
		}
		return new CandidateRegion(minFrame, maxFrame, isProper, clipLength);
	}

	private CandidateRegion findCandidateRegionForUniqueEvent(int targetIdx, EventInfo eventInfo) {
		int minFrame = 1;
		int maxFrame = this.numFrames;
		boolean isProper;
		int clipLength = getRandomClipLength();
		int targetFrame = this.frame[targetIdx];

		/*
		 * To guarantee the uniqueness of target event, we find the locations
		 * having the same event before or after the target event occurs. The
		 * distances of locations with target event are smaller than clip
		 * length. If there is no same event, we set the location as 0 or
		 * numFrames. Then, the candidate region where the target event exists
		 * uniquely is obtained with margin for the obtained locations (as frames).
		 */
		int npf = findNearestPrevFrame(targetIdx, eventInfo);
		if (npf == 0) {
			minFrame = Math.max(1, targetFrame - clipLength + this.margin / 2);
		} else {
			minFrame = Math.max(this.nearestPrevFrame[targetIdx], targetFrame - clipLength)
					+ this.margin / 2;
		}

		npf = findNearestPostFrame(targetIdx, eventInfo);
		if (npf == this.numFrames + 1) {
			maxFrame = Math.min(targetFrame + clipLength - this.margin / 2, this.numFrames);
		} else {
			maxFrame = Math.min(this.nearestPostFrame[targetIdx], targetFrame + clipLength)
					- this.margin / 2;
		}

		/*
		 * Check the candidate region is proper to generate QA. 
		 * 1. the candidate region should be longer than the sampled clip length 
		 * 2. minFrame should be smaller than the frame of target event 
		 * 3. maxFrame should be bigger than the frame of target event
		 */
		isProper = (maxFrame - minFrame + 1) >= clipLength;
		if (minFrame > targetFrame - this.margin / 2)
			isProper = false;
		if (maxFrame < targetFrame + this.margin / 2)
			isProper = false;

		if (this.debugMode == this.DEBUGMODE_LEVEL2) {
			System.out.println(String.format("%s    %d", isProper, clipLength));
		}
		return new CandidateRegion(minFrame, maxFrame, isProper, clipLength);
	}

	/* 
	 * Methods for sampling clip regions 
	 */
	private ClipRegion findRandomClipRegionOverFullClip(String semanticChunk, int targetIdx) {
		/* 
		 * Sampling randomly clip region over the full length of clip.
		 * This method is desinged for state questions which have no target event.
		 */

		int clipLength = getRandomClipLength();

		String[] stateList;
		if (semanticChunk.equals("Mario-Status")) {
			stateList = this.marioState;
		} else if (semanticChunk.equals("Scene-Status")) {
			stateList = this.sceneState;
		} else {
			throw new UnsupportedOperationException(
					"Unknown semanticChunk for state: " + semanticChunk);
		}

		int beginFrame = -1;
		int endFrame = -1;
		boolean findClipRegion = false;
		boolean isSameState;
		while (!findClipRegion) {
			beginFrame = (int) (Math.random() * (this.numFrames - 2 * clipLength)) + 1;
			endFrame = beginFrame + clipLength - 1;

			// Check that the states are same over all the frames
			String initialState = stateList[beginFrame];
			isSameState = true;
			for (int fr = beginFrame + 1; fr <= endFrame; fr++) {
				if (!initialState.equals(stateList[fr])) {
					isSameState = false;
					break;
				}
			}
			findClipRegion = isSameState;
		}
		
		assert beginFrame >= 1:
			"Begining frame is smaller than 0";
		assert endFrame < this.numFrames:
			"end frame is bigger than last frame";

		String targetActionLocation = getTargetActionLocation(targetIdx, beginFrame, endFrame,
				clipLength);
		return new ClipRegion(beginFrame, endFrame, targetActionLocation, findClipRegion);
	}

	private ClipRegion randomSamplingClipRegionFromCandidateRegion(CandidateRegion candRegion, int targetIdx,
			int leftSafeZone, int rightSafeZone) {
		/* 
		 * Sampling randomly clip region near the target event.
		 * Default safe zone is [targetFrame-margin/2, targetFrame+margin/2] to contain
		 * the target event. Note that leftSafeZone and rightSafeZone is designed to
		 * contain reference event.
		 */

		ClipRegion clipRegion = new ClipRegion(-1, -1, "None", false);
		int targetFrame = this.frame[targetIdx];

		if (leftSafeZone == NOPIVOT)
			leftSafeZone = targetFrame - this.margin / 2;
		if (rightSafeZone == NOPIVOT)
			rightSafeZone = targetFrame + this.margin / 2;

		if (leftSafeZone < candRegion.getMinFrame() || rightSafeZone > candRegion.getMaxFrame())
			return clipRegion;

		int beginFrame, endFrame;
		for (int ii = 0; ii < this.maxNumSamplingTrial; ii++) {
			beginFrame = (int) (Math.random() * (leftSafeZone - candRegion.getMinFrame() + 1))
					+ candRegion.getMinFrame();
			endFrame = beginFrame + candRegion.getClipLength() - 1;

			assert beginFrame <= leftSafeZone: 
				String.format("%d | %d | %d", beginFrame, leftSafeZone, candRegion.getMinFrame());

			if ((endFrame >= rightSafeZone) && (endFrame <= candRegion.getMaxFrame())) {
				String targetActionLocation = getTargetActionLocation(targetIdx, beginFrame,
						endFrame, candRegion.getClipLength());

				clipRegion.setBeginFrame(beginFrame);
				clipRegion.setEndFrame(endFrame);
				clipRegion.setFindClip(true);
				clipRegion.setTargetActionLocation(targetActionLocation);
				break;
			}
		}
		return clipRegion;
	}

	/* 
	 * Methods for event-centric questions with no temporal relationship
	 */
	public JSONArray getEventCentricExamples() {

		JSONArray QAList = new JSONArray();
		for (int ie = 0; ie < this.numEvents; ie++) {
			if (this.debugMode == this.DEBUGMODE_LEVEL2) {
				System.out.println(String.format("%d/%d event action is %s", ie + 1, this.numEvents,
						this.action[ie]));
			}
			if (this.action[ie].equals("Kill")) {
				getKillEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Die")) {
				getDieEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Jump")) {
				getJumpEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Hit")) {
				getHitEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Break")) {
				getBreakEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Appear")) {
				getAppearEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Shoot")) {
				getShootEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Throw")) {
				getThrowEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Kick")) {
				getKickEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Hold")) {
				getHoldEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Eat")) {
				getEatEventCentricQA(QAList, ie);
			} else if (this.action[ie].equals("Scene") || this.action[ie].equals("Mario")
					|| this.action[ie].equals("ENDGAME")) {
				continue;
			} else {
				throw new UnsupportedOperationException(
						"Unknown event for event-centric question: " + this.action[ie]);
			}
		}

		return QAList;
	}

	public void getKillEventCentricQA(JSONArray qaList, int targetIdx) {
		/* Generate event-centric QAs for kill event */

		// Question template with no argument
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Kill-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-Where", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-How", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-Who", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		getSimpleEventCentricQA(qaList, "Kill-When-Arg1", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-Where-Arg1", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-How-Arg1", targetIdx, eventInfo);

		// Question template with argument2 (means)
		eventInfo.setInfo(this.action[targetIdx], "None", this.means[targetIdx]);
		getSimpleEventCentricQA(qaList, "Kill-When-Arg2", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-Where-Arg2", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-Who-Arg2", targetIdx, eventInfo);

		// Question template with argument1 and argument2 (target, means)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], this.means[targetIdx]);
		getSimpleEventCentricQA(qaList, "Kill-When-Arg1-Arg2", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kill-Where-Arg1-Arg2", targetIdx, eventInfo);
	}

	public void getDieEventCentricQA(JSONArray qaList, int targetIdx) {

		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Die-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Die-Where", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Die-How", targetIdx, eventInfo);
		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			getSimpleEventCentricQA(qaList, "Die-Who", targetIdx, eventInfo);
		}

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			getSimpleEventCentricQA(qaList, "DieEnemy-When-Arg1", targetIdx, eventInfo);
			getSimpleEventCentricQA(qaList, "DieEnemy-Where-Arg1", targetIdx, eventInfo);
		}
		if (this.target[targetIdx].equals("Falling")) {
			getSimpleEventCentricQA(qaList, "DieFalling-When-Arg1", targetIdx, eventInfo);
		}
		if (this.target[targetIdx].equals("Shell")) {
			getSimpleEventCentricQA(qaList, "DieShell-When-Arg1", targetIdx, eventInfo);
			getSimpleEventCentricQA(qaList, "DieShell-Where-Arg1", targetIdx, eventInfo);
		}
	}

	public void getJumpEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Jump-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Jump-Where", targetIdx, eventInfo);
	}

	public void getHitEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Hit-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Hit-Where", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Hit-What", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		getSimpleEventCentricQA(qaList, "Hit-When-Arg1", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Hit-Where-Arg1", targetIdx, eventInfo);
	}

	public void getBreakEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Break-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Break-Where", targetIdx, eventInfo);
	}

	public void getAppearEventCentricQA(JSONArray qaList, int targetIdx) {
		EventInfo eventInfo;

		// For appear event with ENEMY
		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "Enemy", "None");
			getSimpleEventCentricQA(qaList, "AppearEnemy-Who", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			getSimpleEventCentricQA(qaList, "AppearEnemy-When-Arg1", targetIdx, eventInfo);
			getSimpleEventCentricQA(qaList, "AppearEnemy-Where-Arg1", targetIdx, eventInfo);
		}

		// For appear event with ITEM
		if (isInStringArray(this.ITEMLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "Item", "None");
			getSimpleEventCentricQA(qaList, "AppearItem-What", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			getSimpleEventCentricQA(qaList, "AppearItem-When-Arg1", targetIdx, eventInfo);
			getSimpleEventCentricQA(qaList, "AppearItem-Where-Arg1", targetIdx, eventInfo);
		}
	}

	public void getShootEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Shoot-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Shoot-Where", targetIdx, eventInfo);
	}

	public void getThrowEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Throw-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Throw-Where", targetIdx, eventInfo);
	}

	public void getKickEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Kick-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Kick-Where", targetIdx, eventInfo);
	}

	public void getHoldEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Hold-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Hold-Where", targetIdx, eventInfo);
	}

	public void getEatEventCentricQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getSimpleEventCentricQA(qaList, "Eat-When", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Eat-Where", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Eat-What", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		getSimpleEventCentricQA(qaList, "Eat-When-Arg1", targetIdx, eventInfo);
		getSimpleEventCentricQA(qaList, "Eat-Where-Arg1", targetIdx, eventInfo);
	}

	private void getSimpleEventCentricQA(JSONArray qaList, String semanticChunk, int targetIdx, EventInfo eventInfo) {
		/* Generate event-centric question with no temporal relationship
		 * 1. Sample the clip region and check the uniqueness of target event 
		 * 2. Get question template and answer
		 */
		String questionType = "Event-Centric";
		JSONObject qaInfo = null;
		CandidateRegion candRegion = findCandidateRegionForUniqueEvent(targetIdx, eventInfo);
		if (candRegion.isProper()) {
			ClipRegion clipRegion = randomSamplingClipRegionFromCandidateRegion(
					candRegion, targetIdx, NOPIVOT, NOPIVOT);

			if (clipRegion.isFindClip()) {
				// get question and answer
				String qst = qstTemplates.getQuestion(questionType, semanticChunk, eventInfo);
				String[] ansAndInte = getEventCentricAnswerInterrogative(
						semanticChunk, targetIdx, qst, clipRegion, eventInfo);
				if (ansAndInte == null) return;
				String ans = ansAndInte[0];
				String inte = ansAndInte[1];

				// Obtain question semantic chunk
				String qstSemanticChunk = getQuestionSemanticChunk(
						questionType, semanticChunk, eventInfo);

				// write json object
				qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans, eventInfo,
						candRegion.getClipLength(), questionType, qstSemanticChunk, "NT", inte, targetIdx);
			}
		}
		if (qaInfo != null)
			qaList.add(qaInfo);
	}


	/*
	 * Methods for counting questions with no temporal relationship
	 */
	public JSONArray getCountingExamples() {

		JSONArray QAList = new JSONArray();
		for (int ie = 0; ie < this.numEvents; ie++) {
			if (this.action[ie].equals("Kill")) {
				getKillCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Die")) {
				continue;
			} else if (this.action[ie].equals("Jump")) {
				getJumpCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Hit")) {
				getHitCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Break")) {
				getBreakCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Appear")) {
				getAppearCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Shoot")) {
				getShootCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Throw")) {
				getThrowCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Kick")) {
				getKickCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Hold")) {
				getHoldCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Eat")) {
				getEatCountingQA(QAList, ie);
			} else if (this.action[ie].equals("Scene") || this.action[ie].equals("Mario")
					|| this.action[ie].equals("ENDGAME")) {
				continue;
			} else {
				throw new UnsupportedOperationException(
						"Unknown event for event-centric question: " + this.action[ie]);
			}
		}
		return QAList;
	}

	public void getKillCountingQA(JSONArray qaList, int targetIdx) {
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");

		// Question template without arguments
		getCountingQA(qaList, "Kill", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		getCountingQA(qaList, "Kill-Arg1", targetIdx, eventInfo);

		// Question template with argument2 (means)
		eventInfo.setInfo(this.action[targetIdx], "None", this.means[targetIdx]);
		getCountingQA(qaList, "Kill-Arg2", targetIdx, eventInfo);

		// Question template with argument1 and argument2 (target, means)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], this.means[targetIdx]);
		getCountingQA(qaList, "Kill-Arg1-Arg2", targetIdx, eventInfo);
	}

	public void getJumpCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Jump", targetIdx, eventInfo);
	}

	public void getHitCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Hit", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		getCountingQA(qaList, "Hit-Arg1", targetIdx, eventInfo);
	}

	public void getBreakCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Break", targetIdx, eventInfo);
	}

	public void getAppearCountingQA(JSONArray qaList, int targetIdx) {
		EventInfo eventInfo;

		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "Enemy", "None");
			getCountingQA(qaList, "AppearEnemy", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			getCountingQA(qaList, "AppearEnemy-Arg1", targetIdx, eventInfo);
		}

		if (isInStringArray(this.ITEMLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "Item", "None");
			getCountingQA(qaList, "AppearItem", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			getCountingQA(qaList, "AppearItem-Arg1", targetIdx, eventInfo);
		}
	}

	public void getShootCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Shoot", targetIdx, eventInfo);
	}

	public void getThrowCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Throw", targetIdx, eventInfo);
	}

	public void getKickCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Kick", targetIdx, eventInfo);
	}

	public void getHoldCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Hold", targetIdx, eventInfo);
	}

	public void getEatCountingQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		getCountingQA(qaList, "Eat", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		getCountingQA(qaList, "Eat-Arg1", targetIdx, eventInfo);
	}

	private void getCountingQA(JSONArray qaList, String semanticChunk, int targetIdx, EventInfo eventInfo) {
		/*
		 * 1. Sample the clip region and check the uniqueness of target event 2.
		 * Get question template and answer
		 */
		String questionType = "Counting";
		JSONObject qaInfo = null;
		CandidateRegion candRegion = findSimpleCandidateRegion(targetIdx, eventInfo, 1);
		if (candRegion.isProper()) {
			ClipRegion clipRegion = randomSamplingClipRegionFromCandidateRegion(
					candRegion, targetIdx, NOPIVOT, NOPIVOT);

			if (clipRegion.isFindClip()) {
				// get question and answer
				String qst = qstTemplates.getQuestion(questionType, semanticChunk, eventInfo);
				String ans = Integer.toString(countingEvent(clipRegion.getBeginFrame(),
						clipRegion.getEndFrame(), eventInfo, targetIdx));

				// Obtain question semantic chunk
				String qstSemanticChunk = getQuestionSemanticChunk(
						questionType, semanticChunk, eventInfo);

				// write json object
				qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans, eventInfo,
						candRegion.getClipLength(), questionType, qstSemanticChunk, "NT", "How many", targetIdx);
			}
		}
		if (qaInfo != null)
			qaList.add(qaInfo);
	}

	/* 
	 * Methods for state questions with no temporal relationship
	 */
	public JSONArray getStateExamples(int numOfStateQuestions) {
		/* Obtain state questions with no temporal relationship.
		 * Extract pre-defined numbers of state questions. Here, we obtain only
		 * two types of state, which are state of Mario and scene.
		 */

		JSONArray QAList = new JSONArray();
		EventInfo fakeEventInfo = new EventInfo("State", "None", "None");
		for (int ie = 0; ie < numOfStateQuestions; ie++) {

			getStateQA(QAList, "Mario-Status", fakeEventInfo);
			getStateQA(QAList, "Scene-Status", fakeEventInfo);
		}
		return QAList;
	}

	private void getStateQA(JSONArray qaList, String semanticChunk, EventInfo eventInfo) {
		/*
		 * 1. Sample the clip region randomly over the full length of clip.
		 * 2. Check that the states of Mario/Scene are identical over all frames.
		 * 3. Get question template and answer.
		 */
		String questionType = "State";
		JSONObject qaInfo = null;
		ClipRegion clipRegion = findRandomClipRegionOverFullClip(semanticChunk, NOTARGET);

		if (clipRegion.isFindClip()) {
			// get question and answer
			String qst = qstTemplates.getQuestion(questionType, semanticChunk, eventInfo);
			String ans;
			if (semanticChunk.equals("Mario-Status")) {
				ans = this.marioState[clipRegion.getBeginFrame()];
			} else if (semanticChunk.equals("Scene-Status")) {
				ans = this.sceneState[clipRegion.getBeginFrame()];
			} else {
				throw new UnsupportedOperationException(
						"Unknown semanticChunk for state: " + semanticChunk);
			}

			// Obtain question semantic chunk
			String qstSemanticChunk = getQuestionSemanticChunk(
					questionType, semanticChunk, eventInfo);

			// write json object
			qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans, eventInfo,
					clipRegion.getEndFrame() - clipRegion.getBeginFrame() + 1, questionType,
					qstSemanticChunk, "NT", "What state", NOTARGET);
		}
		if (qaInfo != null)
			qaList.add(qaInfo);
	}


	/*
	 * Methods for event-centric questions with temporal relationship
	 */
	public JSONArray getEventCentricWithTemporalRelationshipExamples() {

		JSONArray QAList = new JSONArray();
		for (int ie = 0; ie < this.numEvents; ie++) {
			if (this.debugMode == this.DEBUGMODE_LEVEL2) {
				System.out.println(String.format("%d/%d event action is %s", 
							ie + 1, this.numEvents, this.action[ie]));
			}
			if (this.action[ie].equals("Kill")) {
				getKillEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Die")) {
				getDieEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Jump")) {
				getJumpEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Hit")) {
				getHitEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Break")) {
				getBreakEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Appear")) {
				getAppearEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Shoot")) {
				getShootEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Throw")) {
				getThrowEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Kick")) {
				getKickEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Hold")) {
				getHoldEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Eat")) {
				getEatEventCentricWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Scene") || this.action[ie].equals("Mario")
					|| this.action[ie].equals("ENDGAME")) {
				continue;
			} else {
				throw new UnsupportedOperationException("Unknown event for "
						+ "event-centric question with temporal relationship: " + this.action[ie]);
			}
		}
		return QAList;
	}

	private void getEventCentricWithEasyTemporalRelationshipQA(JSONArray qaList, 
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo) {
		/*
		 * Generate the event-centric QAs with easy temporal relationship.
		 * 1. Compute the candidate region for the target event while verifying the uniqueness. 
		 * 2. Obtain the list of events, which are candidates of reference event, 
		 * occurring in the candidate region.
		 * 3. Iterating the candidate events and perform following procedure. 
		 *    - Obtain a set of semantic chunk for the candidate event as reference. 
		 *    - Iterating the candidate event with semantic chunks. 
		 *    - Compute the clip region containing both the target event and candidate event. 
		 *    - Check the properness for the candidate event to generate QA in the clip region.
		 *      > if ET, check the uniqueness in the clip region.
		 * 4. Get QA based on the pre-defined template and put it into the return value (JSONArray).
		 */
		if (this.debugMode == this.DEBUGMODE_LEVEL2) {
			return;
		}
		String targetQuestionType = "Event-Centric";
		CandidateRegion candRegion = findCandidateRegionForUniqueEvent(targetIdx, targetEventInfo); // [1]
		if (candRegion.isProper()) {
			int[] candEventBeginEndIdx = getEventsInCandidateRegion(candRegion, targetIdx);

			// iterate the candidate target events
			for (int candIdx = candEventBeginEndIdx[0]; candIdx <= candEventBeginEndIdx[1]; candIdx++) {

				if (candIdx == targetIdx)
					continue;

				// semantic chunk for candidate reference event
				HashMap<String, EventInfo> candEventList = getCandidateEventInfo(candIdx);
				String[] keys = candEventList.keySet().toArray(new String[candEventList.keySet().size()]);
				for (int ik = 0; ik < keys.length; ik++) {

					ClipRegion clipRegion;
					if (candIdx > targetIdx) { // before
						clipRegion = randomSamplingClipRegionFromCandidateRegion(candRegion,
								targetIdx, NOPIVOT, this.frame[candIdx] + this.margin / 2);
					} else { // after
						clipRegion = randomSamplingClipRegionFromCandidateRegion(candRegion,
								targetIdx, this.frame[candIdx] - this.margin / 2, NOPIVOT);
					}

					if (clipRegion.isFindClip()) {

						String candSemanticChunk = keys[ik];
						EventInfo candEventInfo = candEventList.get(candSemanticChunk);

						assert clipRegion.getBeginFrame() >= candRegion.getMinFrame() : String
								.format("Invalid begin frame!!! (%d) < (%d)",
										clipRegion.getBeginFrame(), candRegion.getMinFrame());
						assert clipRegion.getEndFrame() <= candRegion.getMaxFrame() : String.format(
								"Invalid end frame!!! (%d) > (%d)", clipRegion.getEndFrame(),
								candRegion.getMaxFrame());

						// check properness
						if (this.isEqualEvents(targetEventInfo, candEventInfo))
							continue;
						int occurrenceOfCandEvent = countingEvent(clipRegion.getBeginFrame(),
								clipRegion.getEndFrame(), candEventInfo, candIdx);
						if (occurrenceOfCandEvent != 1)
							continue;

						// obtain question
						String refQuestionType = (candIdx > targetIdx) ? "Reference-Before"
								: "Reference-After";
						String refQst = qstTemplates.getQuestion(refQuestionType, candSemanticChunk,
								candEventInfo);
						String targetQst = qstTemplates.getQuestion(targetQuestionType,
								targetSemanticChunk, targetEventInfo);
						targetQst = targetQst.replace("?", " ");
						String qst = targetQst + refQst;

						// obtain question answer
						String[] ansAndInte = getEventCentricAnswerInterrogative(
								targetSemanticChunk, targetIdx, qst, clipRegion, targetEventInfo);
						if (ansAndInte == null) continue;
						String ans = ansAndInte[0];
						String inte = ansAndInte[1];

						// Obtain question semantic chunk
						String qstSemanticChunk = getQuestionSemanticChunk(targetQuestionType,
								targetSemanticChunk, targetEventInfo) + "-"
								+ getQuestionSemanticChunk(refQuestionType, candSemanticChunk,
										candEventInfo);

						// write JSONObject for QA information and append it
						// into JSONArray
						JSONObject qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans,
								targetEventInfo, candRegion.getClipLength(), targetQuestionType,
								qstSemanticChunk, "ET", inte, targetIdx);
						if (qaInfo != null) {
							qaInfo.put("ref_frame", this.frame[candIdx]);
							qaList.add(qaInfo);
						}
					}
				}
			}
		}
	}

	private void getEventCentricWithHardTemporalRelationshipQA(JSONArray qaList, 
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo) {
		/*
		 * Generate the event-centric QAs with hard temporal relationship.
		 */

		if (this.debugMode == this.DEBUGMODE_LEVEL2) {
			return;
		}

		String targetQuestionType = "Event-Centric";
		CandidateRegion candRegion = findSimpleCandidateRegion(targetIdx, targetEventInfo, 2); // not unique
		if (candRegion.isProper()) {
			int[] candEventBeginEndIdx = getEventsInCandidateRegion(candRegion, targetIdx);

			// iterate the candidate target events
			for (int candIdx = candEventBeginEndIdx[0]; candIdx <= candEventBeginEndIdx[1]; candIdx++) {

				if (candIdx == targetIdx)
					continue;

				HashMap<String, EventInfo> candEventList = getCandidateEventInfo(candIdx);
				String[] keys = candEventList.keySet().toArray(new String[candEventList.keySet().size()]);
				for (int ik = 0; ik < keys.length; ik++) {

					ClipRegion clipRegion = (candIdx < targetIdx)
							? randomSamplingClipRegionFromCandidateRegion(candRegion, targetIdx,
									this.frame[candIdx] - this.margin / 2, NOPIVOT)
							: randomSamplingClipRegionFromCandidateRegion(candRegion, targetIdx,
									NOPIVOT, this.frame[candIdx] + this.margin / 2);

					if (clipRegion.isFindClip()) {

						String candSemanticChunk = keys[ik];
						EventInfo candEventInfo = candEventList.get(candSemanticChunk);

						/*
						 * properness check for candidate reference event. 
						 * 1. should be different with the target event 
						 * 2. should be unique in the full clip region
						 */
						if (this.isEqualEvents(targetEventInfo, candEventInfo))
							continue;
						int occurrenceOfCandEvent = countingEvent(clipRegion.getBeginFrame(),
								clipRegion.getEndFrame(), candEventInfo, candIdx);
						if (occurrenceOfCandEvent != 1)
							continue;

						// simply, the distracting event has same action with the reference event
						EventInfo distractingEventInfo = 
								new EventInfo(targetEventInfo.getAction(), "None", "None");

						/*
						 * properness check for target event 
						 * 1. Should be unique in the constrained region 
						 * 2. distracting event should occur more than once
						 */
						int occurrenceOfTargetEvent, occurrenceOfDistractingEvent;
						if (candIdx > targetIdx) { // before
							occurrenceOfTargetEvent = countingEvent(clipRegion.getBeginFrame(),
									this.frame[candIdx] - 1, targetEventInfo, targetIdx);
							occurrenceOfDistractingEvent = countingEvent(
									this.frame[candIdx] + 1, clipRegion.getEndFrame(),
									distractingEventInfo, candIdx);

						} else { // after
							occurrenceOfTargetEvent = countingEvent(this.frame[candIdx] + 1,
									clipRegion.getEndFrame(), targetEventInfo, targetIdx);
							occurrenceOfDistractingEvent = countingEvent(
									clipRegion.getBeginFrame(), this.frame[candIdx] - 1,
									distractingEventInfo, candIdx);
						}
						if (occurrenceOfTargetEvent != 1)
							continue;
						if (occurrenceOfDistractingEvent < 1)
							continue;

						// obtain question
						String refQuestionType = (candIdx > targetIdx) ? "Reference-Before"
								: "Reference-After";
						String refQst = qstTemplates.getQuestion(refQuestionType, candSemanticChunk,
								candEventInfo);
						String targetQst = qstTemplates.getQuestion(targetQuestionType,
								targetSemanticChunk, targetEventInfo);
						targetQst = targetQst.replace("?", " ");
						String qst = targetQst + refQst;

						// obtain question answer
						String[] ansAndInte = getEventCentricAnswerInterrogative(
								targetSemanticChunk, targetIdx, qst, clipRegion, targetEventInfo);
						if (ansAndInte == null) continue;
						String ans = ansAndInte[0];
						String inte = ansAndInte[1];

						// TODO: extreme hard case (different answer obtained
						// from target/distracting event)
						if (this.debugMode == this.DEBUGMODE_LEVEL2) {
							int distractingIdx = (candIdx > targetIdx)
									? getIndexOfEvent(clipRegion.getBeginFrame(),
											this.frame[candIdx] - 1, candIdx, distractingEventInfo)
									: getIndexOfEvent(this.frame[candIdx] + 1,
											clipRegion.getEndFrame(), candIdx,
											distractingEventInfo);

							String[] distAnsInte = getEventCentricAnswerInterrogative(
									targetSemanticChunk, candIdx, qst, clipRegion,
									distractingEventInfo);
							String distAns = distAnsInte[0];
							String distInte = distAnsInte[1];
							if (!distAns.equals(ans))
								System.out.println(distAns + " | " + ans);
						}

						// Obtain question semantic chunk
						String qstSemanticChunk = getQuestionSemanticChunk(targetQuestionType,
								targetSemanticChunk, targetEventInfo) + "-"
								+ getQuestionSemanticChunk(refQuestionType, candSemanticChunk,
										candEventInfo);

						// write JSONObject for QA information and append it into JSONArray
						JSONObject qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans,
								targetEventInfo, candRegion.getClipLength(), targetQuestionType,
								qstSemanticChunk, "HT", inte, targetIdx);
						if (qaInfo != null) {
							qaInfo.put("ref_frame", this.frame[candIdx]);
							qaList.add(qaInfo);
						}
					}
				}
			}
		}
	}

	public void getKillEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Kill-Where", targetIdx, eventInfo);
		callingFuncEventCentricQAWithETHT(qaList, "Kill-How", targetIdx, eventInfo);
		callingFuncEventCentricQAWithETHT(qaList, "Kill-Who", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncEventCentricQAWithETHT(qaList, "Kill-Where-Arg1", targetIdx, eventInfo);
		callingFuncEventCentricQAWithETHT(qaList, "Kill-How-Arg1", targetIdx, eventInfo);

		// Question template with argument2 (means)
		eventInfo.setInfo(this.action[targetIdx], "None", this.means[targetIdx]);
		callingFuncEventCentricQAWithETHT(qaList, "Kill-Where-Arg2", targetIdx, eventInfo);
		callingFuncEventCentricQAWithETHT(qaList, "Kill-Who-Arg2", targetIdx, eventInfo);

		// Question template with argument1 and argument2 (target, means)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], this.means[targetIdx]);
		callingFuncEventCentricQAWithETHT(qaList, "Kill-Where-Arg1-Arg2", targetIdx, eventInfo);
	}

	public void getDieEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Die-Where", targetIdx, eventInfo);
		callingFuncEventCentricQAWithETHT(qaList, "Die-How", targetIdx, eventInfo);
		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			callingFuncEventCentricQAWithETHT(qaList, "Die-Who", targetIdx, eventInfo);
		}

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			callingFuncEventCentricQAWithETHT(qaList, "DieEnemy-Where-Arg1", targetIdx, eventInfo);
		} else if (this.target[targetIdx].equals("Shell")) {
			callingFuncEventCentricQAWithETHT(qaList, "DieShell-Where-Arg1", targetIdx, eventInfo);
		}
	}

	public void getJumpEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Jump-Where", targetIdx, eventInfo);
	}

	public void getHitEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Hit-Where", targetIdx, eventInfo);
		callingFuncEventCentricQAWithETHT(qaList, "Hit-What", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncEventCentricQAWithETHT(qaList, "Hit-Where-Arg1", targetIdx, eventInfo);
	}

	public void getBreakEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Break-Where", targetIdx, eventInfo);
	}

	public void getAppearEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		EventInfo eventInfo;

		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
			callingFuncEventCentricQAWithETHT(qaList, "AppearEnemy-Who", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			callingFuncEventCentricQAWithETHT(qaList, "AppearEnemy-Where-Arg1", targetIdx, eventInfo);
		}

		if (isInStringArray(this.ITEMLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
			callingFuncEventCentricQAWithETHT(qaList, "AppearItem-What", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			callingFuncEventCentricQAWithETHT(qaList, "AppearItem-Where-Arg1", targetIdx, eventInfo);
		}
	}

	public void getShootEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Shoot-Where", targetIdx, eventInfo);
	}

	public void getThrowEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Throw-Where", targetIdx, eventInfo);
	}

	public void getKickEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Kick-Where", targetIdx, eventInfo);
	}

	public void getHoldEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Hold-Where", targetIdx, eventInfo);
	}

	public void getEatEventCentricWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncEventCentricQAWithETHT(qaList, "Eat-Where", targetIdx, eventInfo);
		callingFuncEventCentricQAWithETHT(qaList, "Eat-What", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncEventCentricQAWithETHT(qaList, "Eat-Where-Arg1", targetIdx, eventInfo);
	}

	private void callingFuncEventCentricQAWithETHT(
			JSONArray qaList, String semanticChunk, int targetIdx, EventInfo eventInfo) {
		getEventCentricWithEasyTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo);
		getEventCentricWithHardTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo);
	}


	/* 
	 * Methods for counting QAs with temporal relationship 
	 */ 
	public JSONArray getCountingWithTemporalRelationshipExamples() {

		JSONArray QAList = new JSONArray();
		for (int ie = 0; ie < this.numEvents; ie++) {
			if (this.debugMode == this.DEBUGMODE_LEVEL2) {
				System.out.println(String.format("%d/%d event action is %s", ie + 1, this.numEvents,
						this.action[ie]));
			}
			if (this.action[ie].equals("Kill")) {
				getKillCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Die")) {
				continue;
			} else if (this.action[ie].equals("Jump")) {
				getJumpCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Hit")) {
				getHitCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Break")) {
				getBreakCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Appear")) {
				getAppearCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Shoot")) {
				getShootCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Throw")) {
				getThrowCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Kick")) {
				getKickCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Hold")) {
				getHoldCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Eat")) {
				getEatCountingWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Scene") || this.action[ie].equals("Mario")
					|| this.action[ie].equals("ENDGAME")) {
				continue;
			} else {
				throw new UnsupportedOperationException("Unknown event for "
						+ "counting question with temporal relationship: " + this.action[ie]);
			}
		}
		return QAList;
	}

	private void getCountingWithTemporalRelationshipQA(JSONArray qaList,
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo, String temporalRelationship) {
		/*
		 * Generate the counting QAs with easy temporal relationship.
		 */
		if (this.debugMode == this.DEBUGMODE_LEVEL2) {
			return;
		}
		String targetQuestionType = "Counting";
		JSONArray qaInfos = new JSONArray();
		CandidateRegion candRegion = findSimpleCandidateRegion(targetIdx, targetEventInfo, 2);
		if (candRegion.isProper()) {
			int[] candEventBeginEndIdx = getEventsInCandidateRegion(candRegion, targetIdx);

			// iterate the candidate target events
			for (int candIdx = candEventBeginEndIdx[0]; candIdx <= candEventBeginEndIdx[1]; candIdx++) {

				if (candIdx == targetIdx)
					continue;

				// semantic chunk for candidate reference event
				HashMap<String, EventInfo> candEventList = getCandidateEventInfo(candIdx);
				String[] keys = candEventList.keySet().toArray(new String[candEventList.keySet().size()]);
				for (int ik = 0; ik < keys.length; ik++) {

					ClipRegion clipRegion;
					if (candIdx > targetIdx) { // before
						clipRegion = randomSamplingClipRegionFromCandidateRegion(candRegion,
								targetIdx, NOPIVOT, this.frame[candIdx] + this.margin / 2);
					} else { // after
						clipRegion = randomSamplingClipRegionFromCandidateRegion(candRegion,
								targetIdx, this.frame[candIdx] - this.margin / 2, NOPIVOT);
					}
					

					if (clipRegion.isFindClip()) {
						assert (this.frame[candIdx] >= clipRegion.getBeginFrame() && 
								this.frame[candIdx] <= clipRegion.getEndFrame()):
							String.format("Invalid clip region %d <= %d <= %d <= %d <= %d (%d, %d)", 
									candRegion.getMinFrame(), clipRegion.getBeginFrame(), this.frame[candIdx],
									clipRegion.getEndFrame(), candRegion.getMaxFrame(), this.frame[targetIdx],
									candRegion.getClipLength());

						String candSemanticChunk = keys[ik];
						EventInfo candEventInfo = candEventList.get(candSemanticChunk);

						assert clipRegion.getBeginFrame() >= candRegion.getMinFrame(): 
							String.format("Invalid begin frame!!! (%d) < (%d)",
									clipRegion.getBeginFrame(), candRegion.getMinFrame());
						assert clipRegion.getEndFrame() <= candRegion.getMaxFrame(): 
							String.format("Invalid end frame!!! (%d) > (%d)", 
									clipRegion.getEndFrame(), candRegion.getMaxFrame());

                        /*
                         * properness check for candidate reference event. 
                         * 1. should be different with the target event 
                         * 2. should be unique in the full clip region
                         */ 
						if (this.isEqualEvents(targetEventInfo, candEventInfo))
							continue;
						int occurrenceOfCandEvent = countingEvent(clipRegion.getBeginFrame(),
								clipRegion.getEndFrame(), candEventInfo, candIdx);
						if (occurrenceOfCandEvent != 1)
							continue;
						
                        /*
                         * properness check for target event 
                         * 1. Should occur more than once in the constrained region 
                         * 2. If ET, Should not occur in the opposite region
                         * 3. If HT, Should occur more than once in the opposite region
                         */
						int occurrenceOfTargetEvent, occurrenceOfTargetInOpposite;
                        if (candIdx > targetIdx) { // before
							occurrenceOfTargetEvent = countingEvent(clipRegion.getBeginFrame(),
									this.frame[candIdx]-1, targetEventInfo, targetIdx);
							occurrenceOfTargetInOpposite = countingEvent(
									this.frame[candIdx] + 1, clipRegion.getEndFrame(),
									targetEventInfo, candIdx);

                        } else { // after
							occurrenceOfTargetEvent = countingEvent(this.frame[candIdx]+1,
									clipRegion.getEndFrame(), targetEventInfo, targetIdx);
							occurrenceOfTargetInOpposite = countingEvent(
									clipRegion.getBeginFrame(), this.frame[candIdx] - 1,
									targetEventInfo, candIdx);
                        }  
						if ( occurrenceOfTargetEvent < 2)
							continue;
						if (temporalRelationship.equals("ET")? 
								occurrenceOfTargetInOpposite != 0 : occurrenceOfTargetInOpposite < 1)
							continue;

						// obtain question
						String refQuestionType = (candIdx > targetIdx) ? 
								"Reference-Before" : "Reference-After";
						String refQst = qstTemplates.getQuestion(refQuestionType, candSemanticChunk,
								candEventInfo);
						String targetQst = qstTemplates.getQuestion(targetQuestionType,
								targetSemanticChunk, targetEventInfo);
						targetQst = targetQst.replace("?", " ");
						String qst = targetQst + refQst;

						// obtain question answer
						String ans = Integer.toString(occurrenceOfTargetEvent);

						// Obtain question semantic chunk
						String qstSemanticChunk = getQuestionSemanticChunk(targetQuestionType,
								targetSemanticChunk, targetEventInfo) + "-"
								+ getQuestionSemanticChunk(refQuestionType, candSemanticChunk,
										candEventInfo);

						// write JSONObject for QA information and append it
						// into JSONArray
						JSONObject qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans,
								targetEventInfo, candRegion.getClipLength(), targetQuestionType,
								qstSemanticChunk, temporalRelationship, "How many", targetIdx);
						if (qaInfo != null) {
							qaInfo.put("ref_frame", this.frame[candIdx]);
							qaList.add(qaInfo);
						}
					}
				}
			}
		}
	}

	private void getCountingWithEasyTemporalRelationshipQA(JSONArray qaList,
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo) {
		/* Generate the counting QAs with easy temporal relationship. */
		getCountingWithTemporalRelationshipQA(
				qaList, targetSemanticChunk, targetIdx, targetEventInfo, "ET");
	}

	private void getCountingWithHardTemporalRelationshipQA(JSONArray qaList, 
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo) {
		/* Generate the counting QAs with easy temporal relationship. */
		getCountingWithTemporalRelationshipQA(
				qaList, targetSemanticChunk, targetIdx, targetEventInfo, "HT");
	}

	public void getKillCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Kill", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncCountingQAWithETHT(qaList, "Kill-Arg1", targetIdx, eventInfo);

		// Question template with argument2 (means)
		eventInfo.setInfo(this.action[targetIdx], "None", this.means[targetIdx]);
		callingFuncCountingQAWithETHT(qaList, "Kill-Arg2", targetIdx, eventInfo);

		// Question template with argument1 and argument2 (target, means)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], this.means[targetIdx]);
		callingFuncCountingQAWithETHT(qaList, "Kill-Arg1-Arg2", targetIdx, eventInfo);
	}

	public void getJumpCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Jump", targetIdx, eventInfo);
	}

	public void getHitCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Hit", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncCountingQAWithETHT(qaList, "Hit-Arg1", targetIdx, eventInfo);
	}

	public void getBreakCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Break", targetIdx, eventInfo);
	}

	public void getAppearCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		EventInfo eventInfo;

		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
			callingFuncCountingQAWithETHT(qaList, "AppearEnemy", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			callingFuncCountingQAWithETHT(qaList, "AppearEnemy-Arg1", targetIdx, eventInfo);
		}

		if (isInStringArray(this.ITEMLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
			callingFuncCountingQAWithETHT(qaList, "AppearItem", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			callingFuncCountingQAWithETHT(qaList, "AppearItem-Arg1", targetIdx, eventInfo);
		}
	}

	public void getShootCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Shoot", targetIdx, eventInfo);
	}

	public void getThrowCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Throw", targetIdx, eventInfo);
	}

	public void getKickCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Kick", targetIdx, eventInfo);
	}

	public void getHoldCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Hold", targetIdx, eventInfo);
	}

	public void getEatCountingWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncCountingQAWithETHT(qaList, "Eat", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncCountingQAWithETHT(qaList, "Eat-Arg1", targetIdx, eventInfo);
	}

	private void callingFuncCountingQAWithETHT(
			JSONArray qaList, String semanticChunk, int targetIdx, EventInfo eventInfo) {
		getCountingWithEasyTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo);
		getCountingWithHardTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo);
	}
	
	/* 
	 * Methods for state QAs with temporal relationship 
	 */
	public JSONArray getStateWithTemporalRelationshipExamples() {

		JSONArray QAList = new JSONArray();
		for (int ie = 0; ie < this.numEvents; ie++) {
			if (this.debugMode == this.DEBUGMODE_LEVEL2) {
				System.out.println(String.format("%d/%d event action is %s", ie + 1, this.numEvents,
						this.action[ie]));
			}
			if (this.action[ie].equals("Kill")) {
				getKillStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Die")) {
				getDieStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Jump")) {
				getJumpStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Hit")) {
				getHitStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Break")) {
				getBreakStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Appear")) {
				getAppearStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Shoot")) {
				getShootStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Throw")) {
				getThrowStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Kick")) {
				getKickStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Hold")) {
				getHoldStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Eat")) {
				getEatStateWithTemporalRelationshipQA(QAList, ie);
			} else if (this.action[ie].equals("Scene") || this.action[ie].equals("Mario")
					|| this.action[ie].equals("ENDGAME")) {
				continue;
			} else {
				throw new UnsupportedOperationException("Unknown event for "
						+ "state question with temporal relationship: " + this.action[ie]);
			}
		}
		return QAList;
	}

	private void getStateWithWhenTemporalRelationshipQA(JSONArray qaList, 
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo, String state) {

		String questionType = "State";
		JSONObject qaInfo = null;
		CandidateRegion candRegion = this.findCandidateRegionForUniqueEvent(targetIdx, targetEventInfo);
		if (candRegion.isProper()) {

			ClipRegion clipRegion = randomSamplingClipRegionFromCandidateRegion(
					candRegion, targetIdx, NOPIVOT, NOPIVOT);

			if (clipRegion.isFindClip()) {

				String[] status;
				if (state.equals("Mario-Status")) {
					status = this.marioState;
				} else if (state.equals("Scene-Status")) {
					status = this.sceneState;
				} else {
					throw new UnsupportedOperationException(
							"Unknown semanticChunk for state: " + state);
				}
				
				// If the state is not same over the full clip region, we do not generate QA.
				String initStatus = status[clipRegion.getBeginFrame()];
				for (int fr=clipRegion.getBeginFrame(); fr<=clipRegion.getEndFrame(); fr++) {
					if (!initStatus.equals(status[fr]))
						return;
				}

				// obtain question
				String targetQst = qstTemplates.getQuestion("Reference-When", targetSemanticChunk, targetEventInfo);
				String stateQst = qstTemplates.getQuestion(questionType, state, null);
				stateQst = stateQst.replace("?", " ");
				String qst = stateQst + targetQst;

				// obtain question answer
				String ans = status[targetIdx];

				// Obtain question semantic chunk
				String qstSemanticChunk = getQuestionSemanticChunk(questionType, state, null) + "-"
						+ getQuestionSemanticChunk("Reference-When", targetSemanticChunk,
								targetEventInfo);

				// write JSONObject for QA information and append it into JSONArray
				qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans,
						targetEventInfo, candRegion.getClipLength(), questionType,
						qstSemanticChunk, "ET", "What state", targetIdx);
			}
		}
		if (qaInfo != null)
			qaList.add(qaInfo);
	}
	
	private void getStateWithTemporalRelationshipQA(JSONArray qaList, String targetSemanticChunk,
			int targetIdx, EventInfo targetEventInfo, String temporalRelationship) {
		
		String questionType = "State";
		CandidateRegion candRegion = this.findCandidateRegionForUniqueEvent(targetIdx, targetEventInfo);
		
		if (candRegion.isProper()) {
			
	        ClipRegion clipRegion = randomSamplingClipRegionFromCandidateRegion(
                    candRegion, targetIdx, NOPIVOT, NOPIVOT);
                
            if (clipRegion.isFindClip()) {
                            
            	for (int tt=0; tt<2; tt++) {
            		// if tt==0 before question, if tt==1 after question.
					String prevStatus = this.marioState[clipRegion.getBeginFrame()];
					String postStatus = this.marioState[clipRegion.getEndFrame()];

					/*
					 * Properness check.
					 * 1. check that the state of Mario is same in the constrained region.
					 * 2. if HT, the state should be different in the opposite region.
					 */
					int sFrameInConstrainedRegion, eFrameInConstrainedRegion;
					int sFrameInOppositeRegion, eFrameInOppositeRegion;
					if (tt == 0) { // before
						sFrameInConstrainedRegion = clipRegion.getBeginFrame();
						eFrameInConstrainedRegion = this.frame[targetIdx];
						sFrameInOppositeRegion = this.frame[targetIdx] + 1;
						eFrameInOppositeRegion = clipRegion.getEndFrame();
					} else { // after 
						sFrameInConstrainedRegion = this.frame[targetIdx];
						eFrameInConstrainedRegion = clipRegion.getEndFrame();
						sFrameInOppositeRegion = clipRegion.getBeginFrame();
						eFrameInOppositeRegion = this.frame[targetIdx] - 1;
					}

					String stateInConstrainedRegion = this.marioState[sFrameInConstrainedRegion];
					String stateInOppositeRegion = this.marioState[sFrameInOppositeRegion];
					boolean properInConstrained = true;
					for (int fr=sFrameInConstrainedRegion; fr<=eFrameInConstrainedRegion; fr++) {
						if (!stateInConstrainedRegion.equals(this.marioState[fr])) {
							properInConstrained = false;
							break;
						}
					}
					if (!properInConstrained) continue;
					boolean properInOpposite;
					if (temporalRelationship.equals("ET")) {
						properInOpposite = true;
						for (int fr=sFrameInOppositeRegion; fr<=eFrameInOppositeRegion; fr++) {
							if (!stateInConstrainedRegion.equals(this.marioState[fr])) {
								properInOpposite = false;
								break;
							}
						}

					} else {
						properInOpposite = false;
						for (int fr=sFrameInOppositeRegion; fr<=eFrameInOppositeRegion; fr++) {
							if (!stateInConstrainedRegion.equals(this.marioState[fr])) {
								properInOpposite = true;
								break;
							}
						}
					}
					
					if (properInOpposite) {
						// obtain question
						String semanticChunk = tt == 0 ? "Reference-Before" : "Reference-After";
						String targetQst = qstTemplates.getQuestion(semanticChunk,
								targetSemanticChunk, targetEventInfo);
						String stateQst = qstTemplates.getQuestion(questionType, "Mario-Status",
								null);
						stateQst = stateQst.replace("?", " ");
						String qst = stateQst + targetQst;
						// obtain answer
						String ans = this.marioState[sFrameInConstrainedRegion];
						// Obtain question semantic chunk
						String qstSemanticChunk = getQuestionSemanticChunk(questionType, "Mario-Status", null) + "-"
								+ getQuestionSemanticChunk(semanticChunk, targetSemanticChunk, targetEventInfo);

						JSONObject qaInfo = constructJSONObjectOfQAInfomation(clipRegion, qst, ans,
								targetEventInfo, candRegion.getClipLength(), questionType,
								qstSemanticChunk, temporalRelationship, "What state", targetIdx);
						if (qaInfo != null)
							qaList.add(qaInfo);
					}
            	}
            }
		}
	}

	private void getStateWithEasyTemporalRelationshipQA(JSONArray qaList, 
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo) {
		/* Generate the event-centric QAs with easy temporal relationship. */
		getStateWithTemporalRelationshipQA(qaList, targetSemanticChunk, targetIdx, targetEventInfo, "ET");
	}

	private void getStateWithHardTemporalRelationshipQA(JSONArray qaList, 
			String targetSemanticChunk, int targetIdx, EventInfo targetEventInfo) {
		/* Generate the event-centric QAs with hard temporal relationship. */
		getStateWithTemporalRelationshipQA(qaList, targetSemanticChunk, targetIdx, targetEventInfo, "HT");
	}

	public void getKillStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Kill", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncStateQAWithETHT(qaList, "Kill-Arg1", targetIdx, eventInfo);

		// Question template with argument2 (means)
		eventInfo.setInfo(this.action[targetIdx], "None", this.means[targetIdx]);
		callingFuncStateQAWithETHT(qaList, "Kill-Arg2", targetIdx, eventInfo);

		// Question template with argument1 and argument2 (target, means)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], this.means[targetIdx]);
		callingFuncStateQAWithETHT(qaList, "Kill-Arg1-Arg2", targetIdx, eventInfo);
	}

	public void getDieStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Die", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			callingFuncStateQAWithETHT(qaList, "DieEnemy-Arg1", targetIdx, eventInfo);
		} else if (this.target[targetIdx].equals("Shell")) {
			callingFuncStateQAWithETHT(qaList, "DieShell-Arg1", targetIdx, eventInfo);
		}
	}

	public void getJumpStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		/* Generate state with temporal relationship QAs for Jump event */

		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Jump", targetIdx, eventInfo);
	}

	public void getHitStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Hit", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncStateQAWithETHT(qaList, "Hit-Arg1", targetIdx, eventInfo);
	}

	public void getBreakStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Break", targetIdx, eventInfo);
	}

	public void getAppearStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		EventInfo eventInfo;

		if (isInStringArray(this.ENEMYLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
			callingFuncStateQAWithETHT(qaList, "AppearEnemy", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			callingFuncStateQAWithETHT(qaList, "AppearEnemy-Arg1", targetIdx, eventInfo);
		}

		if (isInStringArray(this.ITEMLIST, this.target[targetIdx])) {
			// Question template without arguments
			eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
			callingFuncStateQAWithETHT(qaList, "AppearItem", targetIdx, eventInfo);

			// Question template with argument1 (target)
			eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
			callingFuncStateQAWithETHT(qaList, "AppearItem-Arg1", targetIdx, eventInfo);
		}
	}

	public void getShootStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Shoot", targetIdx, eventInfo);
	}

	public void getThrowStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Throw", targetIdx, eventInfo);
	}

	public void getKickStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Kick", targetIdx, eventInfo);
	}

	public void getHoldStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Hold", targetIdx, eventInfo);
	}

	public void getEatStateWithTemporalRelationshipQA(JSONArray qaList, int targetIdx) {
		// Question template without arguments
		EventInfo eventInfo = new EventInfo(this.action[targetIdx], "None", "None");
		callingFuncStateQAWithETHT(qaList, "Eat", targetIdx, eventInfo);

		// Question template with argument1 (target)
		eventInfo.setInfo(this.action[targetIdx], this.target[targetIdx], "None");
		callingFuncStateQAWithETHT(qaList, "Eat-Arg1", targetIdx, eventInfo);
	}
	
	private void callingFuncStateQAWithETHT(JSONArray qaList, String semanticChunk, int targetIdx, EventInfo eventInfo) {
		
		getStateWithWhenTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo, "Mario-Status");
		getStateWithWhenTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo, "Scene-Status");
		getStateWithEasyTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo);
		getStateWithHardTemporalRelationshipQA(qaList, semanticChunk, targetIdx, eventInfo);
	}

	/* Getter and Setter methods */
	public String getEventLogFilePath() {
		return eventLogFilePath;
	}

	public void setEventLogFilePath(String eventLogFilePath) {
		this.eventLogFilePath = eventLogFilePath;
	}

	public int getNumEvents() {
		return numEvents;
	}

	public void setNumEvents(int numEvents) {
		this.numEvents = numEvents;
	}

	public int getNumFrames() {
		return numFrames;
	}

	public void setNumFrames(int numFrames) {
		this.numFrames = numFrames;
	}

	public int[] getFrame() {
		return frame;
	}

	public void setFrame(int[] frame) {
		this.frame = frame;
	}

	public String[] getMarioState() {
		return marioState;
	}

	public void setMarioState(String[] marioState) {
		this.marioState = marioState;
	}

	public String[] getSceneState() {
		return sceneState;
	}

	public void setSceneState(String[] sceneState) {
		this.sceneState = sceneState;
	}

	public String[] getMarioLocation() {
		return marioLocation;
	}

	public void setMarioLocation(String[] marioLocation) {
		this.marioLocation = marioLocation;
	}

	public String[] getSpriteLocation() {
		return spriteLocation;
	}

	public void setSpriteLocation(String[] spriteLocation) {
		this.spriteLocation = spriteLocation;
	}

	public String[] getAction() {
		return action;
	}

	public void setAction(String[] action) {
		this.action = action;
	}

	public String[] getTarget() {
		return target;
	}

	public void setTarget(String[] target) {
		this.target = target;
	}

	public String[] getMeans() {
		return means;
	}

	public void setMeans(String[] means) {
		this.means = means;
	}

	public String getVideoPath() {
		return videoPath;
	}

	public void setVideoPath(String videoPath) {
		this.videoPath = videoPath;
	}

	public String getVideoFile() {
		return videoFile;
	}

	public void setVideoFile(String videoFile) {
		this.videoFile = videoFile;
	}
}
