package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.Map;

public abstract class Suspect {
	protected SourcePosition pos;
	protected Map<String, Object> attributes;

	public Suspect(SourcePosition pos) {
		this.pos = pos;
		this.attributes = new HashMap<String, Object>();
	}
	public SourcePosition getPosition() {
		return pos;
	}
	public Object getAttribute(String name) {
		return attributes.get(name);
	}
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}
	@Override
	public String toString() {
		return "Position\t" + pos.toString() + "\nAttributes\t" + attributes.toString();
	}
	
}
