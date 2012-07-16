package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
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
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;
import edu.upenn.cis.tomato.util.DebugUtil;
import edu.upenn.cis.tomato.util.ErrorUtil;

public class StaticAnalyzer {
	
	//TODO: Need to customize JS extractor to read from sourceBundle
	private SourceBundle sourceBundle;
	private CallGraph cg;
	private PointerAnalysis pa;
	private FunctionInvocationAnalyzer fia = new FunctionInvocationAnalyzer();
	protected HashMap<SSAVariable, String> variableNameMapping = new HashMap<SSAVariable, String>();
	protected HashMap<String, SourcePosition> cgNodePositions = new HashMap<String, SourcePosition>();
	protected HashMap<SSAVariable, HashSet<SSAVariable>> aliasAnalysisResult = new HashMap<SSAVariable, HashSet<SSAVariable>>();
	
	//TODO: Build a comprehensive list
	protected static TreeMap<String, String> LANG_CONSTRUCTOR_NAME_MAPPING = new TreeMap<String, String>();
	
	private static String WALA_PREAMBLE = "Lpreamble.js";
	private static String WALA_PROLOGUE = "Lprologue.js";
	private static String FAKE_MAIN_NODE = "__WINDOW_MAIN__";
	private static String FAKE_ROOT_NODE = "LFakeRoot";
	
	//TODO: Add language/environment version
	public StaticAnalyzer(CallGraph cg, PointerAnalysis pa) {
		this.cg = cg;
		this.pa = pa;
	}

