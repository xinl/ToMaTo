package edu.upenn.cis.tomato.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	protected SourceBundle sourceBundle;
	protected List<Policy> policies;
	protected Comparator<Operation> sortByPosition = new SortByPosition();
	protected TreatmentFactory treatmentFactory = new TreatmentFactory();

	public PolicyEnforcer(SourceBundle sourceBundle, List<Policy> policies) {
		this.sourceBundle = sourceBundle;
		this.policies = policies;
	}

	public void enforce() {
		SortedSet<Operation> operations = getAllOperations();

		if (operations.size() == 0) {
			return;
		}

		// organize operations into a tree based on their nesting relationship
		Operation root = makeOperationTree(operations);

		// carry out the operations
		root.operate();

		// add the definition JS file to the beginning of web page
		sourceBundle.addTreatmentDefinitions(treatmentFactory.BASE_OBJECT_NAME + ".js", treatmentFactory.getDefinitions());
	}

	protected SortedSet<Operation> getAllOperations() {
		// use set to avoid multiple treatments on same site
		SortedSet<Operation> operations = new TreeSet<Operation>(sortByPosition);

		// fill in operations set

		StaticAnalyzer sa = new StaticAnalyzer(sourceBundle);

		for (Policy p : policies) {
			Set<Set<PolicyTerm>> staticTerms = p.getStaticTermGroups();
			Set<Set<PolicyTerm>> dynamicTerms = p.getDynamicTermGroups();

			// prepare suspect lists
			SuspectList staticSuspects = filterSuspectList(sa.getAllSuspects(), staticTerms);
			SuspectList dynamicSuspects = filterSuspectList(sa.getAllSuspects(), dynamicTerms);
			dynamicSuspects.removeAll(staticSuspects); // remove overlapping suspects

			// prepare treatments
			Treatment treatment = treatmentFactory.makeTreatment(p);

			// add to the operation list
			for (Suspect s : staticSuspects) {
				operations.add(new Operation(s.getPosition(), s.getType(), true, treatment));
			}
			for (Suspect s : dynamicSuspects) {
				operations.add(new Operation(s.getPosition(), s.getType(), false, treatment));
			}
		}
		return operations;
	}

	protected Operation makeOperationTree(SortedSet<Operation> operations) {
		Set<Operation> markedForRemoval = new HashSet<Operation>();
		for (Operation op : operations) {
			SourcePosition opPos = op.getPosition();
			SortedSet<Operation> tailSet = operations.tailSet(op);
			Iterator<Operation> tailIter = tailSet.iterator();
			if (tailIter.hasNext()) {
				tailIter.next(); // skip op itself
			}
			while (tailIter.hasNext()) {
				Operation tailOp = tailIter.next();
				SourcePosition tailOpPos = tailOp.getPosition();
				if (tailOpPos.getURL().equals(opPos.getURL()) && tailOpPos.getStartOffset() < opPos.getEndOffset()) {
					// if the next operation has the same URL and is nested.
					op.addChild(tailOp);
					markedForRemoval.add(tailOp);
				} else {
					// no need to look further, there won't be any nesting further down
					break;
				}
			}
		}
		// remove the nested Operations from the highest level
		operations.removeAll(markedForRemoval);

		// put these operations as children of an empty root Operation
		Operation root = new Operation(null, null, false, null);
		for (Operation op : operations) {
			root.addChild(op);
		}
		return root;
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

	public class Operation {
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
				children = new TreeSet<Operation>(sortByPosition);
			}
			children.add(op);
		}

		public int operate() {
			int lengthDiff = 0;

			// carry out children operations first, if any
			if (children != null) {
				for (Operation op : children) {
					int childLengthDiff = op.operate();
					if (childLengthDiff != 0) {
						// add diff to the start offsets of all subsequent sibling operations on the same URL
						SortedSet<Operation> tailSet = children.tailSet(op);
						if (tailSet.size() > 1) {
							Iterator<Operation> iter = tailSet.iterator();
							iter.next(); // skip the child itself
							while (iter.hasNext()) {
								Operation sibling = iter.next();
								if (!sibling.getPosition().getURL().equals(op.getPosition().getURL())) {
									break;
								}
								sibling.getPosition().setStartOffset(sibling.getPosition().getStartOffset() + childLengthDiff);
							}

						}
						// add diff to parent's end offset
						lengthDiff += childLengthDiff;
					}
				}
			}

			if (pos == null || suspectType == null || treatment == null) {
				// this only happens to root operation with empty pos/type/treatment
				return 0;
			}

			// carry out the operation in itself

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
			String resultString = treatment.apply(targetString, suspectType, isStatic);
			sourceBundle.setSourceContent(uri, contentString.substring(0, start) + resultString + contentString.substring(end));

			lengthDiff += resultString.length() - targetString.length();

			pos.setEndOffset(pos.getEndOffset() + lengthDiff);
			return lengthDiff;
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
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
			return result;
		}

		// two Operation are considered the same if their pos's are the same.
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Operation other = (Operation) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			return true;
		}

		private PolicyEnforcer getOuterType() {
			return PolicyEnforcer.this;
		}
	}

	private class SortByPosition implements Comparator<Operation> {

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
