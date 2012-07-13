package edu.upenn.cis.tomato.core;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.upenn.cis.tomato.core.PolicyNode.NodeType;
import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;

/**
 * A policy represented in disjunctive normal form.
 * 
 * @author Xin Li
 * @version July 10, 2012
 */
public class Policy {

	// some shortcut names
	static final NodeType AND = NodeType.AND;
	static final NodeType OR = NodeType.OR;
	static final NodeType NOT = NodeType.NOT;
	static final NodeType TERM = NodeType.TERM;
	static final NodeType ACTION = NodeType.ACTION;

	protected String string;
	protected Set<PolicyTermGroup> terms = new HashSet<PolicyTermGroup>();
	// A list of groups connected by OR, the groups each contains policy terms connected by AND.
	protected PolicyAction action;

	public Policy(String string) throws ParseException {
		// example: ActionType = "invoke" & (CallerName = "alert" | CallerName = "foo.bar") & TimeInvoked > 10 : custom("code")
		this.string = string;
		PolicyParser parser = new PolicyParser(string);
		PolicyNode root = parser.getPolicyTree();

		assert (root.getRight().getType() == ACTION);
		this.action = (PolicyAction) root.getRight().getValue();

		PolicyNode termsInDNF = getRawDNF(root.getLeft());
		assert (isDNF(termsInDNF));
		fillTermGroupSet(termsInDNF, terms);
	}

	/**
	 * Put all useful TermGroups from a DNF PolicyNode tree into a Set.
	 * 
	 * The TermGroups within the set is considered to be connected by logic OR.
	 * 
	 * The Set will ensure no duplicate groups exist.
	 * 
	 * @param node
	 *            The root of a PolicyNode tree in disjunctive normal form.
	 * @param groupSet
	 *            The set to be filled in.
	 */
	private void fillTermGroupSet(PolicyNode node, Set<PolicyTermGroup> groupSet) {
		if (node.getType() == OR) {
			fillTermGroupSet(node.getLeft(), groupSet);
			fillTermGroupSet(node.getRight(), groupSet);
		} else {
			PolicyTermGroup newGroup = new PolicyTermGroup();
			if (fillTermGroup(node, newGroup) == true) {
				groupSet.add(newGroup);
			}
		}
	}

	/**
	 * Put all PolicyTerms from the DNF PolicyNode tree into a PolicyTermGroup
	 * and return the usefulness of the Group.
	 * 
	 * The terms within the group is considered to be connected by logic AND.
	 * 
	 * All logic NOTs are internalized into the PolicyTerm by negating its
	 * comparator.
	 * 
	 * A group is consider useless if it contains a pair of mutually negative
	 * terms (e.g.: a&!a).
	 * 
	 * @param node
	 *            The root of a tree that represents an OR-connected group in a
	 *            DNF tree.
	 * @param group
	 *            The group to put terms in.
	 * @return True if this group is useful, otherwise, false.
	 */
	private boolean fillTermGroup(PolicyNode node, PolicyTermGroup group) {
		switch (node.getType()) {
		case AND:
			return fillTermGroup(node.getLeft(), group) && fillTermGroup(node.getRight(), group);
		case NOT:
			PolicyTerm childTerm = (PolicyTerm) node.getLeft().getValue();
			return fillTermGroup(new PolicyNode(TERM, childTerm.negate()), group);
		case TERM:
			PolicyTerm term = (PolicyTerm) node.getValue();
			if (group.hasTerm(term.negate())) {
				// This group contain a&!a. It's not useful anymore.
				return false;
			} else {
				group.addTerm(term);
				return true;
			}
		}
		// Group is not useful if it contains nodes of invalid types.
		return false;
	}

