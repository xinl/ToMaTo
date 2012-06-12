package edu.upenn.cis.tomato.policy;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;
import java.util.TreeSet;

public class PolicyMaker {
	
	//UPENN.MASHUP: policyCollection is the place to store all the policy information, key is the library URL.
	public static TreeMap<String, LibraryPolicy> policyCollection = new TreeMap<String, LibraryPolicy>();	
	public static String policyFileLocation;
	
	public static final int PolicyType_Variable = 0;
	public static final int PolicyType_function = 1;
	
	public static final int PolicyMode_DefaultAllow = 0;
	public static final int PolicyMode_DefaultDeny = 1;
	
	public PolicyMaker(String location)
	{
		policyFileLocation = location;
	}
	
	public static void StorePolicyToFile()
	{
		try 
		{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(policyFileLocation));
			
			if (out != null) 
			{	
				System.out.println("Pushing policy to file.");
				out.writeObject(policyCollection);
	            out.flush();
	            out.close();
			}			
			
			System.out.println("Data storing in file " + policyFileLocation);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
	}
		
	public static void ReadPolicyFromFile()
	{
		try 
		{
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(policyFileLocation));
			System.out.println("Reading object from file " + policyFileLocation + ".");
			Object o = in.readObject(); in.close();
			
			try
			{
				policyCollection = (TreeMap<String, LibraryPolicy>) o;
			}
			catch(Exception e)
			{
				System.out.println(policyFileLocation + " is not a valid policy file.");
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void AddPolicyRule(String library, int policyType, int policyMode, String rule)
	{
		LibraryPolicy lp = policyCollection.get(library);
		if(lp == null)
		{
			lp = new LibraryPolicy();
			lp.libraryID = library;
			policyCollection.put(library, lp);
		}
		
		PolicyItem pi = lp.policy.get(policyType);
		if(pi == null)
		{
			pi = new PolicyItem();
			pi.policyType = policyType;
			lp.policy.put(policyType, pi);
		}
		
		TreeSet<String> rules = pi.rules.get(policyMode);
		if(rules == null)
		{
			rules = new TreeSet<String>();
			pi.rules.put(policyMode, rules);
		}
		
		rules.add(rule);
	}
	
}
