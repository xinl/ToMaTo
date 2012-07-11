package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import edu.upenn.cis.tomato.util.DebugUtil;
import edu.upenn.cis.tomato.util.ErrorUtil;

public class StaticAnalyzer {
	private SourceBundle sourceBundle;
	// need to customize JS extractor to read from sourceBundle
	private CallGraph cg;
	private PointerAnalysis pa;
	private FunctionInvocationAnalyzer fia = new FunctionInvocationAnalyzer();
	private TreeMap<String, TreeMap<Integer, String>> variableNameMapping = new TreeMap<String, TreeMap<Integer, String>>();
	
	protected static String MAIN_FUNCTION = "__WINDOW_MAIN__";
	protected static String WALA_PREAMBLE = "Lpreamble.js";
	protected static String WALA_PROLOGUE = "Lprologue.js";
	protected static String FAKE_ROOT_NODE = "LFakeRoot";

	//TODO: Build a comprehensive list
	protected static TreeMap<String, String> LANG_CONSTRUCTOR_NAME_MAPPING = new TreeMap<String, String>();
	
	//TODO: Add language/environment version
	public StaticAnalyzer(CallGraph cg, PointerAnalysis pa) {
		initializeConstructorNameMapping();
		this.cg = cg;
		this.pa = pa;
	}

	public StaticAnalyzer(SourceBundle sourceBundle) {
		initializeConstructorNameMapping();
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
		
		SuspectList sl = new SuspectList();
		sl.addAll(this.fia.getAllSuspects(this.cg, this.pa));
		return sl;
	}
	
