package com.postechCVlab.MarioQA;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;
import org.apache.commons.cli.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class QApairsGeneration {

	private static final Options options = new Options();
	private static int debugMode = EventLogs.DEBUGMODE_LEVEL0;
	private static int[] threshold = new int[]{300, 5, 20};
	private static String[] dataType = new String[]{"NT", "ET", "HT"};

	public static void main(String[] args) throws Exception {

		addCmdOptions();
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
			printObtainedOptions(cmd);
			if (cmd.hasOption("h"))
				HelpCmdline();
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			HelpCmdline();
			System.exit(1);
			return;
		}
		if (cmd.getOptionValue("l") == null) {
			System.out.println("Not provided log path");
		}
		if (cmd.getOptionValue("d") != null)
			setDebugMode(cmd.getOptionValue("d")); 
		System.out.println("");

		/* Obtain options from command line */
		String logListPath = cmd.getOptionValue("l");
		String configPath = cmd.getOptionValue("c");
		String qstTemplatePath = cmd.getOptionValue("q");
		String phrasePath = cmd.getOptionValue("p");
		
		/* get the log files and generate QAs */
		JSONArray generatedQAs = new JSONArray();
		try {
			BufferedReader br = new BufferedReader (new FileReader(logListPath));
			String logFile;
			
			while ((logFile= br.readLine()) != null) {
				System.out.println("=> Start generating QAs for " + logFile);
				EventLogs eventLogs = new EventLogs(logFile, configPath, qstTemplatePath, phrasePath, debugMode);
				JSONArray qas;
				JSONArray curQAs = new JSONArray();

				// Attach the event-centric QAs with no temporal relationship
				qas = eventLogs.getEventCentricExamples();
				for (int i=0; i<qas.size(); i++) {
					generatedQAs.add(qas.get(i));
					curQAs.add(qas.get(i));
				}

				// Attach the counting QAs with no temporal relationship
				qas = eventLogs.getCountingExamples();
				for (int i=0; i<qas.size(); i++) {
					generatedQAs.add(qas.get(i));
					curQAs.add(qas.get(i));
				}

				// Attach the counting QAs with no temporal relationship
				qas = eventLogs.getStateExamples(generatedQAs.size()/10);
				for (int i=0; i<qas.size(); i++) {
					generatedQAs.add(qas.get(i));
					curQAs.add(qas.get(i));
				}
				
				// Attach the event-centric QAs with temporal relationship
				qas = eventLogs.getEventCentricWithTemporalRelationshipExamples();
				for (int i=0; i<qas.size(); i++) {
					generatedQAs.add(qas.get(i));
					curQAs.add(qas.get(i));
				}

				// Attach the counting QAs with temporal relationship
				qas = eventLogs.getCountingWithTemporalRelationshipExamples();
				for (int i=0; i<qas.size(); i++) {
					generatedQAs.add(qas.get(i));
					curQAs.add(qas.get(i));
				}

				// Attach the counting QAs with temporal relationship
				qas = eventLogs.getStateWithTemporalRelationshipExamples();
				for (int i=0; i<qas.size(); i++) {
					generatedQAs.add(qas.get(i));
					curQAs.add(qas.get(i));
				}

				generatedQAs = filteringQAs(generatedQAs, false);

				String saveTo = String.format("generated_annotation/%s_raw_annotations.json", eventLogs.getVideoFile());
				writeJSONFile(curQAs, saveTo);
				System.out.println(String.format("====> Generated QAs (%d) for %s are saved in %s (# of filtered: %d)",
						curQAs.size(), eventLogs.getVideoFile(), saveTo, generatedQAs.size()));
			}
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		generatedQAs = filteringQAs(generatedQAs, true);
	}
	
	private static void addCmdOptions() {
		/* Obtain options from command line */
		Option llp = new Option("l", "logListPath", true, "path to log of events in Mario gameplay");
		llp.setRequired(true);
		options.addOption(llp);

		Option conf = new Option("c", "confFile", true, "path to configuration file");
		conf.setRequired(true);
		options.addOption(conf);

		Option qtp = new Option("q", "questionTemplatePath", true, "path to configuration file");
		qtp.setRequired(true);
		options.addOption(qtp);

		Option pp = new Option("p", "phrasePath", true, "path to configuration file");
		pp.setRequired(true);
		options.addOption(pp);

		Option debug = new Option("d", "debugMode", true, "debug mode [lev0|lev1|lev2]");
		debug.setRequired(true);
		options.addOption(debug);

		options.addOption("h", "help", false, "print this message");
	}

	private static void HelpCmdline() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("Generation QA pairs from event logs", options);
	}
	
	private static void printObtainedOptions(CommandLine cmd) {
		String printFormat = "%s : %s";
		System.out.println("The parameters are as follows: ");
		System.out.println(String.format(printFormat, "logListPath    ", cmd.getOptionValue("l")));
		System.out.println(String.format(printFormat, "confFile       ", cmd.getOptionValue("c")));
		System.out.println(String.format(printFormat, "qstTemplatePath", cmd.getOptionValue("q")));
		System.out.println(String.format(printFormat, "phrasePath     ", cmd.getOptionValue("p")));
	}

	public static void setDebugMode(String dm) {
		if (dm.equals("lev1")) {
			debugMode = EventLogs.DEBUGMODE_LEVEL1;
		} else if (dm.equals("lev2")) {
			debugMode = EventLogs.DEBUGMODE_LEVEL2;
		} else {
			debugMode = EventLogs.DEBUGMODE_LEVEL0;
		}
		System.out.println(String.format("%s : %s", "debugMode      ", dm));
	}
	
	public static JSONArray filteringQAs(JSONArray anns, boolean lastTime) {

		// Initialize variables
		int numDataType = dataType.length;
		int[] numQAs = new int[numDataType+1];
		Map<String, JSONArray> annMap = new HashMap<String, JSONArray>();
		for (int idt=0; idt<numDataType; idt++) {
			annMap.put(dataType[idt], new JSONArray());
		}

		// Sorting the annotations in terms of temporal relationship difficulty
		for (int ia=0; ia<anns.size(); ia++) {
			JSONObject ann = (JSONObject) anns.get(ia);

			if ( ((String)ann.get("temporal_relationship")).equals("NT") ) {
				annMap.get("NT").add(ann);
			} else if ( ((String)ann.get("temporal_relationship")).equals("ET") ) {
				annMap.get("ET").add(ann);
			} else {
				annMap.get("HT").add(ann);
			}
		}

		// print the number of QAs before being filtered out
		if (lastTime) {
			System.out.println("\n********************************************************");
			for (int idt = 0; idt < numDataType; idt++) {
				System.out.println(String.format("# of raw QAs with %s: %d", dataType[idt],
						annMap.get(dataType[idt]).size()));
			}
		}

        // Filtering out the QAs for each data type (NT, ET, HT)         
		int qid = 1;
		String key;
		String keyFormat = "%s-Answer-%s";
		JSONObject ann;
		JSONArray curAnns;
		JSONArray allJson = new JSONArray();
		Map<String, Integer> allQAPairCount = new HashMap<String, Integer>();
		for (int idt=0; idt<numDataType; idt++) {
			if (lastTime) System.out.println("********************************************************");
			System.out.print(String.format(
					"=> Filtering out for %s ... ", dataType[idt]));
			Map<String, JSONArray> annListForEachQAPair= new HashMap<String, JSONArray>();
			Map<String, Integer> qaPairCount = new HashMap<String, Integer>();
			JSONArray outJson = new JSONArray();

			// Counting and attach annotation along pair of (semanticChunk,answer)
			curAnns = annMap.get(dataType[idt]);
			for (int ia=0; ia<curAnns.size(); ia++) {
				ann = (JSONObject) curAnns.get(ia);
				key = String.format(keyFormat, (String) ann.get("semantic_chunk"), (String) ann.get("answer"));

				qaPairCount.put(key, qaPairCount.containsKey(key)? qaPairCount.get(key)+1 : 1);
				allQAPairCount.put(key, allQAPairCount.containsKey(key)? allQAPairCount.get(key)+1 : 1);
				if (!annListForEachQAPair.containsKey(key)) {
					annListForEachQAPair.put(key, new JSONArray());
				}
				annListForEachQAPair.get(key).add(ann);
			}

			// Obtain final annotations where each key has "threshold" numbers at most.
			String[] keyList = qaPairCount.keySet().toArray(new String[qaPairCount.keySet().size()]);
			for (int ik=0; ik<keyList.length; ik++) {
				key = keyList[ik];

				if (qaPairCount.get(key) <= threshold[idt]) {
					for (int n=0; n<qaPairCount.get(key); n++){
						JSONObject tmpAnn = (JSONObject) annListForEachQAPair.get(key).get(n);
						if (lastTime) {
							tmpAnn.put("qa_id", qid++);
						}
						outJson.add(tmpAnn);
						allJson.add(tmpAnn);
					}
				} else {
					Integer[] randIdx = getShuffledIndex(annListForEachQAPair.get(key).size());
					for (int n=0; n<threshold[idt]; n++){
						JSONObject tmpAnn = (JSONObject) annListForEachQAPair.get(key).get(randIdx[n]);
						if (lastTime) {
							tmpAnn.put("qa_id", qid++);
						}
						outJson.add(tmpAnn);
						allJson.add(tmpAnn);
					}
				}
			}
			
			System.out.println("done");
			if (lastTime) {
				// write information
				numQAs[idt] = outJson.size();
				writeJSONFile(outJson, String.format(
						"generated_annotation/filtered_annotations_%s.json", dataType[idt]));
				System.out.println("==> Theshold: " + String.valueOf(threshold[idt]));
				System.out.println(String.format(
						"===> %s saved in generated_annotation/filtered_annotations_%s.json",
						dataType[idt], dataType[idt]));
			}
		}
		if (lastTime) {
			System.out.println("********************************************************");
			String[] keyList = allQAPairCount.keySet()
					.toArray(new String[allQAPairCount.keySet().size()]);
			System.out.println(String.format("# of unique QA pairs: %d", keyList.length));
			numQAs[numDataType] = 0;
			for (int idt = 0; idt < numDataType; idt++) {
				numQAs[numDataType] += numQAs[idt];
				System.out.println(
						String.format("# of filtered QAs with %s: %d", dataType[idt], numQAs[idt]));
			}
			System.out.println(String.format("# of filtered all QAs: %d", numQAs[numDataType]));
			writeJSONFile(allJson, "generated_annotation/filtered_annotations_ALL.json");
			System.out.println("ALL saved in generated_annotation/filtered_annotations_ALL.json");
			System.out.println("********************************************************");
		}
		return allJson;
	}
	
	private static void writeJSONFile(JSONArray jsonData, String saveTo) {
		try {
			FileWriter fw = new FileWriter(saveTo);
			fw.write(jsonData.toJSONString());
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Integer[] getShuffledIndex(int maxNum) {
		Integer[] arrIdx = new Integer[maxNum];
		for (int n=0; n<maxNum; n++)
			arrIdx[n] = n;

		Collections.shuffle(Arrays.asList(arrIdx));
		return arrIdx;
	}
}
