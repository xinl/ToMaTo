package edu.upenn.cis.tomato.experiment;

import java.io.File;
import java.net.URI;

import net.sourceforge.htmlunit.corejs.javascript.Context;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import edu.upenn.cis.tomato.sandbox.VirtualBrowser;
import edu.upenn.cis.tomato.sandbox.VirtualBrowser.VBSandboxHandler;

public class VirtualBrowserTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//cloneTest();
		//evalTest();
		thisTest();
	}

	private static void evalTest() {
		String prefix = new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/sandbox/";
		String htmlString = prefix + "eval.html";
		URI htmlURI = new File(htmlString).toURI().normalize();
		VirtualBrowser vb = new VirtualBrowser(htmlURI.toString());
		System.out.println(((CollectingAlertHandler)vb.webClient.getAlertHandler()).getCollectedAlerts());
		System.out.println(((VBSandboxHandler)vb.webClientClone.getSandboxHandler()).getCollectedCommands());
	}

	private static void thisTest() {
		String prefix = new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/sandbox/";
		String htmlString = prefix + "this.html";
		URI htmlURI = new File(htmlString).toURI().normalize();
		VirtualBrowser vb = new VirtualBrowser(htmlURI.toString());
		System.out.println(((CollectingAlertHandler)vb.webClient.getAlertHandler()).getCollectedAlerts());
		System.out.println(((VBSandboxHandler)vb.webClientClone.getSandboxHandler()).getCollectedCommands());
		System.out.println(((CollectingAlertHandler)vb.webClientClone.getAlertHandler()).getCollectedAlerts());
		System.out.println(((VBSandboxHandler)vb.webClientClone.getSandboxHandler()).getCollectedCommands());
	}

	private static void cloneTest() {
		String prefix = new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/basic/";
		String htmlString = prefix + "Blank.html";
		URI htmlURI = new File(htmlString).toURI().normalize();
		VirtualBrowser vb = new VirtualBrowser(htmlURI.toString());
//		vb.webClient.getJavaScriptEngine().execute((HtmlPage) vb.webClient.getCurrentWindow().getEnclosedPage(), "var x = 1; alert(x); window.sandbox(\"fork\"); x += 1; alert(x);", "sandbox", 0);

		System.out.println("Original:");
		System.out.println(vb.webClient.getAlertHandler());
		System.out.println(((CollectingAlertHandler)vb.webClient.getAlertHandler()).getCollectedAlerts());
		System.out.println(((VBSandboxHandler)vb.webClientClone.getSandboxHandler()).getCollectedCommands());

//		vb.webClientClone.getJavaScriptEngine().execute((HtmlPage) vb.webClientClone.getCurrentWindow().getEnclosedPage(), "sandbox(\"resume\"); // x += 1; alert(x);", "sandbox", 0);

		System.out.println("Cloned:");
		System.out.println(vb.webClientClone.getAlertHandler());
		System.out.println(((CollectingAlertHandler)vb.webClientClone.getAlertHandler()).getCollectedAlerts());
		System.out.println(((VBSandboxHandler)vb.webClientClone.getSandboxHandler()).getCollectedCommands());
	}



}
