package edu.upenn.cis.tomato.core;

import java.util.Map;

public class SourceBundle {
	private String entryPoint;
	private Map<String, String> sources;

	public SourceBundle(String entryPoint) {
		this.entryPoint = entryPoint;
		//TODO:
	}
	public boolean hasSource(String url) {
		//TODO:
		return false;
		
	}
	public String getSourceText(String url) {
		//TODO:
		return null;
		
	}
	public String getSourceText(String url, int startOffset, int endOffset) {
		//TODO:
		return null;
		
	}
	public void setSourceText(String url, String text) {
		//TODO:
		
	}
	public void saveTo(String path) { // JS paths in HTML will points to saved file
		//TODO:
	}
}
