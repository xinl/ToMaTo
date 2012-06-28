package edu.upenn.cis.tomato.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.IUrlResolver;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.UnicodeReader;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;

public class SourceBundle {
	private String entryPoint;
	private Map<String, String> sources;
	private Set<MappedSourceModule> sourceModules;
	private int anonymousSourceCounter = 0;

	public SourceBundle(String entryPoint) throws IOException {
		this.entryPoint = entryPoint;
		addSource(entryPoint);

		JSSourceExtractor extractor = new DefaultSourceExtractor();
		this.sourceModules = extractor.extractSources(new URL(entryPoint), new JerichoHtmlParser(), new SourceBundleUrlResolver());
	}

	public boolean hasSource(String url) {
		return sources.containsKey(url);

	}

	public String getSource(String url) {
		return sources.get(url);

	}

	public String getSource(String url, int startOffset, int endOffset) {
		return sources.get(url).substring(startOffset, endOffset);

	}

	public Set<MappedSourceModule> getSourceModules() {
		return sourceModules;
	}

	public void addSource(String url) throws IOException {
		sources.put(url, fetchURLContent(url));
	}

	public void addSource(String url, String text) {
		sources.put(url, text);
	}

	public void saveSourceBundleTo(String pathName) throws IOException {

		File path = new File(pathName);

		if (!path.isDirectory() || !path.exists()) {
			throw new IOException("Invalid path.");
		}

		String entryFileName = getFileNameFromURL(entryPoint);
		if (entryFileName == null) {

		}

		for (Map.Entry<String, String> entry : sources.entrySet()) {
			File dir = new File(path, getPathFromURL(entry.getKey()));
			if (!dir.exists()) {
				dir.mkdir();
			}
			File file = new File(dir, getFileNameFromURL(entry.getKey()));
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

	private String getFileNameFromURL(String url) {
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
			return url.substring(separatorIndex + 1);
		}
	}

	private String getPathFromURL(String url) {
		if (url.equals(entryPoint)) {
			return ""; // HTML page should be in the root directory
		}
		
		int separatorIndex = url.lastIndexOf("/");
		String path;
		if (url.startsWith(entryPoint)) {
			// script file is at the same dir level as html page, or deeper
			path = url.substring(entryPoint.length(), separatorIndex + 1); // including the trailing separator
		} else {
			// script file is in some other place
			path = url.substring(0, separatorIndex + 1).replaceFirst("\\w*:/+", ""); // chop off the head and tail
		}
		if (!File.separator.equals("/")) {
			path.replaceAll("/", File.separator); // convert to platform specific path
		}
		return path;

	}

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
