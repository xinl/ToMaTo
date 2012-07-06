package edu.upenn.cis.tomato.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.tomato.core.SourceBundle;

public class SourceBundleTest {
	String prefix = new File("").getAbsolutePath().replace("\\", "/") + "/dat/test/../test/basic/";
	String htmlString = prefix + "Basic.html";
	String scriptString = prefix + "ExternalScript.js";
	URI htmlURI;
	URI scriptURI;
	SourceBundle sb;

	@Before
	public void setUp() throws Exception {
		htmlURI = new File(htmlString).toURI().normalize();
		scriptURI = new File(scriptString).toURI().normalize();
		try {
			sb = new SourceBundle(htmlURI);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed because of IOException!");
		}
	}

	@Test
	public void testSourceBundle() {
		assertTrue(sb != null);
	}

	@Test
	public void testHasSource() {
		// System.out.println(sb.getSourceURIs());
		assertTrue(sb.hasSource(htmlURI));
		assertTrue(sb.hasSource(scriptURI));
	}

	@Test
	public void testGetSource() {
		File scriptFile = new File(scriptString);
		String content = readFile(scriptFile);

		assertEquals(content, sb.getSourceContent(scriptURI));
	}

	@Test
	public void testGetSourceModules() {
		// System.out.println(sb.getSourceModules());
		assertTrue(sb.getSourceModules() != null);
	}

	@Test
	public void testAddSource() throws IOException, URISyntaxException {

		// add with URI only
		File extraFile = new File(prefix + "extra/ExtraScript.js");
		URI extraURI = extraFile.toURI();
		sb.addSource(extraURI);
		assertTrue(sb.hasSource(extraURI));
		assertEquals(readFile(extraFile), sb.getSourceContent(extraURI));

		// add with content
		URI fakeURI = new URI("http://www.example.com/test.js");
		String content = "Lorem ipsum dolor sit amet.";
		sb.addSource(fakeURI, content);
		assertTrue(sb.hasSource(fakeURI));
		assertEquals(content, sb.getSourceContent(fakeURI));
	}

	@Test
	public void testSaveSourceBundleTo() throws IOException, URISyntaxException {
		String tmp = System.getProperty("java.io.tmpdir");
		// System.out.println(tmp);
		// System.out.println(sb.getEntryPointURI());
		// System.out.println(sb.getSourceContent(sb.getEntryPointURI()));
		
		URI extURI = new URI("http://www.example.com///../../js/");
		String extContent = "function fooo(bar) { return bar; }\n";
		sb.addSource(extURI, extContent);
		
		Set<File> files = sb.saveSourceBundleTo(tmp);
		
		File scriptFile = new File(scriptString);
		String scriptExpect = readFile(scriptFile);
		
		File originalHTMLFile = new File(htmlString);
		String htmlOriginal = readFile(originalHTMLFile);
		
		
		for (File file : files) {
			assertTrue(file.exists());
			if (file.getName().equals("ExternalScript.js")) {
				String scriptResult = readFile(file);
				assertEquals(scriptExpect, scriptResult);
			} else if (file.getName().equals("Basic.html")) {
				String htmlResult = readFile(file);
				assertEquals("././", sillyDiff(htmlOriginal, htmlResult));
			} else if (file.getName().equals("common.js")) {
				assertEquals(extContent, readFile(file));
			}
			file.delete();
			//System.out.println(file);
		}

	}

	private String readFile(File file) {
		String content = "";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				content += line + "\n";
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return content;
	}

	private String sillyDiff(String a, String b) {
		int diffStart = 0;
		int diffEnd = Math.max(a.length(), b.length());
		for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
			if (a.charAt(i) != b.charAt(i)) {
				diffStart = i;
				break;
			}
		}
		for (int i = 1; i < Math.min(a.length(), b.length()); i++) {
			if (a.charAt(a.length() - i) != b.charAt(b.length() - i)) {
				diffEnd = i - 1;
				break;
			}
		}
		if (a.length() > b.length()) {
			return a.substring(diffStart, a.length() - diffEnd);
		} else {
			return b.substring(diffStart, b.length() - diffEnd);
		}
	}

}
