package edu.upenn.cis.tomato.core;

import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.IR;

import edu.upenn.cis.tomato.util.DebugUtil;
import edu.upenn.cis.tomato.util.ErrorUtil;


public class FunctionInvocationAnalyzer {

	public static boolean DEBUG = true;
	public static SuspectList<FunctionInvocationSuspect> getAllSuspects(CallGraph cg, PointerAnalysis pa) {
		
		if (DEBUG) {
			DebugUtil.printSeparationLine();
		}
	
		SuspectList<FunctionInvocationSuspect> sl = new SuspectList<FunctionInvocationSuspect>();
		
		// Iterate over All CG Nodes
		for (CGNode node : cg) {
			
			IMethod method = node.getMethod();
			String className = method.getClass().getName().toString();
			String nodeName = method.getDeclaringClass().getName().toString();
			
			boolean isFunctionDefinition = className.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject");
			boolean isApplicationCode = !nodeName.startsWith(StaticAnalyzer.WALA_PROLOGUE) 
										|| !nodeName.startsWith(StaticAnalyzer.WALA_PREAMBLE)
										|| !nodeName.startsWith(StaticAnalyzer.FAKE_ROOT_NODE);
			
			if (isFunctionDefinition && isApplicationCode) {
				
				IR ir = node.getIR();
				String origin = edu.upenn.cis.tomato.util.Util.getCGNodeOrigin(ir,method).toLowerCase();
				Iterator<CallSiteReference> iter_cs = ir.iterateCallSites();
								
				while(iter_cs.hasNext())
				{
					CallSiteReference csr = iter_cs.next();
					Iterator<CGNode> targets = cg.getPossibleTargets(node, csr).iterator();
					while (targets.hasNext()) 
					{
						CGNode target = (CGNode) targets.next();
						String[] functionNameArray = target.getMethod().getDeclaringClass().getName().toString().split("/");
						String functionName = null;

						// Parse out function name
						if (functionNameArray != null
								&& functionNameArray.length > 0) 
						{
							functionName = functionNameArray[functionNameArray.length - 1];
						}
						else 
						{
							ErrorUtil.printErrorMessage("Unexpected function name format.");
						}				
						
					}
				}
			}
		}
		
		return sl;

	}
}
