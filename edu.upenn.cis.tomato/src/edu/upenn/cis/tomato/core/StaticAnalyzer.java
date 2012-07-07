package edu.upenn.cis.tomato.core;

import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;

public class StaticAnalyzer {
	private SourceBundle sourceBundle;
	// need to customize JS extractor to read from sourceBundle
	private SourceModule sourceModule;
	private CallGraph cg;
	private PointerAnalysis pa;
			
	public static String MAIN_FUNCTION = "__WINDOW_MAIN__";
	public static String WALA_PREAMBLE = "Lpreamble.js";
	public static String WALA_PROLOGUE = "Lprologue.js";
	public static String FAKE_ROOT_NODE = "LFakeRoot";

	// consider adding more options?
	public StaticAnalyzer(SourceBundle sourceBundle) {
		//TODO: Add language/environment version
		
		this.sourceBundle = sourceBundle;
		Set<MappedSourceModule> scripts = sourceBundle.getSourceModules();
				
		try 
		{
			JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(
					new WebPageLoaderFactory(new CAstRhinoTranslatorFactory(), null),
					scripts.toArray(new SourceModule[scripts.size()]), 
					CGBuilderType.ZERO_ONE_CFA, 
					AstIRFactory.makeDefaultFactory());
			
			builder.setBaseURL(sourceBundle.getEntryPointURI().toURL());
			this.cg = builder.makeCallGraph(builder.getOptions());
			this.pa = builder.getPointerAnalysis();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public SourceBundle getSourceBundle() {
		return sourceBundle;
	}
	
	public SuspectList getAllSuspects() {
		
		SuspectList<Suspect> sl = new SuspectList<Suspect>();
		sl.addAll(FunctionInvocationAnalyzer.getAllSuspects(this.cg, this.pa));
		return sl;
	}
}
