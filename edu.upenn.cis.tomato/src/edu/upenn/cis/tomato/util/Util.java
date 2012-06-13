package edu.upenn.cis.tomato.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;

import edu.upenn.cis.tomato.application.ToMaTo;
import edu.upenn.cis.tomato.data.ObjectMembers;
import edu.upenn.cis.tomato.data.ViolationSite;

public class Util {

	public static void initializeSystemBuiltinVariables() {
		// TODO Need to make this list comprehensive
		ToMaTo.systemBuiltinVariables.put("window", "DOMWindow this");
		ToMaTo.reverseSystemBuiltinVariables.put("DOMWindow this", "window");
	}

	public static String getCGNodeOrigin(IR ir, IMethod method) {

		SSAInstruction[] instructions = ir.getInstructions();
		for (int i = 0; i < instructions.length; i++) {
			if (instructions[i] == null) {
				continue;
			}

			Position pos = ((AstMethod) method).getSourcePosition(i);
			if (pos != null) {
				String[] result = pos.getURL().getFile().split("/");
				return result[result.length - 1];
			}
		}

		return null;
	}

	public static ArrayList<MethodReference> getSuccessorMethodReference(
			CallGraph cg, CGNode node) {
		ArrayList<MethodReference> MRS = new ArrayList<MethodReference>();
		Iterator<CGNode> iter_SuccessorNodes = cg.getSuccNodes(node);
		while (iter_SuccessorNodes.hasNext()) {
			CGNode SNode = iter_SuccessorNodes.next();
			MRS.add(SNode.getMethod().getReference());
		}

		return MRS;
	}

	// public static String GetRhinoNodeListPosition(LineNumberPosition p)
	// {
	//
	// String result = "[";
	// String[] positionArray = p.getURL().getFile().split("_ToMaTo_");
	// result = result + positionArray[positionArray.length-1] + ":" +
	// p.lineNumber + "]; corresponding to Rhino node list ";
	// for (int l = 0; l < p.RhinoNodeList.size(); l++)
	// {
	// String s = p.RhinoNodeList.get(l);
	// String[] n = s.split(" ");
	// int nodeType = Integer.parseInt(n[0]);
	// result = result + "(" + org.mozilla.javascript.Token.name(nodeType) +
	// ")(" + n[0] + ")@(" + n[1] + ") ";
	//
	// // UPENN.MASHUP: it seems that WALA doesn't care about the literal nodes.
	// String literal = (String) JavaScriptCompressor.literals.get(nodeType);
	// if (literal != null)
	// {
	// result = result + literal + " ";
	// }
	// else if (n.length == 3)
	// {
	// result = result + n[2] + " ";
	// }
	// }
	//
	// return result;
	// }

