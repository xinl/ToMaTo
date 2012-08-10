package edu.upenn.cis.tomato.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	public static final Comparator<Operation> SORT_BY_POSITION_AND_SUSPECT_TYPE = new SortByPositionAndSuspectType();

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
	 * @return A Map representing a log of conflicts result in multiple policy
	 *         wants to modify a same suspect site. The Map's keys are the
	 *         contested SourcePositions. The Map's values are lists of the
	 *         contesting policies. The first policy in the list is the winner.
	 */
	public Map<OperationKey, List<Policy>> enforceOn(SourceBundle sourceBundle) {
		Map<OperationKey, List<Policy>> conflicts = new HashMap<OperationKey, List<Policy>>();

		SortedSet<Operation> operations = getAllOperations(sourceBundle, conflicts);

		if (operations.size() == 0) {
			return conflicts;
		}

		// organize operations into a tree based on their nesting relationship
		operations = nestOperations(operations);

		// carry out the operations
		executeOperations(sourceBundle, operations);

		// add the definition JS file to the beginning of web page
		sourceBundle.addTreatmentDefinitions(treatmentFactory.BASE_OBJECT_NAME + ".js", treatmentFactory.getDefinitions());

		return conflicts;
	}

	/**
	 * Compile a flat collection of Operations applicable to the given
	 * SourceBundle. The list is ordered by position.
	 *
	 * @param sourceBundle
	 *            A SourceBundle the Operations will be applied to.
	 * @param conflicts
	 *            The conflict resolution log to write to, in the case when two
	 *            policies want to operate on a same suspect site.
	 * @return A flat sorted collection of Operations customized to the given
	 *         SourceBundle.
	 */
	protected SortedSet<Operation> getAllOperations(SourceBundle sourceBundle, Map<OperationKey, List<Policy>> conflicts) {
		Map<OperationKey, Operation> operations = new HashMap<OperationKey, Operation>();

		StaticAnalyzer sa = new StaticAnalyzer(sourceBundle);

		for (Map.Entry<Policy, Treatment> entry : policiesAndTreatments.entrySet()) {
			Policy policy = entry.getKey();
			Treatment treatment = entry.getValue();
			Set<Set<PolicyTerm>> staticTerms = policy.getStaticTermGroups();
			Set<Set<PolicyTerm>> dynamicTerms = policy.getDynamicTermGroups();

			// prepare suspect lists
			SuspectList staticSuspects = filterSuspectList(sa.getAllSuspects(), staticTerms);
			SuspectList dynamicSuspects = filterSuspectList(sa.getAllSuspects(), dynamicTerms);
			dynamicSuspects.removeAll(staticSuspects); // remove overlapping suspects

			// add to the operation list
			// clone a new SourcePosition to prevent contaminating suspects when we modify offsets later
			for (Suspect s : staticSuspects) {
				Operation op = new Operation(cloneSourcePosition(s.getPosition()), s.getType(), true, treatment);
				addOperationTo(operations, op, conflicts);
			}
			for (Suspect s : dynamicSuspects) {
				Operation op = new Operation(cloneSourcePosition(s.getPosition()), s.getType(), false, treatment);
				addOperationTo(operations, op, conflicts);
			}
		}

		// now there won't be multiple treatments on same site in operations map any more
		SortedSet<Operation> sortedOperations = new TreeSet<Operation>(SORT_BY_POSITION_AND_SUSPECT_TYPE);
		sortedOperations.addAll(operations.values());
		return sortedOperations;
	}

	private void addOperationTo(Map<OperationKey, Operation> ops, Operation op, Map<OperationKey, List<Policy>> conflicts) {
		/*
		 * In case of multiple treatment competing for one source position, the
		 * following code will ensure the one with highest priority prevails.
		 *
		 * Priority is determined as follows: Policy whose ActionType has higher
		 * priority prevails; if two Policy's have the same ActionType, the one
		 * comes later on the policy list prevails.
		 */
		OperationKey opKey = op.getOperationKey();
		if (ops.containsKey(opKey)) {

			Operation existingOp = ops.get(opKey);
			int existingOpPriority = existingOp.getTreatment().getPolicy().getAction().getType().ordinal();
			int newOpPriority = op.getTreatment().getPolicy().getAction().getType().ordinal();
			if (newOpPriority >= existingOpPriority) {
				ops.put(opKey, op);
			}

			// log conflict resolution
			if (conflicts != null) {
				if (conflicts.containsKey(opKey)) {
					if (newOpPriority >= existingOpPriority) {
						conflicts.get(opKey).add(0, op.getTreatment().getPolicy()); // add to beginning
					} else {
						conflicts.get(opKey).add(op.getTreatment().getPolicy()); // add to end
					}
				} else {
					List<Policy> list = new ArrayList<Policy>();
					list.add(existingOp.getTreatment().getPolicy());
					list.add(op.getTreatment().getPolicy());
					conflicts.put(opKey, list);
				}
			}

		} else {
			ops.put(op.getOperationKey(), op);
		}
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
			SortedSet<Operation> toBeNested = new TreeSet<Operation>(SORT_BY_POSITION_AND_SUSPECT_TYPE);
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
	 */
	public static class Operation {
		protected SourcePosition pos;
		protected SuspectType suspectType;
		protected boolean isStatic;
		protected Treatment treatment;
		protected SortedSet<Operation> children;

		public Operation(SourcePosition pos, SuspectType suspectType, boolean isStatic, Treatment treatment) {
			if (pos == null || suspectType == null) {
				throw new IllegalArgumentException("SourcePositon and/or SuspectType can't be null.");
			}
			this.pos = pos;
			this.suspectType = suspectType;
			this.isStatic = isStatic;
			this.treatment = treatment;
		}

		public void addChild(Operation op) {
			if (children == null) {
				children = new TreeSet<Operation>(SORT_BY_POSITION_AND_SUSPECT_TYPE);
			}
			children.add(op);
		}

		public void addChildren(Collection<Operation> ops) {
			if (children == null) {
				children = new TreeSet<Operation>(SORT_BY_POSITION_AND_SUSPECT_TYPE);
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

		public OperationKey getOperationKey() {
			return new OperationKey(pos, suspectType);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (isStatic ? 1231 : 1237);
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
			result = prime * result + ((suspectType == null) ? 0 : suspectType.hashCode());
			result = prime * result + ((treatment == null) ? 0 : treatment.hashCode());
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
			if (isStatic != other.isStatic)
				return false;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			if (suspectType != other.suspectType)
				return false;
			if (treatment == null) {
				if (other.treatment != null)
					return false;
			} else if (!treatment.equals(other.treatment))
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

	public static class OperationKey {
		protected SourcePosition pos;
		protected SuspectType type;

		public OperationKey(SourcePosition pos, SuspectType type) {
			this.pos = pos;
			this.type = type;
		}

		public SourcePosition getPos() {
			return pos;
		}

		public SuspectType getType() {
			return type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			OperationKey other = (OperationKey) obj;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			if (type != other.type)
				return false;
			return true;
		}

	}

	protected static class SortByPositionAndSuspectType implements Comparator<Operation> {

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
			if (result != 0) {
				return result;
			}
			// if two operation has same position, then operation to DATA_WRITE comes before that to DATA_READ
			// this is affected by the ordering in SuspectType enumeration.
			if (a.getSuspectType() == null) {
				System.out.println("A is NULL!" + a);
			}
			result = a.getSuspectType().ordinal() - b.getSuspectType().ordinal();
			return result;
		}

	}
}
