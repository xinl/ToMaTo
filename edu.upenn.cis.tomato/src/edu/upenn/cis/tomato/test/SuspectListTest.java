package edu.upenn.cis.tomato.test;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.tomato.core.PolicyTerm;
import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;
import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;
import edu.upenn.cis.tomato.core.SourcePosition;
import edu.upenn.cis.tomato.core.Suspect;
import edu.upenn.cis.tomato.core.Suspect.SuspectType;
import edu.upenn.cis.tomato.core.SuspectList;

public class SuspectListTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCopyConstructor() throws MalformedURLException {
		SuspectList sp = new SuspectList();
		Suspect s = new Suspect(new SourcePosition(new URL("http://example.com"), 12, 25), SuspectType.FUNCTION_INVOCATION);
		sp.add(s);
		SuspectList sp2 = new SuspectList(sp);
		assertEquals(sp.size(), sp2.size());
		assertTrue(sp.contains(s));
		assertTrue(sp2.contains(s));
		assertNotSame(sp, sp2);
	}

	@Test
	public void testFilter() throws MalformedURLException {
		SuspectList sp = new SuspectList();
		Suspect[] s = {
				new Suspect(new SourcePosition(new URL("http://e.com/1.js"), 2, 6), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/2.js"), 12, 25), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 6, 26), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 30, 40), SuspectType.FUNCTION_INVOCATION),
				new Suspect(new SourcePosition(new URL("http://e.com/a.js"), 1, 5), SuspectType.FUNCTION_INVOCATION)
		};

		s[0].setAttribute(PropertyName.SITE_NAME, "alert");
		s[1].setAttribute(PropertyName.SITE_NAME, "alert");
		s[2].setAttribute(PropertyName.SITE_NAME, "eval");

		sp.addAll(Arrays.asList(s));

		SuspectList sp1 = new SuspectList(sp);

		Suspect[] expect1 = {s[2], s[3], s[4]};
		sp1.filter(new PolicyTerm(PropertyName.SITE_URL, ComparatorType.EQUAL, "http://e.com/a.js"));
		assertTrue(matches(expect1, sp1));

		Suspect[] expect2 = {s[2], s[4]};
		sp1.filter(new PolicyTerm(PropertyName.SITE_START_OFFSET, ComparatorType.LESS_THAN, new Integer(15)));
		assertTrue(matches(expect2, sp1));

		Suspect[] expect3 = {s[2]};
		sp1.filter(new PolicyTerm(PropertyName.SITE_NAME, ComparatorType.EQUAL, "eval"));
		assertTrue(matches(expect3, sp1));

		Suspect[] expect4 = {};
		sp1.filter(new PolicyTerm(PropertyName.SITE_END_OFFSET, ComparatorType.GREATER_EQUAL_THAN, new Float(11.6)));
		assertTrue(matches(expect4, sp1));
	}

	private boolean matches(Suspect[] expect, SuspectList list) {
		if (expect.length != list.size()) {
			return false;
		}
		if (!list.containsAll(Arrays.asList(expect))) {
			return false;
		}
		return true;
	}

}
