package edu.upenn.cis.tomato.test;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.tomato.core.Policy;
import edu.upenn.cis.tomato.core.PolicyNode;
import edu.upenn.cis.tomato.core.PolicyParser;
import edu.upenn.cis.tomato.core.Policy.PolicyTermGroup;

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
	public void testPolicyGetRawDNF() throws ParseException {
		for (String p : ps) {
			PolicyParser pp = new PolicyParser(p);
			PolicyNode pn = Policy.getRawDNF(pp.getPolicyTree().getLeft());
			//System.out.println(pn);
			assertTrue(Policy.isDNF(pn));
		}
	}

	@Test
	public void testGetTermGroups() throws ParseException {
		// Expected results:
		int[][] expected = {
				{0, 2},
				{0, 2},
				{2, 1}
		};
		
		for (int i = 0; i < ps.length; i++) {
			Policy p = new Policy(ps[i]);
			List<PolicyTermGroup> staticTerms = p.getStaticTermGroups();
			List<PolicyTermGroup> dynamicTerms = p.getDynamicTermGroups();
			//System.out.println("Static:" + staticTerms);
			//System.out.println("Dynamic:" + dynamicTerms);
			assertEquals(expected[i][0], staticTerms.size());
			assertEquals(expected[i][1], dynamicTerms.size());
			
		}
		
	}

}
