package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.Map;

public abstract class Suspect {
	/*
	 * sitePos is presumed to be unique for every possible violation. Therefore
	 * it is used as sole field to compare Suspects' equality.
	 */
	protected SourcePosition sitePos; // We may want to remove this redundancy later
	protected SuspectType type;
	protected Map<String, Object> attributes = new HashMap<String, Object>();

	public Suspect(SourcePosition sitePos) {
		this.sitePos = sitePos;
		attributes.put("SiteURL", sitePos.getURLString());
		attributes.put("SiteStartOffset", sitePos.getStartOffset());
		attributes.put("SiteEndOffset", sitePos.getEndOffset());
	}

	public SourcePosition getPosition() {
		return sitePos;
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	@Override
	public String toString() {
		return "Position\t" + sitePos.toString() + "\nAttributes\t" + attributes.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sitePos == null) ? 0 : sitePos.hashCode());
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
		Suspect other = (Suspect) obj;
		if (sitePos == null) {
			if (other.sitePos != null)
				return false;
		} else if (!sitePos.equals(other.sitePos))
			return false;
		// Two Suspect is considered equal if their sitePos's are equal.
		return true;
	}

	enum SuspectType {
		FUNCTION_INVOCATION_SUSPECT
	}
}
