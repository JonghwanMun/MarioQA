package com.postechCVlab.MarioQA;

import java.util.Random;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class QuestionTemplates {
	
	private JSONObject templates;
	private JSONObject phrases;
	private int debugMode;

	public static Random random = new Random();

	public QuestionTemplates(String templateFilePath, String phrasesFilePath, int debugMode) {
		
		this.debugMode = debugMode;
		JSONParser parser = new JSONParser();
		try {
			templates= (JSONObject) parser.parse(new FileReader(templateFilePath));
			phrases = (JSONObject) parser.parse(new FileReader(phrasesFilePath));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getAnswer(String argument, String form) {
		if (this.debugMode == EventLogs.DEBUGMODE_LEVEL1) 
			System.out.println(argument + "    " + form);
		String ans = (String) ((JSONObject) this.phrases.get(argument)).get(form);
		return ans;
	}

	public String[] getAllQuestions(String questionType, String semanticChunk, EventInfo eventInfo) {
		JSONArray templateList = (JSONArray) ((JSONObject) this.templates.get(questionType)).get(semanticChunk);
		int numCandidateQuestions = templateList.size();
		
		String[] qstList = new String[numCandidateQuestions];
		for (int i=0; i<numCandidateQuestions; i++) {
			String template = (String) templateList.get(i);
			qstList[i] = fillQuestionTemplate(template, eventInfo);
		}
		
		return qstList;
	}

	public String getQuestion(String questionType, String semanticChunk, EventInfo eventInfo) {
		String template = "";
		try {
		JSONArray templateList = (JSONArray) ((JSONObject) this.templates.get(questionType)).get(semanticChunk);
		if (templateList == null) {
			boolean debugTimeing = true;
		}
		int numCandidateQuestions = templateList.size();
		template = (String) templateList.get(random.nextInt(numCandidateQuestions));
		} catch (Exception e) {
			System.out.println(questionType + " " + semanticChunk + eventInfo.toString());
		} finally {
		}
		
		return fillQuestionTemplate(template, eventInfo);
	}
	
	public String fillQuestionTemplate(String template, EventInfo eventInfo) {
		/* If eventInfo is not null, filling the question template 
		 * Basically question templates belong to following forms
		 *   - Mario %v% target
		 *   - target is %pp% (%arg1*%) (%arg2*%) (by Mario)
		 *   - target %gpp% (%arg1*%) (%arg2*%) (by Mario)
		 *   - target %ov% (%arg1*%) (%arg2*%) (by Mario) 
		 */
		
		if (eventInfo == null) {
			return template;
		}
		else {
			// Filling the verb related with the target action
			if (template.contains("%v%")) { // verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("v");
				template = replaceTemplateWord(template, "%v%", tmpArray);
			}
			if (template.contains("%v-p%")) { // past verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("v-p");
				template = replaceTemplateWord(template, "%v-p%", tmpArray);
			}
			if (template.contains("%v-pp%")) { // past participle verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("v-pp");
				template = replaceTemplateWord(template, "%v-pp%", tmpArray);
			}
			if (template.contains("%v-ing%")) { // past participle verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("v-ing");
				template = replaceTemplateWord(template, "%v-ing%", tmpArray);
			}
			if (template.contains("%gpp%")) { // get past participle verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("gpp");
				template = replaceTemplateWord(template, "%gpp%", tmpArray);
			}
			if (template.contains("%gpp-p%")) { // get past participle verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("gpp-p");
				template = replaceTemplateWord(template, "%gpp-p%", tmpArray);
			}
			if (template.contains("%ov%")) { // object verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("ov");
				template = replaceTemplateWord(template, "%ov%", tmpArray);
			}
			if (template.contains("%ovpp%")) { // object past participle verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("ovpp");
				template = replaceTemplateWord(template, "%ovpp%", tmpArray);
			}
			if (template.contains("%sn%")) { // singular noun
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("sn");
				template = replaceTemplateWord(template, "%sn%", tmpArray);
			}
			if (template.contains("%pn%")) { // plural noun
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("pn");
				template = replaceTemplateWord(template, "%pn%", tmpArray);
			}
			if (template.contains("%ve%")) { // verb for the appear action with
												// enemy
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("ve");
				template = replaceTemplateWord(template, "%ve%", tmpArray);
			}
			if (template.contains("%ve-p%")) { // past verb for the appear action with enemy
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("ve-p");
				template = replaceTemplateWord(template, "%ve-p%", tmpArray);
			}
			if (template.contains("%ve-pp%")) { // pp for the appear action with enemy
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getAction())).get("ve-pp");
				template = replaceTemplateWord(template, "%ve-pp%", tmpArray);
			}
			// Filling the argument1 related with the target of action
			if (template.contains("%arg1sn%")) { // argument1 -> singular noun
				template = template.replace("%arg1sn%",
						(String) ((JSONObject) this.phrases.get(eventInfo.getTarget()))
								.get("arg1sn"));
			}
			if (template.contains("%arg1pn%")) { // argument1 -> plural noun
				template = template.replace("%arg1pn%",
						(String) ((JSONObject) this.phrases.get(eventInfo.getTarget()))
								.get("arg1pn"));
			}
			if (template.contains("%arg1v%")) { // argument1 -> verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getTarget())).get("arg1v");
				template = replaceTemplateWord(template, "%arg1v%", tmpArray);
			}
			if (template.contains("%arg1v-p%")) { // argument1 -> verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getTarget())).get("arg1v-p");
				template = replaceTemplateWord(template, "%arg1v-p%", tmpArray);
			}
			if (template.contains("%arg1p%")) { // argument1 -> phrase
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getTarget())).get("arg1p");
				template = replaceTemplateWord(template, "%arg1p%", tmpArray);
			}
			// Filling the argument2 related with the means of action
			if (template.contains("%arg2sn%")) { // argument2 -> singular noun
				template = template.replace("%arg2sn%",
						(String) ((JSONObject) this.phrases.get(eventInfo.getMeans()))
								.get("arg2sn"));
			}
			if (template.contains("%arg2pn%")) { // argument2 -> plural noun
				template = template.replace("%arg2pn%",
						(String) ((JSONObject) this.phrases.get(eventInfo.getMeans()))
								.get("arg2pn"));
			}
			if (template.contains("%arg2v%")) { // argument2 -> verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getMeans())).get("arg2v");
				template = replaceTemplateWord(template, "%arg2v%", tmpArray);
			}
			if (template.contains("%arg2v-p%")) { // argument2 -> past participle verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getMeans())).get("arg2v-p");
				template = replaceTemplateWord(template, "%arg2v-p%", tmpArray);
			}
			if (template.contains("%arg2v-pp%")) { // argument2 -> past participle verb
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getMeans())).get("arg2v-pp");
				template = replaceTemplateWord(template, "%arg2v-pp%", tmpArray);
			}
			if (template.contains("%arg2p%")) { // argument2 -> phrase
				JSONArray tmpArray = (JSONArray) ((JSONObject) this.phrases
						.get(eventInfo.getMeans())).get("arg2p");
				template = replaceTemplateWord(template, "%arg2p%", tmpArray);
			}
			return template;
		}
	}

	public String replaceTemplateWord(String template, String targetWord, JSONArray replacementList) {
		String output;
		if (this.debugMode == EventLogs.DEBUGMODE_LEVEL2 && replacementList.size() > 1) {
			output = template.replaceAll(targetWord, (String) replacementList.get(0));
			for (int i=1; i<replacementList.size(); i++) {
				output += ("\n" + template.replaceAll(targetWord, (String) replacementList.get(i)));
			}
		}
		else {
			output = template.replace(targetWord, samplingArray(replacementList));
		}
		return output;
	}

	public String samplingArray(JSONArray tmpArray) {
		return (String) tmpArray.get(random.nextInt(tmpArray.size()));
	}

	public JSONObject getTemplates() {
		return templates;
	}

	public void setTemplates(JSONObject templates) {
		this.templates = templates;
	}

	public JSONObject getPhrases() {
		return phrases;
	}

	public void setPhrases(JSONObject phrases) {
		this.phrases = phrases;
	}
}
