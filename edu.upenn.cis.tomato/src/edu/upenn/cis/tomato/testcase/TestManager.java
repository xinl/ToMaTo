package edu.upenn.cis.tomato.testcase;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

//import org.mozilla.javascript.RhinoToAstTranslator;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.FileMapping;
import com.ibm.wala.cast.js.html.IdentityUrlResolver;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

import edu.upenn.cis.tomato.application.FunctionInvocation;
import edu.upenn.cis.tomato.policy.example.PolicyExample;

public class TestManager extends JSCallGraphBuilderUtil{
	
//	public static void TestBasicUI() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
//
//		// Mashup URL
//		String MashupURL = "file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\basic\\Basic.html";
//		URL url = new URL(MashupURL); 
//		
//		// Choose Mode
//		RhinoToAstTranslator.WarningMode = true;
//		
//		// JavaScript Basic Definition
//		JSCallGraphBuilderUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
//		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
//
//		// Extract JavaScript from HTML 
//		SourceModule[] sources = getSources(url);
//		
//		// Call Graph
//		JSCFABuilder builder = makeCGBuilder(new WebPageLoaderFactory(translatorFactory), sources, true);
//		builder.setBaseURL(url); CallGraph cg = builder.makeCallGraph(builder.getOptions());
//	
//		// Pointer Analysis
//		PointerAnalysis pa = builder.getPointerAnalysis();
//		
//		// Visualization
//		new JsViewer(cg, pa);
//
//	}
	
