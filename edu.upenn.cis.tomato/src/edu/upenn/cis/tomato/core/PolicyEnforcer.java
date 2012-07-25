package edu.upenn.cis.tomato.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.upenn.cis.tomato.core.Suspect.SuspectType;
import edu.upenn.cis.tomato.core.TreatmentFactory.Treatment;

public class PolicyEnforcer {
	/*
	 * Build patch Lists from each Policies, combine them, then apply each patch
	 * to designated positions in sourceBundle in one-pass.
	 */
	protected Map<Policy, Treatment> policiesAndTreatments = new HashMap<Policy, Treatment>();
	public static final Comparator<Operation> SORT_BY_POSITION = new SortByPosition();
	protected TreatmentFactory treatmentFactory = new TreatmentFactory();

	public PolicyEnforcer(Collection<Policy> policies) {
		for (Policy pol : policies) {
			if (!policiesAndTreatments.containsKey(pol)) { // skip duplicates
				policiesAndTreatments.put(pol, treatmentFactory.makeTreatment(pol));
			}
		}
	}

	public void enforceOn(SourceBundle sourceBundle) {
		SortedSet<Operation> operations = getAllOperations(sourceBundle);

		if (operations.size() == 0) {
			return;
		}

		// organize operations into a tree based on their nesting relationship
		operations = nestOperations(operations);

		// carry out the operations
		executeOperations(sourceBundle, operations);

		// add the definition JS file to the beginning of web page
		sourceBundle.addTreatmentDefinitions(treatmentFactory.BASE_OBJECT_NAME + ".js", treatmentFactory.getDefinitions());
	}

	protected SortedSet<Operation> getAllOperations(SourceBundle sourceBundle) {
		// use set to avoid multiple treatments on same site
		SortedSet<Operation> operations = new TreeSet<Operation>(SORT_BY_POSITION);

		// fill in operations set

		StaticAnalyzer sa = new StaticAnalyzer(sourceBundle);

		for (Map.Entry<Policy, Treatment> entry : policiesAndTreatments.entrySet()) {
			Policy p = entry.getKey();
			Treatment t = entry.getValue();
			Set<Set<PolicyTerm>> staticTerms = p.getStaticTermGroups();
			Set<Set<PolicyTerm>> dynamicTerms = p.getDynamicTermGroups();

			// prepare suspect lists
			SuspectList staticSuspects = filterSuspectList(sa.getAllSuspects(), staticTerms);
			SuspectList dynamicSuspects = filterSuspectList(sa.getAllSuspects(), dynamicTerms);
			dynamicSuspects.removeAll(staticSuspects); // remove overlapping suspects

			// add to the operation list
			for (Suspect s : staticSuspects) {
				operations.add(new Operation(s.getPosition(), s.getType(), true, t));
			}
			for (Suspect s : dynamicSuspects) {
				operations.add(new Operation(s.getPosition(), s.getType(), false, t));
			}
		}
		return operations;
	}

	protected SortedSet<Operation> nestOperations(SortedSet<Operation> operations) {
		Set<Operation> markedForRemoval = new HashSet<Operation>();
		for (Operation op : operations) {
			SortedSet<Operation> toBeNested = new TreeSet<Operation>(SORT_BY_POSITION);
			if (toBeNested.contains(op)) {
				continue;
			}
			SourcePosition opPos = op.getPosition();
			SortedSet<Operation> tailSet = operations.tailSet(op);
			Iterator<Operation> tailIter = tailSet.iterator();
			tailIter.next(); // skip op itself
			while (tailIter.hasNext()) {
				Operation tailOp = tailIter.next();
				SourcePosition tailOpPos = tailOp.getPosition();
				if (tailOpPos.getURL().equals(opPos.getURL()) && tailOpPos.getStartOffset() < opPos.getEndOffset()) {
					// if the next operation has the same URL and should be nested.
					toBeNested.add(tailOp);
				} else {
					// no need to look further, there won't be any nesting further down
					break;
				}
			}
			if (toBeNested.size() > 0) {
				op.addChildren(nestOperations(toBeNested));
			}
			markedForRemoval.addAll(toBeNested);
		}
		// remove the nested Operations from the this level
		operations.removeAll(markedForRemoval);
		return operations;
	}

