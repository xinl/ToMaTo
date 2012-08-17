package edu.upenn.cis.tomato.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A condition used in Policy to match qualified Suspects. A PolicyTerm consists
 * of three parts: a PropertyName referring to an attribute in Suspects, a
 * comparator and a value to express the requirement of such attribute. An
 * example PolicyTerm may express the condition of
 * <code>SiteStartOffset >= 2</code>.
 *
 * @author Xin Li
 * @version July 20, 2012
 */
public class PolicyTerm {
	protected PropertyName propertyName;
	protected List<Object> propertyArgs;
	protected ComparatorType comparator;
	protected Object value;

	public PolicyTerm(String propertyName, ComparatorType comparator, Object value) {
		this(PropertyName.fromString(propertyName), comparator, value);
	}

	public PolicyTerm(PropertyName propertyName, ComparatorType comparator, Object value) {
		this(propertyName, null, comparator, value);
	}

	public PolicyTerm(PropertyName propertyName, List<Object> propertyArgs, ComparatorType comparator, Object value) {
		this.propertyName = propertyName;
		this.propertyArgs = propertyArgs;
		this.comparator = comparator;
		this.value = value;
	}

	/**
	 * Copy constructor
	 *
	 * @param term
	 */
	public PolicyTerm(PolicyTerm term) {
		this.propertyName = term.propertyName;
		this.comparator = term.comparator;
		this.value = term.value;
	}

	/**
	 * Return whether this term is a static term. A static term is a term with a
	 * static PropertyName. (see definition of PropertyName)
	 *
	 * @return true if the term is static, otherwise return false.
	 */
	public boolean isStatic() {
		return propertyName.isStatic;
	}

	public PropertyName getPropertyName() {
		return propertyName;
	}

	public List<Object> getPropertyArgs() {
		return new ArrayList<Object>(propertyArgs);
	}

	public ComparatorType getComparator() {
		return comparator;
	}

	public Object getValue() {
		return value;
	}

	/**
	 * Return a new PolicyTerm whose comparator is negated. (e.g.: <= becomes >,
	 * == becomes !=)
	 *
	 * @return A negated PolicyTerm
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
		case MATCHES:
			newComparator = ComparatorType.NOT_MATCHES;
			break;
		case NOT_MATCHES:
			newComparator = ComparatorType.MATCHES;
			break;
		default:
			assert (false);
		}
		return new PolicyTerm(propertyName, newComparator, value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comparator == null) ? 0 : comparator.hashCode());
		result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
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
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value)) // Polymorphism should take care of comparison of all types of value?
			return false;
		return true;
	}

	@Override
	public String toString() {
		String valueString = "";
		if (value instanceof String) {
			valueString = "\"" + value.toString().replace("\"", "\\\"") + "\""; // escape quotes and surround by quotes
		} else {
			valueString = value.toString();
		}
		if (comparator == ComparatorType.MATCHES || comparator == ComparatorType.NOT_MATCHES) {
			return "" + propertyName + comparator + "(\"" + valueString + "\")";
		} else {
			return "" + propertyName + " " + comparator + " " + valueString;
		}
	}

	/**
	 * Return whether this PolicyTerm applies to a specific value. For example,
	 * the term "SiteStartOffset > 3" applies to a suspect value of integer 4,
	 * but does not apply to integer 2.
	 *
	 * @param suspectValue
	 *            The value to be tested under this term.
	 * @return true if the term applies to the value, otherwise return false.
	 */
	public boolean appliesTo(Object suspectValue) {
		Object o1 = suspectValue;
		Object o2 = value;

		if (comparator == ComparatorType.MATCHES || comparator == ComparatorType.NOT_MATCHES) {
			if (!(o1 instanceof String) || !(o2 instanceof Pattern)) {
				return false;
			}
			Pattern pattern = (Pattern) o2;
			boolean matches = pattern.matcher((String) o1).matches();
			switch (comparator) {
			case MATCHES:
				return matches;
			case NOT_MATCHES:
				return !matches;
			}
		}

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

	public enum ComparatorType {
		EQUAL("=="),
		UNEQUAL("!="),
		GREATER_THAN(">"),
		LESS_THAN("<"),
		GREATER_EQUAL_THAN(">="),
		LESS_EQUAL_THAN("<="),
		MATCHES(".matches"),
		NOT_MATCHES(".notMatches");

		private final String string;

		ComparatorType(String string) {
			this.string = string;
		}

		@Override
		public String toString() {
			return string;
		}
	}

	public enum PropertyName {
		// static properties
		SUSPECT_TYPE("SuspectType", true),
		ALIAS_SUSPECT("AliasSuspect", true),
		ARGUMENT_COUNT("ArgumentCount", true),
		SITE_NAME("SiteName", true),
		SITE_URL("SiteURL", true),
		SITE_START_OFFSET("SiteStartOffset", true),
		SITE_END_OFFSET("SiteEndOffset", true),
		CALLER_NAME("CallerName", true),
		CALLER_WALA_NAME("CallerWALAName", true),
		CALLER_URL("CallerURL", true),
		CALLER_START_OFFSET("CallerStartOffset", true),
		CALLER_END_OFFSET("CallerEndOffset", true),
		CALLEE_NAME("CalleeName", true),
		CALLEE_WALA_NAME("CalleeWALAName", true),
		CALLEE_URL("CalleeURL", true),
		CALLEE_START_OFFSET("CalleeStartOffset", true),
		CALLEE_END_OFFSET("CalleeEndOffset", true),
		IS_CONSTRUCTOR("IsConstructor", true),
		// dynamic properties
		TIMES_INVOKED("TimesInvoked", false),
		EVAL_BEFORE("EvalBefore", false),
		EVAL_AT("EvalAt", false);

		private String string;
		private boolean isStatic;

		PropertyName(String string, boolean isStatic) {
			this.string = string;
			this.isStatic = isStatic;
		}

		static public PropertyName fromString(String str) {
			str = str.replaceAll("([^A-Z])([A-Z])", "$1_$2").toUpperCase();
			return valueOf(str);
		}

		@Override
		public String toString() {
			return string;
		}
	}
}