	public static void testFunctionInvocation() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {

		// Mashup URL
		String mashupURL = (new File("").getAbsolutePath()+"/dat/test/function/BasicFunctionInvocation.html").replace("/", File.separator);
		if(File.separator.equals("\\"))
		{
			mashupURL = "file:///" + mashupURL;
		}
		else
		{
			mashupURL = "file://" + mashupURL;
		}
		
		URL url = new URL(mashupURL); 
		
		// Parse Mashup Page Name
		String[] urlPattern = null;
		if(File.separator.equals("\\"))
		{
			urlPattern = mashupURL.split("\\\\");
		}
		else
		{
			urlPattern = mashupURL.split(File.separator);
		}
		 
		String MashupPageName = "L" + urlPattern[urlPattern.length-1];
		
		// Initialize Policy
		PolicyExample.Example_FunctionInvocation();
        JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
        Set<MappedSourceModule> scripts = WebUtil.extractScriptFromHTML(url);
        MappedSourceModule script = (MappedSourceModule) (scripts.toArray(new SourceModule[scripts.size()])[0]);
        FileMapping mapping = script.getMapping(); // to map line numbers in temporary js file to original web page
        // building call graph
        JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(new WebPageLoaderFactory(
                                                                            new CAstRhinoTranslatorFactory(), 
                                                                            null), 
                                                                    scripts.toArray(new SourceModule[scripts.size()]),
                                                                    CGBuilderType.ZERO_ONE_CFA, 
                                                                    AstIRFactory.makeDefaultFactory());
        builder.setBaseURL(url);
        CallGraph cg = builder.makeCallGraph(builder.getOptions());
		
		// Detect Function Invocation Violation
		FunctionInvocation.detectFunctionInvocationViolation(MashupPageName, cg);
	}
	
//	TODO: Add by Anand, need to be organized.
//	public static void TestObjectMethodInvocation() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
//
//		// Mashup URL
//		String MashupURL = "file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\function\\ObjectMethodInvocation.html";
//		URL url = new URL(MashupURL); 
//		
//		// Parse Mashup Location
//		String[] urlPattern = MashupURL.split("\\\\");
//		String MashupPageName = "L" + urlPattern[urlPattern.length-1];
//		
//		// Choose Mode
//		RhinoToAstTranslator.WarningMode = true;
//		
//		// Initialize Policy
//		PolicyExample.Example_ObjectMethodInvocation();
//		
//		// JavaScript Basic Definition
//		JSCallGraphBuilderUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
//		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
//
//		// Extract JavaScript from HTML 
//		SourceModule[] sources = getSources(url);
//		
//		// Call Graph
//		JSCFABuilder builder = makeCGBuilder(new WebPageLoaderFactory(translatorFactory), sources, true);
//		builder.setBaseURL(url); CallGraph cg = builder.makeCallGraph(builder.getOptions());
//		
//		// Detect Function Invocation Violation
//		FunctionInvocation.DetectFunctionInvocationViolation(MashupPageName, cg);
//	}
	
//	
//	
//	public static void TestAliasAnalysis(String type) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
//
//		// ToMaTo System Initialization
//		edu.upenn.cis.tomato.util.Util.InitializeSystemBuiltinVariables();
//		
//		String MashupURL = null;
//		if(type.equalsIgnoreCase("system"))
//		{
//			// Mashup URL
//			MashupURL = "file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\alias\\Alias_System.html";
//			// Initialize Policy
//			PolicyExample.Example_AliasAnalysis_System();
//		}
//		else if(type.equalsIgnoreCase("global"))
//		{
//			// Mashup URL
//			MashupURL = "file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\alias\\Alias_Global.html";
//			// Initialize Policy
//			PolicyExample.Example_AliasAnalysis_Global();
//		}
//		else if(type.equalsIgnoreCase("user"))
//		{
//			// Mashup URL
//			MashupURL = "file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\alias\\Alias_User.html";
//			// Initialize Policy
//			PolicyExample.Example_AliasAnalysis_User();
//		}
//			
//		URL url = new URL(MashupURL); 
//		
//		// Parse Mashup Location
//		String[] urlPattern = MashupURL.split("\\\\");
//		String MashupPageName = "L" + urlPattern[urlPattern.length-1];
//		
//		// Choose Mode
////		RhinoToAstTranslator.WarningMode = true;		
//		
//		// JavaScript Basic Definition
//		JSCallGraphBuilderUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
//		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
//
//		// Extract JavaScript from HTML 
//		SourceModule[] sources = getSources(url);
//		
//		// Call Graph & Pointer Analysis
//		JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(new WebPageLoaderFactory(translatorFactory), sources, true);
//		builder.setBaseURL(url);
//		CallGraph cg = builder.makeCallGraph(builder.getOptions());
//		PointerAnalysis pa = builder.getPointerAnalysis();
//		
//		// Detect Function Invocation Violation
//		AliasAnalysis.FindVariableAlias(MashupPageName, cg, pa);
//	}
//	
//	public static void TestInformationFlow() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
//
//		// Mashup URL
//		String MashupURL = "file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\function\\ObjectMethodInvocation.html";
//		URL url = new URL(MashupURL); 
//		
//		// Parse Mashup Location
//		String[] urlPattern = MashupURL.split("\\\\");
//		String MashupPageName = "L" + urlPattern[urlPattern.length-1];
//		
//		// Choose Mode
//		RhinoToAstTranslator.WarningMode = true;
//		
//		// Initialize Policy
//		PolicyExample.Example_InformationFlow();
//		
//		// JavaScript Basic Definition
//		Util.setTranslatorFactory(new CAstRhinoTranslatorFactory());
//		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
//
//		// Extract JavaScript from HTML 
//		SourceModule[] sources = getSources(url);
//		
//		// Call Graph
//		JSCFABuilder builder = makeCGBuilder(new WebPageLoaderFactory(translatorFactory), sources, true);
//		builder.setBaseURL(url); CallGraph cg = builder.makeCallGraph(builder.getOptions());
//		
//		ExplodedInterproceduralCFG eicfg = ExplodedInterproceduralCFG.make(cg);
//	    ContextInsensitiveInformationFlow ciif = new ContextInsensitiveInformationFlow(eicfg, MashupPageName);
//	}
//	
//	public static void TestCodeInstrumentation() throws IOException, IllegalArgumentException, CancelException{
//		
//		String MashupURL = "file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\instrumentation\\instrumentation.html";
//		URL url = new URL(MashupURL);
//		
//		// Parse Mashup Location
//		String[] urlPattern = MashupURL.split("\\\\");
//		String MashupPageName = "L" + urlPattern[urlPattern.length-1];
//		
//		// Choose Mode
//		RhinoToAstTranslator.WarningMode = true;
//		
//		// Initialize Policy
//		PolicyExample.Example_Instrumentation();
//		
//		// JavaScript Basic Definition
//		Util.setTranslatorFactory(new CAstRhinoTranslatorFactory());
//		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
//
//		// Extract JavaScript from HTML 
//		SourceModule[] sources = getSources(url);
//		
//		// Call Graph
//		JSCFABuilder builder = com.ibm.wala.cast.js.test.Util.makeCGBuilder(new WebPageLoaderFactory(com.ibm.wala.cast.js.ipa.callgraph.Util.getTranslatorFactory()), sources, true);
//		builder.setBaseURL(url); CallGraph cg = builder.makeCallGraph(builder.getOptions());
//		
//		// Detect Function Invocation Violation
//		FunctionInvocation.DetectFunctionInvocationViolation(MashupPageName, cg);
//		
//		// Dump violation sites to file to interface with the code instrumentation phase
//		edu.upenn.cis.tomato.util.DebugUtil.DEBUG_PrintSeperationLine();
//		edu.upenn.cis.tomato.util.DebugUtil.DEBUG_PrintDebugMessage("File contains violation sites is cached at " + ToMaTo.violationSitePath);
//		edu.upenn.cis.tomato.util.Util.WriteViolationSitesToFile(ToMaTo.ViolationSites, ToMaTo.violationSitePath);
//		edu.upenn.cis.tomato.util.Util.PrintViolationSites(ToMaTo.ViolationSites);
//	}
	
	public static SourceModule[] getSources(URL url) throws IOException {
					
		DefaultSourceExtractor sourceExtractor = new DefaultSourceExtractor();
		Set<MappedSourceModule> sourcesMap = sourceExtractor.extractSources(url, new JerichoHtmlParser(), new IdentityUrlResolver());		
		SourceModule[] sources = new SourceFileModule[sourcesMap.size()]; int i = 0;
		for (SourceModule m : sourcesMap)
		{
			sources[i++] = m;
		}
		
		return sources;
	}

}
