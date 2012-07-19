package edu.upenn.cis.tomato.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.FileMapping;
import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.ITag;
import com.ibm.wala.cast.js.html.IdentityUrlResolver;
import com.ibm.wala.cast.js.html.MappedSourceFileModule;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.SourceRegion;
import com.ibm.wala.cast.js.html.UnicodeReader;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

/**
 * A bundle of the entry point HTML page and all JavaScript file it referred to.
 * It caches these source file in memory and provides routines for read from and
 * write to external destinations. It also builds MappedSourceModule for static
 * analysis.
 * 
 * @author Xin Li
 * @version July 6, 2012
 */
public class SourceBundle {
	private URI entryPointURI;
	private URI baseURI;
	private Map<URI, String> sources = new HashMap<URI, String>();
	private Set<MappedSourceModule> sourceModules;
	private int anonymousSourceCounter = 0;

	/**
	 * Construct new SourceBundle.
	 * 
	 * @param entryPointURI
	 *            the URI of the entry point HTML page.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public SourceBundle(URI entryPointURI) throws InvalidParameterException, IOException {
		if (!entryPointURI.isAbsolute()) {
			throw new InvalidParameterException("Entry Point URI has to be absolute.");
		}
		this.entryPointURI = entryPointURI.normalize();
		this.baseURI = getBaseURI(this.entryPointURI);

		SourceBundleSourceExtractor extractor = new SourceBundleSourceExtractor();
		this.sourceModules = extractor.extractSources(entryPointURI, new JerichoHtmlParser());
	}

	public SourceBundle(String entryPointURIString) throws URISyntaxException, InvalidParameterException, IOException {
		this(new URI(entryPointURIString));
	}

	/**
	 * Return a URI without filename part, if any. (e.g.:
	 * http://example.com/file.html becomes http://example.com/)
	 * 
	 * @param uri
	 * @return The new URI without filename.
	 */
	private URI getBaseURI(URI uri) {
		String path = uri.getPath();
		int separatorIndex = path.lastIndexOf("/");
		String newPath = path.substring(0, separatorIndex + 1);
		String authority = (uri.getAuthority() != null) ? uri.getAuthority() : "";
		try {
			return new URI(uri.getScheme() + "://" + authority + newPath);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the URI of the entry point HTML page.
	 * 
	 * @return the URI of the entry point
	 */
	public URI getEntryPointURI() {
		return entryPointURI;
	}

	/**
	 * Check whether a source of specified URI is in this bundle.
	 * 
	 * @param uri
	 *            URI of the source to check.
	 * @return whether the specified source is in this bundle
	 */
	public boolean hasSource(URI uri) {
		uri = resolveURI(uri);
		return sources.containsKey(uri);

	}

	/**
	 * Get a Set of the URIs of all sources in this bundle.
	 * 
	 * @return A Set of URIs of sources
	 */
	public Set<URI> getSourceURIs() {
		return new HashSet<URI>(sources.keySet());
	}

	/**
	 * Get the content of the source of specified URI.
	 * 
	 * @param uri
	 *            URI of the source
	 * @return the content of the source or <code>null</code> if the source
	 *         doesn't exist.
	 */
	public String getSourceContent(URI uri) {
		uri = resolveURI(uri);
		return sources.get(uri);

	}

	public void setSourceContent(URI uri, String content) {
		uri = resolveURI(uri);
		if (sources.containsKey(uri)) {
			sources.put(uri, content);
		} else {
			System.err.println("No such URI found in this source bundle: " + uri);
		}
	}

	/**
	 * Get an InputStream of the source content of the specified URI.
	 * 
	 * @param uri
	 *            URI to the source
	 * @return An InputStream to the source content.
	 */
	private InputStream getSourceInputStream(URI uri) {
		String str = getSourceContent(uri);
		if (str == null) {
			return null;
		}
		try {
			InputStream is = new ByteArrayInputStream(str.getBytes("UTF-8"));
			return is;
		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
			return null;
		}
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
	 * Add a source to the bundle, fetches the content by URI specified.
	 * 
	 * @param uri
	 *            the URI of the source
	 * @throws IOException
	 */
	public URI addSource(URI uri) throws IOException {
		return addSource(uri, fetchURIContent(uri));
	}

	/**
	 * Add a source to the bundle with designated content.
	 * 
	 * @param uri
	 *            the URI of the new source. It doesn't have to exists or be
	 *            readable.
	 * @param content
	 *            the designated content of the new source
	 */
	public URI addSource(URI uri, String content) {
		URI key = resolveURI(uri);
		sources.put(key, content);
		return key;
	}
	
	public URI addTreatmentDefinitions(String filename, String content) {
		URI uri = null;
		try {
			uri = new URI(filename);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		// add a script tag to <head> tag in entry point page
		String newScriptTag = "<script src=\"" + filename + "\"></script>";
		String htmlText = getSourceContent(getEntryPointURI());
		Source source = new Source(htmlText);
		OutputDocument output = new OutputDocument(source);
		Element headElement = source.getFirstElement(HTMLElementName.HEAD);
		if (headElement == null) {
			Element htmlElement = source.getFirstElement(HTMLElementName.HTML);
			if (htmlElement == null) {
				System.err.println("No <HTML> tag found. Check validity of entry point page.");
				return null;
			}
			output.replace(htmlElement.getContent(), "<head>" + newScriptTag + "</head>" + htmlElement.getContent().toString());
		} else {
			output.replace(headElement.getContent(), newScriptTag + headElement.getContent().toString());
		}
		// add a new source to source bundle
		return addSource(uri, content);
	}

	private URI resolveURI(URI uri) {
		// convert URI to absolute URI
		return baseURI.resolve(uri.normalize());
	}

	/**
	 * Save all sources in the bundle to a local path, including the HTML page
	 * and all referred JavaScript files. It will try to infer file name from
	 * URI. If failed, file names will be set to "default.html" and
	 * "default-n.js", where n is a unique integer assigned. The inferred path
	 * of the script files will be a sub-directory under output path name
	 * whenever possible, otherwise the full directory structure starting from
	 * host name will be put under output path. For example: If the URI is
	 * http://www.example.com/dir/script.js, the script file will be put under
	 * [output_path]/www.example.com/dir/
	 * 
	 * @param pathName
	 * @return a Set of File objects that has been written
	 * @throws IOException
	 */
	public Set<File> saveSourceBundleTo(String pathName) throws IOException {

		File path = new File(pathName);

		if (!path.isDirectory() || !path.exists()) {
			throw new IOException("Invalid path.");
		}

		// preparing local file names and new URIs pointing to the script files
		Map<URI, File> scriptFiles = new HashMap<URI, File>();
		Map<URI, URI> newScriptURIs = new HashMap<URI, URI>();

		File entryPointFile = new File(path, inferFileFromURI(getEntryPointURI()));
		URI newEntryPointURI = entryPointFile.toURI();
		URI newBaseURI = getBaseURI(newEntryPointURI);

		Set<URI> scriptURIs = getSourceURIs();
		scriptURIs.remove(getEntryPointURI());

		for (URI uri : scriptURIs) {
			File file = new File(path, inferFileFromURI(uri));
			URI newURI = newBaseURI.relativize(file.toURI());
			scriptFiles.put(uri, file);
			newScriptURIs.put(uri, newURI);
		}

		// write the updated HTML page first
		writeFile(entryPointFile, localizeScriptSrcs(newScriptURIs));

		// then write all script files
		for (URI uri : scriptURIs) {
			writeFile(scriptFiles.get(uri), sources.get(uri));
		}

		anonymousSourceCounter = 0;

		Set<File> fileWritten = new HashSet<File>(scriptFiles.values());
		fileWritten.add(entryPointFile);
		return fileWritten;
	}

	/**
	 * Update the HTML page's script tags to refer to saved script files.
	 * 
	 * @param uriMapping
	 *            mapping of original absolute URIs and new URIs pointing to the
	 *            newly written script files.
	 * @return modified HTML page content
	 */
	private String localizeScriptSrcs(Map<URI, URI> uriMapping) {
		// Use Jericho parser to modify HTML page
		String htmlText = getSourceContent(getEntryPointURI());
		Source source = new Source(htmlText);
		OutputDocument output = new OutputDocument(source);
		for (StartTag tag : source.getAllStartTags(HTMLElementName.SCRIPT)) {
			String src = tag.getAttributeValue("src");
			if (src != null) {
				URI oldURI;
				try {
					oldURI = resolveURI(new URI(src));
					URI newURI = uriMapping.get(oldURI);
					output.replace(tag.getAttributes().get("src"), "src=\"" + newURI + "\"");
				} catch (URISyntaxException e) {
					e.printStackTrace();
					System.err.println("Invalid URL syntax: " + src);
				}

			}
		}
		return output.toString();
	}

	private void writeFile(File file, String content) throws IOException {
		file.getParentFile().mkdirs();

		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		try {
			bw.write(content);
		} finally {
			bw.close();
		}
	}

	private String inferFileFromURI(URI uri) {
		String result = "";

		URI reURI = baseURI.relativize(uri);
		// now uri will be relative iff it's under baseURI

		if (reURI.isAbsolute()) {
			if (reURI.getAuthority() == null) {
				result += "localhost/";
			} else {
				result += reURI.getAuthority() + "/";
			}
		}

		String path = reURI.normalize().getPath(); // normalize removes "./"s
		path = path.replaceFirst("^/+", ""); // trim extra "/"s at the beginning
		path = path.replaceFirst("^(?:\\.\\./)+", ""); // trim extra "../../"s

		// take care of file name
		int separatorIndex = path.lastIndexOf("/");
		boolean isEntryPoint = uri.equals(entryPointURI);
		if (separatorIndex == path.length() - 1) {
			// if URI has no file name
			if (isEntryPoint) {
				path += "default.html";
			} else {
				anonymousSourceCounter++;
				path += "default-" + anonymousSourceCounter + ".js";
			}
		} else {
			String rawName = path.substring(separatorIndex + 1);
			// assign proper extension
			if (isEntryPoint) {
				if (!path.endsWith(".html") && !rawName.endsWith(".htm")) {
					path += ".html";
				}
			} else {
				if (!rawName.endsWith(".js")) {
					path += ".js";
				}
			}
		}

		if (!File.separator.equals("/")) {
			// convert to platform specific path
			path.replaceAll("/", File.separator);
		}

		result += path;
		return result;
	}

	/**
	 * Fetch the content at an URI.
	 * 
	 * @param uri
	 *            the URI pointing to the content.
	 * @return the content at the specified URI.
	 * @throws IOException
	 */
	private String fetchURIContent(URI uri) throws IOException {
		uri = baseURI.resolve(uri); // convert to absolute URI
		URL url = uri.toURL();

		InputStream inputStream = url.openStream();
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

	protected class SourceBundleSourceExtractor extends DefaultSourceExtractor {

		protected Set<MappedSourceModule> extractSources(URI entryPointURI, IHtmlParser htmlParser) throws IOException {
			// add entry point to Source Bundle
			URI absoluteURI = addSource(entryPointURI);

			URL entryPointURL = absoluteURI.toURL();
			InputStream inputStreamReader = getSourceInputStream(absoluteURI);
			IGeneratorCallback htmlCallback = new HtmlCallBack(entryPointURL);
			htmlParser.parse(entryPointURL, inputStreamReader, htmlCallback, entryPointURL.getFile());

			SourceRegion finalRegion = new SourceRegion();
			htmlCallback.writeToFinalRegion(finalRegion);

			// writing the final region into one SourceFileModule.
			File outputFile = File.createTempFile(new File(entryPointURL.getFile()).getName(), ".js");
			if (outputFile.exists()) {
				outputFile.delete();
			}
			outputFile.deleteOnExit();

			FileMapping fileMapping = finalRegion.writeToFile(new PrintStream(outputFile));
			MappedSourceModule singleFileModule = new MappedSourceFileModule(outputFile, outputFile.getName(), fileMapping);
			return Collections.singleton(singleFileModule);
		}

		protected class HtmlCallBack extends DefaultSourceExtractor.HtmlCallback {

			public HtmlCallBack(URL entrypointUrl) {
				super(entrypointUrl, new IdentityUrlResolver());
			}

			@Override
			protected void handleScript(ITag tag) {
				Pair<String, Position> value = tag.getAttributeByName("src");

				try {
					if (value != null) {
						// script is out-of-line
						String srcString = value.fst;
						URI srcURI = new URI(srcString);
						URI absoluteURI = addSource(srcURI);
						scriptRegion.println(getSourceContent(absoluteURI), tag.getElementPosition(), absoluteURI.toURL());
					}

				} catch (IOException e) {
					System.err.println("Error reading script file: " + e.getMessage());
				} catch (URISyntaxException e) {
					System.err.println("Invalid URI syntax: " + value.fst);
				}
			}

		}
	}
}
