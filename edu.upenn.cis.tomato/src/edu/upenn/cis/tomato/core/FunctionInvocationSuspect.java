package edu.upenn.cis.tomato.core;

import java.util.Set;

public class FunctionInvocationSuspect extends Suspect {
	public FunctionInvocationSuspect(Position caller, Position callee) {
		super(caller);
		super.attributes.put("CalleePosition", callee);
	}
	public Position getCallerPosition() {
		return super.pos;
	}
	public Position getCalleePosition() {
		return (Position) super.getAttribute("CalleePosition");
	}
	public Set<Suspect> getAliases() {
		//TODO: return the alias group for callee
		return null;
	}
}
