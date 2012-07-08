package edu.upenn.cis.tomato.frontend;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import edu.upenn.cis.tomato.core.Policy;
import edu.upenn.cis.tomato.core.PolicyEnforcer;
import edu.upenn.cis.tomato.core.SourceBundle;

public class CommandLineFrontEnd {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// error checking omitted
		String entryPoint = args[0];
		SourceBundle src;
		try {
			src = new SourceBundle(entryPoint);
		} catch (Exception e) {
			System.out.println("Error reading source files.\n");
			e.printStackTrace();
			return;
		}

		String outputPath = args[1];

		List<Policy> policies = new ArrayList<Policy>();
		for (int i = 2; i < args.length; i++) {
			Policy p;
			try {
				p = new Policy(args[i]);
			} catch (ParseException e) {
				System.out.println("Error parsing policy string.\n");
				e.printStackTrace();
				return;
			}
			policies.add(p);
		}

		PolicyEnforcer.enforce(src, policies);

		try {
			src.saveSourceBundleTo(outputPath);
		} catch (IOException e) {
			System.out.println("Error writing file.\n");
			e.printStackTrace();
			return;
		}
	}

}
