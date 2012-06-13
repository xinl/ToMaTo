package edu.upenn.cis.tomato.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;

import edu.upenn.cis.tomato.application.ContextInsensitiveInformationFlow;
import edu.upenn.cis.tomato.application.ToMaTo;
import edu.upenn.cis.tomato.data.ObjectMembers;

public class DebugUtil {
	
	public static final String DebugPrompt = "[ToMaTo System Debug]";
	
	public static void DEBUG_PrintDebugMessage(String msg)
	{	
		System.out.println(DebugPrompt + " " + msg);
	}
	
	public static void DEBUG_PrintSeperationLine()
	{	
		System.out.println("\n===== ----------------------------------- =====");
	}
	
	public static void DEBUG_PrintAllCGNodes(CallGraph cg, boolean full)
	{
		System.out.println("\n" + DebugPrompt + " ===== All CG Nodes =====");
		Iterator<CGNode> iter = cg.iterator(); CGNode node; int i = 0; int j = 0;
		while (iter.hasNext()) 
		{ 
			node = (CGNode) iter.next();
			String ClassName = node.getMethod().getClass().getName().toString();
			String NodeName = node.getMethod().getDeclaringClass().getName().toString();
			if(ClassName.equalsIgnoreCase(ToMaTo.CGNodeClassName))
			{
				i++; 
				System.out.println("[CG Node] " + NodeName);
			}
			else
			{
				j++;
				if(full)
				{
					System.out.println("[CG Node] " + NodeName);
				}
			}
		}
		
		System.out.print(DebugPrompt + " Total Number of CG Nodes: ");
		if(full)
		{
			System.out.println(i+j);
		}
		else
		{
			System.out.println(i);
		}
	}
	
	public static void DEBUG_PrintSuccessorCGNodes(CallGraph cg, CGNode node)
	{
		System.out.println("\n" + DebugPrompt + " ===== Successor CG Nodes =====");
		Iterator<CGNode> iter_SuccessorNodes = cg.getSuccNodes(node); String result = ""; int i = 0;
		while(iter_SuccessorNodes.hasNext())
		{
			CGNode SNode = iter_SuccessorNodes.next(); i++;
			IMethod SNode_Method = SNode.getMethod();
			result = result + "[CG Node] " + SNode_Method.getDeclaringClass().getName().toString() + "\n";
		}
		
		System.out.println("[CG Node] " + node.getMethod().getDeclaringClass().getName().toString() + " has " + i + " successor:");
		System.out.print(result);
	}
	
	public static void DEBUG_PrintDefinedAndUsed (CGNode node)
	{
		if(node == null)
		{
			return;
		}
		
		IR ir = node.getIR(); IMethod method = ir.getMethod(); SSAInstruction[] ssai = ir.getInstructions();

		System.out.println("\n" + DebugPrompt + " ===== CG Node [" + method.getDeclaringClass().getName().toString() + "] Defined & Used =====");
		
		for(int i=0; i<ssai.length; i++)
		{
			if(ssai[i] == null)
			{
				continue;
			}
			
			System.out.print(DebugPrompt + " SSA Instruction [" + i + "][" + ssai[i].toString() + "]" );
			
			for(int j=0; j<ssai[i].getNumberOfDefs(); j++)
			{
				String[] ln = ir.getLocalNames(i, ssai[i].getDef(j));
				if(ln!=null)
				{
					for(int k=0; k<ln.length; k++)
					{
						System.out.print(" defines variable [" + ssai[i].getDef(j) + " - " + ln[k] + "];");
					}
				}
			}
			
			for(int j=0; j<ssai[i].getNumberOfUses(); j++)
			{
				String[] ln = ir.getLocalNames(i, ssai[i].getUse(j));
				if(ln!=null)
				{
					for(int k=0; k<ln.length; k++)
					{
						System.out.print(" uses variable [" + ssai[i].getUse(j) + " - " + ln[k] + "];");
					}
				}
			}
			
			System.out.println("");
		}
	}
	
	public static void DEBUG_PrintFunctionRangeList(ArrayList<ObjectMembers> frl, String listName)
	{
		System.out.println("\n===== ----------------------------------- =====");
		DEBUG_PrintDebugMessage("Function Range List [ " + listName+ " ]");
		System.out.println("===== ----------------------------------- =====");
		for(ObjectMembers member:frl)
		{
			member.print();
		}
	}
	
	public static void DEBUG_InformationFLow_PrintVariableNumbering() {
		
		System.out.println("\n" + DebugPrompt + " ===== Variable Numbering ===== Format: [Global Numbering] - [Context and Variable Name] ===== ");
		
		Iterator<Pair<CGNode, String>> iter = ContextInsensitiveInformationFlow.variableNumbering.iterator();
		while (iter.hasNext()) 
		{
			Pair<CGNode, String> e = (Pair<CGNode, String>) iter.next();
			System.out.println("[" + ContextInsensitiveInformationFlow.variableNumbering.getMappedIndex(e) + "] - " + e);
		}
	}
	
