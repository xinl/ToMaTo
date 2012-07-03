package edu.upenn.cis.tomato.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.IUrlResolver;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.UnicodeReader;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;

/**
 * A bundle of the entry point HTML page and all JavaScript file it referred to.
 * It caches these source file in memory and provides routines for read from and
 * write to external destinations. It also builds MappedSourceModule for static
 * analysis.
 * 
 * @author Xin Li
 * @version July 3, 2012
 */
public class SourceBundle {
	private String entryPoint;
	private Map<String, String> sources;
	private Set<MappedSourceModule> sourceModules;
	private int anonymousSourceCounter = 0;

	/**
	 * Construct new SourceBundle.
	 * 
	 * @param entryPoint
	 *            the URL of the entry point HTML page.
	 * @throws IOException
	 */
	public SourceBundle(String entryPoint) throws IOException {
		this.entryPoint = entryPoint;
		addSource(entryPoint);

		JSSourceExtractor extractor = new DefaultSourceExtractor();
		this.sourceModules = extractor.extractSources(new URL(entryPoint), new JerichoHtmlParser(), new SourceBundleUrlResolver());
	}

	/**
	 * Check whether a source of specified URL is in this bundle.
	 * 
	 * @param url
	 *            URL of the source to check.
	 * @return whether the specified source is in this bundle
	 */
	public boolean hasSource(String url) {
		return sources.containsKey(url);

	}

	/**
	 * Get the content of the source of specified URL.
	 * 
	 * @param url
	 *            URL of the source
	 * @return the content of the source or <code>null</code> if the source
	 *         doesn't exist.
	 */
	public String getSource(String url) {
		return sources.get(url);

	}

	/**
	 * Get a substring of source content of specified URL. The substring begins
	 * at <code>startOffset</code> and ends with character at
	 * <code>endOffset - 1</code>.
	 * 
	 * @param url
	 *            URL of the source
	 * @param startOffset
	 *            the character index of the start of the substring
	 * @param endOffset
	 *            the character index of the end of the substring
	 * @return the substring of source content or <code>null</code> if the
	 *         source doesn't exist.
	 */
	public String getSource(String url, int startOffset, int endOffset) {
		String content = sources.get(url);
		if (content == null)
			return null;
		else
			return content.substring(startOffset, endOffset);

	}

	/**
	 * Get mapped source modules for further static analysis.
	 * 
	 * @return a set of mapped source modules
	 */
	public Set<MappedSourceModule> getSourceModules() {
		return sourceModules;
	}

	/**
	 * Add a source to the bundle, fetches the content by URL specified.
	 * 
	 * @param url
	 *            the URL of the source
	 * @throws IOException
	 */
	public void addSource(String url) throws IOException {
		sources.put(url, fetchURLContent(url));
	}

	/**
	 * Add a source to the bundle with designated content.
	 * 
	 * @param url
	 *            the URL of the new source. It doesn't have to exists or be
	 *            readable.
	 * @param content
	 *            the designated content of the new source
	 */
	public void addSource(String url, String content) {
		sources.put(url, content);
	}

	/**
	 * Save all sources in the bundle to a local path, including the HTML page
	 * and all referred JavaScript files. It will try to infer file name from
	 * URL. If failed, file names will be set to "default.html" and
	 * "default-n.js", where n is a unique integer assigned. The inferred path
	 * of the script files will be a sub-directory under output path name
	 * whenever possible, otherwise the full directory structure starting from
	 * host name will be put under output path. For example: If the URL is
	 * http://www.example.com/dir/script.js, the script file will be put under
	 * [output_path]/www.example.com/dir/
	 * 
	 * @param pathName
	 * @throws IOException
	 */
	public void saveSourceBundleTo(String pathName) throws IOException {

		File path = new File(pathName);

		if (!path.isDirectory() || !path.exists()) {
			throw new IOException("Invalid path.");
		}

		// TODO: we should update the src of script tags in HTML page,
		// but that's difficult before WALA guys fix SourceExtractor inheritance
		// issue (https://github.com/wala/WALA/issues/3)
		for (Map.Entry<String, String> entry : sources.entrySet()) {
			File dir = new File(path, inferPathFromURL(entry.getKey()));
			if (!dir.exists()) {
				dir.mkdir();
			}
			File file = new File(dir, inferFileNameFromURL(entry.getKey()));
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getName());
			BufferedWriter bw = new BufferedWriter(fw);
			try {
				bw.write(entry.getValue());
			} finally {
				bw.close();
			}
		}

