package edu.upenn.cis.tomato.experiment;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.IdentityUrlResolver;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;

import com.ibm.wala.cast.js.loader.JavaScriptLoader;

import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.test.Util;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

import edu.upenn.cis.tomato.application.ToMaTo;
import edu.upenn.cis.tomato.data.ObjectMembers;
import edu.upenn.cis.tomato.util.ErrorUtil;

public class ObjectMemberAnalyzer {
	public static void Analyze() throws Exception
	{
		// Web page to be analyzed
		String strUrl ="file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\objectMemberInvocation\\ObjectTest.html";
		URL url = new URL(strUrl); String[] urlPattern = strUrl.split("\\\\"); String MashupPageName = "L" + urlPattern[urlPattern.length-1];
				
		// Data structure for storing analysis result
		ArrayList<ObjectMembers> GlobalFunctionRangeList = new ArrayList<ObjectMembers>();
		ArrayList<ObjectMembers> MemberFunctionInvocationRangeList = new ArrayList<ObjectMembers>();
		ArrayList<ObjectMembers> MemberFunctionDefinitionRangeList = new ArrayList<ObjectMembers>();
		
		// Invoke WALA for analysis
		Util.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
		SourceModule[] sources = getSources(url);
		JSCFABuilder builder = com.ibm.wala.cast.js.test.Util.makeCGBuilder(new WebPageLoaderFactory(com.ibm.wala.cast.js.ipa.callgraph.Util.getTranslatorFactory()), sources, true);
		CallGraph cg = builder.makeCallGraph(builder.getOptions());
		
		Iterator<CGNode> iter = cg.iterator();
		while(iter.hasNext())
		{
			CGNode node = iter.next(); IMethod method = node.getMethod(); String className = method.getClass().getName().toString(); String nodeName = method.getDeclaringClass().getName().toString();
			
			if(className.equalsIgnoreCase(ToMaTo.CGNodeClassName) && nodeName.startsWith(MashupPageName))
			{
				
				String[] functionNames = nodeName.split("/"); String functionName; String[] anonymousPattern = null;
				if(functionNames != null)
				{
					functionName = functionNames[functionNames.length-1];
					anonymousPattern = functionName.split("_");
			
					if (anonymousPattern.length > 1 && anonymousPattern[anonymousPattern.length-2].equalsIgnoreCase("anonymous"))
					{
						ObjectMembers anonMap= edu.upenn.cis.tomato.util.Util.GetLineRangeForCGNode(node, functionName);
						if(anonMap!=null)
						{
							MemberFunctionInvocationRangeList.add(anonMap);
						}
					}
					else
					{
						ObjectMembers functionMap = edu.upenn.cis.tomato.util.Util.GetLineRangeForCGNode(node, functionName);
						if(functionMap!=null)
						{
							GlobalFunctionRangeList.add(functionMap);
						}
						MemberFunctionDefinitionRangeList.addAll(edu.upenn.cis.tomato.util.Util.GetLineRangeForMemberFunctionDefinition(node, functionName));
					}
				}
				else
				{
					ErrorUtil.ErrorMessage("Ill formated function name.");
				}
			}
		}
		
		edu.upenn.cis.tomato.util.Util.RefineMemberFunctionDefinitionList(GlobalFunctionRangeList, MemberFunctionDefinitionRangeList);
		edu.upenn.cis.tomato.util.Util.RefineMemberFunctionInvocationList(MemberFunctionInvocationRangeList, MemberFunctionDefinitionRangeList);
		
		edu.upenn.cis.tomato.util.DebugUtil.DEBUG_PrintFunctionRangeList(GlobalFunctionRangeList, "Global Function");
		edu.upenn.cis.tomato.util.DebugUtil.DEBUG_PrintFunctionRangeList(MemberFunctionDefinitionRangeList, "Member Function Definition");
		edu.upenn.cis.tomato.util.DebugUtil.DEBUG_PrintFunctionRangeList(MemberFunctionInvocationRangeList, "Member Function Invocation");
		
	}
	
	private static SourceModule[] getSources(URL url) throws Exception
	{
		JSSourceExtractor sourceExtractor = new DefaultSourceExtractor();
		Set<MappedSourceModule> sourcesMap = sourceExtractor.extractSources(url, new JerichoHtmlParser(), new IdentityUrlResolver());
		SourceModule[] sources = new SourceFileModule[sourcesMap.size()];
		int i = 0;
		for (SourceModule m : sourcesMap){
			sources[i++] = m;
		}
		
		return sources;
	}
	
}
