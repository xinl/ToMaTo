package edu.upenn.cis.tomato.test;

import java.io.File;
import java.net.URI;
import java.util.Iterator;

import org.junit.Test;

import edu.upenn.cis.tomato.core.SourceBundle;
import edu.upenn.cis.tomato.core.StaticAnalyzer;
import edu.upenn.cis.tomato.core.Suspect;
import edu.upenn.cis.tomato.core.SuspectList;
import edu.upenn.cis.tomato.util.DebugUtil;


public class StaticAnalyzerTest {

	@Test
	public void testFunctionInvocationAnalyzer() throws Exception {

		boolean DEBUG = true;
		String prefix = new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/../test/function/";
		String htmlString = prefix + "BasicFunctionInvocation.html";
		URI htmlURI = new File(htmlString).toURI().normalize();
		SourceBundle sb = new SourceBundle(htmlURI);
        StaticAnalyzer sa = new StaticAnalyzer(sb);
        SuspectList sl = sa.getAllSuspects();
        if (DEBUG) {
			DebugUtil.printSeparationLine();
			System.out.println("===== Function Invocation Suspect List =====\n");
			Iterator<Suspect> iter_sl = sl.iterator();
			while (iter_sl.hasNext()) {
				Suspect fis = iter_sl.next();
				System.out.println(fis);
			}
		}
	}

}
