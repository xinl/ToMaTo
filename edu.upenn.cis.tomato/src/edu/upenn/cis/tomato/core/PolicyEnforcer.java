package edu.upenn.cis.tomato.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.upenn.cis.tomato.core.TreatmentFactory.Treatment;

public class PolicyEnforcer {
	/*
	 * Build patch Lists from each Policies, combine them, then apply each patch
	 * to designated positions in sourceBundle in one-pass.
	 */
	static public void enforce(SourceBundle sourceBundle, List<Policy> policies) {
		// use Map to avoid multiple operations on same site
		Map<Suspect, Treatment> operations = new HashMap<Suspect, Treatment>();
		
		// fill in operation map
		StaticAnalyzer sa = new StaticAnalyzer(sourceBundle);
		TreatmentFactory tf = new TreatmentFactory();
		for (Policy p : policies) {
			Set<Set<PolicyTerm>> staticTerms = p.getStaticTermGroups();
			Set<Set<PolicyTerm>> dynamicTerms = p.getDynamicTermGroups();
			
			// prepare suspect lists
			SuspectList staticSuspects = filterSuspectList(sa.getAllSuspects(), staticTerms);
			SuspectList dynamicSuspects = filterSuspectList(sa.getAllSuspects(), dynamicTerms);
			dynamicSuspects.removeAll(staticSuspects); // remove overlapping suspects
			
			// prepare treatments
			Treatment treatment = tf.makeTreatment(p);
			
			// add to the operation list
			for (Suspect s : staticSuspects) {
				operations.put(s, treatment);
			}
			for (Suspect s : dynamicSuspects) {
				operations.put(s, treatment);
			}
		}
		
		// TODO: organize operations into a tree based on nesting relationship
		
		// TODO: carry out the operations
		
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
