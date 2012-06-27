package edu.upenn.cis.tomato.core;

public class PolicyAction {
	PolicyActionType type;
	String content;
	
	enum PolicyActionType {
		ALLOW, PROHIBIT, CUSTOM
	}
}
