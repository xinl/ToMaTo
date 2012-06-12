package edu.upenn.cis.tomato.util;

public class ErrorUtil {
	
	public static final String ErrorPrompt = "[ToMaTo System Error]";
	
	public static void ErrorMessage(String msg)
	{
		System.err.println(ErrorPrompt + " " + msg);
	}

}
