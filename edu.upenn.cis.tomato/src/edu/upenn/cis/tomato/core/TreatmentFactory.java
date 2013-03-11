package edu.upenn.cis.tomato.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;
import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;
import edu.upenn.cis.tomato.core.Suspect.SuspectType;

public class TreatmentFactory {
	public final String BASE_OBJECT_NAME = makeBaseObjectName();
	static private final Pattern FUNCTION_INVOCATION_PATTERN = Pattern.compile("^([^\\(]+)\\((.*)\\)$", Pattern.DOTALL);
	// regex for x++, x--, ++x, --x;
	// if matched, x can be retrieved from group 1, 2, 3, 4 respectively.
	static private final Pattern UNARY_ASSIGNMENT_PATTERN = Pattern.compile("^(?:(.+)\\+\\+)|(?:(.+)--)|(?:\\+\\+(.+))|(?:--(.+))$", Pattern.DOTALL);
	// regex for x += y, x -= y, etc.
	// group 1: left side expression; group 2: self assignment operator (e.g. + in +=); group 3: right side expression
	static private final Pattern ASSIGNMENT_PATTERN = Pattern.compile("^([^=]+?)([\\+\\-\\*/%]?)=(.*)$", Pattern.DOTALL);
	private final List<String> definitions = new ArrayList<String>();

	private int count = 0;

	public TreatmentFactory() {
		String bootstrap = "function " + BASE_OBJECT_NAME + "() {};\n";
		bootstrap += BASE_OBJECT_NAME + ".eval = eval;";
		bootstrap += BASE_OBJECT_NAME + ".ret = function(v) { return v; };";
		bootstrap += BASE_OBJECT_NAME + ".evalAfter = (typeof(window.sandbox) !== \"function\") "
				+ "? function() {return false;} "
				+ ": function(expr, func, context, args) {"
				+ "sandbox(\"fork\");"
				+ "if(window.isClone == true) { func.apply(context, args); if (eval(expr)) {sandbox(\"setResultTrue\");} else {sandbox(\"setResultFalse\")}}"
				+ "else {sandbox(\"getResult\"); return window.sandboxResult;}"
				+ "};";
		definitions.add(bootstrap);
	}

	public Treatment makeTreatment(Policy policy) {
		// build definition
		count++;
		String funcName = "t" + count;
		String funcSignature = BASE_OBJECT_NAME + "." + funcName + " = function(_ToMaTo)";
		Set<String> staticVars = new HashSet<String>();
		Set<String> epilog = new HashSet<String>();

		// recreate original arguments array
		String args = "arguments = Array.prototype.slice.apply(arguments, [1, arguments.length]);";

		// build condition
		// TODO: a solution to name conflicts on _cond, etc will be referring to them as arguments[x] but it'll make code harder to read
		String cond = "var _cond = _ToMaTo.isStatic";
		Set<Set<PolicyTerm>> dynamicTermGroups = policy.getDynamicTermGroups();
		for (Set<PolicyTerm> group : dynamicTermGroups) {
			cond += " || (";
			for (PolicyTerm term : group) {
				if (!term.isStatic()) { // all static terms are true and omitted
					switch (term.getPropertyName()) {
					case TIMES_INVOKED:
						cond += "this." + funcName + ".TimesInvoked " + getComparatorValueJSString(term) + " &&";
						staticVars.add(BASE_OBJECT_NAME + "." + funcName + ".TimesInvoked = 0;");
						epilog.add("this." + funcName + ".TimesInvoked++;");
						break;
					case EVAL_BEFORE:
						cond += "_ToMaTo.evalBefore[\"" + term.getPropertyArgs().get(0) + "\"] " + getComparatorValueJSString(term) + " &&";
						break;
					case EVAL_AT:
						cond += BASE_OBJECT_NAME + ".eval('" + term.getPropertyArgs().get(0) + "')" + " &&";
						break;
					case EVAL_AFTER:
						cond += BASE_OBJECT_NAME + ".evalAfter('" + term.getPropertyArgs().get(0) + "', _ToMaTo.oldFunc, _ToMaTo.oldThis, arguments)" + " &&";
						break;
					}
				}
			}
			// trim one excessive " &&" at the end
			cond = cond.substring(0, cond.length() - " &&".length());
			cond += ")";
		}
		cond += ";";

		// build return value
		String retVar = "var _retVar = undefined;";

		// build if clause
		String ifClause = "if (_cond) ";
		switch (policy.getAction().getType()) {
		case PROHIBIT:
			ifClause += "{ ; } else { _retVar = _ToMaTo.oldFunc.apply(_ToMaTo.oldThis, arguments); }";
			break;
		case CUSTOM:
			ifClause += policy.getAction().getContent();
			break;
		}

		// combine all parts into definition string
		String def = funcSignature + " {\n";
		def += "\t" + args + "\n";
		def += "\t" + cond + "\n";
		def += "\t" + retVar + "\n";
		def += "\t" + ifClause + "\n";
		if (epilog.size() > 0) {
			def += "\t" + "if (!_ToMaTo.isStatic) {\n";
			for (String s : epilog) {
				def += "\t\t" + s + "\n";
			}
			def += "\t}\n";
		}
		def += "\t" + "return _retVar;\n";
		def += "};\n"; // close function
		// the "static variables" initializers after function definition
		for (String s : staticVars) {
			def += s + "\n";
		}

		definitions.add(def);

		return new Treatment(count, policy);

	}

