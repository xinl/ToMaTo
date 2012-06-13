package edu.upenn.cis.tomato.policy;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.upenn.cis.tomato.application.ToMaTo;

public class PolicyChecker {
	
	public static final String SYSTEM_VARIABLE_KEY = "System Variables";
	public static final String GLOBAL_VARIABLE_KEY = "Global Variables";
	
	public static boolean isFunctionInvocationAllowed(String library, String function)
	{
		LibraryPolicy lp = PolicyMaker.policyCollection.get(library);
		if(lp == null)
		{
			return true;
		}
		
		PolicyItem pi = lp.policy.get(PolicyMaker.PolicyType_function);
		if(pi == null)
		{
			return true;
		}
		
		TreeSet<String> rules = pi.rules.get(PolicyMaker.PolicyMode_DefaultAllow);
		if(rules != null)
		{
			Iterator<String> iter_rules = rules.iterator();
			while(iter_rules.hasNext())
			{
				String rule = iter_rules.next();
				if(rule.equals(function))
				{
					return false;
				}
			}
			
			return true;
		}
		else
		{
			rules = pi.rules.get(PolicyMaker.PolicyMode_DefaultDeny);
			if(rules == null)
			{
				return true;
			}
			else
			{
				boolean isFound = false;
				Iterator<String> iter_rules = rules.iterator();
				while(iter_rules.hasNext())
				{
					String rule = iter_rules.next();
					if(rule.equals(function))
					{
						isFound = true;
						break;
					}
				}
				
				if(isFound)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
		}
		
	}
	
	public static TreeMap<String, TreeSet<String>> fetchVariableOfInterestSet ()
	{
		TreeMap<String, TreeSet<String>> Result = new TreeMap<String, TreeSet<String>>();
		Iterator<Entry<String, LibraryPolicy>> iter_pc = PolicyMaker.policyCollection.entrySet().iterator();
		while(iter_pc.hasNext())
		{
			TreeMap<Integer, PolicyItem> lp = iter_pc.next().getValue().policy;
			PolicyItem pi = lp.get(PolicyMaker.PolicyType_Variable);
			if(pi != null)
			{
				 Iterator<Entry<Integer, TreeSet<String>>> iter_rules = pi.rules.entrySet().iterator();
				 while(iter_rules.hasNext())
				 {
					 Iterator<String> iter_rule = iter_rules.next().getValue().iterator();
					 while(iter_rule.hasNext())
					 {
						 String[] ParsedRule = iter_rule.next().split(" ");
						 if(ParsedRule.length == 1)
						 {
							 // Match the variable name with the list of system built-in objects
							 // If true do the translation
							 if(ToMaTo.systemBuiltinVariables.containsKey(ParsedRule[0]))
							 {
								 String systemVariable = ToMaTo.systemBuiltinVariables.get(ParsedRule[0]);
								 TreeSet<String> SystemVariables = Result.get(SYSTEM_VARIABLE_KEY);
								 if(SystemVariables == null)
								 {
									 SystemVariables = new TreeSet<String>();
									 Result.put(SYSTEM_VARIABLE_KEY, SystemVariables);
								 }
								 
								 SystemVariables.add(systemVariable);
							 }
							 else
							 {
								 TreeSet<String> GlobalVariables = Result.get(GLOBAL_VARIABLE_KEY);
								 if(GlobalVariables == null)
								 {
									 GlobalVariables = new TreeSet<String>();
									 Result.put(GLOBAL_VARIABLE_KEY, GlobalVariables);
								 }
								 
								 GlobalVariables.add(ParsedRule[0]);
							 }
						 }
						 if(ParsedRule.length == 2)
						 {
							 TreeSet<String> LocalVariables = Result.get(ParsedRule[0]);
							 if(LocalVariables == null)
							 {
								 LocalVariables = new TreeSet<String>();
								 Result.put(ParsedRule[0], LocalVariables);
							 }
							 LocalVariables.add(ParsedRule[1]);
						 }
					 }
				 }
			}
			
		}
		
		return Result;
	}
	
	public static TreeSet<String> fetchExternalLibraryIDs()
	{
		TreeSet<String> result = new TreeSet<String>();
		Iterator<Entry<String, LibraryPolicy>> iter_pc = PolicyMaker.policyCollection.entrySet().iterator();
		while(iter_pc.hasNext())
		{
			Entry<String, LibraryPolicy> libraryPolicyWithName = iter_pc.next();
			String libraryID = libraryPolicyWithName.getKey();
			TreeMap<Integer, PolicyItem> lp = libraryPolicyWithName.getValue().policy;
			PolicyItem pi = lp.get(PolicyMaker.PolicyType_Variable);
			if(pi != null)
			{
				result.add(libraryID);
			}
		}
		
		return result;
	}
	
	public static int isLibraryExternal(TreeSet<String> externalLibrarySet, String LibraryOrigin)
	{
		int result = 0;
		if (externalLibrarySet.contains(LibraryOrigin))
		{
			result = 1;
		}
		return result;
	}

}
