package edu.upenn.cis.tomato.test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;

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