	protected void initializeConstructorNameMapping() {
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LObject", "Object");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LFunction", "Function");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LArray", "Array");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LStringObject", "String");
		//TODO: Need to make sure the last two make sense
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LNumber", "Number");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LRegExp", "RegExp");
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
			
			if(ssai[i] instanceof AstGlobalRead){		
				int def_vn = ((AstGlobalRead) ssai[i]).getDef();
				String[] gn = ((AstGlobalRead) ssai[i]).getGlobalName().split(" ");
				if (gn.length == 2) {
					String scope = gn[0];
					String ln = gn[1];
					if (includeScope) {
						nodeVariableNameMapping.put(def_vn, ln + "@" + scope);
					} else {
						nodeVariableNameMapping.put(def_vn, ln);
					}
				} else {
					ErrorUtil.printErrorMessage("Failed to parse AstGlobalRead instruction.");
				}
			}else if(ssai[i] instanceof AstLexicalRead){
				
				Access[] access = ((AstLexicalRead) ssai[i]).getAccesses();
				for(int j = 0; j < ((AstLexicalRead) ssai[i]).getAccessCount(); j++){
					int vn = access[j].valueNumber;
					String ln = access[j].variableName;
					String scope = access[j].variableDefiner;
					
					if (includeScope) {
						nodeVariableNameMapping.put(vn, ln + "@" + scope);
					} else {
						nodeVariableNameMapping.put(vn, ln);
					}
				}
			}
		}
		return nodeVariableNameMapping;
	}
	
	private String getCGNodeFunctionName(String declaringName)
	{
		String functionName = null;
		String[] functionNameArray = declaringName.split("/");
		
		// Parse function name
		if (functionNameArray != null && functionNameArray.length > 0) {
			functionName = functionNameArray[functionNameArray.length - 1];
		} else {
			ErrorUtil.printErrorMessage("Unexpected function name format.");
		}
		
		String trueName = StaticAnalyzer.LANG_CONSTRUCTOR_NAME_MAPPING.get(functionName);
		if(trueName != null){
			return trueName;
		} else {
			return functionName;
		}
	}
	
	private SourcePosition getCGNodePosition(IR ir, IMethod method) {

		SourcePosition pos = null;
		SSAInstruction[] instructions = ir.getInstructions();
		for (int i = 0; i < instructions.length; i++) {

			if (instructions[i] == null) {
				continue;
			}

			Position walaPos = ((AstMethod) method).getSourcePosition(i);
			if (walaPos != null) {

				if (pos == null) {
					pos = new SourcePosition(walaPos.getURL(), walaPos.getFirstOffset(), walaPos.getLastOffset());
				} else {
					if (pos.getStartOffset() > walaPos.getFirstOffset()) {
						pos.setStartOffset(walaPos.getFirstOffset());
					}
					if (pos.getEndOffset() < walaPos.getLastLine()) {
						pos.setEndOffset(walaPos.getLastOffset());
					}
				}
			}
		}
		return pos;
	}
	
	private class FunctionInvocationAnalyzer {

		private static final boolean DEBUG = true;
		private SuspectList getAllSuspects(CallGraph cg, PointerAnalysis pa) {
			
			
			SuspectList sl = new SuspectList();
			HashMap<String, SourcePosition> nodePositions = new HashMap<String, SourcePosition>();
			
			// Iterate over all CG nodes to build the <CGNodeName, SourcePosition> mapping
			// This is particularly useful to handle constructor invocation suspect
			for (CGNode node : cg) {
				IMethod nodeMethod = node.getMethod();
				String nodeClassName = nodeMethod.getClass().getName().toString();
				String nodeName = nodeMethod.getDeclaringClass().getName().toString();

				boolean isFunctionDefinition = nodeClassName.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
				boolean isApplicationCode = !nodeName.startsWith(StaticAnalyzer.FAKE_ROOT_NODE);
				
				if (isFunctionDefinition && isApplicationCode) {
					IR nodeIR = node.getIR();
					SourcePosition nodePosition = getCGNodePosition(nodeIR,nodeMethod);
					nodePositions.put(nodeName,nodePosition);
				}
			}
			
			// Iterate over all CG nodes to build SuspectList
			for (CGNode callerNode : cg) {
				
				IMethod callerMethod = callerNode.getMethod();
				String callerClassName = callerMethod.getClass().getName().toString();
				String callerNodeName = callerMethod.getDeclaringClass().getName().toString();
				String callerFunctionName = getCGNodeFunctionName(callerNodeName);
				
				boolean isFunctionDefinition = callerClassName.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
				boolean isApplicationCode = !callerNodeName.startsWith(StaticAnalyzer.WALA_PROLOGUE) 
											&& !callerNodeName.startsWith(StaticAnalyzer.WALA_PREAMBLE)
											&& !callerNodeName.startsWith(StaticAnalyzer.FAKE_ROOT_NODE);
				
				if (isFunctionDefinition && isApplicationCode) {
					
					if (DEBUG) {
						DebugUtil.printSeparationLine();
						DebugUtil.printDebugMessage("[Caller Function]\t" + callerFunctionName);
						DebugUtil.printCGNodeDefinedAndUsed(callerNode);
					}
					
					SourcePosition callerPosition = nodePositions.get(callerNodeName);
					
					if (callerPosition == null) {
						ErrorUtil.printErrorMessage("Failed to identify CG node postition.");
					}
					
					IR callerIR = callerNode.getIR();
					TreeMap<Integer, String> variableNameMapping = getCGNodeVariableNameMapping(callerNode, callerNodeName, callerMethod, false, null);
					
					Iterator<CallSiteReference> iter_csr = callerIR.iterateCallSites();			
					while (iter_csr.hasNext()) {
						
						CallSiteReference csr = iter_csr.next();
						Iterator<CGNode> targets = cg.getPossibleTargets(callerNode, csr).iterator();
						while (targets.hasNext()) {
							
							CGNode calleeNode = (CGNode) targets.next();
							IMethod calleeMethod = calleeNode.getMethod();
							String calleeClassName = calleeMethod.getClass().getName().toString();
							String calleeNodeName = calleeNode.getMethod().getDeclaringClass().getName().toString(); 
							String calleeFunctionName = getCGNodeFunctionName(calleeNodeName);
							isFunctionDefinition = calleeClassName.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
							boolean isConstructor = calleeClassName.equalsIgnoreCase("com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector$JavaScriptConstructor");
							boolean isLanguageConstructor = StaticAnalyzer.LANG_CONSTRUCTOR_NAME_MAPPING.containsValue(calleeFunctionName);
							
							//TODO: Need to handle the dispatch case;
							SSAInstruction[] callerInstructions = callerIR.getInstructions();
							SourcePosition calleePosition = nodePositions.get(calleeNodeName);
							
							if (calleePosition == null && !isLanguageConstructor) {
								ErrorUtil.printErrorMessage("Failed to identify CG node postition.");
							}
													
							IntSet is = callerIR.getCallInstructionIndices(csr);
							IntIterator iter_is = is.intIterator();
							while (iter_is.hasNext()) {
								int ssaIndex = iter_is.next();
								JavaScriptInvoke instruction = (JavaScriptInvoke) callerInstructions[ssaIndex];
								int argCount = instruction.getNumberOfParameters() - 2;
								
								//TODO: Should look into this to see why sometimes the var name is null or in strange format
								System.out.println(variableNameMapping.get(instruction.getFunction()));
															
								CAstSourcePositionMap.Position p = ((AstMethod) callerMethod).getSourcePosition(ssaIndex);
								if(p!=null){
									FunctionInvocationSuspect fis = new FunctionInvocationSuspect(
											new SourcePosition(p.getURL(), p.getFirstOffset(), p.getLastOffset()),
											calleePosition);
									sl.add(fis);
									fis.setAttribute("CallerName", callerFunctionName);
									fis.setAttribute("CallerPosition", callerPosition);
									fis.setAttribute("CalleeName", calleeFunctionName);
									fis.setAttribute("ArgumentCount", argCount);
									fis.setAttribute("IsConstructor", isConstructor);
									
									if (DEBUG) {
										DebugUtil.printDebugMessage("[Created Function Invocation Suspect]\t" + calleeFunctionName);
										System.out.println(fis.toString()+"\n");
									}
								}
							}
						}
					}
				}
			}
			return sl;
		}
	}
}
