package edu.upenn.cis.tomato.data;
import java.util.*;
public class ObjectMembers {
	
	private String functionName;
	
	private ArrayList<Integer> range;
	private String className;
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	private String locationName;
	public String getFunctionName() {
		return functionName;
	}
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
	public ArrayList<Integer> getRange() {
		return range;
	}
	public void setRange(ArrayList<Integer> range) {
		this.range = range;
	}
	public String getLocationName() {
		return locationName;
	}
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	public ObjectMembers(String functionName, ArrayList<Integer> range, String nodeName) {
		this.functionName=functionName;
		this.range=range;
		this.locationName=nodeName;
	}
	public void print()
	{
		System.out.println("[Function Name]: "+ this.functionName);
		System.out.print("[Range] @ " + this.locationName);
		System.out.println(" [ " + this.range.get(0) + " - " + this.range.get(this.range.size()-1) + " ]");
	}
}