	private String getComparatorValueJSString(PolicyTerm term) {
		if (term.getComparator() == ComparatorType.MATCHES) {
			return ".match(/" + term.getValue() + "/) !== null";
		} else if (term.getComparator() == ComparatorType.NOT_MATCHES) {
			return ".match(/" + term.getValue() + "/) === null";
		} else {
			String comp = "";
			switch (term.getComparator()) {
			case EQUAL:
				comp += " === ";
				break;
			case UNEQUAL:
				comp += " !== ";
				break;
			default:
				comp += " " + term.getComparator() + " ";
			}
			if (term.getValue() instanceof String) {
				return comp + "\"" + term.getValue() + "\"";
			} else {
				return comp + term.getValue();
			}
		}
	}

	public String getDefinitions() {
		String defs = "";
		for (String s : definitions) {
			defs += s + "\n";
		}
		return defs;
	}

	protected String makeBaseObjectName() {
		UUID uuid = UUID.randomUUID();
		return "_" + longToBase64(uuid.getMostSignificantBits()) + longToBase64(uuid.getLeastSignificantBits());
	}

	protected String longToBase64(long num) {
		String base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "$_";
		String encoded = "";
		for (int i = 0; i < 11; i++) {
			int index = (int) (num & 0x3f);
			encoded += base64chars.charAt(index);
			num >>= 6;
		}
		return encoded;
	}

	public class Treatment {
		private final String treatmentFuncName;
		private final int index;
		private final Policy policy; // the policy this treatment based on

		protected Treatment(int index, Policy policy) {
			this.index = index;
			this.treatmentFuncName = BASE_OBJECT_NAME + ".t" + index;
			this.policy = policy;
		}

		public int getIndex() {
			return index;
		}

		public Policy getPolicy() {
			return policy;
		}

		public String apply(String str, SuspectType suspectType, boolean isStatic) {
			switch (suspectType) {
			case FUNCTION_INVOCATION:
				return applyToFunctionInvocation(str, isStatic);
			case DATA_READ:
				return applyToDataRead(str, isStatic);
			case DATA_WRITE:
				return applyToDataWrite(str, isStatic);
			}
			return null;
		}

		protected String applyToFunctionInvocation(String str, boolean isStatic) {
			Matcher matcher = FUNCTION_INVOCATION_PATTERN.matcher(str);
			if (!matcher.matches()) {
				System.err.println("No function invocation code found. Treatment aborted.");
				return str;
			}
			String oldFuncName = matcher.group(1);
			String oldArgs = matcher.group(2);

			String argStatic = (isStatic ? "true" : "false");
			int dotIndex = oldFuncName.lastIndexOf(".");
			String argThis = (dotIndex >= 0 ? oldFuncName.substring(0, dotIndex) : "null");
			String argFunc = oldFuncName;
			String evalBefore = buildEvalBeforeString();

			// assemble the new text to be put at this suspect site
			String newText = treatmentFuncName + "(";
			newText += "{isStatic: " + argStatic + ", oldThis: " + argThis + ", oldFunc: " + argFunc;
			if (evalBefore.length() > 0) {
				newText += ", evalBefore: " + evalBefore;
			}
			newText += "}";
			if (oldArgs.length() > 0) {
				newText += ", " + oldArgs;
			}
			newText += ")";
			return newText;
		}

