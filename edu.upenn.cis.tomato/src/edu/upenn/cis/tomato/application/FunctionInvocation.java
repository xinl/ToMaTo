package edu.upenn.cis.tomato.application;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.wala.cast.js.html.IncludedPosition;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import edu.upenn.cis.tomato.data.ViolationSite;
import edu.upenn.cis.tomato.policy.PolicyChecker;
import edu.upenn.cis.tomato.util.DebugUtil;
import edu.upenn.cis.tomato.util.ErrorUtil;
import edu.upenn.cis.tomato.util.WarningUtil;

public class FunctionInvocation {

	public static void detectFunctionInvocationViolation(
			String pageNamePattern, CallGraph cg) {
		// TODO Test function overload
		// TODO Test function undefined
		// TODO Add function parameter support for policy

		String origin = "";
		DebugUtil.printSeparationLine();
	
		// Iterate over All CG Nodes
		for (CGNode node : cg) {
			IMethod method = node.getMethod();
			String className = method.getClass().getName().toString();
			String nodeName = method.getDeclaringClass().getName().toString();
			
			// Assume that all the functions for which we define policy are
			// within the mashup page.
			if (className
					.equalsIgnoreCase("com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject")
					&& nodeName.startsWith(pageNamePattern)) {
				
				// Get the origin of the CG node in question
				IR ir = node.getIR();
				// origin = edu.upenn.cis.tomato.util.Util.getCGNodePosition(ir,method).toLowerCase();
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
						
						// According to policy checking result, we issue warnings if
						// necessary.
						boolean checkResult = PolicyChecker
								.isFunctionInvocationAllowed(origin, functionName);
						
						if (checkResult == false) 
						{
							IntSet is = ir.getCallInstructionIndices(csr);
							IntIterator is_iter = is.intIterator();
							ViolationSite vs = null;
							while (is_iter.hasNext()) 
							{
								int ssaIndex = is_iter.next();
								IncludedPosition p = (IncludedPosition) ((AstMethod) method)
										.getSourcePosition(ssaIndex);
								WarningUtil.printFunctionInvocationWarning(origin,
										functionName, "" + p.getFirstOffset() + "-"
												+ p.getLastOffset());
								if (vs == null) 
								{
									vs = new ViolationSite();
									vs.setType(ViolationSite.ViolationTypes.FunctionInvocation);
									vs.setDescription(functionName);
									vs.setSite(p);
								}
							}

							ArrayList<ViolationSite> vsl = ToMaTo.violationSites.get(vs.getUrl());
							if (vsl == null)
							{
								vsl = new ArrayList<ViolationSite>();
								ToMaTo.violationSites.put(vs.getUrl(), vsl);
							}
							vsl.add(vs);
						}
					}
				}
			}
		}

	}
}
