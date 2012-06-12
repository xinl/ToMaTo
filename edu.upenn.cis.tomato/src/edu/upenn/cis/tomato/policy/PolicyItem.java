package edu.upenn.cis.tomato.policy;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

public class PolicyItem implements Serializable{
	
	private static final long serialVersionUID = 1;
	//UPENN.MASHUP: policyType encodes policy type, for example, "0-variable", "1-function".
	public int policyType = -1;
	
	//UPENN.MASHUP: the key encodes policy mode, for example, "0-DefaultAllow", "1-DefaultDeny".
	public TreeMap<Integer, TreeSet<String>> rules = new TreeMap<Integer, TreeSet<String>>();
}
