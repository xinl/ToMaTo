package edu.upenn.cis.tomato.test;

import java.io.File;
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

import edu.upenn.cis.tomato.core.StaticAnalyzer;


public class StaticAnalyzerTest {
	
	@Test
	public void testFunctionInvocationAnalyzer() throws Exception {
		
		String mashupURL = (new File("").getAbsolutePath()+"/dat/test/function/BasicFunctionInvocation.html").replace("/", File.separator);
		if (File.separator.equals("\\")) {
			mashupURL = "file:///" + mashupURL;
		} else {
			mashupURL = "file://" + mashupURL;
		}
		
		URL url = new URL(mashupURL); 
				
		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
        Set<MappedSourceModule> scripts = WebUtil.extractScriptFromHTML(url);
        JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(
        		new WebPageLoaderFactory(new CAstRhinoTranslatorFactory(), null), 
        		scripts.toArray(new SourceModule[scripts.size()]),
        		CGBuilderType.ZERO_ONE_CFA,
        		AstIRFactory.makeDefaultFactory());
        builder.setBaseURL(url);
        CallGraph cg = builder.makeCallGraph(builder.getOptions());
        PointerAnalysis pa = builder.getPointerAnalysis();
        StaticAnalyzer sa = new StaticAnalyzer(cg, pa);
        sa.getAllSuspects();
	}
	
}
