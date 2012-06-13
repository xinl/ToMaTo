package edu.upenn.cis.tomato.experiment;
import java.util.*;


public class ExperimentMain {

	public static Map<String,ArrayList<Integer>> memberFunctionRange = new HashMap<String,ArrayList<Integer>>();
	public static void main(String[] args) {
		
		// The gateway to all the experiments
		try 
		{
			// [Goal] Try to understand object member method invocation.
			// For object member function in the test examples, we generates call graph, prints SSA instructions, position, used & defined variable.
			// One can choose to print CGNode for function definitions or invocations.
			// ObjectMemberInvocation.AnalyzeJavascriptCode(true, true);
			
			ObjectMemberAnalyzer.analyze();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

}
