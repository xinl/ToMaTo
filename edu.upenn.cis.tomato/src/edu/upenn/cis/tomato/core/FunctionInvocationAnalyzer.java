package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import edu.upenn.cis.tomato.util.DebugUtil;
import edu.upenn.cis.tomato.util.ErrorUtil;

public class FunctionInvocationAnalyzer {

	public static boolean DEBUG = true;
	public static SuspectList<FunctionInvocationSuspect> getAllSuspects(StaticAnalyzer sa) {
		
		
		SuspectList<FunctionInvocationSuspect> sl = new SuspectList<FunctionInvocationSuspect>();
		HashMap<String, SourcePosition> nodePositions = new HashMap<String, SourcePosition>();
		
		// Iterate over all CG nodes to build the <CGNodeName, SourcePosition> mapping
		// This is particularly useful to handle constructor invocation suspect
		for (CGNode node : sa.getCG()) {
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
		for (CGNode callerNode : sa.getCG()) {
			
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
				
				IR callerIR = callerNode.getIR();
				TreeMap<Integer, String> variableNameMapping = sa.getCGNodeVariableNameMapping(callerNode, callerNodeName, callerMethod, false, null);
				
				Iterator<CallSiteReference> iter_csr = callerIR.iterateCallSites();			
				while (iter_csr.hasNext()) {
					
					CallSiteReference csr = iter_csr.next();
					Iterator<CGNode> targets = sa.getCG().getPossibleTargets(callerNode, csr).iterator();
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
	
	private static String getCGNodeFunctionName(String declaringName)
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
	
	private static SourcePosition getCGNodePosition(IR ir, IMethod method) {

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
}
