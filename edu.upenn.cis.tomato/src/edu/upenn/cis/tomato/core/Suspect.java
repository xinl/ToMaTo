package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;

public class Suspect {
	/*
	 * sitePos is presumed to be unique for every possible violation. Therefore
	 * it is used as sole field to compare Suspects' equality.
	 */
	protected SourcePosition sitePos; // We may want to remove this redundancy later
	protected SuspectType type;
	protected Map<PropertyName, Object> attributes = new HashMap<PropertyName, Object>();

	public Suspect(SourcePosition sitePos, SuspectType type) {
		this.sitePos = sitePos;
		this.type = type;
		attributes.put(PropertyName.SITE_URL, sitePos.getURLString());
		attributes.put(PropertyName.SITE_START_OFFSET, sitePos.getStartOffset());
		attributes.put(PropertyName.SITE_END_OFFSET, sitePos.getEndOffset());
		attributes.put(PropertyName.SUSPECT_TYPE, type);
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

	public SuspectType getType() {
		return type;
	}

	public String toSignatureString() {
		return "[Suspect Signature] [Position] " + sitePos + "\t[Type] " + type + "\n";
	}

	@Override
	public String toString() {

		String result = this.toSignatureString() + "===== Attribute List =====\n";
		Iterator<Entry<PropertyName, Object>> iter_attr = attributes.entrySet().iterator();
		while (iter_attr.hasNext()) {
			Entry<PropertyName, Object> entry = iter_attr.next();
			PropertyName name = entry.getKey();
			Object value = entry.getValue();
			if (!name.equals(PropertyName.ALIAS_SUSPECT)) {
				result = result + "[" + name + "] " + value + "\n";
			}
		}

		@SuppressWarnings("unchecked")
		HashSet<Suspect> aliasSet = (HashSet<Suspect>) attributes.get(PropertyName.ALIAS_SUSPECT);
		if(aliasSet != null) {
			result = result + "===== Alias Suspect =====\n";
			Iterator<Suspect> iter_as = aliasSet.iterator();
			while (iter_as.hasNext()) {
				result = result + iter_as.next().toSignatureString();
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sitePos == null) ? 0 : sitePos.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (type != other.type)
			return false;
		return true;
	}

	public enum SuspectType {
		FUNCTION_INVOCATION;

		public static SuspectType fromString(String str) {
			str = str.replaceAll("([^A-Z])([A-Z])", "$1_$2").toUpperCase();
			return valueOf(str);
		}
	}
}
