/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.upenn.cis.tomato.application;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillAll;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.upenn.cis.tomato.data.BitVectorIntersection;
import edu.upenn.cis.tomato.policy.PolicyChecker;
import edu.upenn.cis.tomato.util.DebugUtil;

 /**
 * Computes interprocedural information flow in a context-insensitive manner.
 */

public class ContextInsensitiveInformationFlow {

  /**
   * the exploded interprocedural control-flow graph on which to compute the analysis
  */
	
  // Used to build CG Node filters	
  public static String PageNamePattern;	
  public static String FunctionNamePattern;
  
  // Policy decides which libraries are external
  public static TreeSet<String> externalLibrarySet;
  
  // Data structure used to solve information flow
  public static ExplodedInterproceduralCFG icfg;
  public static OrdinalSetMapping<Pair<CGNode, String>> variableNumbering;
  public static TreeMap<String, Integer> functionOrigin = new TreeMap<String, Integer>();
  public static TreeMap<String, TreeMap<Integer, Integer>> instructionOrigin = new TreeMap<String, TreeMap<Integer, Integer>>();
  public static TreeMap<String, TreeMap<Integer, TreeSet<String>>> objectFieldMap = new TreeMap<String, TreeMap<Integer, TreeSet<String>>>();
  public static TreeMap<String, Pair<CGNode, String>> variable2ObjectField = new TreeMap<String, Pair<CGNode, String>>();  

  // Use to debug function [initialize]
  public static boolean initialize_DEBUG_Output = false;
  public static boolean initialize_DEBUG_Flow = true;
  public static boolean initialize_DEBUG_Phi = false;
  // Use to debug function [getNodeTransferFunction]
  public static boolean getNodeTransferFunction_DEBUG_Flow = true;
  public static boolean getNodeTransferFunction_DEBUG_Output = false;
  // Use to debug function [getEdgeTransferFunction]
  public static boolean getEdgeTransferFunction_DEBUG = false;
  // Use to debug function [analyze]
  public static boolean analyze_DEBUG = true;
  // Use to debug all the data structures
  public static boolean Data_DEBUG = true;

  public ContextInsensitiveInformationFlow(ExplodedInterproceduralCFG eicfg, String pageNamePattern) {
	
	// For debugging purpose to build CG Node filters
	PageNamePattern = pageNamePattern; FunctionNamePattern = "compute";
	// This is used to determine instruction origin
	icfg = eicfg;  externalLibrarySet = PolicyChecker.FetchExternalLibraryIDs();
	// To build all the data structure needed
	variableNumbering = this.initialize();
    
    if(Data_DEBUG)
    {
    	// DebugUtil.DEBUG_InformationFLow_PrintVariableNumbering();
    	DebugUtil.DEBUG_InformationFLow_PrintVariableNameMap();
        
    	// DebugUtil.DEBUG_InformationFLow_PrintObjectFieldMap();
    	// DebugUtil.DEBUG_InformationFLow_PrintVariable2ObjectField();
        
    	// DebugUtil.DEBUG_InformationFLow_PrintFunctionOrigin();
    	// DebugUtil.DEBUG_InformationFLow_PrintInstructionOrigin();
    }
    
    // To solve information flow
    this.analyze();
  }

