package edu.upenn.cis.tomato.core;

import java.util.Set;

public class FunctionInvocationSuspect extends Suspect {
	public FunctionInvocationSuspect(SourcePosition callSitePos) {
		super(callSitePos);
	}
	public SourcePosition getCallerPosition() {
		return super.sitePos;
	}
	public Set<Suspect> getAliases() {
		//TODO: return the alias group for callee
		return null;
	}
}
