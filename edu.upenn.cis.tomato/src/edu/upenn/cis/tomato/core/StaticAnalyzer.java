package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.ssa.*;
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
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;
import edu.upenn.cis.tomato.core.Suspect.SuspectType;
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
	protected static HashMap<String, String> LANG_CONSTRUCTOR_NAME_MAPPING = new HashMap<String, String>();
	
	private static String WALA_PREAMBLE = "Lpreamble.js";
	private static String WALA_PROLOGUE = "Lprologue.js";
	private static String FAKE_MAIN_NODE = "__WINDOW_MAIN__";
	private static String FAKE_ROOT_NODE = "LFakeRoot";
		
	public StaticAnalyzer(SourceBundle sourceBundle) {
		this.sourceBundle = sourceBundle;
		Set<MappedSourceModule> scripts = this.sourceBundle.getSourceModules();
		//TODO: Add language/environment-specific preamble versions
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
		
		boolean DEBUG = false;
		
		initializeAnalysis();
		SuspectList sl = new SuspectList();
		sl.addAll(this.fia.getAllSuspects(this.cg, this.pa));
		if (DEBUG) {
			DebugUtil.printSeparationLine();
			System.out.println("===== Function Invocation Suspect List =====\n");
			Iterator<Suspect> iter_sl = sl.iterator();
			while (iter_sl.hasNext()) {
				Suspect fis = (Suspect) iter_sl.next();
				System.out.println(fis);
			}
		}
		return sl;
	}
		
	public void initializeAnalysis() {
		boolean DEBUG = false;
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
		
		if (DEBUG) {
			DebugUtil.printDebugMessage("[VariableNameMapping Size] " + variableNameMapping.size());
		}
		getAliasAnalysisResult();
	}
	
	private void initializeConstructorNameMapping() {
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LObject", "Object");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LFunction", "Function");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LArray", "Array");
		LANG_CONSTRUCTOR_NAME_MAPPING.put("LStringObject", "String");
		// TODO: Need to make sure the last two make sense. Probably need to
		// refer to code in com.ibm.wala.cast.js.loader.JavaScriptLoader;
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
		
		boolean DEBUG = false;
		
		if (node == null) {
			return null;
		}
				
		IR ir = node.getIR();
		HashMap<SSAVariable, String> nodeVariableNameMapping = new HashMap<SSAVariable, String>();
		HashSet<SSAVariable> missingPutFieldVariables = new HashSet<SSAVariable>();
		// HashMap<Integer, HashSet<Integer>> phiMap = initializePhiMap(ir);
				
		if (DEBUG) {
			DebugUtil.printCGNodeDefinedAndUsed(node);
		}
		
		SSAInstruction[] ssai = ir.getInstructions();
		for (int i = 0; i < ssai.length; i++) {		
			if (ssai[i] == null) {
				continue;
			}
			
			boolean isObjectMemberAccess = false;
			
			for (int j = 0; j < ssai[i].getNumberOfDefs(); j++) {
				int def_vn = ssai[i].getDef(j);
				String[] ln = ir.getLocalNames(i, def_vn);
				SSAVariable var = new SSAVariable(nodeName, def_vn);
				if (ln != null) {
					for (int k = 0; k < ln.length; k++) {
						if(!ln[k].startsWith("$$")) {
							putVariableNameMappingEntry(nodeVariableNameMapping, var, ln[k], nodeName, includeScope);
						} else {
							isObjectMemberAccess = true;
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
						if(!ln[k].startsWith("$$")) {
							putVariableNameMappingEntry(nodeVariableNameMapping, var, ln[k], nodeName, includeScope);
						} else {
							isObjectMemberAccess = true;
						}
					} 
				}
			}
			
			// We enumerate every type of SSA instruction to make sure we never miss one type
			if (ssai[i] instanceof AstGlobalRead) {
				int vn = ((AstGlobalRead) ssai[i]).getDef();
				SSAVariable var = new SSAVariable(nodeName, vn);
				String[] gn = ((AstGlobalRead) ssai[i]).getGlobalName().split(" ");
				if (gn.length == 2) {
					String scope = gn[0];
					String ln = gn[1];
					putVariableNameMappingEntry(nodeVariableNameMapping, var, ln, scope, includeScope);
				} else {
					ErrorUtil.printErrorMessage("Failed to parse AstGlobalRead instruction.");
				}
			} else if (ssai[i] instanceof AstGlobalWrite) {
				int vn = ((AstGlobalWrite) ssai[i]).getVal();
				SSAVariable var = new SSAVariable(nodeName, vn);
				String[] gn = ((AstGlobalWrite) ssai[i]).getGlobalName().split(" ");
				if (gn.length == 2) {
					String scope = gn[0];
					String ln = gn[1];
					putVariableNameMappingEntry(nodeVariableNameMapping, var, ln, scope, includeScope);
				} else {
					ErrorUtil.printErrorMessage("Failed to parse AstGlobalWrite instruction.");
				}
			} else if (ssai[i] instanceof AstLexicalRead) {
				Access[] access = ((AstLexicalRead) ssai[i]).getAccesses();
				for (int j = 0; j < access.length; j++) {
					int vn = access[j].valueNumber;
					String ln = access[j].variableName;
					String scope = access[j].variableDefiner;
					SSAVariable var = new SSAVariable(nodeName, vn);
					putVariableNameMappingEntry(nodeVariableNameMapping, var, ln, scope, includeScope);
				}
			} else if (ssai[i] instanceof AstLexicalWrite) {
				Access[] access = ((AstLexicalWrite) ssai[i]).getAccesses();
				for (int j = 0; j < access.length; j++) {
					int vn = access[j].valueNumber;
					String ln = access[j].variableName;
					String scope = access[j].variableDefiner;
					SSAVariable var = new SSAVariable(nodeName, vn);
					putVariableNameMappingEntry(nodeVariableNameMapping, var, ln, scope, includeScope);
				}
			} else if (ssai[i] instanceof EachElementHasNextInstruction) { 
				// TODO: Need to fill in necessary code
			} else if (ssai[i] instanceof EachElementGetInstruction) { 
				// TODO: Need to fill in necessary code
			} else if (ssai[i] instanceof SSAUnaryOpInstruction || ssai[i] instanceof SSABinaryOpInstruction) { 
				// class SSABinaryOpInstruction == com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$1
				// Nothing needs to be done for this type	
			} else if (ssai[i] instanceof SSAGetInstruction) { 
				// class com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$3
				// TODO: Need to fill in necessary code
			} else if (ssai[i] instanceof SSANewInstruction) {
				// class com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$4
				// TODO: Need to fill in necessary code
			} else if (ssai[i] instanceof SSAPutInstruction) {
				// class com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$5
				SSAVariable objVar = new SSAVariable(nodeName, ((SSAPutInstruction) ssai[i]).getUse(0));
				SSAVariable memberVar = new SSAVariable(nodeName, ((SSAPutInstruction) ssai[i]).getVal());
				String objName = nodeVariableNameMapping.get(objVar);
				if (objName == null) {
					objName = "" + objVar.getVariableNumber();
					if(!nodeName.startsWith(WALA_PROLOGUE)) {
						missingPutFieldVariables.add(memberVar);
					}
				}
				String memberName = objName + "." + ((SSAPutInstruction) ssai[i]).getDeclaredField().getName().toString();
				putVariableNameMappingEntry(nodeVariableNameMapping, memberVar, memberName, nodeName, includeScope);
				
			} else if (ssai[i] instanceof SetPrototype) {
				// TODO: Potentially useful for reasoning about prototype chain
				// first use var - object; second use - prototype;
			} else if (ssai[i] instanceof PrototypeLookup) {
				// TODO: Potentially useful for reasoning about prototype chain
				// use - object; def - prototype;
			} else if (ssai[i] instanceof JavaScriptPropertyRead) { 
				// TODO: Potentially useful for reasoning about prototype chain
				// This corresponds to JS syntax in form "c = a[b];" 
				// Need prototype chain to be properly tracked to solve object name
				JavaScriptPropertyRead inst = (JavaScriptPropertyRead) ssai[i]; 
				SSAVariable objVar = new SSAVariable(nodeName, inst.getObjectRef());
				SSAVariable memberVar = new SSAVariable(nodeName, inst.getMemberRef());
				String objName = nodeVariableNameMapping.get(objVar);
				String memberName = nodeVariableNameMapping.get(memberVar); 
				SSAVariable defVar = new SSAVariable(nodeName, inst.getDef());
				if(objName != null && memberName != null) {
					putVariableNameMappingEntry(nodeVariableNameMapping, defVar, objName+"["+memberName+"]", nodeName, includeScope);
				}
				else {
					// TODO: Further improvement is required for this case
					// ErrorUtil.printErrorMessage("Failed to get variable name when parsing JavaScriptPropertyRead.");
				}
			} else if (ssai[i] instanceof JavaScriptPropertyWrite) {
				// TODO: Potentially useful for reasoning about prototype chain
				// This corresponds to JS syntax in form "a[b] = c;" 	
			} else if (ssai[i] instanceof JavaScriptInvoke) {
				// We don't really expect to get new information from
				// JavaScriptInvoke instructions
				JavaScriptInvoke inst = (JavaScriptInvoke) ssai[i];
				if (inst.getNumberOfLexicalReads() > 0 || inst.getNumberOfLexicalWrites() > 0) {
					ErrorUtil.printErrorMessage("Unexpected JavaScriptInvoke instruction format.");
					for (int j = 0; j < inst.getNumberOfLexicalReads(); j++) {
						Access access = inst.getLexicalUse(j);
						int vn = access.valueNumber;
						String ln = access.variableName;
						String scope = access.variableDefiner;
						SSAVariable var = new SSAVariable(nodeName, vn);
						putVariableNameMappingEntry(nodeVariableNameMapping, var, ln, scope, includeScope);
					}
					for (int j = 0; j < inst.getNumberOfLexicalWrites(); j++) {
						Access access = inst.getLexicalDef(j);
						int vn = access.valueNumber;
						String ln = access.variableName;
						String scope = access.variableDefiner;
						SSAVariable var = new SSAVariable(nodeName, vn);
						putVariableNameMappingEntry(nodeVariableNameMapping, var, ln, scope, includeScope);
					}
				}
				
				if (isObjectMemberAccess) {
					Position pos = ((AstMethod) method).getSourcePosition(i);
					try {
						String sourceCode = this.sourceBundle.getSourceContent(pos.getURL().toURI());
						if (sourceCode != null) {
							String[] sourceInst = sourceCode.substring(pos.getFirstOffset(), pos.getLastOffset()).split("\\(");
							if (sourceInst.length >= 2) {
								SSAVariable funcVar = new SSAVariable(nodeName, inst.getFunction());
								putVariableNameMappingEntry(nodeVariableNameMapping, funcVar, sourceInst[0], nodeName, includeScope);
							} else {
								ErrorUtil.printErrorMessage("Failed to parse JavaScriptInvoke instruction.");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			} else if (ssai[i] instanceof SSAReturnInstruction) { 
				// Nothing needs to be done for this type
			} else if (ssai[i] instanceof JavaScriptCheckReference) { 
				// Nothing needs to be done for this type
			} else if (ssai[i] instanceof AstIsDefinedInstruction) {
				// Nothing needs to be done for this type
				// use - object; fieldRef - field name; def - boolean value;
			} else if (ssai[i] instanceof SSAConditionalBranchInstruction || ssai[i] instanceof SSAGotoInstruction) {
				// Nothing needs to be done for this type
			} else {
				if(DEBUG) {
					System.out.println(ssai[i].getClass() + "\t" + ssai[i]);
				}
			}
		}
		
		Iterator<SSAVariable> iter_mpfv = missingPutFieldVariables.iterator();
		while (iter_mpfv.hasNext()) {
			SSAVariable memberVar = iter_mpfv.next();
			String[] names = nodeVariableNameMapping.get(memberVar).split("\\.");
			if (names.length != 2) {
			} else {
				SSAVariable objVar = new SSAVariable(nodeName, Integer.parseInt(names[0]));
				String objName = nodeVariableNameMapping.get(objVar);
				if (objName != null) {
					String memberName = objName + "." + names[1];
					putVariableNameMappingEntry(nodeVariableNameMapping, memberVar, memberName, nodeName, includeScope);
				} else {
					// TODO: Further improvement is required for this case
					// ErrorUtil.printErrorMessage("Failed to get object name when parsing SSAPutInstruction.");
				}
			}
		}
		
		this.variableNameMapping.putAll(nodeVariableNameMapping);
		return nodeVariableNameMapping;
	}
	
	private HashMap<Integer, HashSet<Integer>> initializePhiMap(IR ir) {		
		HashMap<Integer, HashSet<Integer>> result = new HashMap<Integer, HashSet<Integer>>();
		Iterator<? extends SSAInstruction> iter_phi = ir.iteratePhis();
		while (iter_phi.hasNext()) {
			SSAPhiInstruction instruction = (SSAPhiInstruction) iter_phi.next();
			for (int i = 0; i < instruction.getNumberOfDefs(); i++) {
				int def_vn = instruction.getDef(i);
				HashSet<Integer> useSet = result.get(def_vn);
				if (useSet == null) {
					useSet = new HashSet<Integer>();
					result.put(def_vn, useSet);
				}
				for (int j = 0; j < instruction.getNumberOfUses(); j++) {
					useSet.add(instruction.getUse(j));
				}
			}
		}
		return result;
	}
	
	private void putVariableNameMappingEntry (HashMap<SSAVariable, String> nodeVariableNameMapping, SSAVariable var, String localName, String scope, boolean includeScope) {
		if (includeScope) {
			nodeVariableNameMapping.put(var, localName + "@" + scope);
		} else {
			nodeVariableNameMapping.put(var, localName);
		}	
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
		
		private HashMap<String, HashSet<Suspect>> suspectAliasIndex = new HashMap<String, HashSet<Suspect>>();
		
		
		private boolean isFunctionDefinition(String name) {
			return name.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
		}

		private boolean isApplicationCode(String name) {
			return !name.startsWith(WALA_PROLOGUE) && !name.startsWith(WALA_PREAMBLE) && !name.startsWith(FAKE_ROOT_NODE);
		}

		private boolean isMainNode(String name) {
			return name.equals(FAKE_MAIN_NODE);
		}

		private boolean isWrapperNode(String name) {
			return (name.split("/").length == 1);
		}

		private boolean isLanguageConstructor(String name) {
			return StaticAnalyzer.LANG_CONSTRUCTOR_NAME_MAPPING.containsValue(name);
		}
		
		private SuspectList getAllSuspects(CallGraph cg, PointerAnalysis pa) {
			
			SuspectList sl = new SuspectList();
			
			for (CGNode callerNode : cg) {
				
				IMethod callerMethod = callerNode.getMethod();
				String callerClassName = callerMethod.getClass().getName().toString();
				String callerNodeName = callerMethod.getDeclaringClass().getName().toString();
				String callerFunctionName = getCGNodeFunctionName(callerNodeName);
				
				if (isFunctionDefinition(callerClassName) && isApplicationCode(callerNodeName) && !isWrapperNode(callerNodeName)) {
					
					if (DEBUG) {
						DebugUtil.printSeparationLine();
						DebugUtil.printDebugMessage("[Caller Function]\t" + callerFunctionName);
						DebugUtil.printCGNodeDefinedAndUsed(callerNode);
					}
					
					System.out.println(callerNodeName);
					
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
							boolean isConstructor = calleeClassName.equalsIgnoreCase("com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector$JavaScriptConstructor");
							boolean isAnonymous = (calleeFunctionName.split("@").length == 2);
																					
							if (isMainNode(callerFunctionName)) {
								continue;
							}
							
							SSAInstruction[] callerInstructions = callerIR.getInstructions();
							SourcePosition calleePosition = cgNodePositions.get(calleeNodeName);
							
							if (calleePosition == null && !isLanguageConstructor(calleeFunctionName)) {
								ErrorUtil.printErrorMessage("Failed to identify CG node postition.");
							}
													
							IntSet is = callerIR.getCallInstructionIndices(csr);
							IntIterator iter_is = is.intIterator();
							while (iter_is.hasNext()) {
								int ssaIndex = iter_is.next();
								JavaScriptInvoke instruction = (JavaScriptInvoke) callerInstructions[ssaIndex];
								int argCount = instruction.getNumberOfParameters() - 2;
								SSAVariable funcVar = new SSAVariable(callerNodeName, instruction.getFunction());
																
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
								if (p != null) {
									Suspect fis = new Suspect(new SourcePosition(p.getURL(), p.getFirstOffset(), p.getLastOffset()),
											SuspectType.FUNCTION_INVOCATION);
									sl.add(fis);
									fis.setAttribute(PropertyName.CALLER_NAME, callerFunctionName);
									fis.setAttribute(PropertyName.CALLER_WALA_NAME, callerNodeName);
									if (callerPosition != null) {
										fis.attributes.put(PropertyName.CALLER_URL, callerPosition.getURLString());
										fis.attributes.put(PropertyName.CALLER_START_OFFSET, callerPosition.getStartOffset());
										fis.attributes.put(PropertyName.CALLER_END_OFFSET, callerPosition.getEndOffset());
									}

									if (!isConstructor) {
										String funcVarName = variableNameMapping.get(funcVar);
										if (funcVarName == null) {
											ErrorUtil.printErrorMessage("Failed to identify function variable name.");
										} else {
											fis.setAttribute(PropertyName.SITE_NAME, funcVarName);
										}
									}

									if (isAnonymous) {
										fis.setAttribute(PropertyName.CALLEE_NAME, "");
									} else {
										fis.setAttribute(PropertyName.CALLEE_NAME, calleeFunctionName);
									}

									fis.setAttribute(PropertyName.CALLEE_WALA_NAME, calleeNodeName);
									if (calleePosition != null) {
										fis.attributes.put(PropertyName.CALLEE_URL, calleePosition.getURLString());
										fis.attributes.put(PropertyName.CALLEE_START_OFFSET, calleePosition.getStartOffset());
										fis.attributes.put(PropertyName.CALLEE_END_OFFSET, calleePosition.getEndOffset());
									}
									fis.setAttribute(PropertyName.ARGUMENT_COUNT, argCount);
									fis.setAttribute(PropertyName.IS_CONSTRUCTOR, isConstructor);

									HashSet<Suspect> aliasSuspectSet = suspectAliasIndex.get(calleeNodeName);
									if (aliasSuspectSet == null) {
										aliasSuspectSet = new HashSet<Suspect>();
										suspectAliasIndex.put(calleeNodeName, aliasSuspectSet);
									}
									aliasSuspectSet.add(fis);

									if (DEBUG) {
										DebugUtil.printDebugMessage("[Created Function Invocation Suspect]\t" + calleeFunctionName);
										System.out.println(fis.toString() + "\n");
									}
								}
							}
						}
					}
				}
			}
			
			Iterator<Suspect> iter_sl = sl.iterator();
			while (iter_sl.hasNext()) {
				Suspect fis = (Suspect) iter_sl.next();
				HashSet<Suspect> aliasSuspectSet = suspectAliasIndex.get(fis.getAttribute(PropertyName.CALLEE_WALA_NAME));
				if (aliasSuspectSet != null && aliasSuspectSet.size() > 1) {
					fis.setAttribute(PropertyName.ALIAS_SUSPECT, aliasSuspectSet);
				}
			}
			
			return sl;
		}
	}
}
