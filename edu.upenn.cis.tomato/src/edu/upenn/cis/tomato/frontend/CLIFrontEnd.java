package edu.upenn.cis.tomato.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.upenn.cis.tomato.core.Policy;
import edu.upenn.cis.tomato.core.PolicyEnforcer;
import edu.upenn.cis.tomato.core.PolicyEnforcer.OperationKey;
import edu.upenn.cis.tomato.core.SourceBundle;

public class CLIFrontEnd {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Invalid number of arguments.");
			System.out.println("Usage:");
			System.out.println("CommandLineFrontEnd <policy file> <entry point file> <output path>\"");
			System.out.println("<policy file> is a plain text file contains policy strings on separate lines.");
			return;
		}

		// read policies

		List<Policy> policies = new ArrayList<Policy>();
		File policyFile = new File(args[0]);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(policyFile));
			String line;
			try {
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					Policy p;
					try {
						p = new Policy(line);
					} catch (ParseException e) {
						System.out.println("Error parsing policy string: " + e.getMessage() + "\n" + line);
						e.printStackTrace();
						return;
					}
					policies.add(p);
				}
			} catch (IOException e2) {
				System.out.println("Error reading policy file: " + args[0]);
				e2.printStackTrace();
				return;
			}
		} catch (FileNotFoundException e2) {
			System.out.println("Policy file not found: " + args[0]);
			e2.printStackTrace();
			return;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}

		// prepare source bundle

		File entryPoint;
		try {
			entryPoint = new File(args[1]).getCanonicalFile();
		} catch (IOException e1) {
			System.out.println("Invalid entry point files path: " + args[1]);
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

		// enforce policies on source bundle

		PolicyEnforcer pe = new PolicyEnforcer(policies);
		Map<OperationKey, List<Policy>> conflicts = pe.enforceOn(src);

		// generate conflict log
		String conflictLog = "";
		for (Map.Entry<OperationKey, List<Policy>> entry : conflicts.entrySet()) {
			OperationKey opKey = entry.getKey();
			List<Policy> contestants = entry.getValue();
			conflictLog += "Conflict site: " + opKey.getPos() + " of suspect type:" + opKey.getType() + "\n";
			conflictLog += "Prevailing policy: " + contestants.get(0) + "\n";
			conflictLog += "Overriden policies:" + contestants.subList(1, contestants.size()) + "\n";
		}

		if (conflictLog.length() > 0) {
			System.out.println(conflictLog);
		}

		// save modified source bundle to disk

		String outputPath = args[2];

		try {
			src.saveSourceBundleTo(outputPath);
		} catch (IOException e) {
			System.out.println("Error writing file to: ");
			e.printStackTrace();
			return;
		}
	}

}
