package com.postechCVlab.MarioQA.returnObjects;

public class ClipRegion {

	private int beginFrame;
	private int endFrame;
	private String targetActionLocation;
	private boolean findClip;
	
	public ClipRegion(int beginFrame, int endFrame, 
			String targetActionLocation, boolean findClip) {
		// TODO Auto-generated constructor stub
		
		this.beginFrame = beginFrame;
		this.endFrame = endFrame;
		this.targetActionLocation = targetActionLocation;
		this.findClip = findClip;
	}

	public int getBeginFrame() {
		return beginFrame;
	}

	public void setBeginFrame(int beginFrame) {
		this.beginFrame = beginFrame;
	}

	public int getEndFrame() {
		return endFrame;
	}

	public void setEndFrame(int endFrame) {
		this.endFrame = endFrame;
	}

	public String getTargetActionLocation() {
		return targetActionLocation;
	}

	public void setTargetActionLocation(String targetActionLocation) {
		this.targetActionLocation = targetActionLocation;
	}

	public boolean isFindClip() {
		return findClip;
	}

	public void setFindClip(boolean findClip) {
		this.findClip = findClip;
	}

}
