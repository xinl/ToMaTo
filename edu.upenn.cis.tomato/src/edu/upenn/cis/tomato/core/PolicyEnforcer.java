package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PolicyEnforcer {
	/*
	 * Build patch Lists from each Policies, combine them, then apply each patch
	 * to designated positions in sourceBundle in one-pass.
	 */
	static public void enforce(SourceBundle sourceBundle, List<Policy> policies) {
		// use Map to avoid multiple operations on same site
		Map<SourcePosition, Treatment> operations = new HashMap<SourcePosition, Treatment>();
		
		// fill in operation map
		StaticAnalyzer sa = new StaticAnalyzer(sourceBundle);
		for (Policy p : policies) {
			Set<Set<PolicyTerm>> staticTerms = p.getStaticTermGroups();
			Set<Set<PolicyTerm>> dynamicTerms = p.getDynamicTermGroups();
			
			// prepare suspect lists
			SuspectList staticSuspects = filterSuspectList(sa.getAllSuspects(), staticTerms);
			SuspectList dynamicSuspects = filterSuspectList(sa.getAllSuspects(), dynamicTerms);
			dynamicSuspects.removeAll(staticSuspects); // remove overlapping suspects
			
			// prepare treatments
			PolicyAction action = p.getAction();
			
			// add to the operation list
			for (Suspect s : staticSuspects) {
				// TODO: put position-treatment pairs into operation list
			}
			for (Suspect s : dynamicSuspects) {
				// TODO: put position-treatment pairs into operation list
			}
		}
		
		// carry out the operations
		Set<String> definitions = new HashSet<String>();
		for (Map.Entry<SourcePosition, Treatment> op : operations.entrySet()) {
			SourcePosition sp = op.getKey();
			Treatment t = op.getValue();
			// TODO: drop in the treatment at position and add to definitions
		}
		// TODO: add the definitions to the beginning of web page
	}

	static private SuspectList filterSuspectList(SuspectList baseList, Set<Set<PolicyTerm>> termGroups) {
		SuspectList suspects = new SuspectList();
		//TODO: avoid repeating the same filtering by caching filtering result?
		for (Set<PolicyTerm> termGroup : termGroups) {
			SuspectList sl = new SuspectList(baseList);
			for (PolicyTerm term : termGroup) {
				if (term.isStatic()) {
					// ignore dynamic terms when filtering
					sl.filter(term);
				}
			}
			suspects.addAll(sl);
		}
		return suspects;
	}

}
