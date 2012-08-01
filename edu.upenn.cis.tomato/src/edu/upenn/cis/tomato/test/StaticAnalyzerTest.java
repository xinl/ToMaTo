package edu.upenn.cis.tomato.test;

import java.io.File;
import java.net.URI;

import org.junit.Test;

import edu.upenn.cis.tomato.core.SourceBundle;
import edu.upenn.cis.tomato.core.StaticAnalyzer;


public class StaticAnalyzerTest {

	@Test
	public void testFunctionInvocationAnalyzer() throws Exception {

		String prefix = new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/../test/function/";
		String htmlString = prefix + "BasicFunctionInvocation.html";
		URI htmlURI = new File(htmlString).toURI().normalize();
		SourceBundle sb = new SourceBundle(htmlURI);
        StaticAnalyzer sa = new StaticAnalyzer(sb);
        sa.getAllSuspects();
	}

}
