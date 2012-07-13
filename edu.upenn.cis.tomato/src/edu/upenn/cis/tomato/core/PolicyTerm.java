package edu.upenn.cis.tomato.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PolicyTerm {
	protected TermType type;
	protected String propertyName;
	protected ComparatorType comparator;
	protected Object value;
	private final static Set<String> STATIC_PROPERTY_NAMES = new HashSet<String>(Arrays.asList(
			"ActionType",
			"SiteName",
			"SiteURL",
			"SiteStartOffset",
			"SiteEndOffset",
			"CallerName",
			"CallerURL",
			"CallerStartOffset",
			"CallerEndOffset",
			"CalleeName",
			"CalleeURL",
			"CalleeStartOffset",
			"CalleeEndOffset"));
	private final static Set<String> DYNAMIC_PROPERTY_NAMES = new HashSet<String>(Arrays.asList(
			"TimeInvoked"));

	public PolicyTerm(String propertyName, ComparatorType comparator, Object value) throws IllegalArgumentException {
		this.propertyName = propertyName;
		this.comparator = comparator;
		this.value = value;
		if (STATIC_PROPERTY_NAMES.contains(propertyName)) {
			this.type = TermType.STATIC;
		} else if (DYNAMIC_PROPERTY_NAMES.contains(propertyName)) {
			this.type = TermType.DYNAMIC;
		} else {
			throw new IllegalArgumentException("Illegal property name.");
		}
	}
	
	public PolicyTerm(PolicyTerm term) {
		this.propertyName = term.propertyName;
		this.comparator = term.comparator;
		this.value = term.value;
		this.type = term.type;
	}
	
	public boolean isStatic() {
		return type == TermType.STATIC;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public ComparatorType getComparator() {
		return comparator;
	}

	public Object getValue() {
		return value;
	}

	/**
	 * Apply NOT to this term. e.g. <= becomes >
	 */
	public PolicyTerm negate() {
		ComparatorType newComparator = null;
		switch (comparator) {
		case EQUAL:
			newComparator = ComparatorType.UNEQUAL;
			break;
		case UNEQUAL:
			newComparator = ComparatorType.EQUAL;
			break;
		case GREATER_THAN:
			newComparator = ComparatorType.LESS_EQUAL_THAN;
			break;
		case LESS_THAN:
			newComparator = ComparatorType.GREATER_EQUAL_THAN;
			break;
		case GREATER_EQUAL_THAN:
			newComparator = ComparatorType.LESS_THAN;
			break;
		case LESS_EQUAL_THAN:
			newComparator = ComparatorType.GREATER_THAN;
			break;
		default:
			assert(false);
		}
		return new PolicyTerm(propertyName, newComparator, value);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comparator == null) ? 0 : comparator.hashCode());
		result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		PolicyTerm other = (PolicyTerm) obj;
		if (comparator != other.comparator)
			return false;
		if (propertyName == null) {
			if (other.propertyName != null)
				return false;
		} else if (!propertyName.equals(other.propertyName))
			return false;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value)) // Can polymorphism take care of all types of value?
			return false;
		return true;
	}

	@Override
	public String toString() {
		String valueString = "";
		if (value instanceof String) {
			valueString = "\"" + value + "\"";
		} else {
			valueString = value.toString();
		}
		return "" + propertyName + " " + comparator + " " + valueString;
	}
	
	public boolean appliesTo(Suspect suspect) {
		Object o1 = suspect.getAttribute(propertyName);
		Object o2 = value;
		int result;
		if (o1 instanceof Comparable && o2 instanceof Comparable) {
			@SuppressWarnings("unchecked")
			Comparable<Object> c1 = (Comparable<Object>) o1;
			@SuppressWarnings("unchecked")
			Comparable<Object> c2 = (Comparable<Object>) o2;
			try {
				result = c1.compareTo(c2);
			} catch (ClassCastException e) {
				// two objects are of different type
				return false;
			}
			
			switch (comparator) {
			case EQUAL:
				return result == 0;
			case UNEQUAL:
				return result != 0;
			case GREATER_THAN:
				return result == 1;
			case LESS_THAN:
				return result == -1;
			case GREATER_EQUAL_THAN:
				return result >= 0;
			case LESS_EQUAL_THAN:
				return result <= 0;
			}
		}
		return false;
	}

	public enum TermType {
		STATIC,
		DYNAMIC
	}
	
	public enum ComparatorType {
		EQUAL				("="),
		UNEQUAL				("!="),
		GREATER_THAN		(">"),
		LESS_THAN			("<"),
		GREATER_EQUAL_THAN	(">="),
		LESS_EQUAL_THAN		("<=");
		
		private final String string;
		
		ComparatorType(String string) {
			this.string = string;
		}
		
		public String toString() {
			return string;
		}
	}
}
