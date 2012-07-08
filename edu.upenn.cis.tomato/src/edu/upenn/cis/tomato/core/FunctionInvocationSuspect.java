package edu.upenn.cis.tomato.core;

import java.util.Set;

public class FunctionInvocationSuspect extends Suspect {
	public FunctionInvocationSuspect(SourcePosition caller, SourcePosition callee) {
		super(caller);
		super.attributes.put("CalleePosition", callee);
	}
	public SourcePosition getCallerPosition() {
		return super.pos;
	}
	public SourcePosition getCalleePosition() {
		return (SourcePosition) super.getAttribute("CalleePosition");
	}
	public Set<Suspect> getAliases() {
		//TODO: return the alias group for callee
		return null;
	}
}
