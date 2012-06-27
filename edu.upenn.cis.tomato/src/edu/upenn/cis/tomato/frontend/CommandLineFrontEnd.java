package edu.upenn.cis.tomato.frontend;

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
		SourceBundle src = new SourceBundle(entryPoint);

		String outputPath = args[1];

		List<Policy> policies = new ArrayList<Policy>();
		for (int i = 2; i < args.length; i++) {
			Policy p = new Policy(args[i]);
			policies.add(p);
		}

		PolicyEnforcer.enforce(src, policies);

		src.saveTo(outputPath);
	}

}
