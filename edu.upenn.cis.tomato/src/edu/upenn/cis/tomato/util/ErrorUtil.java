package edu.upenn.cis.tomato.util;

public class ErrorUtil {
	
	public static final String ERROR_PROMPT = "[ToMaTo System Error]";
	
	public static void printErrorMessage(String msg)
	{
		System.err.println(ERROR_PROMPT + " " + msg);
	}

}
