package edu.upenn.cis.tomato.sandbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ContinuationPending;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SandboxHandler;
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

/**
 * A virtual web browser that support sandboxing.
 *
 * @author Xin Li
 */
public class VirtualBrowser implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5387486278240746013L;
	public static WebClient webClient;
	public static WebClient webClientClone;
	public static boolean result;
	public VBSandboxHandler sandboxHandler = new VBSandboxHandler();
	public CollectingAlertHandler alertHandler = new CollectingAlertHandler();
	public ContinuationPending contPending;

	public VirtualBrowser(String url) {
		webClient = new WebClient();
		webClient.setAlertHandler(alertHandler);
		webClient.setSandboxHandler(sandboxHandler);
		webClient.waitForBackgroundJavaScript(60000);
		webClient.waitForBackgroundJavaScriptStartingBefore(10000);
		try {
			webClient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<String> getAlerts() {
		return alertHandler.getCollectedAlerts();
	}

	public List<String> getSandboxCommands() {
		return sandboxHandler.getCollectedCommands();
	}

	private static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}

	private static Object deepCopy(Object obj) throws IOException, ClassNotFoundException {
		return deserialize(serialize(obj));
	}

	/**
	 * A simple sandbox handler that keeps track of commands in a list.
	 */
	public class VBSandboxHandler implements SandboxHandler, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 3629220363691553278L;
		private final List<String> collectedCommands;

		/**
		 * Creates a new instance, initializing it with an empty list.
		 */
		public VBSandboxHandler() {
			this(new ArrayList<String>());
		}

		/**
		 * Creates an instance with the specified list.
		 *
		 * @param list
		 *            the list to store alerts in
		 */
		public VBSandboxHandler(final List<String> list) {
			WebAssert.notNull("list", list);
			collectedCommands = list;
		}

		/**
		 * Handles the sandbox command. This implementation will store the
		 * message in a list for retrieval later.
		 *
		 * @param page
		 *            the page that sent the command
		 * @param message
		 *            the message in the command
		 */
		@Override
		public void handleSandbox(final Page page, final String message) {
			collectedCommands.add(message);

			if (message.equals("fork")) {
				Context context = Context.getCurrentContext();
				contPending = context.captureContinuation();
				try {
					webClientClone = (WebClient) deepCopy(webClient);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				webClientClone.getJavaScriptEngine().executeWithoutContinuation((HtmlPage) webClientClone.getCurrentWindow().getEnclosedPage(), "window.isClone = true; sandbox(\"resume\");", "sandbox_resume", 0);
			} else if (message.equals("resume")) {
				Context context = Context.getCurrentContext();
				context.resumeContinuation(contPending.getContinuation(), (Window) webClientClone.getCurrentWindow().getScriptObject(), null);
			} else if (message.equals("setResultTrue")) {
				result = true;
				//TODO: halt JS execution here
			} else if (message.equals("setResultFalse")) {
				result = false;
				//TODO: halt JS exectuion here
			} else if (message.equals("getResult")) {
				webClient.getJavaScriptEngine().executeWithoutContinuation((HtmlPage) webClient.getCurrentWindow().getEnclosedPage(), "window.sandboxResult = " + result, "sandbox_result", 0);
			}

		}

		/**
		 * Returns a list containing the message portion of any collected
		 * commands.
		 *
		 * @return a list of alert messages
		 */
		public List<String> getCollectedCommands() {
			return collectedCommands;
		}
	}
}
