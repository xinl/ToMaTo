package edu.upenn.cis.tomato.core;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

import edu.upenn.cis.tomato.application.ToMaTo;

public class StaticAnalyzer {
	private SourceBundle sourceBundle;
	// need to customize JS extractor to read from sourceBundle
	private SourceModule sourceModule;
	private CallGraph cg;
	private PointerAnalysis pa;
	private TreeMap<String, TreeMap<Integer, String>> variableNameMapping = new TreeMap<String, TreeMap<Integer, String>>();
	
	protected static String MAIN_FUNCTION = "__WINDOW_MAIN__";
	protected static String WALA_PREAMBLE = "Lpreamble.js";
	protected static String WALA_PROLOGUE = "Lprologue.js";
	protected static String FAKE_ROOT_NODE = "LFakeRoot";

	// consider adding more options?
	//TODO: Add language/environment version
	public StaticAnalyzer(CallGraph cg, PointerAnalysis pa) {
		this.cg = cg;
		this.pa = pa;
	}

	public StaticAnalyzer(SourceBundle sourceBundle) {
		
		this.sourceBundle = sourceBundle;
		Set<MappedSourceModule> scripts = this.sourceBundle.getSourceModules();
				
		try {
			JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(
					new WebPageLoaderFactory(new CAstRhinoTranslatorFactory(), null),
					scripts.toArray(new SourceModule[scripts.size()]), 
					CGBuilderType.ZERO_ONE_CFA, AstIRFactory.makeDefaultFactory());

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
	
	public CallGraph getCG() {
		return cg;
	}

	public PointerAnalysis getPA() {
		return pa;
	}

	public SuspectList getAllSuspects() {
		
		SuspectList<Suspect> sl = new SuspectList<Suspect>();
		sl.addAll(FunctionInvocationAnalyzer.getAllSuspects(this));
		return sl;
	}
	
	protected TreeMap<Integer, String> getCGNodeVariableNameMapping(
			CGNode node, String nodeName, IMethod method,
			boolean includeScope, TreeSet<Integer> variableSet) {
		
		if (node == null) {
			return null;
		}

		TreeMap<Integer, String> nodeVariableNameMapping = this.variableNameMapping.get(nodeName);
		if (nodeVariableNameMapping == null) {
			nodeVariableNameMapping = new TreeMap<Integer, String>();
			this.variableNameMapping.put(nodeName, nodeVariableNameMapping);
		} else {
			return nodeVariableNameMapping;
		}

		IR ir = node.getIR();
		SSAInstruction[] ssai = ir.getInstructions();
		for (int i = 0; i < ssai.length; i++) {
			
			if (ssai[i] == null) {
				continue;
			}
			
			// filter out declaration IRs. Or else all variables will become aliases.
			/*if ((ssai[i] instanceof AstGlobalRead) && ((AstGlobalRead) ssai[i]).getGlobalName().equals("global $$undefined")) {
				 continue;
			}*/

			for (int j = 0; j < ssai[i].getNumberOfDefs(); j++) {
				int def_vn = ssai[i].getDef(j);
				String[] ln = ir.getLocalNames(i, def_vn);
				if (variableSet != null) {
					variableSet.add(def_vn);
				}
				if (ln != null) {
					for (int k = 0; k < ln.length; k++) {
						if (includeScope) {
							nodeVariableNameMapping.put(def_vn, ln[k] + "@" + nodeName);
						} else {
							nodeVariableNameMapping.put(def_vn, ln[k]);
						}
					}
				}
			}

			for (int j = 0; j < ssai[i].getNumberOfUses(); j++) {
				int use_vn = ssai[i].getUse(j);
				String[] ln = ir.getLocalNames(i, use_vn);
				if (variableSet != null) {
					variableSet.add(use_vn);
				}
				if (ln != null) {
					for (int k = 0; k < ln.length; k++) {
						if (includeScope) {
							nodeVariableNameMapping.put(use_vn, ln[k] + "@" + nodeName);
						} else {
							nodeVariableNameMapping.put(use_vn, ln[k]);
						}
					}
				}
			}
		}

		return nodeVariableNameMapping;
	}
}
