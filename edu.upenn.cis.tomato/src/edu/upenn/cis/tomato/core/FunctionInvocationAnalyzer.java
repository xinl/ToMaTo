package edu.upenn.cis.tomato.core;

import java.util.Iterator;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import edu.upenn.cis.tomato.util.DebugUtil;

public class FunctionInvocationAnalyzer {

	public static boolean DEBUG = true;
	public static SuspectList<FunctionInvocationSuspect> getAllSuspects(CallGraph cg, PointerAnalysis pa) {
			
		SuspectList<FunctionInvocationSuspect> sl = new SuspectList<FunctionInvocationSuspect>();
		
		// Iterate over All CG Nodes
		for (CGNode callerNode : cg) {
			
			IMethod callerMethod = callerNode.getMethod();
			String callerClassName = callerMethod.getClass().getName().toString();
			String callerNodeName = callerMethod.getDeclaringClass().getName().toString();
			String callerFunctionName = edu.upenn.cis.tomato.util.Util.getCGNodeFunctionName(callerNodeName);
			
			boolean isFunctionDefinition = callerClassName.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
			boolean isApplicationCode = !callerNodeName.startsWith(StaticAnalyzer.WALA_PROLOGUE) 
										&& !callerNodeName.startsWith(StaticAnalyzer.WALA_PREAMBLE)
										&& !callerNodeName.startsWith(StaticAnalyzer.FAKE_ROOT_NODE);
			
			if (isFunctionDefinition && isApplicationCode) {
				
				if (DEBUG) {
					DebugUtil.printSeparationLine();
					DebugUtil.DEBUG_PrintDebugMessage("[Caller Function]\t" + callerFunctionName);
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
						String calleeFunctionName = edu.upenn.cis.tomato.util.Util.getCGNodeFunctionName(calleeNodeName);
						isFunctionDefinition = calleeClassName.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
						if(!isFunctionDefinition){
							continue;
						}
						
						IR calleeIR = calleeNode.getIR();
						Position calleePosition = edu.upenn.cis.tomato.util.Util.getCGNodePosition(calleeIR,calleeMethod);
												
						IntSet is = callerIR.getCallInstructionIndices(csr);
						IntIterator iter_is = is.intIterator();
						while (iter_is.hasNext()) {
							int ssaIndex = iter_is.next();
							CAstSourcePositionMap.Position p = ((AstMethod) callerMethod).getSourcePosition(ssaIndex);
							if(p!=null){
								FunctionInvocationSuspect fis = new FunctionInvocationSuspect(
										new Position(p.getURL(), p.getFirstOffset(), p.getLastOffset()),
										calleePosition);
								sl.add(fis);
								fis.setAttribute("CallerName", callerFunctionName);
								fis.setAttribute("CalleeName", calleeFunctionName);
								
								if (DEBUG) {
									DebugUtil.DEBUG_PrintDebugMessage("[Created Function Invocation Suspect]\t" + calleeFunctionName);
									System.out.println(fis.toString());
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
