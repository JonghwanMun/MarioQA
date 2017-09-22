package com.postechCVlab.MarioQA;

public class EventInfo {

	private String action;
	private String target;
	private String means;

	public EventInfo(String action, String target, String means) {

		this.action = action;
		this.target = target;
		this.means = means;
	}

	public void setInfo(String action, String target, String means) {

		this.action = action;
		this.target = target;
		this.means = means;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getMeans() {
		return means;
	}

	public void setMeans(String means) {
		this.means = means;
	}
	
	public String toString() {
		String message = String.format("action (%s) | target (%s) | means (%s)", 
				this.action, this.target, this.means);
		return message;
	}
}
