package edu.upenn.cis.tomato.core;

public class PolicyNode {
	private NodeType type;
	private Object value;
	private PolicyNode left;
	private PolicyNode right;
	
	public PolicyNode(NodeType type) {
		this.type = type;
		this.value = null;
	}
	
	public PolicyNode(NodeType type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public PolicyNode clone() {
		PolicyNode clone = new PolicyNode(type, value);
		if (left != null) clone.left = left.clone();
		if (right != null) clone.right = right.clone();
		return clone;
	}

	public PolicyNode getLeft() {
		return left;
	}

	public void setLeft(PolicyNode left) {
		this.left = left;
	}

	public PolicyNode getRight() {
		return right;
	}

	public void setRight(PolicyNode right) {
		this.right = right;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	public boolean isLeaf() {
		return left == null && right == null;
	}

	public String toString() {
		switch(type) {
		case ROOT:
		case AND:
		case OR:
			return "(" + left.toString() + ") " + type.toString() + " (" + right.toString() + ")";
		case NOT:
			return type.toString() + "(" + left.toString() + ")";
		case TERM:
		case ACTION:
			return value.toString();
		default:
			return "";
		}
	}

	public enum NodeType {
		ROOT	(":", 4),
		AND		("&", 2),
		OR		("|", 3),
		NOT		("!", 1),
		TERM	("T", 0),
		ACTION	("A", 0);
		
		private final String string;
		public final int priority;
		
		NodeType(String string, int priority) {
			this.string = string;
			this.priority = priority;
		}
		
		public String toString() {
			return string;
		}
	}
}