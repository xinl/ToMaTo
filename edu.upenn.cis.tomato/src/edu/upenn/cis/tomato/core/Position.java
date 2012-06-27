package edu.upenn.cis.tomato.core;

import java.net.URL;

public class Position {
	private URL url;
	private int startOffset;
	private int endOffset;

	public Position(URL url, int startOffset, int endOffset) {

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
}
