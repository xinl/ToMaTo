package edu.upenn.cis.tomato.experiment;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.IdentityUrlResolver;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.impl.LineNumberPosition;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

import edu.upenn.cis.tomato.application.ToMaTo;
import edu.upenn.cis.tomato.util.DebugUtil;
import edu.upenn.cis.tomato.util.ErrorUtil;


public class ObjectMemberInvocation {
	
	public static void analyzeJavaScriptCode(boolean IsPrintDefinition, boolean IsPrintInvocation) throws Exception
	{
		// Web page to be analyzed
		String strUrl ="file:\\"+(new File("").getAbsolutePath())+"\\dat\\test\\objectMemberInvocation\\ObjectTest.html";
		URL url = new URL(strUrl); String[] urlPattern = strUrl.split("\\\\"); String MashupPageName = "L" + urlPattern[urlPattern.length-1];
		
		// Function name filter
		String[] interestedFunctions ={"myObject", "invokeObj1", "Person", "makeHimSpeak", "employee", "yabadabado", "objdef", "invokeObj2", "invokeJSON", "foo", "invokeAnonymous", "invokeWith"};
		// String[] interestedFunctions ={"objdef", "yabadabado"};
		List<String> targetNames = Arrays.asList(interestedFunctions);
		
		// Invoke WALA for analysis
		//JSCallGraphBuilderUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
		SourceModule[] sources = getSources(url);
		JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(new WebPageLoaderFactory(new CAstRhinoTranslatorFactory(), null), sources, CGBuilderType.ZERO_ONE_CFA, AstIRFactory.makeDefaultFactory());
		CallGraph cg = builder.makeCallGraph(builder.getOptions());
		
		DebugUtil.DEBUG_PrintSeperationLine();
				
		Iterator<CGNode> iter = cg.iterator();
		while(iter.hasNext())
		{
			CGNode node = iter.next(); IR ir = node.getIR(); SSAInstruction[] instructions = ir.getInstructions();
			IMethod method = node.getMethod(); String className = method.getClass().getName().toString(); String nodeName = method.getDeclaringClass().getName().toString();
			
			// Filter out useless functions
			String[] functionNames = nodeName.split("/"); String functionName = ""; String[] anonymousPattern = null;
			if(className.equalsIgnoreCase(ToMaTo.CGNodeClassName) && nodeName.startsWith(MashupPageName))
			{
				if(functionNames != null)
				{
					functionName = functionNames[functionNames.length-1];
				}
				else
				{
					ErrorUtil.printErrorMessage("Ill formated function name.");
				}
				
				anonymousPattern = functionName.split("_");
				
				if( (IsPrintDefinition && targetNames.contains(functionName)) || (IsPrintInvocation && anonymousPattern.length > 1 && anonymousPattern[anonymousPattern.length-2].equalsIgnoreCase("anonymous")) )
				{
					DebugUtil.DEBUG_PrintDebugMessage("CGNode name: "+functionName);
					
					for(int i=0; i<instructions.length; i++)
					{
						SSAInstruction instruction = instructions[i]; 
						if(instruction == null)
						{
							continue;
						}
												
						String SSAType = instruction.getClass().getName().toString();
						System.out.println("\n[Instruction] " + instruction);
						System.out.println("[SSA Type] " + SSAType);

						LineNumberPosition pos = (LineNumberPosition) ((AstMethod) method).getSourcePosition(i);

						if(pos!=null)
						{
							System.out.println("[Position] @ " + pos);
						}

						if(instruction.getNumberOfDefs()>0 || instruction.getNumberOfUses()>0)
						{
							for(int j=0; j<instruction.getNumberOfDefs(); j++)
							{
								String[] ln = ir.getLocalNames(i, instruction.getDef(j));
								if(ln!=null)
								{
									for(int k=0; k<ln.length; k++)
									{
										System.out.println("[Variable Defined] " + instruction.getDef(j) + " - " + ln[k]);
									}
								}
							}	
					
							for(int j=0; j<instruction.getNumberOfUses(); j++)
							{
								String[] ln = ir.getLocalNames(i, instruction.getUse(j));
								if(ln!=null)
								{
									for(int k=0; k<ln.length; k++)
									{
										System.out.println("[Variable Used] " + instruction.getUse(j) + " - " + ln[k]);
									}
								}
							}
						}
					}
					
					DebugUtil.DEBUG_PrintSeperationLine();
				}
			}	
		}
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
