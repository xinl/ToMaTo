package edu.upenn.cis.tomato.application;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

import edu.upenn.cis.tomato.data.ViolationSite;
import edu.upenn.cis.tomato.testcase.TestManager;
import edu.upenn.cis.tomato.util.ErrorUtil;

public class ToMaTo extends JSCallGraphBuilderUtil {

	public static TreeMap<String, TreeMap<Integer, String>> variableNameMapping = new TreeMap<String, TreeMap<Integer, String>>();
	public static TreeMap<String, TreeMap<Integer, TreeSet<String>>> positionMapping = new TreeMap<String, TreeMap<Integer, TreeSet<String>>>();
	public static HashMap<URL, ArrayList<ViolationSite>> violationSites = new HashMap<URL, ArrayList<ViolationSite>>();

	public static String mainFunctionName = "__WINDOW_MAIN__";
	public static String systemPreambleName = "Lpreamble.js";
	public static String systemPrologueName = "Lprologue.js";
	public static String fakeRootNodeName = "LFakeRoot";
	public static String violationSitePath = edu.upenn.cis.tomato.util.Util.getViolationSitesFilePath();

	public static String CGNodeClassName = "com.ibm.wala.cast.js.loader.JavaScriptLoader$JavaScriptMethodObject";

	public static TreeMap<String, String> systemBuiltinVariables = new TreeMap<String, String>();
	public static TreeMap<String, String> reverseSystemBuiltinVariables = new TreeMap<String, String>();

	public static void main(String args[]) throws ClassHierarchyException,
			IllegalArgumentException, IOException, CancelException {

		try {
			
			TestManager.testFunctionInvocation();
			// TestManager.TestAliasAnalysis("system");
			// TestManager.TestAliasAnalysis("global");
			// TestManager.TestAliasAnalysis("user");
			// TestManager.TestInformationFlow();
			// TestManager.TestCodeInstrumentation();

			// TODO: Added by Anand, need to be organized.
			// TestManager.TestBasicUI();
			// TestManager.TestObjectMethodInvocation();
		} catch (Exception e) {
			ErrorUtil.printErrorMessage("Unexpected error from WALA analysis library.");
			e.printStackTrace();
		}
	}

}