	public static void DEBUG_InformationFLow_PrintVariableNameMap() {
		
		Iterator<Entry<String, TreeMap<Integer, String>>> iter = ToMaTo.variableNameMapping.entrySet().iterator();
		while (iter.hasNext()) 
		{
			Entry<String, TreeMap<Integer, String>> e = iter.next();
			System.out.println("\n" + DebugPrompt + " ===== Variable Mapping of CG Node [" + e.getKey() + "] ===== ");
			Iterator<Entry<Integer, String>> iter_vn = e.getValue().entrySet().iterator();
			while (iter_vn.hasNext())
			{
				Entry<Integer, String> e_vn = iter_vn.next(); String ns = e_vn.getValue();
				System.out.println("[" + e_vn.getKey() +" - " + ns + "]");
			}
		}
	}
	
	public static void DEBUG_InformationFLow_PrintObjectFieldMap() {
		
		System.out.println("\n ===== Object Field Mapping ===== \n");
		
		Iterator<Entry<String, TreeMap<Integer, TreeSet<String>>>>  iter = ContextInsensitiveInformationFlow.objectFieldMap.entrySet().iterator();
		Entry<String, TreeMap<Integer, TreeSet<String>>> e;
		while (iter.hasNext()) 
		{
			e = iter.next();
			String methodName = (String) e.getKey();
			TreeMap<Integer, TreeSet<String>> vn = (TreeMap<Integer, TreeSet<String>>) e.getValue();
			Iterator<Entry<Integer, TreeSet<String>>> iter_vn = vn.entrySet().iterator();
			Entry<Integer, TreeSet<String>> e_vn;
			while (iter_vn.hasNext())
			{
				e_vn = iter_vn.next();
				int object_vn = (Integer) e_vn.getKey();
				
				if(ToMaTo.variableNameMapping.get(methodName).get(object_vn)!=null)
				{
					System.out.print(ToMaTo.variableNameMapping.get(methodName).get(object_vn) + " has fields [ ");
				}
				else
				{
					System.out.print("[" + object_vn + "@"+ methodName + "] has fields [ ");
				}
				
				TreeSet<String> nss = (TreeSet<String>) e_vn.getValue();
				for(String ns : nss)
				{
					System.out.print(ns + " ");
				}
				
				System.out.println("]");
			}
		}
	}
	
	public static void DEBUG_InformationFLow_PrintVariable2ObjectField() {
		
		System.out.println("\n" + DebugPrompt + " ===== Variable to Object & Field Mapping ===== ");	
		Iterator<Entry<String, Pair<CGNode, String>>> iter = ContextInsensitiveInformationFlow.variable2ObjectField.entrySet().iterator();
		while (iter.hasNext()) 
		{
			Entry<String, Pair<CGNode, String>> e = iter.next();
			String variable = (String) e.getKey(); Pair<CGNode, String> p = (Pair<CGNode, String>) e.getValue();
			System.out.println("Variable [" + variable + "] - Name [" + p.snd +"] - Global Numbering [" + ContextInsensitiveInformationFlow.variableNumbering.getMappedIndex(p) + "]");
		}
	}
	
	public static void DEBUG_InformationFLow_PrintInstructionOrigin() {
		
		Iterator<Entry<String, TreeMap<Integer, Integer>>> iter = ContextInsensitiveInformationFlow.instructionOrigin.entrySet().iterator();
		while (iter.hasNext()) 
		{
			Entry<String, TreeMap<Integer, Integer>> e = iter.next();
			System.out.println("\n" + DebugPrompt + " ===== Instruction Origin of CG Node [" + e.getKey() + "] ===== Label: 0 - Internal, 1 - External ===== ");
			TreeMap<Integer, Integer> vn = (TreeMap<Integer, Integer>) e.getValue();
			Iterator<Entry<Integer, Integer>> iter_vn = vn.entrySet().iterator();
			while (iter_vn.hasNext())
			{
				Entry<Integer, Integer> e_vn = iter_vn.next();
				System.out.println("INSTRUCTION [" + e_vn.getKey() +"] - ORIGIN [" + e_vn.getValue() + "]");
			}
		}
	}
	
	public static void DEBUG_InformationFLow_PrintFunctionOrigin() {
		
		System.out.println("\n" + DebugPrompt + " ===== Function Origin ===== Label: 0 - Internal, 1 - External ===== ");
		Iterator<Entry<String, Integer>> iter = ContextInsensitiveInformationFlow.functionOrigin.entrySet().iterator();
		while (iter.hasNext()) 
		{
			Entry<String, Integer> e = iter.next();
			System.out.println("[" + e.getKey() + " - " + e.getValue() + "]");
		}
	}

}