	public static TreeMap<Integer, String> getLocalVariableNameMapping(
			CGNode node, String nodeName, IMethod method,
			boolean IsScopeIncluded, TreeSet<Integer> variableSet) {
		if (node == null) {
			return null;
		}

		TreeMap<Integer, String> LocalVariableNameMap = ToMaTo.variableNameMapping
				.get(nodeName);
		if (LocalVariableNameMap == null) {
			LocalVariableNameMap = new TreeMap<Integer, String>();
			ToMaTo.variableNameMapping.put(nodeName, LocalVariableNameMap);
		} else {
			return LocalVariableNameMap;
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
						if (IsScopeIncluded) {
							LocalVariableNameMap.put(def_vn, ln[k] + "@"
									+ nodeName);
						} else {
							LocalVariableNameMap.put(def_vn, ln[k]);
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
						if (IsScopeIncluded) {
							LocalVariableNameMap.put(use_vn, ln[k] + "@"
									+ nodeName);
						} else {
							LocalVariableNameMap.put(use_vn, ln[k]);
						}
					}
				}
			}
		}

		return LocalVariableNameMap;
	}

	public static ObjectMembers getLineRangeForCGNode(CGNode node,
			String functionName) {
		ObjectMembers members = null;
		ArrayList<Integer> range = new ArrayList<Integer>();

		IMethod method = node.getMethod();
		IR ir = node.getIR();
		SSAInstruction[] instructions = ir.getInstructions();
		String location = "";
		for (int i = 0; i < instructions.length; i++) {
			SSAInstruction instruction = instructions[i];
			if (instruction == null) {
				continue;
			}

			Position pos = ((AstMethod) method).getSourcePosition(i);
			if (pos != null) {
				range.add(pos.getLastLine());
				location = pos.getURL().toString();
			}
		}

		// Keep only the first and last entries
		ArrayList<Integer> functionSourceRange = new ArrayList<Integer>();
		Collections.sort(range);
		if (range.size() > 0) {
			functionSourceRange.add(range.get(0));
			functionSourceRange.add(range.get(range.size() - 1));
			members = new ObjectMembers(functionName, functionSourceRange,
					location);
		} else {
			ErrorUtil
					.printErrorMessage("Unable to get the source code range for function ["
							+ functionName + "].");
		}
		return members;
	}

	public static ArrayList<ObjectMembers> getLineRangeForMemberFunctionDefinition(
			CGNode node, String nodeName) {
		boolean IsDebug = true;
		if (IsDebug) {
			DebugUtil.DEBUG_PrintSeperationLine();
			DebugUtil
					.DEBUG_PrintDebugMessage("Get Line Range for Member Method Definition for Function ["
							+ nodeName + "]\n");
		}

		ArrayList<ObjectMembers> MemberFunctionDefinitionRangeList = new ArrayList<ObjectMembers>();

		IR ir = node.getIR();
		SSAInstruction[] instructions = ir.getInstructions();
		IMethod method = node.getMethod();
		ArrayList<Integer> memberRange = new ArrayList<Integer>();

		int def = 0, use = 0, pdef = 0, puse = 0, maxposition = 0;
		boolean isOverApproximation = false;
		Position pos1 = null, pos2 = null, pos3 = null;
		String location = "";
		String memberName = "";

		for (int i = 0; i < instructions.length; i++) {
			SSAInstruction instruction = instructions[i];
			if (instruction == null) {
				continue;
			}

			String SSAType = instruction.getClass().getName().toString();
			if (SSAType == null) {
				continue;
			}

			// Check for "function "
			if (SSAType.equals("com.ibm.wala.cast.ir.ssa.AstGlobalRead")) {
				if (instruction.toString().contains("global Function")) {
					if (!memberName.equals("") && isOverApproximation) {
						ArrayList<Integer> memRange = new ArrayList<Integer>();
						Collections.sort(memberRange);
						memRange.add(memberRange.get(0));
						memRange.add(memberRange.get(memberRange.size() - 1));
						MemberFunctionDefinitionRangeList
								.add(new ObjectMembers(memberName, memRange,
										location));
					}

					isOverApproximation = false;
					maxposition = 0;
					pdef = instruction.getDef();
					if (IsDebug) {
						DebugUtil
								.DEBUG_PrintDebugMessage("Get function constructor.");
						DebugUtil.DEBUG_PrintDebugMessage("Define variable v"
								+ pdef);
					}

					memberRange = new ArrayList<Integer>();
					pos1 = ((AstMethod) method).getSourcePosition(i);
					if (pos1 != null) {
						if (pos1.getFirstLine() >= maxposition) {
							memberRange.add(pos1.getFirstLine());
							maxposition = pos1.getFirstLine();
							location = pos1.getURL().toString();
							if (IsDebug) {
								DebugUtil
										.DEBUG_PrintDebugMessage("Position @line ["
												+ pos1.getFirstLine() + "]");
							}
						}
					}
				}
				continue;
			}

			// Check for "function ()"
			if (SSAType.equals("com.ibm.wala.cast.js.ssa.JavaScriptInvoke")
					&& !isOverApproximation) {
				if (instruction.toString().contains("construct")) {
					puse = instruction.getUse(0);
					if (pdef == puse) {
						def = instruction.getDef();
						if (IsDebug) {
							DebugUtil
									.DEBUG_PrintDebugMessage("Match function constructor variable v"
											+ puse);
						}

						pos2 = ((AstMethod) method).getSourcePosition(i);
						if (pos2 != null) {
							if (pos2.getFirstLine() >= maxposition) {
								memberRange.add(pos2.getFirstLine());
								maxposition = pos2.getFirstLine();
								memberRange.add(pos2.getFirstLine());
								if (IsDebug) {
									DebugUtil
											.DEBUG_PrintDebugMessage("Position @line ["
													+ pos2.getFirstLine() + "]");
								}
							}
						}
					}
				}
				continue;
			}

			// Check for "function functionName()"
			if (SSAType
					.equals("com.ibm.wala.cast.js.loader.JavaScriptLoader$1$1$5")
					&& !isOverApproximation) {
				if (instruction.toString().contains("put")) {
					memberName = "";
					use = instruction.getUse(1);
					if (def == use) {
						if (IsDebug) {
							DebugUtil
									.DEBUG_PrintDebugMessage("Match function name variable v"
											+ use);
						}

						pos3 = ((AstMethod) method).getSourcePosition(i);
						if (pos3 != null) {
							if (pos3.getFirstLine() >= maxposition) {
								memberRange.add(pos3.getFirstLine());
								maxposition = pos3.getFirstLine();
								memberRange.add(pos3.getFirstLine());
								if (IsDebug) {
									DebugUtil
											.DEBUG_PrintDebugMessage("Position @line ["
													+ pos3.getFirstLine() + "]");
								}
							}
						}

						if (instruction.toString().contains("<")) {
							String instr = instruction.toString().substring(
									instruction.toString().indexOf("<"),
									instruction.toString().indexOf(">"));
							String[] tokens = instr.split(",");
							if (tokens.length > 2) {
								memberName = tokens[2].trim();
								if (IsDebug) {
									DebugUtil
											.DEBUG_PrintDebugMessage("Get member function name ["
													+ memberName + "].");
								}
							}
						}

						def = 0;
						use = 0;
						pdef = 0;
						puse = 0;
						pos3 = null;
						pos2 = null;
						pos1 = null;
						isOverApproximation = true;
					}
				}
				continue;
			}
			// Padding position before the next member function definition
			if (isOverApproximation
					&& !SSAType.equals("com.ibm.wala.ssa.SSAReturnInstruction")) {
				Position pos = ((AstMethod) method).getSourcePosition(i);
				if (pos != null) {
					if (pos.getFirstLine() >= maxposition) {
						memberRange.add(pos.getFirstLine());
						maxposition = pos.getFirstLine();
						memberRange.add(pos.getFirstLine());
						if (IsDebug) {
							DebugUtil
									.DEBUG_PrintDebugMessage("Over Approximation Position @line ["
											+ pos.getFirstLine() + "]");
						}
					}
				}
			}
			// Until "return" is reached
			if (isOverApproximation
					&& SSAType.equals("com.ibm.wala.ssa.SSAReturnInstruction")) {
				if (!memberName.equals("")) {
					ArrayList<Integer> memRange = new ArrayList<Integer>();
					Collections.sort(memberRange);
					memRange.add(memberRange.get(0));
					memRange.add(memberRange.get(memberRange.size() - 1));
					MemberFunctionDefinitionRangeList.add(new ObjectMembers(
							memberName, memRange, location));
					isOverApproximation = false;
					break;
				}
			}
		}
		return MemberFunctionDefinitionRangeList;
	}

	public static void refineMemberFunctionDefinitionList(
			ArrayList<ObjectMembers> GlobalFunctions,
			ArrayList<ObjectMembers> MemberFunctions) {

		for (ObjectMembers member : MemberFunctions) {
			boolean findParent = false;
			for (ObjectMembers parent : GlobalFunctions) {
				if (member.getLocationName().equals(parent.getLocationName())) {
					if (parent.getRange().get(0) <= member.getRange().get(0)
							&& parent.getRange().get(
									parent.getRange().size() - 1) >= member
									.getRange().get(
											member.getRange().size() - 1)) {
						member.setClassName(parent.getFunctionName());
						member.setFunctionName(member.getClassName() + "."
								+ member.getFunctionName());
						findParent = true;
						break;
					}
				}
			}

			if (!findParent) {
				member.setClassName("JSON");
				member.setFunctionName("[JSON Data] "
						+ member.getFunctionName());
			}
		}
	}

	public static void refineMemberFunctionInvocationList(
			ArrayList<ObjectMembers> AnonFunctions,
			ArrayList<ObjectMembers> MemberFunctions) {
		for (ObjectMembers anonymousFunction : AnonFunctions) {
			int start = anonymousFunction.getRange().get(0);
			for (ObjectMembers memberFunctionDefinition : MemberFunctions) {
				if (anonymousFunction.getLocationName().equals(
						memberFunctionDefinition.getLocationName())) {
					if (start >= memberFunctionDefinition.getRange().get(0)
							&& start <= memberFunctionDefinition.getRange()
									.get(memberFunctionDefinition.getRange()
											.size() - 1)) {
						anonymousFunction
								.setFunctionName(memberFunctionDefinition
										.getFunctionName());
						anonymousFunction.setClassName(memberFunctionDefinition
								.getClassName());
					}
				}
			}
		}
	}

	public static String getViolationSitesFilePath() {
		String filePath = new File("").getAbsolutePath();
		if (filePath.endsWith("tomato")) {
			filePath = filePath.replaceAll("edu.upenn.cis.tomato",
					"edu.upenn.cis.tomato.instrumentation")
					+ "\\cache\\ToMaTo_ViolationSites.vsf";
		} else if (filePath.endsWith("instrumentation")) {
			filePath = filePath + "\\cache\\ToMaTo_ViolationSites.vsf";
		}

		return filePath;
	}

	public static void printViolationSites(
			HashMap<String, ArrayList<ViolationSite>> ViolationSites) {
		DebugUtil.DEBUG_PrintSeperationLine();
		DebugUtil
				.DEBUG_PrintDebugMessage("ToMaTo violation sites interface with code instrumentation: \n");
		Iterator<Entry<String, ArrayList<ViolationSite>>> iter = ViolationSites
				.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, ArrayList<ViolationSite>> e = iter.next();
			String URL = e.getKey();
			DebugUtil.DEBUG_PrintDebugMessage("[External Library] " + URL);
			ArrayList<ViolationSite> vs = e.getValue();
			for (int i = 0; i < vs.size(); i++) {
				vs.get(i).printViolationSite();
			}
		}
	}

	public static void writeViolationSitesToFile(Object O, String URL)
			throws IOException {
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(URL));
			out.writeObject(O);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
