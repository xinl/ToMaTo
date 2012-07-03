package edu.upenn.cis.tomato.core;

import java.util.Map;

public abstract class Suspect {
	protected Position pos;
	protected Map<String, Object> attributes;

	public Suspect(Position pos) {
		this.pos = pos;
	}
	public Position getPosition() {
		return pos;
	}
	public Object getAttribute(String name) {
		return attributes.get(name);
	}
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}
}
