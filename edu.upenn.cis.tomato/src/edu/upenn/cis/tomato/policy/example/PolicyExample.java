package edu.upenn.cis.tomato.policy.example;

import edu.upenn.cis.tomato.policy.PolicyMaker;

public class PolicyExample {
	
	public static void Example_FunctionInvocation()
	{
		String libID1 = "ExternalScript-1.js";
		String libID2 = "ExternalScript-2.js";
		PolicyMaker.addPolicyRule(libID1.toLowerCase(), PolicyMaker.PolicyType_function, PolicyMaker.PolicyMode_DefaultAllow, "alert");
		PolicyMaker.addPolicyRule(libID2.toLowerCase(), PolicyMaker.PolicyType_function, PolicyMaker.PolicyMode_DefaultDeny, "alert");
	}
	
	public static void Example_AliasAnalysis_System()
	{
		String libID = "ExternalScript.js";
		PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "window");
	}
	
	public static void Example_AliasAnalysis_Global()
	{
		String libID = "ExternalScript.js";
		PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "gVariable");
	}
	
	public static void Example_AliasAnalysis_User()
	{
		String libID = "ExternalScript.js";
		PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "userAlias myCars");
		PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "userAlias myTrucks");
	}
	
	public static void Example_AliasAnalysis_Function()
    {
            String libID = "Alias_Function.js";
            PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "myObject showArgs");
            PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "myObject showMsg");
            PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "invokeObj1 foo");
    }
	
	// TODO Add by Anand, need to be organized.
	// Source code of ExternalJS1.js is placed under dat/todo/
	public static void Example_InformationFlow()
	{
		String libID = "ExternalJS1.js";
		PolicyMaker.addPolicyRule(libID.toLowerCase(), PolicyMaker.PolicyType_Variable, PolicyMaker.PolicyMode_DefaultAllow, "o");
	}
	
	// TODO Add by Anand, need to be organized.
	// Source code of ExternalJS1.js is placed under dat/todo/
	public static void Example_ObjectMethodInvocation()
	{
		String libID1 = "ExternalJS1.js";
		PolicyMaker.addPolicyRule(libID1.toLowerCase(), PolicyMaker.PolicyType_function, PolicyMaker.PolicyMode_DefaultAllow, "hello");
		PolicyMaker.addPolicyRule(libID1.toLowerCase(), PolicyMaker.PolicyType_function, PolicyMaker.PolicyMode_DefaultAllow, "alert");
	}
	
	// TODO Need more examples for testing code instrumentation
	public static void Example_Instrumentation()
	{
		String libID1 ="ExternalScript-1.js";
		PolicyMaker.addPolicyRule(libID1.toLowerCase(), PolicyMaker.PolicyType_function, PolicyMaker.PolicyMode_DefaultAllow, "alert");
		String libID2 ="ExternalScript-2.js";
		PolicyMaker.addPolicyRule(libID2.toLowerCase(), PolicyMaker.PolicyType_function, PolicyMaker.PolicyMode_DefaultAllow, "alert");
	}
}
