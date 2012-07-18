package edu.upenn.cis.tomato.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.upenn.cis.tomato.core.Suspect.SuspectType;

public class TreatmentFactory {
	public final String BASE_OBJECT_NAME = makeBaseObjectName();
	static private final Pattern FUNCTION_INVOCATION_PATTERN = Pattern.compile("^([^\\(]+)\\((.*)\\)$", Pattern.DOTALL);
	private List<String> definitions = new ArrayList<String>();

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
		// TODO: a solution to name conflicts on _cond, etc will be referring to them as argument[x] but it'll make code harder to read
		String cond = "var _cond = _static";
		Set<Set<PolicyTerm>> dynamicTermGroups = policy.getDynamicTermGroups();
		for (Set<PolicyTerm> group : dynamicTermGroups) {
			cond += " || (";
			for (PolicyTerm term : group) {
				if (!term.isStatic()) { // all static terms are true and omitted
					if (term.getPropertyName().equals("TimeInvoked")) {
						cond += "this" + funcName + ".TimeInvoked " + term.getComparator() + " " + term.getValue() + " &&";
						staticVars.add(BASE_OBJECT_NAME + "." + funcName + ".TimeInvoked = 0");
						epilog.add("this." + funcName + ".TimeInvoked++;");
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
			ifClause += ";"; // do nothing with empty statement
			break;
		case CUSTOM:
			ifClause += policy.getAction().getContent();
			break;
		}

		// combine all parts into definition string
		String def = funcSignature + " {";
		def += args;
		def += cond;
		def += retVar;
		def += ifClause;
		if (epilog.size() > 0) {
			def += "if (!_static) {";
			for (String s : epilog) {
				def += s;
			}
			def += "}";
		}
		def += "return _retVar;";
		def += "};"; // close function
		// the "static variables" initializers after function definition
		for (String s : staticVars) {
			def += s;
		}

		definitions.add(def);
		
		return new Treatment(BASE_OBJECT_NAME + "." + funcName);

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
		private String newFuncName;
		
		protected Treatment(String newFuncName) {
			this.newFuncName = newFuncName;
		}
		
		public String apply(String str, SuspectType suspectType, boolean isStatic) {
			switch (suspectType) {
			case FUNCTION_INVOCATION_SUSPECT:
				return applyToFunctionInvocation(str, isStatic);
			}
			return null;
		}
		
		protected String applyToFunctionInvocation(String str, boolean isStatic) {
			Matcher matcher = FUNCTION_INVOCATION_PATTERN.matcher(str);
			String oldFuncName = matcher.group(1);
			String oldArgs = matcher.group(2);
			
			String argStatic = (isStatic ? "true" : "false") + ", ";
			int dotIndex = oldFuncName.lastIndexOf(".");
			String argContext = (dotIndex >= 0 ? oldFuncName.substring(0, dotIndex) : "null") + ", ";
			String argFunc = dotIndex >= 0 ? oldFuncName.substring(dotIndex + 1, oldFuncName.length()) : oldFuncName;
			if (oldArgs.length() != 0) {
				argFunc += ", ";
			}
			
			return newFuncName + "(" + argStatic + argContext + argFunc + oldArgs + ")";
		}
	}
}
