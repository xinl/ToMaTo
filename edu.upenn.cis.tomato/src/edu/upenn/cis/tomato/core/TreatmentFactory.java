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
	private final List<String> definitions = new ArrayList<String>();

	private int count = 0;

	public TreatmentFactory() {
		definitions.add("function " + BASE_OBJECT_NAME + "() {}");
	}

	public Treatment makeTreatment(Policy policy) {
		// build definition
		count++;
		String funcName = "t" + count;
		String funcSignature = BASE_OBJECT_NAME + "." + funcName + " = function (_static, _context, _func)";
		Set<String> staticVars = new HashSet<String>();
		Set<String> epilog = new HashSet<String>();

		// recreate original arguments array
		String args = "arguments = Array.prototype.slice.apply(arguments, [3, arguments.length]);";

		// build condition
		// TODO: a solution to name conflicts on _cond, etc will be referring to them as arguments[x] but it'll make code harder to read
		String cond = "var _cond = _static";
		Set<Set<PolicyTerm>> dynamicTermGroups = policy.getDynamicTermGroups();
		for (Set<PolicyTerm> group : dynamicTermGroups) {
			cond += " || (";
			for (PolicyTerm term : group) {
				if (!term.isStatic()) { // all static terms are true and omitted
					if (term.getPropertyName() == PropertyName.TIMES_INVOKED) {
						cond += "this." + funcName + ".TimesInvoked" + getComparatorValueJSString(term) + " &&";
						staticVars.add(BASE_OBJECT_NAME + "." + funcName + ".TimesInvoked = 0;");
						epilog.add("this." + funcName + ".TimesInvoked++;");
					}
				}
			}
			// trim one excessive " &&" at the end
			cond = cond.substring(0, cond.length() - " &&".length());
			cond += ")";
		}
		cond += ";";

		// build return value
		String retVar = "var _retVar = null;";

		// build if clause
		String ifClause = "if (_cond) ";
		switch (policy.getAction().getType()) {
		case PROHIBIT:
			ifClause += "{ ; } else { _func.apply(_context, arguments); }";
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
			def += "\t" + "if (!_static) {\n";
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

		return new Treatment(BASE_OBJECT_NAME + "." + funcName);

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
		// regex String to match within the suspect site
		private final String newFuncName;

		protected Treatment(String newFuncName) {
			this.newFuncName = newFuncName;
		}

		public String apply(String str, SuspectType suspectType, boolean isStatic) {
			switch (suspectType) {
			case FUNCTION_INVOCATION:
				return applyToFunctionInvocation(str, isStatic);
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

			String argStatic = (isStatic ? "true" : "false") + ", ";
			int dotIndex = oldFuncName.lastIndexOf(".");
			String argContext = (dotIndex >= 0 ? oldFuncName.substring(0, dotIndex) : "null") + ", ";
			String argFunc = oldFuncName;
			if (oldArgs.length() != 0) {
				argFunc += ", ";
			}

			return newFuncName + "(" + argStatic + argContext + argFunc + oldArgs + ")";
		}
	}
}