  public OrdinalSetMapping<Pair<CGNode, String>> initialize() {
		
		// Lattice value set 
		ArrayList<Pair<CGNode, String>> variableOrigin = new ArrayList<Pair<CGNode, String>>();
		
		// Iterator over all CG nodes
		CallGraph cg = icfg.getCallGraph();
		for (CGNode node : cg)
		{
			IR ir = node.getIR(); IMethod method = ir.getMethod();
			String nodeName = method.getDeclaringClass().getName().toString();
			String className = method.getClass().getName().toString();
			
			// CG node filter
			if(initialize_DEBUG_Flow)
			{
				if (!(nodeName.startsWith(PageNamePattern) && nodeName.endsWith(FunctionNamePattern))) 
				{
					continue;
				}
			}

			// FakeRoot node need special care
			boolean IsFakeRoot = false;
			if(nodeName.equalsIgnoreCase(ToMaTo.fakeRootNodeName))
			{
				IsFakeRoot = true;
			}
			
			// Filter out constructors
			if(!className.equalsIgnoreCase(ToMaTo.CGNodeClassName) && !IsFakeRoot)
			{
				continue;
			}
			
			// Determine normal instruction origin: 0 - Internal; 1 - External.
			int sourceOrigin = 0;
			if(!IsFakeRoot)
			{
				sourceOrigin = PolicyChecker.IsLibraryExternal(externalLibrarySet, edu.upenn.cis.tomato.util.Util.GetCGNodeOrigin(ir, method).toLowerCase());
			}
			ContextInsensitiveInformationFlow.functionOrigin.put(nodeName, sourceOrigin);
			
			// Determine invocation instruction origin: 0 - Internal; 1 - External.
			ArrayList<CGNode> calleeNodeSet = new ArrayList<CGNode>();
			Iterator<CGNode> iter_succNodes = cg.getSuccNodes(node);
			while(iter_succNodes.hasNext())
			{
				CGNode sNode = iter_succNodes.next(); IMethod sNode_Method = sNode.getMethod(); IR sNode_ir = sNode.getIR();
				if(sNode_Method.getClass().getName().toString().equalsIgnoreCase(ToMaTo.CGNodeClassName))
				{
					calleeNodeSet.add(sNode);
					functionOrigin.put(sNode_Method.getDeclaringClass().getName().toString(), PolicyChecker.IsLibraryExternal(externalLibrarySet, edu.upenn.cis.tomato.util.Util.GetCGNodeOrigin(sNode_ir, sNode_Method).toLowerCase()));
				}
			}
			if(initialize_DEBUG_Output)
			{
				DebugUtil.DEBUG_PrintSuccessorCGNodes(cg, node);
			}
			
			// Initialize Instruction Origin
			TreeMap<Integer, Integer> instrOrg = instructionOrigin.get(nodeName);
			if(instrOrg == null)
			{
				instrOrg = new TreeMap<Integer, Integer>();
				instructionOrigin.put(nodeName, instrOrg);
			}
						
			TreeSet<Integer> variableSet = new TreeSet<Integer>();
			
			// Build a basic variable name mapping
			edu.upenn.cis.tomato.util.Util.GetLocalVariableNameMapping(node, nodeName, method, true, variableSet);
			if(initialize_DEBUG_Output)
			{
				DebugUtil.DEBUG_PrintDefinedAndUsed(node);
			}
			
			// Process Phi Node
			TreeMap<Integer, TreeSet<Integer>> PhiMap = initializePhiInstruction(ir);
			
			if(initialize_DEBUG_Output)
			{
				System.out.println("\n ===== CGNode [" + nodeName + "] SSA ===== \n");
			}
			
			SSAInstruction[] instructions = ir.getInstructions();
			for (int i = 0; i < instructions.length; i++) 
			{
				if(instructions[i]==null)
				{
					continue;
				}
				
				if(initialize_DEBUG_Output)
				{
					System.out.println("Instruction [" + i +"] " + instructions[i]);
				}
				
				instrOrg.put(i, sourceOrigin);
				
				if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptCheckReference") 
						|| instructions[i].getClass().getName().startsWith("com.ibm.wala.ssa.SSAReturnInstruction")
						|| instructions[i].getClass().getName().startsWith("com.ibm.wala.ssa.SSAGotoInstruction")
						|| instructions[i].getClass().getName().startsWith("com.ibm.wala.ssa.SSAConditionalBranchInstruction")
						|| instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction"))
				{
					if(initialize_DEBUG_Output)
					{
						System.out.println("Do not process instruction type " + instructions[i].getClass().getName());
					}
				}
				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction")) 
				{
					if(initialize_DEBUG_Output)
					{
						AstIsDefinedInstruction instruction = (AstIsDefinedInstruction) instructions[i];
						if(instruction.getFieldRef() != null)
						{
							// getUse(1) returns AstIsDefinedInstruction.fieldVal
							// getUse(0) returns rval
							// getDef() returns lval
							System.out.println(instruction.getUse(1) + " " + instruction.getFieldRef().getName() + " " + instruction.getDef() + " " + instruction.getUse(0));
						}
					}
				}
				else if(instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite"))
				{
					JavaScriptPropertyWrite instruction = (JavaScriptPropertyWrite) instructions[i];
					
					// Object Field
					TreeMap<Integer, TreeSet<String>> vnf = ContextInsensitiveInformationFlow.objectFieldMap.get(nodeName);
					if(vnf == null)
					{
						vnf = new TreeMap<Integer, TreeSet<String>>();
						objectFieldMap.put(nodeName, vnf);
					}
					
					int object_vn = instruction.getObjectRef();
					int field = instruction.getMemberRef();
					
					TreeSet<String> fn = vnf.get(object_vn);
					if(fn == null)
					{
						fn = new TreeSet<String>();
						vnf.put(object_vn, fn);
					}
					
					String[] field_ln = ir.getLocalNames(i, field);
					if(field_ln!=null)
					{
						// TODO double check this is useful
						/*TreeSet<String> ns = vnm.get(field);
						if(ns == null)
						{
							ns = new TreeSet<String>();
							vnm.put(field, ns);
						}*/
						
						for(int k=0; k<field_ln.length; k++)
						{
							fn.add(field_ln[k]);
						}
					}
					
					// Variable Numbering
					variableOrigin.add(Pair.make(node, object_vn + "." + field));
			}
				else if(instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead"))
				{
					JavaScriptPropertyRead instruction = (JavaScriptPropertyRead) instructions[i];
					
					int object = instruction.getObjectRef();
					int field = instruction.getMemberRef();
					
					TreeSet<Integer> phi_vn = PhiMap.get(object);
					
					if(phi_vn != null)
					{
						for(Integer vn : phi_vn)
						{
							// Object Field
							TreeMap<Integer, TreeSet<String>> vnf = objectFieldMap.get(nodeName);
							if(vnf == null)
							{
								vnf = new TreeMap<Integer, TreeSet<String>>();
								objectFieldMap.put(nodeName, vnf);
							}
							
							TreeSet<String> fn = vnf.get(vn);
							if(fn == null)
							{
								fn = new TreeSet<String>();
								vnf.put(vn, fn);
							}
							
							String[] ln = ir.getLocalNames(i, field);
							if(ln!=null)
							{						
								for(int k=0; k<ln.length; k++)
								{
									fn.add(ln[k]);
								}
							}
							
							// Variable Name Mapping
							TreeMap<Integer, String> nm = ToMaTo.VariableNameMapping.get(nodeName);
							if(nm!=null && nm.get(vn)!=null)
							{
								String o = nm.get(vn);
								if(ln!=null)
								{						
									for(int k=0; k<ln.length; k++)
									{
										nm.put(vn, o+"."+ln[k]);
									}
								}
							}
							else
							{
								if(ln!=null)
								{						
									for(int k=0; k<ln.length; k++)
									{
										nm.put(vn, vn+"."+ln[k]);
									}
								}
							}
							
							// Variable 2 Object Field
							ContextInsensitiveInformationFlow.variable2ObjectField.put(instruction.getDef()+"@"+nodeName, Pair.make(node, vn+"."+field));
						}
					}
					else
					{
						Assertions.UNREACHABLE();
					}
					
				}
				
				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.ir.ssa.EachElementGetInstruction"))
				{
					EachElementGetInstruction instruction = (EachElementGetInstruction) instructions[i];
					
					// Put Variable Origin
					variableOrigin.add(Pair.make(node, instruction.getUse(0)+"."+instruction.getDef()));
					
				}
				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.ssa.SSAUnaryOpInstruction"))
				{
					SSAUnaryOpInstruction instruction = (SSAUnaryOpInstruction) instructions[i];
				}
				else if (instructions[i].getClass().getName().equalsIgnoreCase("com.ibm.wala.cast.ir.ssa.AstLexicalRead")) 
				{
					com.ibm.wala.cast.ir.ssa.AstLexicalRead instruction = (com.ibm.wala.cast.ir.ssa.AstLexicalRead) instructions[i];
					Access[] ac = instruction.getAccesses();
					for (int j = 0; j < ac.length; j++) 
					{
						String vn = "";
						if(ac[j].variableDefiner == null)
						{
							vn = ac[j].variableName+"@"+nodeName;
						}
						else
						{
							vn = ac[j].variableName+"@"+ac[j].variableDefiner;
						}
						
						ToMaTo.VariableNameMapping.get(nodeName).put(ac[j].valueNumber, vn);
					}
				}
				
				else if (instructions[i].getClass().getName().equalsIgnoreCase("com.ibm.wala.cast.ir.ssa.AstLexicalWrite")) 
				{
					com.ibm.wala.cast.ir.ssa.AstLexicalWrite instruction = (com.ibm.wala.cast.ir.ssa.AstLexicalWrite) instructions[i];
					Access[] ac = instruction.getAccesses();
					for (int j = 0; j < ac.length; j++) 
					{						
						String vn = "";
						if(ac[j].variableDefiner == null)
						{
							vn = ac[j].variableName+"@"+nodeName;
						}
						else
						{
							vn = ac[j].variableName+"@"+ac[j].variableDefiner;
						}
						
						ToMaTo.VariableNameMapping.get(nodeName).put(ac[j].valueNumber, vn);
					}
				}

				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$1")) 
				{
					SSABinaryOpInstruction instruction = (SSABinaryOpInstruction) instructions[i];
				}
				
				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$3")) 
				{
					SSAGetInstruction instruction = (SSAGetInstruction) instructions[i];
									
					if(initialize_DEBUG_Output)
					{
						//getDef() returns result field
						System.out.println(instruction.getDef() + " " + instruction.getDeclaredField().getName());
						
						System.out.print("Use [ " );
						for(int j=0; j<instructions[i].getNumberOfUses(); j++)
						{
							int vn = instructions[i].getUse(j);
							System.out.print(vn + " ");
						}
						System.out.println("]" );
						
						System.out.print("Def [ " );
						for(int j=0; j<instructions[i].getNumberOfDefs(); j++)
						{
							int vn = instructions[i].getDef(j);
							System.out.print(vn + " ");
						}
						
						System.out.println("]" );
					}
					
					if(instruction.getNumberOfUses() == 1)
					{
						int Use = instruction.getUse(0);
						TreeSet<Integer> phi_vn = PhiMap.get(Use);
						
						if(phi_vn == null)
						{
							if(initialize_DEBUG_Output)
							{
								System.out.println("Get Instruction Without Phi " + nodeName + " " + instruction);
							}
							
							TreeMap<Integer, String> nm = ToMaTo.VariableNameMapping.get(nodeName);
							if(nm!=null && nm.get(Use)!=null)
							{
								String object = nm.get(Use);
								String[] component = object.split("@");
								nm.put(Use, component[0]+"."+instruction.getDeclaredField().getName()+"@"+component[1]);
							}
							else
							{
								nm.put(Use, Use+"."+instruction.getDeclaredField().getName()+"@"+nodeName);
							}
							
							ContextInsensitiveInformationFlow.variable2ObjectField.put(instruction.getDef()+"@"+nodeName, Pair.make(node, Use+"."+instruction.getDeclaredField().getName().toString()));
						}
						
						if(phi_vn != null && phi_vn.contains(instruction.getDef()))
						{
							phi_vn.remove(instruction.getDef());
							
							for(Integer vn : phi_vn)
							{
								TreeMap<Integer, String> nm = ToMaTo.VariableNameMapping.get(nodeName);	
								if(nm!=null && nm.get(vn)!=null)
								{
									String object = nm.get(vn);
									String[] component = object.split("@");
									nm.put(vn, component[0]+"."+instruction.getDeclaredField().getName()+"@"+component[1]);
								}
								else
								{
									nm.put(vn, vn+"."+instruction.getDeclaredField().getName()+"@"+nodeName);
								}
								
								ContextInsensitiveInformationFlow.variable2ObjectField.put(instruction.getDef()+"@"+nodeName, Pair.make(node, vn+"."+instruction.getDeclaredField().getName().toString()));
							}
						}
						
						if(phi_vn != null && !phi_vn.contains(instruction.getDef()))
						{
							for(Integer vn : phi_vn)
							{
								if(ContextInsensitiveInformationFlow.objectFieldMap.get(nodeName)!=null && ContextInsensitiveInformationFlow.objectFieldMap.get(nodeName).get(vn)!=null)
								{
									if(ContextInsensitiveInformationFlow.objectFieldMap.get(nodeName).get(vn).contains(instruction.getDeclaredField().getName().toString()))
									{
										TreeMap<Integer, String> nm = ToMaTo.VariableNameMapping.get(nodeName);
										
										if(nm!=null && nm.get(vn)!=null)
										{
											String object = nm.get(vn);
											String[] component = object.split("@");
											nm.put(vn, component[0]+"."+instruction.getDeclaredField().getName()+"@"+component[1]);
										}
										else
										{
											nm.put(vn, vn+"."+instruction.getDeclaredField().getName()+"@"+nodeName);
										}
									}
								}
								
								ContextInsensitiveInformationFlow.variable2ObjectField.put(instruction.getDef()+"@"+nodeName, Pair.make(node, vn+"."+instruction.getDeclaredField().getName().toString()));
							}
						}
					}
					else
					{
						Assertions.UNREACHABLE();
					}
					
				}
				
				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$4")) 
				{
					SSANewInstruction instruction = (SSANewInstruction) instructions[i];
				}
				
				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$5")) 
				{
					SSAPutInstruction instruction = (SSAPutInstruction) instructions[i];
					
					if(initialize_DEBUG_Output)
					{
						System.out.println("Put " + instruction.getUse(1) + " into " + instruction.getUse(0) + " as " + instruction.getDeclaredFieldType() + " " +instruction.getDeclaredField().getName());
					}
					
					// Object Field
					TreeMap<Integer, TreeSet<String>> vnf = ContextInsensitiveInformationFlow.objectFieldMap.get(nodeName);
					if(vnf == null)
					{
						vnf = new TreeMap<Integer, TreeSet<String>>();
						ContextInsensitiveInformationFlow.objectFieldMap.put(nodeName, vnf);
					}
					
					int object_vn = instruction.getUse(0);
					TreeSet<String> fn = vnf.get(object_vn);
					if(fn == null)
					{
						fn = new TreeSet<String>();
						vnf.put(object_vn, fn);
					}
					
					fn.add(instruction.getDeclaredField().getName().toString());
					variableOrigin.add(Pair.make(node, object_vn + "." + instruction.getDeclaredField().getName().toString()));
				}
				
				else if (instructions[i].getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptInvoke")) 
				{
					JavaScriptInvoke instruction = (JavaScriptInvoke) instructions[i];
					
					if(instruction.getNumberOfReturnValues()>1)
					{
						Assertions.UNREACHABLE();
					}
					
					if(initialize_DEBUG_Output)
					{
						for(int j=0; j<instruction.getNumberOfUses(); j++)
						{
							int vn = instruction.getUse(j);
							String[] ln = ir.getLocalNames(i, vn);
							if(ln!=null)
							{
								for(int k=0; k<ln.length; k++)
								{
									if(!instruction.isLexicalUse(j))
									{
										System.out.println("Read " + j +"-th variable [" + vn + " , " + ln[k] + "]");
									}
								}
							}
						}
						
						for(int j=0; j<instruction.getNumberOfDefs(); j++)
						{
							int vn = instruction.getDef(j);
							String[] ln = ir.getLocalNames(i, vn);
							if(ln!=null)
							{
								for(int k=0; k<ln.length; k++)
								{
									if(!instruction.isLexicalDef(j))
									{
										System.out.println("Write " + j +"-th variable [" + vn + " , " + ln[k] + "]");
									}
								}
							}
						}
						
						for(int j=0; j<instruction.getNumberOfReturnValues(); j++)
						{
							int vn = instruction.getReturnValue(j);
							String[] ln = ir.getLocalNames(i, vn);
							if(ln!=null)
							{
								for(int k=0; k<ln.length; k++)
								{
									System.out.println("Return " + j +"-th variable [" + vn + " , " + ln[k] + "]");
								}
							}
						}
						
						System.out.print("Parameters [ ");
						// start with 1 because the first Use is function name
						for(int j=1; j<instruction.getNumberOfParameters(); j++)
						{
							System.out.print(instruction.getUse(j) + " ");
						}
						
						System.out.println("]");
					}

//					Access[] ac = instruction.lexicalReads;
//					if(ac !=null)
//					{
						for (int j = 0; j < instruction.getNumberOfLexicalReads(); j++) 
						{
							Access ac = instruction.getLexicalUse(j);
							if(initialize_DEBUG_Output)
							{
								System.out.println("Lexical Read " + j +"-th lexical variable [" + ac.valueNumber + " , " + ac.variableName +"@"+ ac.variableDefiner + "]");
							}
							
							String vn = "";
							if(ac.variableDefiner == null)
							{
								vn = ac.variableName+"@"+nodeName;
							}
							else
							{
								vn = ac.variableName+"@"+ac.variableDefiner;
							}
							
							ToMaTo.VariableNameMapping.get(nodeName).put(ac.valueNumber, vn);
						}
//					}

//					ac = instruction.lexicalWrites;
//					if(ac !=null)
//					{
						for (int j = 0; j < instruction.getNumberOfLexicalWrites(); j++) 
						{
							Access ac = instruction.getLexicalDef(j);
							if(initialize_DEBUG_Output)
							{
								System.out.println("Lexical Write " + j +"-th lexical variable [" + ac.valueNumber + " , " + ac.variableName +"@"+ ac.variableDefiner + "]");
							}
							
							String vn = "";
							if(ac.variableDefiner == null)
							{
								vn = ac.variableName+"@"+nodeName;
							}
							else
							{
								vn = ac.variableName+"@"+ac.variableDefiner;
							}
							
							ToMaTo.VariableNameMapping.get(nodeName).put(ac.valueNumber, vn);
						}
//					}
					
					String[] ln = ir.getLocalNames(i, instruction.getFunction());
					if(ln!=null)
					{
						for(int k=0; k<ln.length; k++)
						{
							String fn = ln[k];
							for(int j=0; j<calleeNodeSet.size(); j++)
							{
								String calleeName = calleeNodeSet.get(j).getMethod().getDeclaringClass().getName().toString();
								if(initialize_DEBUG_Output)
								{
									System.out.println(ln[k] + " " + calleeName);
								}
								if(calleeName.endsWith(fn))
								{
									instrOrg.put(i, ContextInsensitiveInformationFlow.functionOrigin.get(calleeName));
									break;
								}
							}
						}
					}
				}
				else
				{
					// TODO handle this
					// Assertions.UNREACHABLE();
					if(initialize_DEBUG_Output)
					{
					
						System.out.println(nodeName);
						System.out.println(instructions[i].getClass().getName().toString());
						System.out.println(instructions[i]);
					}
				}
				
				for(int j=0; j<instructions[i].getNumberOfDefs(); j++)
				{
					variableSet.add(instructions[i].getDef(j));
				}
				
				for(int j=0; j<instructions[i].getNumberOfUses(); j++)
				{
					variableSet.add(instructions[i].getUse(j));
				}
			}
			
			if(initialize_DEBUG_Output)
			{
				System.out.println("\n ===== SSA Variable Names ===== \n");
			}
			
			Iterator iter_vs = variableSet.iterator();
			while(iter_vs.hasNext())
			{
				int vn = (Integer)iter_vs.next();
				variableOrigin.add(Pair.make(node, String.valueOf(vn)));
				if(initialize_DEBUG_Output)
				{
					System.out.println(vn);
				}
			}
		}

		return  new ObjectArrayMapping<Pair<CGNode, String>>(variableOrigin.toArray(new Pair[variableOrigin.size()]));
	}
  
  public TreeMap<Integer, TreeSet<Integer>> initializePhiInstruction(IR ir)
  {
		TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<Integer, TreeSet<Integer>>();
		Iterator<? extends SSAInstruction> iter_phi = ir.iteratePhis();
		while (iter_phi.hasNext()) 
		{
			SSAPhiInstruction instruction = (SSAPhiInstruction) iter_phi.next();
			for (int i = 0; i < instruction.getNumberOfDefs(); i++) 
			{
				int def_vn = instruction.getDef(i); TreeSet<Integer> useSet = result.get(def_vn);
				if (useSet == null) 
				{
					useSet = new TreeSet<Integer>();
					result.put(def_vn, useSet);
				}
				for (int j = 0; j < instruction.getNumberOfUses(); j++) 
				{
					useSet.add(instruction.getUse(j));
				}
			}
		}
		
		return result;
  }

  public class TransferFunctions implements ITransferFunctionProvider<BasicBlockInContext<IExplodedBasicBlock>, BitVectorVariable> {

    public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
    	
    	return BitVectorIntersection.instance();
    }

    public boolean hasEdgeTransferFunctions() {
      return true;
    }

    public boolean hasNodeTransferFunctions() {
      return true;
    }
    
    public UnaryOperator<BitVectorVariable> getNodeTransferFunction(BasicBlockInContext<IExplodedBasicBlock> node) {
    	
		IExplodedBasicBlock ebb = node.getDelegate();
		CGNode cgNode = node.getNode();
		String methodName = node.getMethod().getDeclaringClass().getName().toString();
		IR ir = cgNode.getIR();
		SymbolTable st = ir.getSymbolTable();

		// Ignore constructors
		if (cgNode.getMethod().getClass().getName().toString().equalsIgnoreCase("com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector$JavaScriptConstructor"))
		{
			return BitVectorIdentity.instance();
		}

		// CGNode filter, for debugging
		if(getNodeTransferFunction_DEBUG_Flow)
		{
			if (!(methodName.startsWith(PageNamePattern) && methodName.endsWith(FunctionNamePattern)))
			{
				return BitVectorIdentity.instance();
			}
		}
		

        SSAInstruction SSA_instruction = ebb.getInstruction();
        int instructionIndex = ebb.getFirstInstructionIndex();
        if(SSA_instruction == null)
        {
        	if(getNodeTransferFunction_DEBUG_Output)
        	{
        		System.out.println("NULL SSA @ " + methodName + " [" + instructionIndex + "]" );
        	}
        	
        	return BitVectorIdentity.instance();
        }
        
        int iorigin = 0;
        try
        {
        	iorigin = instructionOrigin.get(methodName).get(instructionIndex);
        }
        catch(Exception e)
        {
        	System.out.println(methodName + " " + instructionIndex);
        }
        
        
        if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptInvoke")) 
		{
			JavaScriptInvoke instruction = (JavaScriptInvoke) SSA_instruction;
			
			if(instruction.getNumberOfReturnValues()>1)
			{
				Assertions.UNREACHABLE();
			}
			
			if(iorigin != 0)
			{
				BitVector kill = new BitVector();
		        BitVector gen = new BitVector();
		        
		        int vn = instruction.getReturnValue(0);
				int Ret = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));
				gen.set(Ret);
				
				return new BitVectorKillGen(kill, gen);
			}
		}
        
        /*if (SSA_instruction.getClass().getName().equalsIgnoreCase("com.ibm.wala.cast.ir.ssa.AstLexicalRead")) 
		{
			com.ibm.wala.cast.ir.ssa.AstLexicalRead instruction = (com.ibm.wala.cast.ir.ssa.AstLexicalRead) SSA_instruction;
			
			BitVector kill = new BitVector();
	        BitVector gen = new BitVector();
	        
			Access[] ac = instruction.getAccesses();
			for (int j = 0; j < ac.length; j++) 
			{
				int vn = ac[j].valueNumber;
				if(ac[j].variableDefiner == null)
				{
					if(iorigin == 0)
					{
						gen.set(variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn))));
					}
					else
					{
						kill.set(variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn))));
					}
					
				}
			}
			
			return new BitVectorKillGen(kill, gen);
		}
        
        else if (SSA_instruction.getClass().getName().equalsIgnoreCase("com.ibm.wala.cast.ir.ssa.AstLexicalWrite")) 
		{
			com.ibm.wala.cast.ir.ssa.AstLexicalWrite instruction = (com.ibm.wala.cast.ir.ssa.AstLexicalWrite) SSA_instruction;
			
			BitVector kill = new BitVector();
	        BitVector gen = new BitVector();
			
			Access[] ac = instruction.getAccesses();
			for (int j = 0; j < ac.length; j++) 
			{						
				int vn = ac[j].valueNumber;
				if(st.getValue(vn)!=null)
				{
					if(iorigin == 0)
					{
						gen.set(variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn))));
					}
					else
					{
						kill.set(variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn))));
					}
					
					return new BitVectorKillGen(kill, gen);
				}
				else
				{					
					TreeSet<Integer> numberingSet = getGlobalNumberingForVariableName(cgNode, methodName, ac[j].variableName+"@"+ac[j].variableDefiner);
					if(numberingSet != null && numberingSet.size()>0)
					{
						BitVectorVariable bitUse = new BitVectorVariable();
						bitUse.set(variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn))));
						return new BitVectorInFlowDependent(bitUse, numberingSet, "AstLexicalWrite");
					}
					else
					{
						return BitVectorIdentity.instance();
					}
				}
			}
		}
        else if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.ir.ssa.EachElementGetInstruction"))
		{
        	EachElementGetInstruction instruction = (EachElementGetInstruction) SSA_instruction;
			
			int Def = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(instruction.getDef())));
			
			if(instruction.getNumberOfUses() == 1)
			{
				int Use = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(instruction.getUse(0))));
				BitVectorVariable bitUse = new BitVectorVariable();
				bitUse.set(Use);
				
				TreeSet<Integer> setDef = new TreeSet<Integer>();
				setDef.add(Def);
				return new BitVectorInFlowDependent(bitUse, setDef, "EachElementGetInstruction");
			}
			else
			{
				Assertions.UNREACHABLE();
			}
		}
        
		else if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.ssa.SSAUnaryOpInstruction"))
		{
			SSAUnaryOpInstruction instruction = (SSAUnaryOpInstruction) SSA_instruction;
			
			int Def = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(instruction.getDef())));
			if(instruction.getNumberOfUses() == 1)
			{
				int Use = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(instruction.getUse(0))));
				BitVectorVariable bitUse = new BitVectorVariable();
				bitUse.set(Use);
				
				TreeSet<Integer> setDef = new TreeSet<Integer>();
				setDef.add(Def);
				return new BitVectorInFlowDependent(bitUse, setDef, "SSAUnaryOpInstruction");
			}
			else
			{
				Assertions.UNREACHABLE();
			}
		}
		
		else if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$1")) 
		{
			SSABinaryOpInstruction instruction = (SSABinaryOpInstruction) SSA_instruction;
			
			if(instruction.getNumberOfUses() == 2)
			{
				int Def = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(instruction.getDef())));
				TreeSet<Integer> setDef = new TreeSet<Integer>();
				setDef.add(Def);
				
				int use_1 = instruction.getUse(0);
 				int use_2 = instruction.getUse(1);
 				int Use_1 = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(use_1)));
 				int Use_2 = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(use_2)));
 				
 				BitVectorVariable bitUse = new BitVectorVariable();
 				
 				if(iorigin != 0)
 				{
 					if(st.getValue(use_1)!=null || st.getValue(use_2)!=null)
 					{
 						BitVector kill = new BitVector();
 					    BitVector gen = new BitVector();
 					    kill.set(Def);
 					    return new BitVectorKillGen(kill, gen);
 					}
 					
 					bitUse.set(Use_1);
 					bitUse.set(Use_2);
 					
 					return new BitVectorInFlowDependent(bitUse, setDef, "SSABinaryOpInstruction");
				}
 				else
 				{
 					if(st.getValue(use_1)!=null && st.getValue(use_2)!=null)
 					{
 						BitVector kill = new BitVector();
 					    BitVector gen = new BitVector();
 					    gen.set(Def);
 					    return new BitVectorKillGen(kill, gen);
 					}
 					else
 					{
 						if(st.getValue(use_1)==null)
 	 	 				{
 	 						bitUse.set(Use_1);
 	 	 				}
 						if(st.getValue(use_2)==null)
 	 	 				{
 	 						bitUse.set(Use_2);
 	 	 				}
 						
 	 					return new BitVectorInFlowDependent(bitUse, setDef, "SSABinaryOpInstruction");
 					}
 				}
			}
			else
			{
				Assertions.UNREACHABLE();
			}
		}
		
		else if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$3")) 
		{
			SSAGetInstruction instruction = (SSAGetInstruction) SSA_instruction;
			
			int vn = instruction.getDef();			
			Pair<CGNode, String> ObjField = variable2ObjectField.get(vn+"@"+methodName);
			
			if(ObjField == null)
			{
				if(getNodeTransferFunction_DEBUG_Output)
				{
					System.out.println(methodName + " " + instruction + " " + vn);
				}
			}
			
			int Use = variableNumbering.getMappedIndex(ObjField);
			
			if(Use < 0)
			{				
				CGNode objFieldNode = ObjField.fst;
				String[] ObjFieldName = ObjField.snd.split("\\.");
				
				if(getNodeTransferFunction_DEBUG_Output)
				{
					System.out.println("Get without Define " + methodName + " " + instruction + " " + vn);
				}
				
				Use = variableNumbering.getMappedIndex(Pair.make(objFieldNode, ObjFieldName[0]));
			}
			
			int Def = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));
			TreeSet<Integer> setDef = new TreeSet<Integer>();
			setDef.add(Def);
			
			BitVectorVariable bitUse = new BitVectorVariable();
			bitUse.set(Use);
			
			return new BitVectorInFlowDependent(bitUse, setDef, "SSAGetInstruction");
		}
        
		else if(SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead"))
		{
			JavaScriptPropertyRead instruction = (JavaScriptPropertyRead) SSA_instruction;
			
			int vn = instruction.getDef();
		
			Pair<CGNode, String> ObjField = variable2ObjectField.get(vn+"@"+methodName);
			int Use = variableNumbering.getMappedIndex(variable2ObjectField.get(vn+"@"+methodName));
			
			if(Use < 0)
			{
				CGNode objFieldNode = ObjField.fst;
				String[] ObjFieldName = ObjField.snd.split("\\.");
				
				if(getNodeTransferFunction_DEBUG_Output)
				{
					System.out.println("Read without Definition " + methodName + " " + instruction + " " + vn);
				}
				
				Use = variableNumbering.getMappedIndex(Pair.make(objFieldNode, ObjFieldName[0]));
			}
			else
			{
				int Def = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));			
				BitVectorVariable bitUse = new BitVectorVariable();
				bitUse.set(Use);
				
				TreeSet<Integer> setDef = new TreeSet<Integer>();
				setDef.add(Def);
				return new BitVectorInFlowDependent(bitUse, setDef, "JavaScriptPropertyRead");
			}
		}
		
		else if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$4")) 
		{			
			SSANewInstruction instruction = (SSANewInstruction) SSA_instruction;
			
			BitVector kill = new BitVector();
	        BitVector gen = new BitVector();
			
			for(int j=0; j<instruction.getNumberOfDefs(); j++)
			{
				int vn = instruction.getDef(j);
				int vn_index = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));
				
				if(vn_index == -1)
				{
					if(getNodeTransferFunction_DEBUG_Output)
					{
						System.out.println("SSA New Instruction " + instruction + " cannot find Def [" + j + "] from VariableNumbering.");
						System.out.println(methodName + " " + instructionIndex);
						System.out.println(((AstMethod) cgNode.getMethod()).getSourcePosition(instructionIndex));
					}
				}
				
				if(iorigin == 0)
				{
					gen.set(vn_index);
				}
				else
				{
					kill.set(vn_index);
				}
			}
			
			if(getNodeTransferFunction_DEBUG_Output)
			{
				System.out.println("SSA New Instruction: Gen " + gen + ", KILL " + kill);
			}
			
			return new BitVectorKillGen(kill, gen);
		}
		else if(SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite"))
		{
			JavaScriptPropertyWrite instruction = (JavaScriptPropertyWrite) SSA_instruction;
			
			if(instruction.getNumberOfUses() == 3)
			{
				int object_vn = instruction.getObjectRef();
				int Def = variableNumbering.getMappedIndex(Pair.make(cgNode, object_vn + "." + instruction.getMemberRef()));
				
				TreeSet<Integer> setDef = new TreeSet<Integer>();
				setDef.add(Def);
				
				int use_vn = instruction.getUse(2);
				if(st.getValue(use_vn)!=null)
				{
					BitVector kill = new BitVector();
			        BitVector gen = new BitVector();
			        
					if(iorigin == 0)
					{
						gen.set(Def);
					}
					else
					{
						kill.set(Def);
					}
					
					return new BitVectorKillGen(kill, gen);
				}
				else
				{
					int Use = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(use_vn)));
					
					BitVectorVariable bitUse = new BitVectorVariable();
					bitUse.set(Use);
					
					return new BitVectorInFlowDependent(bitUse, setDef, "JavaScriptPropertyWrite");
				}
			}
			else
			{
				if(getNodeTransferFunction_DEBUG_Output)
				{
					System.out.println(methodName + " " + instruction + " " + ((AstMethod) cgNode.getMethod()).getSourcePosition(instructionIndex));
					System.out.println("Number of Use: " + instruction.getNumberOfUses());
					for(int j = 0; j < instruction.getNumberOfUses(); j++)
					{
						System.out.println("Use " + instruction.getUse(j));
					}
				}	
				
				Assertions.UNREACHABLE();
			}
		}
		else if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$5")) 
		{
			SSAPutInstruction instruction = (SSAPutInstruction) SSA_instruction;
			
			if(instruction.getNumberOfUses() == 2)
			{
				int object_vn = instruction.getUse(0);
				int Def = variableNumbering.getMappedIndex(Pair.make(cgNode, object_vn + "." + instruction.getDeclaredField().getName().toString()));
				
				TreeSet<Integer> setDef = new TreeSet<Integer>();
				setDef.add(Def);
				
				int use_vn = instruction.getUse(1);
				if(st.getValue(use_vn)!=null)
				{
					BitVector kill = new BitVector();
			        BitVector gen = new BitVector();
			        
					if(iorigin == 0)
					{
						gen.set(Def);
					}
					else
					{
						kill.set(Def);
					}
					
					return new BitVectorKillGen(kill, gen);
				}
				else
				{
					int Use = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(use_vn)));
					
					if(Use < 0)
					{
						if(getNodeTransferFunction_DEBUG_Output)
						{
							System.out.println(methodName + " " + instruction + " " + use_vn);
						}
					}
					
					BitVectorVariable bitUse = new BitVectorVariable();
					bitUse.set(Use);
					return new BitVectorInFlowDependent(bitUse, setDef, "SSAPutInstruction");
				}
				
			}
			else
			{
				Assertions.UNREACHABLE();
			}
		}
		
		else if (SSA_instruction.getClass().getName().startsWith("com.ibm.wala.cast.js.ssa.JavaScriptInvoke")) 
		{
			JavaScriptInvoke instruction = (JavaScriptInvoke) SSA_instruction;
			
			if(instruction.getNumberOfReturnValues()>1)
			{
				Assertions.UNREACHABLE();
			}
			
			if(iorigin != 0)
			{
				BitVector kill = new BitVector();
		        BitVector gen = new BitVector();
		        
		        int vn = instruction.getReturnValue(0);
				int Ret = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));
				kill.set(Ret);
				
				return new BitVectorKillGen(kill, gen);
			}
			else
			{
				BitVectorVariable bitUse = new BitVectorVariable();
				for(int j=1; j<instruction.params.length; j++)
				{
					int vn = instruction.params[j];
					if(st.getValue(vn)==null)
					{
						int Use = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));
						bitUse.set(Use);
					}
				}
				
				// Not set yet
				if(bitUse.V == null)
				{
					BitVector kill = new BitVector();
			        BitVector gen = new BitVector();
			        
			        int vn = instruction.getReturnValue(0);
					int Ret = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));
					gen.set(Ret);
					
					if(getNodeTransferFunction_DEBUG_Output)
					{
						System.out.println("SSA Invoke Instruction: Gen " + gen + ", KILL " + kill);
					}
					
					return new BitVectorKillGen(kill, gen);
				}
				else
				{
					int vn = instruction.getReturnValue(0);
					int Ret = variableNumbering.getMappedIndex(Pair.make(cgNode, String.valueOf(vn)));
					
					TreeSet<Integer> setDef = new TreeSet<Integer>();
					setDef.add(Ret);
					
					return new BitVectorInFlowDependent(bitUse, setDef, "JavaScriptInvoke");
				}
			}
		}
		else
		{
			return BitVectorIdentity.instance();
		}*/
        
        return BitVectorIdentity.instance();
        
      }

    public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dst) 
    {
    	/*if(Flow_Edge_DEBUG)
    	{
    		if(!src.getNode().equals(dst.getNode()))
        	{
        		SSAInstruction i = src.getDelegate().getInstruction();
        		
        		if(i!=null)
        		{
        			System.out.println("[Source] " + i.getClass());
        		}
        		else
        		{
        			System.out.println("[Source] " + i);
        		}
        		
        		i = dst.getDelegate().getInstruction();
        		
        		if(i!=null)
        		{
        			System.out.println("[Destination] " + i.getClass());
        		}
        		else
        		{
        			System.out.println("[Destination] " + i);
        		}
        		
        		System.out.println();
        	}
    	}*/
    	
    	if (isCallToReturnEdge(src, dst)) 
        {
          return BitVectorKillAll.instance();
        }
        else if(isInvocationEdge(src,dst))
        {
      	  if(getEdgeTransferFunction_DEBUG)
      	  {
      		  System.out.println(src.getNode().getMethod().getDeclaringClass().getName() + " invoke " + dst.getNode().getMethod().getDeclaringClass().getName());
      		  // System.out.println("[Source] " + src.getDelegate().getInstruction());
      		  // System.out.println("[Destination] " + dst.getDelegate().getInstruction());
      	  }
      	  
      	  return BitVectorIdentity.instance();
        }
        else if(isReturnEdge(src,dst))
        {
      	  if(getEdgeTransferFunction_DEBUG)
      	  {
      		  System.out.println(src.getNode().getMethod().getDeclaringClass().getName() + " return to " + dst.getNode().getMethod().getDeclaringClass().getName());
      		  // System.out.println("[Source] " + src.getDelegate().getInstruction());
      		  // System.out.println("[Destination] " + dst.getDelegate().getInstruction());
      	  }

      	  return BitVectorIdentity.instance();
        }
        else 
        {
          return BitVectorIdentity.instance();
        }
    }

