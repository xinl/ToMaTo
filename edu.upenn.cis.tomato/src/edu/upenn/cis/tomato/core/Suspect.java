package edu.upenn.cis.tomato.core;

import java.util.Map;

public abstract class Suspect {
	private Position pos;
	// too general? move to subclasses as named fields and make this class abstract?
	private Map<String, Object> attributes;

	public Suspect(Position pos) {
		this.pos = pos;
		//TODO
	}
	public Position getPosition() {
		//TODO
		return null;
	}
	public Object getAttribute(String name) {
		//TODO
		return null;
	}
	public void setAttribute(String name, Object value) {
		//TODO
	}
}
