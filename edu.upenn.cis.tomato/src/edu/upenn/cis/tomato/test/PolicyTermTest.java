package edu.upenn.cis.tomato.test;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.tomato.core.PolicyTerm;
import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;
import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;

public class PolicyTermTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testIsStatic() {
		PolicyTerm pt1 = new PolicyTerm("SiteName", ComparatorType.EQUAL, "Blah.");
		PolicyTerm pt2 = new PolicyTerm("TimesInvoked", ComparatorType.LESS_THAN, new Integer(3));
		assertTrue(pt1.isStatic());
		assertFalse(pt2.isStatic());
	}

	@Test
	public void testNegate() {
		PolicyTerm pt1 = new PolicyTerm("SiteName", ComparatorType.EQUAL, "Blah.");
		PolicyTerm pt2 = new PolicyTerm("SiteName", ComparatorType.UNEQUAL, "Blah.");
		PolicyTerm pt3 = new PolicyTerm("SiteName", ComparatorType.GREATER_THAN, "Blah.");
		PolicyTerm pt4 = new PolicyTerm("SiteName", ComparatorType.LESS_THAN, "Blah.");
		PolicyTerm pt5 = new PolicyTerm("SiteName", ComparatorType.GREATER_EQUAL_THAN, "Blah.");
		PolicyTerm pt6 = new PolicyTerm("SiteName", ComparatorType.LESS_EQUAL_THAN, "Blah.");
		PolicyTerm pt7 = new PolicyTerm("SiteName", ComparatorType.MATCHES, "Blah.");
		PolicyTerm pt8 = new PolicyTerm("SiteName", ComparatorType.NOT_MATCHES, "Blah.");

		assertEquals(pt2, pt1.negate());
		assertEquals(pt1, pt2.negate());
		assertEquals(pt6, pt3.negate());
		assertEquals(pt5, pt4.negate());
		assertEquals(pt4, pt5.negate());
		assertEquals(pt3, pt6.negate());
		assertEquals(pt8, pt7.negate());
		assertEquals(pt7, pt8.negate());

		assertNotSame(pt2, pt1.negate());
	}

	@Test
	public void testEquals() {
		PolicyTerm pt1 = new PolicyTerm("SiteName", ComparatorType.EQUAL, "Blah.");
		PolicyTerm pt2 = new PolicyTerm("SiteName", ComparatorType.EQUAL, "Blah.");
		PolicyTerm pt3 = new PolicyTerm("CallerName", ComparatorType.EQUAL, "Blah.");
		PolicyTerm pt4 = new PolicyTerm("SiteName", ComparatorType.GREATER_EQUAL_THAN, "Blah.");
		PolicyTerm pt5 = new PolicyTerm("SiteName", ComparatorType.EQUAL, "Blue.");
		PolicyTerm pt6 = new PolicyTerm(PropertyName.SITE_NAME, ComparatorType.EQUAL, new String("Blah."));

		assertTrue(pt1.equals(pt2));
		assertFalse(pt1.equals(pt3));
		assertFalse(pt1.equals(pt4));
		assertFalse(pt1.equals(pt5));
		assertTrue(pt1.equals(pt6));
	}

	@Test
	public void testAppliesTo() {
		PolicyTerm pt1 = new PolicyTerm("SiteName", ComparatorType.EQUAL, "Blah.");
		PolicyTerm pt2 = new PolicyTerm("SiteStartOffset", ComparatorType.LESS_EQUAL_THAN, new Integer(3));
		PolicyTerm pt3 = new PolicyTerm("SiteURL", ComparatorType.MATCHES, Pattern.compile(".*/a\\.js$"));

		assertTrue(pt1.appliesTo("Blah."));
		assertTrue(pt1.appliesTo(new String("Blah.")));
		assertFalse(pt1.appliesTo("Blue."));
		assertFalse(pt1.appliesTo(new Integer(2)));

		assertTrue(pt2.appliesTo(new Integer(3)));
		assertTrue(pt2.appliesTo(new Integer(1)));
		assertTrue(pt2.appliesTo(new Integer(-3)));
		assertFalse(pt2.appliesTo(new Integer(4)));
		assertFalse(pt2.appliesTo("3"));

		assertTrue(pt3.appliesTo("http://e.com/test/a.js"));
		assertFalse(pt3.appliesTo("http://e.com/1.js"));
		assertFalse(pt3.appliesTo(new Integer(4)));

	}

}