    private boolean isCallToReturnEdge(BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dst) 
    {
      SSAInstruction srcInst = src.getDelegate().getInstruction();
      return srcInst instanceof SSAAbstractInvokeInstruction && src.getNode().equals(dst.getNode());
    }
    
    private boolean isInvocationEdge(BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dst) 
    {
      SSAInstruction srcInst = src.getDelegate().getInstruction();
      return srcInst instanceof SSAAbstractInvokeInstruction && !src.getNode().equals(dst.getNode());
    }
    
    private boolean isReturnEdge(BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dst) 
    {
      SSAInstruction srcInst = src.getDelegate().getInstruction();
      return (srcInst == null) && !src.getNode().equals(dst.getNode());
    }
    
    public TreeSet<Integer> getGlobalNumberingForVariableName(CGNode node, String nodeName, String variableName)
    {
 	   TreeMap<Integer, String> vn2Name = ToMaTo.VariableNameMapping.get(nodeName);
 	   if(vn2Name == null)
 	   {
 		   return null;
 	   }
 	   else
 	   {
 		   	TreeSet<Integer> GlobalNumberingSet = new TreeSet<Integer>();
 		   	Iterator iter_vn = vn2Name.entrySet().iterator();
 			Entry e_vn;
 			while (iter_vn.hasNext())
 			{
 				e_vn = (Entry) iter_vn.next();
 				TreeSet<String> nss = (TreeSet<String>) e_vn.getValue();
 				int vn = (Integer) e_vn.getKey();
 				for(String ns : nss)
 				{
 					if(ns.equalsIgnoreCase(variableName))
 					{
 						int globalNumbering = variableNumbering.getMappedIndex(Pair.make(node, String.valueOf(vn)));
 						if(globalNumbering != -1)
 						{
 							GlobalNumberingSet.add(globalNumbering);
 						}
 					}
 				}
 			}
 			
 			return GlobalNumberingSet;
 	   }
    }
  }

  /**
   * run the analysis
   * 
   * @return the solver used for the analysis, which contains the analysis result
   */ 
  
   public BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> analyze() {
   
	// the framework describes the dataflow problem, in particular the underlying graph and the transfer functions
    BitVectorFramework<BasicBlockInContext<IExplodedBasicBlock>, Pair<CGNode, String>> framework = new BitVectorFramework<BasicBlockInContext<IExplodedBasicBlock>, Pair<CGNode, String>>(icfg, new TransferFunctions(), variableNumbering);
    BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> solver = new BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>>(framework);
    
    try
    {
      solver.solve(null);
    } 
    catch (CancelException e) 
    {
      // this shouldn't happen
      assert false;
    }
    
    if (analyze_DEBUG)
    {
      for (BasicBlockInContext<IExplodedBasicBlock> ebb : icfg) 
      {
    	  // Function of Interest
    	  // String functionNamePattern = "inner1";
    	  // String functionNamePattern = "";
    	  
    	  String methodName = ebb.getMethod().getDeclaringClass().getName().toString();
    	  TreeMap<Integer, String> nameMap = ToMaTo.VariableNameMapping.get(methodName);
    	  TreeMap<Integer, Integer> originMap = instructionOrigin.get(methodName);
    	  
		  // CGNode filter, for debugging
		  if (!(methodName.startsWith(PageNamePattern) && methodName.endsWith(FunctionNamePattern))) 
		  {
			  continue;
		  }
		  
		  // Ignore NULL instructions
		  if(ebb.getDelegate().getInstruction() == null)
		  {
			  continue;
		  }
		  else
		  {
		      BitVectorVariable in = solver.getIn(ebb);
		      System.out.println("\n[" + in.getValue().size() + "] External variables/information flowed in." );
		      IntIterator iter_in = in.getValue().intIterator();
		      while(iter_in.hasNext())
		      {
		    	  int global_vn = iter_in.next();
		    	  Pair<CGNode, String> in_pair = variableNumbering.getMappedObject(global_vn);
		    	  String in_vn = in_pair.snd;
		    	  if(in_vn.contains("."))
		    	  {
		    		in_vn = in_vn.split("\\.")[0];  
		    	  }
		    	  int vn =  Integer.parseInt(in_vn);
		    	  String origin_name = nameMap.get(vn);
		    	  if(origin_name == null)
		    	  {
		    		  System.out.println("[" + vn + "]");
		    	  }
		    	  else
		    	  {
		    		  System.out.println("[" + origin_name + "]");  
		    	  }
		      }
		      
			  System.out.println("<< ===== ORIGIN [" + originMap.get(ebb.getFirstInstructionIndex())+ "] [" + ebb.getDelegate().getInstruction() + "] ===== >");
		      
		      BitVectorVariable out = solver.getOut(ebb);
		      System.out.println("[" + out.getValue().size() + "] external variables/information flowed out." );
		      IntIterator iter_out = out.getValue().intIterator();
		      while(iter_out.hasNext())
		      {
		    	  int global_vn = iter_out.next();
		    	  Pair<CGNode, String> out_pair = variableNumbering.getMappedObject(global_vn);
		    	  String out_vn = out_pair.snd;
		    	  if(out_vn.contains("."))
		    	  {
		    		  out_vn = out_vn.split("\\.")[0];  
		    	  }
		    	  int vn =  Integer.parseInt(out_vn);
		    	  String origin_name = nameMap.get(vn);
		    	  if(origin_name == null)
		    	  {
		    		  System.out.println("[" + vn + "]");
		    	  }
		    	  else
		    	  {
		    		  System.out.println("[" + origin_name + "]");  
		    	  }
		      }
		  }
      }
    }
    
    return solver;
  }
   
   	public Pair<CGNode, String> getNodeAndInstrForNumber(int num) 
   	{
	    return variableNumbering.getMappedObject(num);
	}
	
}
