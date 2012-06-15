package edu.upenn.cis.tomato.application;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

import edu.upenn.cis.tomato.policy.PolicyChecker;
import edu.upenn.cis.tomato.util.DebugUtil;
import edu.upenn.cis.tomato.util.ErrorUtil;
import edu.upenn.cis.tomato.util.WarningUtil;

public class AliasAnalysis {
	
	public static final boolean DEBUG_ALIAS_ANALYSIS = false;
	
	public static final String POINTER_KEYWORD = "LocalPointerKey";
	
	public static void findVariableAlias(String pageNamePattern, CallGraph cg, PointerAnalysis pa) {

		// Variables of Interest
		TreeMap<String, TreeSet<String>> VariableOfInterestSet = PolicyChecker.fetchVariableOfInterestSet();
		TreeMap<String, String> QueryVariableSet = new TreeMap<String, String>();
		TreeSet<String> AnswerVariableSet = new TreeSet<String>();
		
		TreeSet<String> SystemVariableOfInterestList = VariableOfInterestSet.get(PolicyChecker.SYSTEM_VARIABLE_KEY);
		TreeMap<String, TreeSet<String>> SystemVariableOfInterestSet = new TreeMap<String, TreeSet<String>>();
		if(SystemVariableOfInterestList!=null)
		{
			Iterator<String> iter_svoil = SystemVariableOfInterestList.iterator();
			while(iter_svoil.hasNext())
			{
				String svoi = iter_svoil.next();
				String[] svoiArray = svoi.split(" ");
				if(svoiArray.length == 2)
				{
					TreeSet<String> variableSet = SystemVariableOfInterestSet.get(svoiArray[0]);
					if(variableSet == null)
					{
						variableSet = new TreeSet<String>();
						SystemVariableOfInterestSet.put(svoiArray[0], variableSet);
					}
					
					variableSet.add(svoiArray[1]);
				}
				else
				{
					ErrorUtil.printErrorMessage("Unexpected format of system variable list required alias analysis.");
				}
			}
		}
		// Iterate over All CGNodes to build QueryVariableSet
	    Iterator<CGNode> iter_cg = cg.iterator(); 
		while (iter_cg.hasNext()) 
		{
			CGNode node = (CGNode) iter_cg.next();
			IMethod method = node.getMethod();
			String className = method.getClass().getName().toString();
			String nodeName = method.getDeclaringClass().getName().toString();
			String[] functionNameArray = nodeName.split("/"); String functionName = "";
			if(functionNameArray != null)
			{
				functionName = functionNameArray[functionNameArray.length-1];
			}
			
			// Take care of all the functions defined within the mashup page.
			if(className.equalsIgnoreCase(ToMaTo.CGNodeClassName) && nodeName.startsWith(pageNamePattern))
			{
				TreeMap<Integer, String> LocalVariableNameMap = edu.upenn.cis.tomato.util.Util.getLocalVariableNameMapping(node, nodeName, method, false, null);
				TreeSet<String> FunctionVariableOfInterest = null;
				
				// Take care of global but not system built-in variables.
				if(nodeName.endsWith(ToMaTo.mainFunctionName))
				{
					FunctionVariableOfInterest = VariableOfInterestSet.get(PolicyChecker.GLOBAL_VARIABLE_KEY);
				}
				else
				{
					FunctionVariableOfInterest = VariableOfInterestSet.get(functionName);
				}
				
				if(FunctionVariableOfInterest != null)
				{
					if(DEBUG_ALIAS_ANALYSIS)
					{
						DebugUtil.DEBUG_PrintDefinedAndUsed(node);
					}
					
					Iterator<Entry<Integer, String>> iter_lvnm = LocalVariableNameMap.entrySet().iterator();
					while(iter_lvnm.hasNext())
					{
						Entry<Integer, String> e = (Entry<Integer, String>) iter_lvnm.next();
						int ln = (Integer) e.getKey(); String on = (String) e.getValue();
						if(FunctionVariableOfInterest.contains(on))
						{
							QueryVariableSet.put(nodeName + " " + ln, on);
						}
					}
				}
			}
			// Take care of all the system built-in functions.
			else if(className.equalsIgnoreCase(ToMaTo.CGNodeClassName))
			{
				TreeMap<Integer, String> LocalVariableNameMap = edu.upenn.cis.tomato.util.Util.getLocalVariableNameMapping(node, nodeName, method, false, null);
				TreeSet<String> FunctionVariableOfInterest = SystemVariableOfInterestSet.get(functionName);

				if(FunctionVariableOfInterest != null)
				{
					if(DEBUG_ALIAS_ANALYSIS)
					{
						DebugUtil.DEBUG_PrintDefinedAndUsed(node);
					}
					
					Iterator<Entry<Integer, String>> iter_lvnm = LocalVariableNameMap.entrySet().iterator();
					while(iter_lvnm.hasNext())
					{
						Entry<Integer, String> e = (Entry<Integer, String>) iter_lvnm.next();
						int ln = (Integer) e.getKey(); String on = (String) e.getValue();
						if(FunctionVariableOfInterest.contains(on))
						{
							String knownLocalName = ToMaTo.reverseSystemBuiltinVariables.get(functionName + " " + on);
							if(knownLocalName == null)
							{
								ErrorUtil.printErrorMessage("Cannot find reverse name mapping for system built-in variable.");
							}
							else
							{
								QueryVariableSet.put(nodeName + " " + ln, knownLocalName);
							}
						}
					}
				}
			}
		}
		//XXX
		System.out.println(QueryVariableSet);
		// Do pointer analysis according to QueryVariableSet and build AnswerVariableSet
		HeapGraph hg = pa.getHeapGraph(); Iterator<PointerKey> iter_pks = pa.getPointerKeys().iterator();
		while(iter_pks.hasNext())
		{
			PointerKey pk = (PointerKey) iter_pks.next();
			if(pk.getClass().toString().endsWith(POINTER_KEYWORD))
			{
				String methodName = ((com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)pk).getNode().getMethod().getDeclaringClass().getName().toString();
				int vn = ((com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)pk).getValueNumber();
				String QueryKey = methodName + " " + vn;
				if(QueryVariableSet.containsKey(QueryKey))
				{
					Iterator<Object> SucIKSIter = hg.getSuccNodes(pk);
					while(SucIKSIter.hasNext())
					{
						Iterator<Object> PredPKSIter = hg.getPredNodes(SucIKSIter.next());
						while(PredPKSIter.hasNext())
						{
							Object apk = PredPKSIter.next();
							if(apk.getClass().toString().endsWith(POINTER_KEYWORD) && !apk.equals(pk) && apk.toString().startsWith("[Node: <Code body of function"))
							{											
								LocalPointerKey alpk = (com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey) apk; int vn_alias = alpk.getValueNumber();
								CGNode aliasNode = alpk.getNode(); IMethod aliasMethod = aliasNode.getMethod(); String aliasNodeName = aliasMethod.getDeclaringClass().getName().toString();
								TreeMap<Integer, String> AliasLocalVariableNameMap = edu.upenn.cis.tomato.util.Util.getLocalVariableNameMapping(aliasNode, aliasNodeName, aliasMethod, false, null);
								if(AliasLocalVariableNameMap != null)
								{
									AnswerVariableSet.add(QueryKey + " " + aliasNodeName + " " + vn_alias + " " + AliasLocalVariableNameMap.get(vn_alias));
								}
							}
						}
					}
				}
			}
		}
		
		WarningUtil.printAliasAnalysisWarning(QueryVariableSet, AnswerVariableSet, true, true);
	}

