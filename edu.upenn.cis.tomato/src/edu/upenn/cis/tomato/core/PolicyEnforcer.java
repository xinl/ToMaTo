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

/**
 * An enforcer to modify SourceBundles according to the rules set in a
 * collection of Policy's.
 *
 * @author Xin Li
 * @version July 25, 2012
 *
 */
public class PolicyEnforcer {
	public static final Comparator<Operation> SORT_BY_POSITION = new SortByPosition();

	protected Map<Policy, Treatment> policiesAndTreatments = new HashMap<Policy, Treatment>();
	protected TreatmentFactory treatmentFactory = new TreatmentFactory();

	public PolicyEnforcer(Collection<Policy> policies) {
		for (Policy pol : policies) {
			if (!policiesAndTreatments.containsKey(pol)) { // skip duplicates
				policiesAndTreatments.put(pol, treatmentFactory.makeTreatment(pol));
			}
		}
	}

	/**
	 * Enforce the Policy's in this PolicyEnforcer on a SourceBundle. If a
	 * Policy is applicable to the given SourceBundle, the content of the
	 * SourceBundle will be changed according to the action designated by the
	 * Policy.
	 *
	 * @param sourceBundle
	 *            SourceBundle to be enforced upon.
	 */
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

	/**
	 * Compile a flat list of Operations applicable to the given SourceBundle.
	 * The list is ordered by position.
	 *
	 * @param sourceBundle
	 *            A SourceBundle the Operations will be applied to.
	 * @return A flat list of Operations customized to the given SourceBundle.
	 */
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
			// clone a new SourcePosition to prevent contaminating suspects when we modify offsets later
			for (Suspect s : staticSuspects) {
				operations.add(new Operation(cloneSourcePosition(s.getPosition()), s.getType(), true, t));
			}
			for (Suspect s : dynamicSuspects) {
				operations.add(new Operation(cloneSourcePosition(s.getPosition()), s.getType(), false, t));
			}
		}
		return operations;
	}

	private SourcePosition cloneSourcePosition(SourcePosition pos) {
		return new SourcePosition(pos.getURL(), pos.getStartOffset(), pos.getEndOffset());
	}

	/**
	 * Rearrange the operations with nesting position ranges (start offset - end
	 * offset) into a tree.
	 *
	 * @param operations
	 *            A flat list of Operations ordered by their position.
	 * @return The converted Operation list.
	 */
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

	/**
	 * Execute the nested operations on a SourceBundle. Note: the set of
	 * Operation must be nested by calling nestedOperations() before execution,
	 * or else they may not be executed properly.
	 *
	 * @param sourceBundle
	 *            The SourceBundle to be operated on.
	 * @param operations
	 *            A set of properly nested Operations to be executed.
	 * @return The total difference in length (the lengths from start offsets to
	 *         corresponding end offsets) after execution. This is used to
	 *         recursively update the parent and sibling offsets.
	 */
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

	/**
	 * Filter a SuspectList with static terms in a set of term group sets, which
	 * represents the DNF logic of a Policy's terms.
	 *
	 * @param baseList
	 *            The SuspectList to be filtered.
	 * @param termGroups
	 *            A Set of term group Sets to filter the SuspectList with.
	 * @return The filtered SuspectList.
	 */
	protected SuspectList filterSuspectList(SuspectList baseList, Set<Set<PolicyTerm>> termGroups) {
		SuspectList suspects = new SuspectList();
		//TODO: avoid repeating the same filtering by caching filtering result?
		for (Set<PolicyTerm> termGroup : termGroups) {
			SuspectList sl = new SuspectList(baseList);
			for (PolicyTerm term : termGroup) {
				if (term.isStatic()) {
					// ignore dynamic terms when filtering
					sl.filter(term);
					if (sl.size() == 0) {
						// Empty set's union with any other set will be empty.
						break;
					}
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

	/**
	 * An operation to modify a segment of a source file.
	 *
	 * It contains information about the target segment's position, the
	 * Treatment to apply on such segment, and how should the treatment be
	 * applied (depends on what type the corresponding Suspect is and whether
	 * it's static).
	 *
	 * An Operation can have children Operations, which represents nested
	 * relationship of the target segments.
	 *
	 * The equality of two Operations depends only on the position field. When
	 * combined with SortedSet, this can prevent a same position be operated
	 * upon twice.
	 *
	 */
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

		public SuspectType getSuspectType() {
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
