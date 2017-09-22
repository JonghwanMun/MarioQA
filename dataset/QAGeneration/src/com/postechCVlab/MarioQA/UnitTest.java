package com.postechCVlab.MarioQA;

import com.postechCVlab.MarioQA.returnObjects.*;
import org.json.simple.JSONArray;                                                                                                    
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class UnitTest {

	public static QuestionTemplates qstTemplates;
	public static void main(String[] args) {
		
		//test1FillingTemplate(); 	// test 1
		
		/* test 2
		JSONArray tmp = new JSONArray();
		test2Reference(tmp);
		System.out.println(tmp.toString());
		*/
		
		int ii=0;
		for (int i=0; i<3; i++) {
			System.out.println(++ii);
		}
	}
	
	public static void test2Reference(JSONArray jArr) {
		JSONObject tmp = new JSONObject();
		tmp.put("test1", "1");
		tmp.put("test2", "2");
		jArr.add(tmp);
	}
	
	public static void test1FillingTemplate() {
		qstTemplates = new QuestionTemplates("templates.json", "phrases.json", EventLogs.DEBUGMODE_LEVEL1);

		// Event-Centric questions 
		String questionType = "Event-Centric";
		String semanticChunk;
		EventInfo eventInfo;

		/*
		// Kill event
		semanticChunk = "Kill-When";
		EventInfo eventInfo = new EventInfo("Kill", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Where";
		eventInfo = new EventInfo("Kill", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Kill-How";
		eventInfo = new EventInfo("Kill", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Who";
		eventInfo = new EventInfo("Kill", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-When-Arg1";
		eventInfo = new EventInfo("Kill", "RedKoopa", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Where-Arg1";
		eventInfo = new EventInfo("Kill", "RedKoopa", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Kill-How-Arg1";
		eventInfo = new EventInfo("Kill", "RedKoopa", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Kill-When-Arg2";
		eventInfo = new EventInfo("Kill", "None", "Fireball");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Where-Arg2";
		eventInfo = new EventInfo("Kill", "None", "Fireball");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Who-Arg2";
		eventInfo = new EventInfo("Kill", "None", "Fireball");
		printQuestions(questionType, semanticChunk, eventInfo);
		*/

		/*
		// Die event
		semanticChunk = "Die-When";
		eventInfo = new EventInfo("Die", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Die-Where";
		eventInfo = new EventInfo("Die", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Die-How";
		eventInfo = new EventInfo("Die", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Die-Who";
		eventInfo = new EventInfo("Die", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieEnemy-When-Arg1";
		eventInfo = new EventInfo("Die", "RedKoopa", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieEnemy-Where-Arg1";
		eventInfo = new EventInfo("Die", "RedKoopa", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieFalling-When-Arg1";
		eventInfo = new EventInfo("Die", "Falling", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieShell-When-Arg1";
		eventInfo = new EventInfo("Die", "Shell", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieShell-Where-Arg1";
		eventInfo = new EventInfo("Die", "Shell", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		*/
		
		/*
		// Jump
		semanticChunk = "Jump-When";
		eventInfo = new EventInfo("Jump", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Jump-Where";
		eventInfo = new EventInfo("Jump", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		*/
		
		/*
		// Hit
		semanticChunk = "Hit-When";
		eventInfo = new EventInfo("Hit", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Hit-Where";
		eventInfo = new EventInfo("Hit", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Hit-What";
		eventInfo = new EventInfo("Hit", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Hit-When-Arg1";
		eventInfo = new EventInfo("Hit", "Coin Block", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Hit-Where-Arg1";
		eventInfo = new EventInfo("Hit", "Fireflower Block", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		*/
		
		/*
		// Break
		semanticChunk = "Break-When";
		eventInfo = new EventInfo("Break", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Break-Where";
		eventInfo = new EventInfo("Break", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		// Appear
		semanticChunk = "AppearEnemy-Who";
		eventInfo = new EventInfo("Appear", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearEnemy-When-Arg1";
		eventInfo = new EventInfo("Appear", "GreenKoopa", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearEnemy-Where-Arg1";
		eventInfo = new EventInfo("Appear", "BulletBill", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearItem-What";
		eventInfo = new EventInfo("Appear", "Mushroom", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearItem-Where-Arg1";
		eventInfo = new EventInfo("Appear", "Mushroom", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearItem-When-Arg1";
		eventInfo = new EventInfo("Appear", "Fireflower", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		// Shoot
		semanticChunk = "Shoot-When";
		eventInfo = new EventInfo("Shoot", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Shoot-Where";
		eventInfo = new EventInfo("Shoot", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		// Throw 
		semanticChunk = "Throw-When";
		eventInfo = new EventInfo("Throw", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Throw-Where";
		eventInfo = new EventInfo("Throw", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		// Kick
		semanticChunk = "Kick-When";
		eventInfo = new EventInfo("Kick", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Kick-Where";
		eventInfo = new EventInfo("Kick", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		// Hold
		semanticChunk = "Hold-When";
		eventInfo = new EventInfo("Hold", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Hold-Where";
		eventInfo = new EventInfo("Hold", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		// Eat
		semanticChunk = "Eat-When";
		eventInfo = new EventInfo("Eat", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Eat-Where";
		eventInfo = new EventInfo("Eat", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Eat-What";
		eventInfo = new EventInfo("Eat", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Eat-When-Arg1";
		eventInfo = new EventInfo("Eat", "Coin", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		
		semanticChunk = "Eat-Where-Arg1";
		eventInfo = new EventInfo("Eat", "Mushroom", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
		*/
		

		questionType = "Reference-Before";
		//questionType = "Reference-After";

		semanticChunk = "Kill";
		eventInfo = new EventInfo("Kill", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Arg1";
		eventInfo = new EventInfo("Kill", "Flower", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Arg2";
		eventInfo = new EventInfo("Kill", "None", "Shell");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kill-Arg1-Arg2";
		eventInfo = new EventInfo("Kill", "Spiky", "Stomp");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Die";
		eventInfo = new EventInfo("Die", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieEnemy-Arg1";
		eventInfo = new EventInfo("Die", "Goomba", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieFalling-Arg1";
		eventInfo = new EventInfo("Die", "Falling", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "DieShell-Arg1";
		eventInfo = new EventInfo("Die", "Shell", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Jump";
		eventInfo = new EventInfo("Jump", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Hit";
		eventInfo = new EventInfo("Hit", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Hit-Arg1";
		eventInfo = new EventInfo("Hit", "Coin Block", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Break";
		eventInfo = new EventInfo("Break", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearEnemy";
		eventInfo = new EventInfo("Appear", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearItem";
		eventInfo = new EventInfo("Appear", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearEnemy-Arg1";
		eventInfo = new EventInfo("Appear", "BulletBill", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "AppearItem-Arg1";
		eventInfo = new EventInfo("Appear", "Fireflower", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Shoot";
		eventInfo = new EventInfo("Shoot", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Throw";
		eventInfo = new EventInfo("Throw", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Kick";
		eventInfo = new EventInfo("Kick", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Hold";
		eventInfo = new EventInfo("Hold", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Eat";
		eventInfo = new EventInfo("Eat", "None", "None");
		printQuestions(questionType, semanticChunk, eventInfo);

		semanticChunk = "Eat-Arg1";
		eventInfo = new EventInfo("Eat", "Coin", "None");
		printQuestions(questionType, semanticChunk, eventInfo);
	}

	public static void printQuestions(String questionType, String semanticChunk, EventInfo eventInfo) {
		System.out.println("## " + questionType + " " + semanticChunk);
		String[] qsts = qstTemplates.getAllQuestions(questionType, semanticChunk, eventInfo);
		for (int i=0; i<qsts.length; i++) {
			System.out.println(String.format("%s", qsts[i]));
		}
		System.out.println(" ");
	}
}
