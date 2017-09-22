package com.postechCVlab.MarioQA.returnObjects;

public class CandidateRegion {

	private int minFrame;
	private int maxFrame;
	private boolean proper;
	private int clipLength;

	public CandidateRegion(int minFrame, int maxFrame, 
			boolean proper, int clipLength) {

		this.minFrame = minFrame;
		this.maxFrame = maxFrame;
		this.proper = proper;
		this.clipLength = clipLength;
	}

	public int getMinFrame() {
		return minFrame;
	}

	public void setMinFrame(int minFrame) {
		this.minFrame = minFrame;
	}

	public int getMaxFrame() {
		return maxFrame;
	}

	public void setMaxFrame(int maxFrame) {
		this.maxFrame = maxFrame;
	}

	public boolean isProper() {
		return proper;
	}

	public void setProper(boolean proper) {
		this.proper = proper;
	}

	public int getClipLength() {
		return clipLength;
	}

	public void setClipLength(int clipLength) {
		this.clipLength = clipLength;
	}
}
