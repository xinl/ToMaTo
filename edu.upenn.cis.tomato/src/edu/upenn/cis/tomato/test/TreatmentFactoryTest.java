package edu.upenn.cis.tomato.test;

import static org.junit.Assert.*;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.tomato.core.Policy;
import edu.upenn.cis.tomato.core.Suspect.SuspectType;
import edu.upenn.cis.tomato.core.TreatmentFactory;
import edu.upenn.cis.tomato.core.TreatmentFactory.Treatment;

public class TreatmentFactoryTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testTreatmentFactory() throws ParseException {
		Policy[] ps = {
				new Policy("SiteName == \"foo.bar\" | SiteURL == \"http://t.co/a.js\" & TimesInvoked > 3 : prohibit"),
				new Policy("SiteStartOffset == 9 & SiteEndOffset == 24 : custom(\"{ _retVal = 2001; } else { alert(123); }\")") };
		TreatmentFactory tf = new TreatmentFactory();
		for (Policy p : ps) {
			tf.makeTreatment(p);
		}
		String bon = tf.BASE_OBJECT_NAME;
		assertTrue(bon.length() == 23);

		String expected = "function " + bon + "(){}";
		expected += bon + ".t1 = function (_static, _context, _func) { " + "arguments = Array.prototype.slice.apply(arguments, [3, arguments.length]);"
				+ "var _cond = _static || (this.t1.TimesInvoked > 3);" + "var _retVar = null;" + "if (_cond) {;} else { _retVar = _func.apply(_context, arguments);}" + "if (!_static) {this.t1.TimesInvoked++;}"
				+ "return _retVar;" + "};" + bon + ".t1.TimesInvoked = 0;";
		expected += bon + ".t2 = function (_static, _context, _func) { " + "arguments = Array.prototype.slice.apply(arguments, [3, arguments.length]);"
				+ "var _cond = _static;" + "var _retVar = null;" + "if (_cond) { _retVal = 2001; } else { alert(123); } " + "return _retVar;" + "};";

		assertEquals(removeSpace(expected), removeSpace(tf.getDefinitions()));
		//System.out.println(tf.getDefinitions());
	}

	@Test
	public void testTreatment() throws ParseException {
		Policy p1 = new Policy("SiteName == \"foo.bar\" | SiteURL == \"http://t.co/a.js\" & TimesInvoked > 3 : prohibit");
		Policy p2 = new Policy("SiteStartOffset == 9 & SiteEndOffset == 24 : custom(\"{ _retVal = 2001; } else { alert(123); }\")");
		TreatmentFactory tf = new TreatmentFactory();
		Treatment t1 = tf.makeTreatment(p1);
		Treatment t2 = tf.makeTreatment(p2);
		String result1 = t1.apply("foo.bar(a, b, c)", SuspectType.FUNCTION_INVOCATION, true);
		String result2 = t2.apply("alert()", SuspectType.FUNCTION_INVOCATION, false);
		String expected1 = tf.BASE_OBJECT_NAME + ".t1(true, foo, foo.bar, a, b, c)";
		String expected2 = tf.BASE_OBJECT_NAME + ".t2(false, null, alert)";
		assertEquals(removeSpace(expected1), removeSpace(result1));
		assertEquals(removeSpace(expected2), removeSpace(result2));
	}

	private String removeSpace(String str) {
		return str.replaceAll("\\s", "");
	}

}