	// TODO This part of legacy code need to be handled
	/*public static TreeMap<Integer, TreeSet<Position>> BuildVariablePositionMapping (CGNode node)
	{
		if(node == null)
		{
			return null;
		}
		
		String nodeName = node.getMethod().getDeclaringClass().getName().toString();
		
		TreeMap<Integer, TreeSet<Position>> lineMap = lineMapSet.get(nodeName);
		if(lineMap == null)
		{
			lineMap = new TreeMap<Integer, TreeSet<Position>>();
			lineMapSet.put(nodeName, lineMap);
		}
		else
		{
			return lineMap;
		}
		
		IR ir = node.getIR();
		// IMethod store all the origin information
		IMethod method = ir.getMethod();
		// SSAInstruction store concrete SSA instruction
		SSAInstruction[] ssai = ir.getInstructions();

		if(verboseBuildLineMapping)
		{
			System.out.println("\n ===== Build Line Mapping ===== \n");
			System.out.println(nodeName + "\n");
		}
		
		for(int i=0; i<ssai.length; i++)
		{
			if(ssai[i] == null)
			{
				continue;
			}
			
			Position pos = ((AstMethod) method).getSourcePosition(i);
			if(pos == null)
			{
				continue;
			}
			
			if(verboseBuildNameMapping)
			{
				System.out.print("SSA Instruction @line [" + i + "] corresponding to source " + pos);
			}
			
			for(int j=0; j<ssai[i].getNumberOfDefs(); j++)
			{
				int vn = ssai[i].getDef(j);
				TreeSet<Position> ps = lineMap.get(vn);
				if(ps == null)
				{
					ps = new TreeSet<Position>();
					lineMap.put(vn, ps);
				}
				ps.add(pos);				
			}
			
						
			for(int j=0; j<ssai[i].getNumberOfUses(); j++)
			{
				int vn = ssai[i].getUse(j);
				TreeSet<Position> ps = lineMap.get(vn);
				if(ps == null)
				{
					ps = new TreeSet<Position>();
					lineMap.put(vn, ps);
				}
				ps.add(pos);
			}
			
		}
		
		return lineMap;
	}*/

}
