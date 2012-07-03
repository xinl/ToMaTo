package edu.upenn.cis.tomato.core;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.upenn.cis.tomato.core.PolicyNode.NodeType;
import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;

public class Policy {
	
	// some shortcut names
	static final NodeType AND = NodeType.AND;
	static final NodeType OR = NodeType.OR;
	static final NodeType NOT = NodeType.NOT;
	static final NodeType TERM = NodeType.TERM;
	static final NodeType ACTION = NodeType.ACTION;
	
	protected String string;
	protected Set<PolicyTermGroup> terms;
	// A list of groups connected by OR, the groups each contains policy terms connected by AND.
	protected PolicyAction action;
	
	public Policy(String string) throws ParseException {
		// example: ActionType = "invoke" & (CallerName = "alert" | CallerName = "foo.bar") & TimeInvoked > 10 : custom("code")
		this.string = string;
		PolicyParser parser = new PolicyParser(string);
		PolicyNode root = parser.getPolicyTree();
		
		assert(root.getRight().getType() == ACTION);
		this.action = (PolicyAction) root.getRight().getValue();
		
		//normalizePolicyTree(root);
		PolicyNode termsInDNF = getRawDNF(root.getLeft());
		simplifyDNFTree(termsInDNF, null);
		
	}

	private boolean simplifyDNFTree(PolicyNode node, PolicyTermGroup group) {
		if (node == null)
			return false;
		switch (node.getType()) {
		case OR:
			if (node.getLeft().getType() == OR) {
				simplifyDNFTree(node.getLeft(), null);
			} else {
				PolicyTermGroup newGroup = new PolicyTermGroup();
				if (simplifyDNFTree(node.getLeft(), newGroup)) {
					terms.add(newGroup);
					return true;
				}
			}
			if (node.getRight().getType() == OR) {
				simplifyDNFTree(node.getRight(), null);
			} else {
				PolicyTermGroup newGroup = new PolicyTermGroup();
				if (simplifyDNFTree(node.getRight(), newGroup)) {
					terms.add(newGroup);
					return true;
				}
			}
		case AND:
			if (simplifyDNFTree(node.getLeft(), group) == false)
				return false;
			return simplifyDNFTree(node.getRight(), group);

		case NOT:
			PolicyTerm term = (PolicyTerm) node.getLeft().getValue();
			return group.addTerm(term.negate());
			
		case TERM:
			return group.addTerm((PolicyTerm) node.getValue());
		}
		return false;
	}

	private PolicyNode getRawDNF(PolicyNode node) {
		if (node == null) return null;
		
		// One level trees
		if (node.getType() == TERM) {
			PolicyTerm term = (PolicyTerm) node.getValue();
			if (term.comparator == ComparatorType.UNEQUAL || 
				term.comparator == ComparatorType.GREATER_EQUAL_THAN || 
				term.comparator == ComparatorType.LESS_EQUAL_THAN) {
				// put negated term node under a NOT node
				PolicyNode newNode = new PolicyNode(NOT);
				newNode.setLeft(new PolicyNode(TERM, term.negate()));
				return newNode;
			}
		}
		
		// Two levels trees
		assert(node.getLeft() != null);
		switch (node.getType()) {
		case NOT:
			assert(node.getRight() == null);
			if (node.getLeft().getType() == TERM) {
				return node;
			}
			break;
		case OR:
		case AND:
			assert(node.getRight() != null);
			if (node.getLeft().getType() == TERM && node.getRight().getType() == TERM) {
				return node;
			}
			break;
		default:
			assert(false);	
		}
		
		// Three or more levels
		node.setLeft(getRawDNF(node.getLeft()));
		node.setRight(getRawDNF(node.getRight()));
		assert(isDNF(node.getLeft()));
		assert(node.getRight() == null || isDNF(node.getRight()));
		
		// Deal with NOT node
		assert(node.getRight() != null);
		if (node.getType() == NOT) {
			assert(node.getRight() == null);
			switch (node.getLeft().getType()) {
			case NOT:
				// Simplify double negation
				assert(node.getLeft().getRight() == null);
				assert(node.getLeft().getLeft() != null);
				PolicyNode newNode = node.getLeft().getLeft();
				assert(isDNF(newNode));
				return newNode;
			case AND:
			case OR:
				// De Morgan's Law
				PolicyNode childNode = node.getLeft();
				PolicyNode leftNotNode = new PolicyNode(NOT);
				PolicyNode rightNotNode = new PolicyNode(NOT);
				
				leftNotNode.setLeft(childNode.getLeft());
				rightNotNode.setLeft(childNode.getRight());
				
				PolicyNode rootNode = new PolicyNode((node.getLeft().getType() == AND)? OR : AND);
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
			// x can be any kind of node, because the clone will copy all children nodes.
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
				
				return getRawDNF(orNode); // take care of step 2 of expanding (a | b) & (c | d)
			}
		}
		
		return null;
	}
	private void swapChildren(PolicyNode node) {
		PolicyNode temp = node.getLeft();
		node.setLeft(node.getRight());
		node.setRight(temp);
	}

	private boolean isDNF(PolicyNode node) {
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

	public List<PolicyTermGroup> getStaticTerms() {
		List<PolicyTermGroup> staticTerms = new ArrayList<PolicyTermGroup>();
		for (PolicyTermGroup group : terms) {
			if (group.isAllStatic()) {
				staticTerms.add(group);
			}
		}
		return staticTerms;
	}

	public List<PolicyTermGroup> getDynamicTerms() {
		List<PolicyTermGroup> dynamicTerms = new ArrayList<PolicyTermGroup>();
		for (PolicyTermGroup group : terms) {
			if (!group.isAllStatic()) {
				dynamicTerms.add(group);
			}
		}
		return dynamicTerms;
	}

	public class PolicyTermGroup implements Iterable<PolicyTerm> {
		private Set<PolicyTerm> terms = new HashSet<PolicyTerm>();
		private boolean isAllStatic = true;

		public PolicyTermGroup() {

		}

		public boolean addTerm(PolicyTerm term) {
			if (terms.contains(term.negate())) {
				// this group contain a&!a, is not useful anymore.
				return false;
			}
			if (!term.isStatic()) {
				isAllStatic = false;
			}
			terms.add(term);
			return true;
		}

		public boolean isAllStatic() {
			return isAllStatic;
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
		

	}
}