	/**
	 * Convert a PolicyNode tree into disjunctive normal form. The converted
	 * tree may contain logic that can be further simplified.
	 * 
	 * @param node
	 *            The root of the PolicyNode tree to convert.
	 * @return The root of the converted tree.
	 */
	public static PolicyNode getRawDNF(PolicyNode node) {
		if (node == null)
			return null;

		// One level trees
		if (node.getType() == TERM) {
			PolicyTerm term = (PolicyTerm) node.getValue();
			if (term.comparator == ComparatorType.UNEQUAL || term.comparator == ComparatorType.GREATER_EQUAL_THAN
					|| term.comparator == ComparatorType.LESS_EQUAL_THAN) {
				// put negated term node under a NOT node
				PolicyNode newNode = new PolicyNode(NOT);
				newNode.setLeft(new PolicyNode(TERM, term.negate()));
				return newNode;
			} else {
				return node;
			}
		}

		// Two levels trees
		assert (node.getLeft() != null);
		switch (node.getType()) {
		case NOT:
			assert (node.getRight() == null);
			if (node.getLeft().getType() == TERM) {
				return node;
			}
			break;
		case OR:
		case AND:
			assert (node.getRight() != null);
			if (node.getLeft().getType() == TERM && node.getRight().getType() == TERM) {
				return node;
			}
			break;
		default:
			assert (false);
		}

		// Three or more levels
		node.setLeft(getRawDNF(node.getLeft()));
		node.setRight(getRawDNF(node.getRight()));
		assert (isDNF(node.getLeft()));
		assert (node.getRight() == null || isDNF(node.getRight()));

		// Deal with NOT node
		assert (node.getRight() != null);
		if (node.getType() == NOT) {
			assert (node.getRight() == null);
			switch (node.getLeft().getType()) {
			case NOT:
				// Simplify double negation
				assert (node.getLeft().getRight() == null);
				assert (node.getLeft().getLeft() != null);
				PolicyNode newNode = node.getLeft().getLeft();
				assert (isDNF(newNode));
				return newNode;
			case AND:
			case OR:
				// De Morgan's Law
				PolicyNode childNode = node.getLeft();
				PolicyNode leftNotNode = new PolicyNode(NOT);
				PolicyNode rightNotNode = new PolicyNode(NOT);

				leftNotNode.setLeft(childNode.getLeft());
				rightNotNode.setLeft(childNode.getRight());

				PolicyNode rootNode = new PolicyNode((node.getLeft().getType() == AND) ? OR : AND);
				rootNode.setLeft(leftNotNode);
				rootNode.setRight(rightNotNode);

				return getRawDNF(rootNode);
			}

		}

		// Normalization
		// put smaller priority nodes as left as possible
		// TERM < NOT < AND < OR
		if (node.getLeft().getType().priority > node.getRight().getType().priority) {
			swapChildren(node);
		}

		if (node.getType() == OR) {
			return node;
		}

		if (node.getType() == AND) {
			// x & (a | b) ==> (x & a) | (x & b)
			// x can be any kind of node, including AND, because the clone will copy all children nodes.
			if (node.getRight().getType() == OR) {
				PolicyNode andNode = node;
				PolicyNode x = node.getLeft();
				PolicyNode orNode = node.getRight();
				PolicyNode a = node.getRight().getLeft();
				PolicyNode b = node.getRight().getRight();

				andNode.setRight(a);

				PolicyNode xClone = x.clone(); // Doesn't matter if PolicyTerm (value) is deeply cloned or not.
				PolicyNode newAndNode = new PolicyNode(AND);
				newAndNode.setLeft(xClone);
				newAndNode.setRight(b);

				orNode.setLeft(andNode);
				orNode.setRight(newAndNode);

				return getRawDNF(orNode); // Recursion take care of step 2 of expanding (a | b) & (c | d)
			} else {
				return node;
			}
		}
		assert (false); //we should never reach here.
		return null;
	}

	static private void swapChildren(PolicyNode node) {
		PolicyNode temp = node.getLeft();
		node.setLeft(node.getRight());
		node.setRight(temp);
	}

	static public boolean isDNF(PolicyNode node) {
		switch (node.getType()) {
		case TERM:
			return node.getLeft() == null && node.getRight() == null;
		case NOT:
			if (node.getLeft() == null || node.getRight() != null || !isDNF(node.getLeft())) {
				return false;
			}
			return (node.getLeft().getType() == TERM);
		case AND:
			if (node.getLeft() == null || node.getRight() == null || !isDNF(node.getLeft()) || !isDNF(node.getRight())) {
				return false;
			}
			return node.getLeft().getType() != OR && node.getRight().getType() != OR;
		case OR:
			if (node.getLeft() == null || node.getRight() == null || !isDNF(node.getLeft()) || !isDNF(node.getRight())) {
				return false;
			}
			return true;
		}
		return false;
	}

	public String toString() {
		return string;
	}

	public Set<Set<PolicyTerm>> getStaticTermGroups() {
		Set<Set<PolicyTerm>> staticTerms = new HashSet<Set<PolicyTerm>>();
		for (PolicyTermGroup group : terms) {
			if (group.isAllStatic()) {
				staticTerms.add(group.getTerms());
			}
		}
		return staticTerms;
	}

	public Set<Set<PolicyTerm>> getDynamicTermGroups() {
		Set<Set<PolicyTerm>> dynamicTerms = new HashSet<Set<PolicyTerm>>();
		for (PolicyTermGroup group : terms) {
			if (!group.isAllStatic()) {
				dynamicTerms.add(group.getTerms());
			}
		}
		return dynamicTerms;
	}
	
	public Set<Set<PolicyTerm>> getAllTermGroups() {
		Set<Set<PolicyTerm>> allTerms = new HashSet<Set<PolicyTerm>>();
		for (PolicyTermGroup group : terms) {
			allTerms.add(group.getTerms());
		}
		return allTerms;
	}
	
	public PolicyAction getAction() {
		return action;
	}

	/**
	 * A group of PolicyTerms connected by logic AND.
	 * 
	 */
	public class PolicyTermGroup implements Iterable<PolicyTerm> {
		private Set<PolicyTerm> terms = new HashSet<PolicyTerm>();
		private boolean isAllStatic = true;

		public PolicyTermGroup() {

		}

		public boolean hasTerm(PolicyTerm term) {
			return terms.contains(term);
		}

		public void addTerm(PolicyTerm term) {
			if (!term.isStatic()) {
				isAllStatic = false;
			}
			terms.add(term);
		}

		public boolean isAllStatic() {
			return isAllStatic;
		}
		
		public Set<PolicyTerm> getTerms() {
			return terms;
		}

		@Override
		public Iterator<PolicyTerm> iterator() {
			return terms.iterator();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (isAllStatic ? 1231 : 1237);
			result = prime * result + ((terms == null) ? 0 : terms.hashCode());
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
			PolicyTermGroup other = (PolicyTermGroup) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (isAllStatic != other.isAllStatic)
				return false;
			if (terms == null) {
				if (other.terms != null)
					return false;
			} else if (!terms.equals(other.terms))
				return false;
			return true;
		}

		private Policy getOuterType() {
			return Policy.this;
		}

		@Override
		public String toString() {
			return terms.toString();

		}

	}
}
