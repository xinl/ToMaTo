package edu.upenn.cis.tomato.experiment;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class URIOperations {

	/**
	 * @param args
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws URISyntaxException, MalformedURLException, IOException {
		
		URI uri1 = new URI("http://www.example.com/path/to/file.html?foo=2");
		URI uri2 = new URI("file://" + new File("").getAbsolutePath().replace("\\", "/") + "/dat/./test/alias/Alias_Function.html");
		URI uri3 = new URI("http://www.example.com/path/to/");
		URI uri4 = new URI("http://www.example.com/path/to/");
		System.out.println(uri3.normalize());
		//uri3.toURL().openStream();
		System.out.println(uri4.relativize(uri1));
		URI uri5 = uri4.relativize(uri4.resolve(uri3));
		String path = uri5.normalize().getPath(); // normalize removes "./"s
		path = path.replaceFirst("^/+", ""); // trim extra "/"s
		path = path.replaceFirst("^/(\\.\\./)+", ""); // trim "/../../../"s
		System.out.println(uri2.getAuthority());
		System.out.println("".substring(0, 0));
		//System.out.println(uri2.getPath());
	}

}
