package edu.upenn.cis.tomato.policy;

import java.io.Serializable;
import java.util.TreeMap;

public class LibraryPolicy implements Serializable{

	private static final long serialVersionUID = 1;
	//UPENN.MASHUP: library ID is the url of the library in question.
	public String libraryID = "";
	//UPENN.MASHUP: policy is a collection of all the policy items, key is the library type.
	public TreeMap<Integer, PolicyItem> policy = new TreeMap<Integer, PolicyItem>();

}
