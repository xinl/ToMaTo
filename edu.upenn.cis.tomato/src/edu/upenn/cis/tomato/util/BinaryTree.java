package edu.upenn.cis.tomato.util;

public class BinaryTree<T> {
	protected T value;
	protected BinaryTree<T> left;
	protected BinaryTree<T> right;
	
	public BinaryTree(T value, BinaryTree<T> left, BinaryTree<T> right) {
		this.value = value;
		this.left = left;
		this.right = right;
	}
	
	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public BinaryTree<T> getLeft() {
		return left;
	}

	public void setLeft(BinaryTree<T> left) {
		this.left = left;
	}

	public BinaryTree<T> getRight() {
		return right;
	}

	public void setRight(BinaryTree<T> right) {
		this.right = right;
	}
	
	public boolean isLeaf() {
		return left == null && right == null;
	}
	
	public void visitInorder(BinaryTreeVisitor<T> visitor) {
		if (left != null) left.visitInorder(visitor);
		visitor.visit(this);
		if (right != null) right.visitInorder(visitor);
	}
	
	public void visitPreorder(BinaryTreeVisitor<T> visitor) {
		visitor.visit(this);
		if (left != null) left.visitPreorder(visitor);
		if (right != null) right.visitPreorder(visitor);
	}
	
	public void visitPostorder(BinaryTreeVisitor<T> visitor) {
		if (left != null) left.visitPostorder(visitor);
		if (right != null) right.visitPostorder(visitor);
		visitor.visit(this);
	}
	
	public interface BinaryTreeVisitor<T> {
		public void visit(BinaryTree<T> binaryTree);
	}
	
}
