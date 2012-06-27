package edu.upenn.cis.tomato.core;

import java.util.List;
import java.util.Map;

public class PolicyEnforcer {
	/*
	 * Build patch Lists from each Policies, combine them, then apply each patch
	 * to designated positions in sourceBundle in one-pass.
	 */
	static public void enforce(SourceBundle sourceBundle, List<Policy> policies) {
		Map<Position, Treatment> operations;
		// Use Map to avoid multiple operations on same site.
	}

	static private SuspectList makeSuspectList(SourceBundle src, Policy policy) {
		return null;
	}

}
