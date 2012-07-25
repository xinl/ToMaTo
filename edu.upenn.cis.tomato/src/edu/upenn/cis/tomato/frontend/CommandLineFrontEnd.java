package edu.upenn.cis.tomato.frontend;

import java.io.File;
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
		if (args.length < 3) {
			System.out.println("Usage:");
			System.out.println("CommandLineFrontEnd <entry point file> <output path> \"<policy string>\"");
			System.out.println("Multiple \"<policy string>\"'s are allowed.");
			return;
		}

		File entryPoint;
		try {
			entryPoint = new File(args[0]).getCanonicalFile();
		} catch (IOException e1) {
			System.out.println("Invalid entry point files path: " + args[0]);
			e1.printStackTrace();
			return;
		}
		SourceBundle src;
		try {
			src = new SourceBundle(entryPoint.toURI());
		} catch (Exception e) {
			System.out.println("Error reading source files: " + e.getMessage());
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
				System.out.println("Error parsing policy string: " + e.getMessage());
				e.printStackTrace();
				return;
			}
			policies.add(p);
		}

		PolicyEnforcer pe = new PolicyEnforcer(policies);
		pe.enforceOn(src);

		try {
			src.saveSourceBundleTo(outputPath);
		} catch (IOException e) {
			System.out.println("Error writing file to: ");
			e.printStackTrace();
			return;
		}
	}

}
