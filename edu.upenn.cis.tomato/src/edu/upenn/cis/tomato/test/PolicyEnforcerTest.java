package edu.upenn.cis.tomato.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.tomato.core.Policy;
import edu.upenn.cis.tomato.core.PolicyEnforcer;
import edu.upenn.cis.tomato.core.PolicyEnforcer.Operation;
import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;
import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;
import edu.upenn.cis.tomato.core.Suspect.SuspectType;
import edu.upenn.cis.tomato.core.PolicyTerm;
import edu.upenn.cis.tomato.core.SourceBundle;
import edu.upenn.cis.tomato.core.SourcePosition;
import edu.upenn.cis.tomato.core.Suspect;
import edu.upenn.cis.tomato.core.SuspectList;

public class PolicyEnforcerTest {

	@Before
	public void setUp() throws Exception {
	}

	private SuspectList makeBaseList() throws MalformedURLException {
		SuspectList baseList = new SuspectList();
		Suspect[] s = {
				new Suspect(new SourcePosition(new URL("http://e.com/1.js"), 2, 6), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/2.js"), 12, 25), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 6, 26), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 30, 40), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 1, 5), SuspectType.FUNCTION_INVOCATION) };
		s[0].setAttribute(PropertyName.SITE_NAME, "alert");
		s[1].setAttribute(PropertyName.SITE_NAME, "alert");
		s[2].setAttribute(PropertyName.SITE_NAME, "eval");
		baseList.addAll(Arrays.asList(s));
		return baseList;
	}

	@Test
	public void testFilterSuspectList() throws MalformedURLException {
		SuspectList baseList = makeBaseList();

		// SiteURL.matches(".*/a\\.js" & (SiteStartOffset == 6 | TimesInvoked >= 3)
		Set<Set<PolicyTerm>> termGroups = new HashSet<Set<PolicyTerm>>();
		termGroups.add(new HashSet<PolicyTerm>(Arrays.asList(new PolicyTerm[] {
				new PolicyTerm(PropertyName.SITE_URL, ComparatorType.MATCHES, Pattern.compile(".*/a\\.js")),
				new PolicyTerm(PropertyName.SITE_START_OFFSET, ComparatorType.EQUAL, new Integer(6)) })));
		termGroups.add(new HashSet<PolicyTerm>(Arrays.asList(new PolicyTerm[] {
				new PolicyTerm(PropertyName.SITE_URL, ComparatorType.MATCHES, Pattern.compile(".*/a\\.js")),
				new PolicyTerm(PropertyName.TIMES_INVOKED, ComparatorType.GREATER_EQUAL_THAN, new Integer(3)) })));

		PE pe = new PE(new ArrayList<Policy>());
		SuspectList resultList = pe.filterSuspectList(baseList, termGroups);

		SuspectList expectedList = new SuspectList();
		expectedList.addAll(Arrays.asList(new Suspect[] {
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 6, 26), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 30, 40), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 1, 5), SuspectType.FUNCTION_INVOCATION) }));

		assertEquals(expectedList, resultList);
	}

	@Test
	public void testGetAllOperations() throws ParseException, InvalidParameterException, IOException {
		List<Policy> policies = new ArrayList<Policy>();
		policies.add(new Policy("SiteURL.matches(\".*-1\\.js$\") & (SiteStartOffset >= 90 | TimesInvoked >= 3): prohibit"));
		PE pe = new PE(policies);

		File file = new File(new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/core/function/BasicFunctionInvocation.html");
		SourceBundle sb = new SourceBundle(file.toURI());

		SortedSet<Operation> ops = pe.getAllOperations(sb, null);
		// System.out.println(ops);
		assertEquals(3, ops.size());
		for (Operation op : ops) {
			assertTrue(op.getPosition().getURLString().endsWith("-1.js"));
			if (op.getPosition().getStartOffset() >= 90) {
				assertTrue(op.isStatic());
			} else {
				assertFalse(op.isStatic());
			}
		}
	}

	@Test
	public void testNestOperations() throws MalformedURLException {
		// The SourcePositions below represents the following nesting situation:
		// (No. 0-7 from top to bottom, from left to right)
		// In a.js:
		//   |__|
		// |____| |__|__|
		// |____________| |__|
		// In b.js:
		//  |___| |_____|
		SourcePosition[] sps = {
				new SourcePosition(new URL("http://e.com/a.js"), 2, 4),
				new SourcePosition(new URL("http://e.com/a.js"), 0, 4),
				new SourcePosition(new URL("http://e.com/a.js"), 5, 7),
				new SourcePosition(new URL("http://e.com/a.js"), 7, 10),
				new SourcePosition(new URL("http://e.com/a.js"), 0, 10),
				new SourcePosition(new URL("http://e.com/a.js"), 11, 13),
				new SourcePosition(new URL("http://e.com/b.js"), 1, 4),
				new SourcePosition(new URL("http://e.com/b.js"), 5, 10), };
		SortedSet<Operation> ops = new TreeSet<Operation>(PolicyEnforcer.SORT_BY_POSITION_AND_SUSPECT_TYPE);
		for (SourcePosition sp : sps) {
			ops.add(new Operation(sp, SuspectType.FUNCTION_INVOCATION, false, null));
		}

		PE pe = new PE(new ArrayList<Policy>());
		SortedSet<Operation> result = pe.nestOperations(ops);

//		System.out.println("Result:");
//		for (Operation op : result) {
//			System.out.println(op);
//		}

		// prepare expected tree
		List<Operation> opse = new ArrayList<Operation>();
		for (SourcePosition sp : sps) {
			opse.add(new Operation(sp, SuspectType.FUNCTION_INVOCATION, false, null));
		}

		Operation[] opa = new Operation[0];
		opa = opse.toArray(opa);
		opa[1].addChild(opa[0]);
		opa[4].addChild(opa[1]);
		opa[4].addChild(opa[2]);
		opa[4].addChild(opa[3]);
		SortedSet<Operation> expected = new TreeSet<Operation>(PolicyEnforcer.SORT_BY_POSITION_AND_SUSPECT_TYPE);
		expected.add(opa[4]);
		expected.add(opa[5]);
		expected.add(opa[6]);
		expected.add(opa[7]);

//		System.out.println("Expected:");
//		for (Operation op : expected) {
//			System.out.println(op);
//		}

		assertEquals(expected, result);
	}

	@Test
	public void testEnforceOn() throws ParseException, InvalidParameterException, IOException {
		List<Policy> policies = new ArrayList<Policy>();
		policies.add(new Policy("SiteURL.matches(\".*-1\\.js$\") : prohibit"));

		PolicyEnforcer pe = new PolicyEnforcer(policies);

		File file = new File(new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/core/function/BasicFunctionInvocation.html");
		SourceBundle sb = new SourceBundle(file.toURI());
		pe.enforceOn(sb);
		//sb.saveSourceBundleTo(".");
		boolean defFileFound = false;
		for (URI uri : sb.getSourceURIs()) {
			if (defFileFound == false && uri.toString().matches(".*_.*\\.js")) {
				defFileFound = true;
				continue;
			}
			if (uri.toString().endsWith("-1.js")) {
				String content = sb.getSourceContent(uri);
				assertTrue(content.contains(".t1({isStatic: true, oldThis: null, oldFunc: alert}, z)"));
				assertTrue(content.contains(".t1({isStatic: true, oldThis: null, oldFunc: alert}, \"some words\")"));
				assertTrue(content.contains(".t1({isStatic: true, oldThis: null, oldFunc: addition}, 1, 2)"));
			}
			if (uri.toString().endsWith(".html")) {
				String content = sb.getSourceContent(uri);
				assertTrue(content.contains("src=\"" + pe.getTreatmentFactory().BASE_OBJECT_NAME + ".js\""));
			}
		}
		assertTrue(defFileFound);
	}

	/**
	 * Subclass for testing non-public functions in PolocyEnforcer
	 *
	 */
	class PE extends PolicyEnforcer {

		public PE(Collection<Policy> policies) {
			super(policies);
		}

		@Override
		protected SortedSet<Operation> getAllOperations(SourceBundle sourceBundle, Map<OperationKey, List<Policy>> conflicts) {
			return super.getAllOperations(sourceBundle, conflicts);
		}

		@Override
		protected SortedSet<Operation> nestOperations(SortedSet<Operation> operations) {
			return super.nestOperations(operations);
		}

		@Override
		protected SuspectList filterSuspectList(SuspectList baseList, Set<Set<PolicyTerm>> termGroups) {
			return super.filterSuspectList(baseList, termGroups);
		}

		protected class SortByPosition extends PolicyEnforcer.SortByPositionAndSuspectType {

		}

	}

}