	public StaticAnalyzer(SourceBundle sourceBundle) {
		this.sourceBundle = sourceBundle;
		Set<MappedSourceModule> scripts = this.sourceBundle.getSourceModules();
		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
		try {
			JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(
					new WebPageLoaderFactory(new CAstRhinoTranslatorFactory(), null),
					scripts.toArray(new SourceModule[scripts.size()]), 
					CGBuilderType.ZERO_ONE_CFA, AstIRFactory.makeDefaultFactory());

			builder.setBaseURL(sourceBundle.getEntryPointURI().toURL());
			this.cg = builder.makeCallGraph(builder.getOptions());
			this.pa = builder.getPointerAnalysis();
		} catch (Exception e) {
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
		
		boolean DEBUG = true;
		
		initializeAnalysis();
		SuspectList sl = new SuspectList();
		sl.addAll(this.fia.getAllSuspects(this.cg, this.pa));
		if (DEBUG) {
			DebugUtil.printSeparationLine();
			System.out.println("===== Function Invocation Suspect List =====\n");
			Iterator<Suspect> iter_sl = sl.iterator();
			while (iter_sl.hasNext()) {
				FunctionInvocationSuspect fis = (FunctionInvocationSuspect) iter_sl.next();
				// System.out.println(fis.attributes.get("CallerWALAName") + "\t" + fis.attributes.get("CalleeWALAName"));
				System.out.println(fis);
			}
		}
		return sl;
	}
		
	public void initializeAnalysis() {
		initializeConstructorNameMapping();

		for (CGNode node : this.cg) {
			IMethod nodeMethod = node.getMethod();
			String nodeClassName = nodeMethod.getClass().getName().toString();
			String nodeName = nodeMethod.getDeclaringClass().getName().toString();
			
			boolean isFunctionDefinition = nodeClassName.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
			boolean isApplicationCode = !nodeName.startsWith(StaticAnalyzer.FAKE_ROOT_NODE);
			
			if (isFunctionDefinition && isApplicationCode) {
				
				// Build variable name mapping
				getCGNodeVariableNameMapping(node, nodeName, nodeMethod, false);
				
				// Build node position list
				IR nodeIR = node.getIR();
				SourcePosition nodePosition = getCGNodePosition(nodeIR,nodeMethod);
				cgNodePositions.put(nodeName,nodePosition);
			}
		}
		getAliasAnalysisResult();
	}
	
	private void initializeConstructorNameMapping() {
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LObject", "Object");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LFunction", "Function");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LArray", "Array");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LStringObject", "String");
		//TODO: Need to make sure the last two make sense
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LNumber", "Number");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LRegExp", "RegExp");
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
		
	private HashMap<SSAVariable, String> getCGNodeVariableNameMapping(
			CGNode node, String nodeName, IMethod method, boolean includeScope) {
		
		if (node == null) {
			return null;
		}

		HashMap<SSAVariable, String> nodeVariableNameMapping = new HashMap<SSAVariable, String>();
		
		IR ir = node.getIR();
		SSAInstruction[] ssai = ir.getInstructions();
		for (int i = 0; i < ssai.length; i++) {
			
			if (ssai[i] == null) {
				continue;
			}
						
			for (int j = 0; j < ssai[i].getNumberOfDefs(); j++) {
				int def_vn = ssai[i].getDef(j);
				String[] ln = ir.getLocalNames(i, def_vn);
				if (ln != null) {
					for (int k = 0; k < ln.length; k++) {
						SSAVariable var = new SSAVariable(nodeName, def_vn);
						if (includeScope) {
							nodeVariableNameMapping.put(var, ln[k] + "@" + nodeName);
						} else {
							nodeVariableNameMapping.put(var, ln[k]);
						}
					}
				}
			}

			for (int j = 0; j < ssai[i].getNumberOfUses(); j++) {
				int use_vn = ssai[i].getUse(j);
				String[] ln = ir.getLocalNames(i, use_vn);
				SSAVariable var = new SSAVariable(nodeName, use_vn);
				if (ln != null) {
					for (int k = 0; k < ln.length; k++) {
						if (includeScope) {
							nodeVariableNameMapping.put(var, ln[k] + "@" + nodeName);
						} else {
							nodeVariableNameMapping.put(var, ln[k]);
						}
					}
				}
			}
			
			if(ssai[i] instanceof AstGlobalRead){		
				int def_vn = ((AstGlobalRead) ssai[i]).getDef();
				SSAVariable var = new SSAVariable(nodeName, def_vn);
				String[] gn = ((AstGlobalRead) ssai[i]).getGlobalName().split(" ");
				if (gn.length == 2) {
					String scope = gn[0];
					String ln = gn[1];
					if (includeScope) {
						nodeVariableNameMapping.put(var, ln + "@" + scope);
					} else {
						nodeVariableNameMapping.put(var, ln);
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
					SSAVariable var = new SSAVariable(nodeName, vn);
					if (includeScope) {
						nodeVariableNameMapping.put(var, ln + "@" + scope);
					} else {
						nodeVariableNameMapping.put(var, ln);
					}
				}
			}
		}
		
		this.variableNameMapping.putAll(nodeVariableNameMapping);
		return nodeVariableNameMapping;
	}
	
	private void getAliasAnalysisResult() {
		
		boolean DEBUG = false;
		
		if (this.pa == null) {
			ErrorUtil.printErrorMessage("Alias analysis result is null.");
			return;
		}

		HeapGraph hg = pa.getHeapGraph();
		Iterator<PointerKey> iter_pks = pa.getPointerKeys().iterator();
		while (iter_pks.hasNext()) {
			PointerKey pk = (PointerKey) iter_pks.next();
			if (pk instanceof LocalPointerKey) {
				String methodName = ((LocalPointerKey) pk).getNode().getMethod().getDeclaringClass().getName().toString();
				int vn = ((LocalPointerKey) pk).getValueNumber();
				SSAVariable variable = new SSAVariable(methodName, vn);
				if(DEBUG) {
					DebugUtil.printDebugMessage("[PointerKey] " + pk + "\t[Variable Name] " + variableNameMapping.get(variable));
				}
				if (!this.aliasAnalysisResult.containsKey(variable)) {
					HashSet<SSAVariable> aliasSet = new HashSet<SSAVariable>();
										
					Iterator<Object> iter_spks = hg.getSuccNodes(pk);
					while (iter_spks.hasNext()) {
						Iterator<Object> iter_ppks = hg.getPredNodes(iter_spks.next());
						while (iter_ppks.hasNext()) {
							Object apk = iter_ppks.next();
							if ((apk instanceof LocalPointerKey) && !apk.equals(pk)) {
								LocalPointerKey alpk = (com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey) apk;
								String aliasMethodName = alpk.getNode().getMethod().getDeclaringClass().getName().toString();
								int aliasVN = alpk.getValueNumber();
								SSAVariable aliasVariable = new SSAVariable(aliasMethodName, aliasVN);
								aliasSet.add(aliasVariable);
								if(DEBUG) {
									DebugUtil.printDebugMessage("[Alias PointerKey] " + apk + "\t[Alias Variable Name] " + variableNameMapping.get(aliasVariable));
								}
							}
						}
					}
					
					if (aliasSet.size() > 0) {
						aliasSet.add(variable);
						Iterator<SSAVariable> iter_as = aliasSet.iterator();
						while (iter_as.hasNext()) {
							SSAVariable ssav = iter_as.next();
							this.aliasAnalysisResult.put(ssav, aliasSet);
						}
					}
				}
			}
		}
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
	
	protected class FunctionInvocationAnalyzer {

		private static final boolean DEBUG = false;
		
		private HashSet<String> walaNodeFilter = new HashSet<String>();
		private HashMap<String, HashSet<FunctionInvocationSuspect>> suspectAliasIndex = new HashMap<String, HashSet<FunctionInvocationSuspect>>();
		
		private void initialzeNodeFilter() {
			walaNodeFilter.add("make_node0");
			walaNodeFilter.add("make_node1");
			walaNodeFilter.add("make_node2");
			walaNodeFilter.add("make_node3");
			walaNodeFilter.add("make_node4");
		}
				
		private SuspectList getAllSuspects(CallGraph cg, PointerAnalysis pa) {
			
			initialzeNodeFilter();
			SuspectList sl = new SuspectList();
			// Iterate over all CG nodes to build SuspectList
			for (CGNode callerNode : cg) {
				
				IMethod callerMethod = callerNode.getMethod();
				String callerClassName = callerMethod.getClass().getName().toString();
				String callerNodeName = callerMethod.getDeclaringClass().getName().toString();
				String callerFunctionName = getCGNodeFunctionName(callerNodeName);
				
				boolean isFunctionDefinition = callerClassName.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
				boolean isApplicationCode = !callerNodeName.startsWith(WALA_PROLOGUE) 
											&& !callerNodeName.startsWith(WALA_PREAMBLE)
											&& !callerNodeName.startsWith(FAKE_ROOT_NODE);
				boolean isWALASpecificCaller = walaNodeFilter.contains(callerFunctionName);
				boolean isMainNode = callerFunctionName.equals(FAKE_MAIN_NODE);
				boolean isWrapperNode = (callerNodeName.split("/").length == 1);
				
				
				if (isFunctionDefinition && isApplicationCode && !isWALASpecificCaller && !isWrapperNode) {
					
					if (DEBUG) {
						DebugUtil.printSeparationLine();
						DebugUtil.printDebugMessage("[Caller Function]\t" + callerFunctionName);
						DebugUtil.printCGNodeDefinedAndUsed(callerNode);
					}
					
					SourcePosition callerPosition = cgNodePositions.get(callerNodeName);
					
					if (callerPosition == null) {
						ErrorUtil.printErrorMessage("Failed to identify CG node postition.");
					}
					
					IR callerIR = callerNode.getIR();
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
							boolean isAnonymous = (calleeFunctionName.split("@").length == 2);
							boolean isLanguageConstructor = StaticAnalyzer.LANG_CONSTRUCTOR_NAME_MAPPING.containsValue(calleeFunctionName);
							boolean isWALASpecificCallee = walaNodeFilter.contains(calleeFunctionName);
							
							if (isMainNode && isWALASpecificCallee) {
								continue;
							}
							
							//TODO: Need to investigate the dispatch case;
							SSAInstruction[] callerInstructions = callerIR.getInstructions();
							SourcePosition calleePosition = cgNodePositions.get(calleeNodeName);
							
							if (calleePosition == null && !isLanguageConstructor) {
								ErrorUtil.printErrorMessage("Failed to identify CG node postition.");
							}
													
							IntSet is = callerIR.getCallInstructionIndices(csr);
							IntIterator iter_is = is.intIterator();
							while (iter_is.hasNext()) {
								int ssaIndex = iter_is.next();
								JavaScriptInvoke instruction = (JavaScriptInvoke) callerInstructions[ssaIndex];
								int argCount = instruction.getNumberOfParameters() - 2;
								SSAVariable funcVar = new SSAVariable(callerNodeName, instruction.getFunction());
								
								//TODO: Should investigate why sometimes the var name is either null or in strange format
								if (DEBUG) {
									DebugUtil.printDebugMessage(" [Referred Function Name] " + variableNameMapping.get(funcVar) + "\n");
									if(!isConstructor) {
										DebugUtil.printDebugMessage(" [Function Variable Alias Analysis] " + funcVar);
										HashSet<SSAVariable> aliasSet = aliasAnalysisResult.get(funcVar);
										if (aliasSet != null) {
											Iterator<SSAVariable> iter_as = aliasSet.iterator();
											while (iter_as.hasNext()) {
												SSAVariable aliasVar = iter_as.next();
												System.out.println(aliasVar + "\t[Name] " + variableNameMapping.get(aliasVar));
											}
										}
										System.out.println("");
									}
								}
																							
								CAstSourcePositionMap.Position p = ((AstMethod) callerMethod).getSourcePosition(ssaIndex);
								if(p!=null){
									FunctionInvocationSuspect fis = new FunctionInvocationSuspect(
											new SourcePosition(p.getURL(), p.getFirstOffset(), p.getLastOffset()), funcVar);
									sl.add(fis);
									fis.setAttribute(PropertyName.CALLER_NAME, callerFunctionName);
									fis.setAttribute(PropertyName.CALLER_WALA_NAME, callerNodeName);
									if (callerPosition != null) {
										fis.attributes.put(PropertyName.CALLER_URL, callerPosition.getURLString());
										fis.attributes.put(PropertyName.CALLER_START_OFFSET, callerPosition.getStartOffset());
										fis.attributes.put(PropertyName.CALLER_END_OFFSET, callerPosition.getEndOffset());
									}
									fis.setAttribute(PropertyName.CALLEE_NAME, calleeFunctionName);
									fis.setAttribute(PropertyName.CALLEE_WALA_NAME, calleeNodeName);
									if (calleePosition != null) {
										fis.attributes.put(PropertyName.CALLEE_URL, callerPosition.getURLString());
										fis.attributes.put(PropertyName.CALLEE_START_OFFSET, callerPosition.getStartOffset());
										fis.attributes.put(PropertyName.CALLEE_END_OFFSET, callerPosition.getEndOffset());
									}
									fis.setAttribute(PropertyName.ARGUMENT_COUNT, argCount);
									fis.setAttribute(PropertyName.IS_CONSTRUCTOR, isConstructor);
									fis.setAttribute(PropertyName.IS_ANONYMOUS, isAnonymous);
									
									HashSet<FunctionInvocationSuspect> aliasSuspectSet = suspectAliasIndex.get(calleeNodeName);
									if(aliasSuspectSet == null) {
										aliasSuspectSet = new HashSet<FunctionInvocationSuspect>();
										suspectAliasIndex.put(calleeNodeName, aliasSuspectSet);
									}
									aliasSuspectSet.add(fis);
									
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
			
			Iterator<Suspect> iter_sl = sl.iterator();
			while (iter_sl.hasNext()) {
				FunctionInvocationSuspect fis = (FunctionInvocationSuspect) iter_sl.next();
				HashSet<FunctionInvocationSuspect> aliasSuspectSet = suspectAliasIndex.get(fis.getAttribute(PropertyName.CALLEE_WALA_NAME));
				if (aliasSuspectSet != null && aliasSuspectSet.size() > 1) {
					fis.setAttribute(PropertyName.ALIAS_SUSPECT, aliasSuspectSet);
				}
			}
			
			return sl;
		}
	}
}