	protected int executeOperations(SourceBundle sourceBundle, SortedSet<Operation> operations) {
		int totalLengthDiff = 0;

		for (Operation op : operations) {
			int opLengthDiff = 0;
			// carry out children operations first
			if (op.children != null) {
				opLengthDiff += executeOperations(sourceBundle, op.children);
				// add diff to the end offsets of this operation
				if (opLengthDiff != 0) {
					op.getPosition().setEndOffset(op.getPosition().getEndOffset() + opLengthDiff);
				}
			}

			// carry out the operation itself
			SourcePosition pos = op.getPosition();
			int start = pos.getStartOffset();
			int end = pos.getEndOffset();
			URI uri = null;
			try {
				uri = pos.getURL().toURI();
			} catch (URISyntaxException e) {
				System.err.println("Invalid URI Syntax.");
				e.printStackTrace();
				return 0;
			}

			String contentString = sourceBundle.getSourceContent(uri);
			String targetString = contentString.substring(start, end);
			String resultString = op.treatment.apply(targetString, op.suspectType, op.isStatic);
			sourceBundle.setSourceContent(uri, contentString.substring(0, start) + resultString + contentString.substring(end));

			opLengthDiff += resultString.length() - targetString.length();

			if (opLengthDiff != 0) {
				// add diff to the start and end offsets of all subsequent sibling operations on the same URL
				SortedSet<Operation> tailSet = operations.tailSet(op);
				Iterator<Operation> iter = tailSet.iterator();
				iter.next(); // skip the child itself
				while (iter.hasNext()) {
					Operation sibling = iter.next();
					if (!sibling.getPosition().getURL().equals(op.getPosition().getURL())) {
						break;
					}
					sibling.getPosition().setStartOffset(sibling.getPosition().getStartOffset() + opLengthDiff);
					sibling.getPosition().setEndOffset(sibling.getPosition().getEndOffset() + opLengthDiff);
				}
			}
			totalLengthDiff += opLengthDiff;
		}
		return totalLengthDiff;
	}

	protected SuspectList filterSuspectList(SuspectList baseList, Set<Set<PolicyTerm>> termGroups) {
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

	public Set<Policy> getPolicies() {
		return policiesAndTreatments.keySet();
	}

	public TreatmentFactory getTreatmentFactory() {
		return treatmentFactory;
	}

	public static class Operation {
		protected SourcePosition pos;
		protected SuspectType suspectType;
		protected boolean isStatic;
		protected Treatment treatment;
		protected SortedSet<Operation> children;

		public Operation(SourcePosition pos, SuspectType suspectType, boolean isStatic, Treatment treatment) {
			this.pos = pos;
			this.suspectType = suspectType;
			this.isStatic = isStatic;
			this.treatment = treatment;
		}

		public void addChild(Operation op) {
			if (children == null) {
				children = new TreeSet<Operation>(SORT_BY_POSITION);
			}
			children.add(op);
		}

		public void addChildren(Collection<Operation> ops) {
			if (children == null) {
				children = new TreeSet<Operation>(SORT_BY_POSITION);
			}
			children.addAll(ops);
		}

		public SortedSet<Operation> getChildren() {
			return children;
		}

		public SourcePosition getPosition() {
			return pos;
		}

		public SuspectType getType() {
			return suspectType;
		}

		public boolean isStatic() {
			return isStatic;
		}

		public Treatment getTreatment() {
			return treatment;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Operation other = (Operation) obj;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return toString("");
		}

		public String toString(String indent) {
			String str = indent + "Operation [pos=" + pos + ", suspectType=" + suspectType + ", isStatic=" + isStatic + ", treatment=" + treatment + "]";
			if (children != null) {
				indent += "\t";
				str += " has children:";
				for (Operation op : children) {
					str += "\n" + op.toString(indent);
				}
			}
			return str;
		}
	}

	protected static class SortByPosition implements Comparator<Operation> {

		@Override
		public int compare(Operation a, Operation b) {
			SourcePosition aPos = a.getPosition();
			SourcePosition bPos = b.getPosition();
			int result = aPos.getURLString().compareTo(bPos.getURLString());
			if (result != 0) {
				return result;
			}
			result = aPos.getStartOffset() - bPos.getStartOffset();
			if (result != 0) {
				return result;
			}
			// Operations with larger end offset comes first, so that ones with larger offset range comes first
			// this guarantee the nested child Operation will appear later than its parent.
			result = bPos.getEndOffset() - aPos.getEndOffset();
			return result;
		}

	}
}
