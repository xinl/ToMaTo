package edu.upenn.cis.tomato.test;

import static org.junit.Assert.*;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.tomato.core.PolicyNode;
import edu.upenn.cis.tomato.core.PolicyParser;

public class PolicyTest {

	String[] ps = {
			"ActionType = \"invoke\" & (CallerName = \"alert\" | CallerName = \"foo.bar\") & TimeInvoked > 10 : custom(\"code\")",
			"(CallerName = \"eval\" | CallerName = \"foo.bar\") & TimeInvoked >= -10.2 : prohibit",
			"! ! ! (! CallerName = \"alert\" & CallerName = \"foo.bar\") | ! TimeInvoked >= 10 : custom(\"{foo}\")" };

	String[] bad_ps = {
			// bad operators
			"ActionType == \"invoke\" && (CallerName <> \"alert\" || CallerName == \"foo.bar\") : custom(\"code\")",
			// bad names
			"(1CallerName = \"alert\" | -CallerName = \"foo.bar\") & TimeInvoked >= -10.2 : prohibit",
			// incomplete
			"TimeInvoked < 3",
			// bad action argument
			"! ! ! (! CallerName = \"alert\" & CallerName = \"foo.bar\") | ! TimeInvoked >= 10 : custom(22)" };

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testPolicyParser() throws ParseException {
		for (String p : ps) {
			PolicyParser pp = new PolicyParser(p);
			PolicyNode pn = pp.getPolicyTree();
			//System.out.println(pn.toString());
			assertEquals(p, pn.toString());
		}
	}
	
	@Test
	public void testBadPolicyString() {
		for (String p : bad_ps) {
			try {
				PolicyParser pp = new PolicyParser(p);
				pp.getPolicyTree();
				//System.out.println(pn.toString());
				fail("No exception thown on bad input.");
			} catch (ParseException e) {
				
			}
		}
	}

	@Test
	public void testPolicy() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetStaticTerms() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDynamicTerms() {
		fail("Not yet implemented");
	}

}
