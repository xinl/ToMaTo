package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.Map;

import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;

public abstract class Suspect {
	/*
	 * sitePos is presumed to be unique for every possible violation. Therefore
	 * it is used as sole field to compare Suspects' equality.
	 */
	protected SourcePosition sitePos; // We may want to remove this redundancy later
	protected SuspectType type;
	protected Map<PropertyName, Object> attributes = new HashMap<PropertyName, Object>();

	public Suspect(SourcePosition sitePos) {
		this.sitePos = sitePos;
		attributes.put(PropertyName.SITE_URL, sitePos.getURLString());
		attributes.put(PropertyName.SITE_START_OFFSET, sitePos.getStartOffset());
		attributes.put(PropertyName.SITE_END_OFFSET, sitePos.getEndOffset());
	}

	public SourcePosition getPosition() {
		return sitePos;
	}

	public Object getAttribute(PropertyName name) {
		return attributes.get(name);
	}

	public void setAttribute(PropertyName name, Object value) {
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
