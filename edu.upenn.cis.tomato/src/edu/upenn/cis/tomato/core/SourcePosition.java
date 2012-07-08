package edu.upenn.cis.tomato.core;

import java.net.URL;

public class SourcePosition {
	private URL url;
	private int startOffset;
	private int endOffset;

	public SourcePosition(URL url, int startOffset, int endOffset) {
		this.url = url;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}

	public URL getURL() {
		return url;
	}

	public String getURLString() {
		return url.toString();
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}
	
	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}

	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	@Override
	public String toString() {
		if (url != null) {
			return "[URL] " + this.getURLString() + "\t[Offset] " + this.startOffset + " - " + this.endOffset;
		} else
			return null;
	}
}
