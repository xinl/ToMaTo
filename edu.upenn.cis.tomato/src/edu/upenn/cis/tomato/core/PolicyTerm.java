package edu.upenn.cis.tomato.core;

public class PolicyTerm {
	PolicyTermType type;
	String propertyName;
	ComparatorType comparator;
	Object value;
	
	/**
	 * Apply NOT to this term. e.g. <= becomes >
	 */
	public void negate() {
		
	}
	
	enum PolicyTermType {
		STATIC, DYNAMIC
	}
	
	enum ComparatorType {
		EQUAL, UNEQUAL, GREATER_THAN, LESS_THAN, GREATER_EQUAL_THAN, LESS_EQUAL_THAN
	}
}