		protected String applyToDataRead(String str, boolean isStatic) {
			String argStatic = "isStatic: " + (isStatic ? "true" : "false");
			String evalBefore = buildEvalBeforeString();
			if (evalBefore.length() > 0) {
				evalBefore = ", evalBefore: " + evalBefore;
			}
			String retFunc = BASE_OBJECT_NAME + ".ret";
			String oldFunc = ", oldFunc: " + retFunc;

			String newText = "";

			Matcher matcher = UNARY_ASSIGNMENT_PATTERN.matcher(str);
			if (matcher.matches()) {
				// we've got something like x++ or ++x
				if (matcher.group(1) != null) {
					// x++ will become ret(t(x), x = x + 1)
					String x = matcher.group(1);
					newText += retFunc + "(" + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + "), " + x + " = "  + x + " + 1)";
				} else if (matcher.group(2) != null) {
					// x--
					String x = matcher.group(2);
					newText += retFunc + "(" + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + "), " + x + " = "  + x + " - 1)";
				} else if (matcher.group(3) != null) {
					// ++x will become t(x = x + 1)
					String x = matcher.group(3);
					newText += treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + " = " + x + " + 1)";
				} else if (matcher.group(4) != null) {
					// --x
					String x = matcher.group(4);
					newText += treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + " = " + x + " - 1)";
				}
			} else {
				// we've got simply a variable name, change it to t(x)
				newText += treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + str + ")";
			}
			return newText;
		}

		protected String applyToDataWrite(String str, boolean isStatic) {
			String argStatic = "isStatic: " + (isStatic ? "true" : "false");
			String evalBefore = buildEvalBeforeString();
			if (evalBefore.length() > 0) {
				evalBefore = ", evalBefore: " + evalBefore;
			}
			String retFunc = BASE_OBJECT_NAME + ".ret";
			String oldFunc = ", oldFunc: " + retFunc;

			String newText = "";

			Matcher matcher = UNARY_ASSIGNMENT_PATTERN.matcher(str);
			if (matcher.matches()) {
				// we've got something like x++, ++x
				if (matcher.group(1) != null) {
					// x++ becomes ret(x, x = t(x + 1))
					String x = matcher.group(1);
					newText += retFunc + "(" + x + ", " + x + " = " + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + " + 1" + "))";
				} else if (matcher.group(2) != null) {
					// x--
					String x = matcher.group(2);
					newText += retFunc + "(" + x + ", " + x + " = " + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + " - 1" + "))";
				} else if (matcher.group(3) != null) {
					// ++x becomes (x = t(x + 1))
					String x = matcher.group(3);
					newText += "(" + x + " = " + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + " + 1))";
				} else if (matcher.group(4) != null) {
					// --x
					String x = matcher.group(4);
					newText += "(" + x + " = " + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + " - 1))";
				}
			} else {
				// we've got something like x = y or x += y
				// Note: if the site has been treated of data read, the else clause in following code happens to be able to handle _xxx.ret(x, x = t(x) + 1) as well
				matcher = ASSIGNMENT_PATTERN.matcher(str);
				if (matcher.matches()) {
					if (matcher.group(2) != null) {
						// something like x += y will become x = t(x + y)
						String x = matcher.group(1);
						String op = matcher.group(2);
						String y = matcher.group(3);
						newText += x + " = " + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + x + op + y + ")";
					} else {
						// x = y will become x = t(y)
						String x = matcher.group(1);
						String y = matcher.group(3);
						newText += x + " = " + treatmentFuncName + "({" + argStatic + oldFunc + evalBefore + "}, " + y + ")";
					}
				} else {
					System.err.println("No data write code found. Treatment aborted.");
					return str;
				}
			}
			return newText;
		}

		private String buildEvalBeforeString() {
			String evalBefore = "";
			Set<Set<PolicyTerm>> dynamicTermGroups = policy.getDynamicTermGroups();
			for (Set<PolicyTerm> group : dynamicTermGroups) {
				for (PolicyTerm term : group) {
					if (term.propertyName == PropertyName.EVAL_BEFORE) {
						String evalStr = (String) term.getPropertyArgs().get(0);
						evalBefore += "\"" + evalStr + "\": " + BASE_OBJECT_NAME + ".eval(\"" + evalStr + "\"), ";
					}
				}
			}
			if (evalBefore.length() > 0) {
				// trim one excessive ", " at the end
				evalBefore = evalBefore.substring(0, evalBefore.length() - ", ".length());
				evalBefore = "{" + evalBefore + "}";
			}
			return evalBefore;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + index;
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
			Treatment other = (Treatment) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (index != other.index)
				return false;
			return true;
		}

		private TreatmentFactory getOuterType() {
			return TreatmentFactory.this;
		}

	}
}