		anonymousSourceCounter = 0;
	}

	private String inferFileNameFromURL(String url) {
		url = URI.create(url).toString(); // escape url string
		int separatorIndex = url.lastIndexOf("/");

		if (separatorIndex < 0 || separatorIndex == url.length() - 1) {
			// if URL has no file name
			if (url.equals(entryPoint)) {
				return "default.html";
			} else {
				anonymousSourceCounter++;
				return "default-" + anonymousSourceCounter + ".js";
			}
		} else {
			// discard fragment and query parts, if any
			int anchorIndex = url.lastIndexOf("#");
			int queryIndex = url.lastIndexOf("?");
			int endIndex;
			if (anchorIndex < 0 && queryIndex < 0) {
				// both not found
				endIndex = url.length();
			} else if (anchorIndex >= 0 && queryIndex >= 0) {
				// both found
				endIndex = (anchorIndex < queryIndex) ? anchorIndex : queryIndex;
			} else {
				// one found, one not found
				endIndex = (anchorIndex > queryIndex) ? anchorIndex : queryIndex;
			}
			String rawName = url.substring(separatorIndex + 1, endIndex);
			// assign proper extension
			if (url.equals(entryPoint)) {
				if (rawName.endsWith(".html") || rawName.endsWith(".htm")) {
					return rawName;
				} else {
					return rawName + ".html";
				}
			} else {
				if (rawName.endsWith(".js")) {
					return rawName;
				} else {
					return rawName + ".js";
				}
			}
		}
	}

	private String inferPathFromURL(String url) {
		url = URI.create(url).toString(); // escape url string
		if (url.equals(entryPoint)) {
			return ""; // HTML page should be in the root directory
		}

		int separatorIndex = url.lastIndexOf("/");
		String path;
		if (url.startsWith(entryPoint)) {
			// script file is at the same dir level as html page, or deeper
			path = url.substring(entryPoint.length(), separatorIndex + 1);
			// + 1 to including the trailing separator
		} else {
			// script file is in some other place
			path = url.substring(0, separatorIndex + 1).replaceFirst("\\w*:/+", "");
			// chop off the head and tail, note all /'s are choped from head.
		}
		if (!File.separator.equals("/")) {
			path.replaceAll("/", File.separator); // convert to platform
													// specific path
		}
		return path;

	}

	/**
	 * Fetch the content at an URL.
	 * 
	 * @param urlString
	 *            the URL pointing to the content.
	 * @return the content at the specified URL.
	 * @throws IOException
	 */
	public static String fetchURLContent(String urlString) throws IOException {
		URL url = new URL(urlString);
		InputStream inputStream = url.openConnection().getInputStream();
		try {
			String line;
			BufferedReader reader = new BufferedReader(new UnicodeReader(inputStream, "UTF8"));
			StringBuffer buf = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				buf.append(line).append("\n");
			}
			return buf.toString();
		} finally {
			inputStream.close();
		}
	}

	/**
	 * The URL resolver passed to the source extractor to intercept and cache
	 * the external JavaScript files referenced in the entry point page.
	 */
	public class SourceBundleUrlResolver implements IUrlResolver {

		public URL resolve(URL input) {
			try {
				addSource(input.toString());
			} catch (IOException e) {
				throw new RuntimeException("Error reading URL: " + input);
			}
			return input;
		}

		public URL deResolve(URL input) {
			return input;
		}

	}
}
