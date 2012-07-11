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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endOffset;
		result = prime * result + startOffset;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourcePosition other = (SourcePosition) obj;
		if (endOffset != other.endOffset)
			return false;
		if (startOffset != other.startOffset)
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
}
