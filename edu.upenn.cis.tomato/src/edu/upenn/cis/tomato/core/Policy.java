package edu.upenn.cis.tomato.core;

import java.util.List;

public class Policy {
	private List<List<PolicyTerm>> policyTerms;
	// A list of groups connected by OR, the groups each contains policy terms connected by AND.
	
	private PolicyAction actionIfTrue;
	private PolicyAction actionIfFalse;
	
	public Policy(String str) {
		//TODO
	}
	public String toString() {
		return null;
	}
	// Is there a more inter-operative way to define trees other than using String?
	// Do we expose the tree building facilities to the front-end?
	public Policy getStaticTerms() {
		return null;
	}
	public Policy getDynamicTerms() {
		return null;
	}
}
