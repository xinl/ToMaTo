package edu.upenn.cis.tomato.sandbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SandboxHandler;
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.WebClient;

/**
 * A virtual web browser that support sandboxing.
 * @author Xin Li
 */
public class VirtualBrowser implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5387486278240746013L;
	public WebClient webClient;
	public WebClient webClientClone;
	public VBSandboxHandler sandboxHandler = new VBSandboxHandler();
	public CollectingAlertHandler alertHandler = new CollectingAlertHandler();

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
		private final List<String> collectedCommands_;

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
			collectedCommands_ = list;
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
			collectedCommands_.add(message);
			try {
				webClientClone = (WebClient) deepCopy(webClient);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		/**
		 * Returns a list containing the message portion of any collected
		 * commands.
		 *
		 * @return a list of alert messages
		 */
		public List<String> getCollectedCommands() {
			return collectedCommands_;
		}
	}
}
