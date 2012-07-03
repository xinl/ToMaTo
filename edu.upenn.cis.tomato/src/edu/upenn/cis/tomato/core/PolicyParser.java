package edu.upenn.cis.tomato.core;

import java.text.ParseException;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.upenn.cis.tomato.core.PolicyNode.NodeType;
import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;

public class PolicyParser {
	protected String input;
	protected PolicyNode root;
	protected Stack<PolicyNode> stack = new Stack<PolicyNode>();
	protected int cursor;
	protected Map<TokenType, Matcher> matchers;
	
	public PolicyParser(String input) {
		this.input = input;
		for (TokenType type : TokenType.values()) {
			matchers.put(type, type.getPattern().matcher(input));
		}
	}
	
	public PolicyNode getPolicyTree() throws ParseException {
		parsePolicy();
		return stack.pop();
	}

	protected void reset() {
		cursor = 0;
		stack.clear();
		root = null;
	}
	
	
	protected void parsePolicy() throws ParseException {
		reset();
		parseTermClause();
		consumeToken(TokenType.ROOT);
		parseAction();
		skipWhiteSpace();
		if (cursor != input.length()) {
			error("Exccesive text at the end.");
		}
		makeBinaryTree();
	}
	
	protected void parseTermClause() throws ParseException {
		if (peekToken(TokenType.NOT)) {
			consumeToken(TokenType.NOT);
			parseTermClause();
			makeUnaryTree();
		} else if (peekToken(TokenType.LEFT_PAREN)) {
			skipToken(TokenType.LEFT_PAREN);
			parseTermClause();
			skipToken(TokenType.RIGHT_PAREN);
		} else {
			parseTerm();
		}
		
		if (peekToken(TokenType.AND)) {
			consumeToken(TokenType.AND);
			parseTermClause();
			makeBinaryTree();
		} else if (peekToken(TokenType.OR)) {
			consumeToken(TokenType.OR);
			parseTermClause();
			makeBinaryTree();
		}
	}
	
	protected void parseTerm() throws ParseException {
		PolicyTerm term = new PolicyTerm(parseName(), parseComparator(), parseValue());
		PolicyNode node = new PolicyNode(PolicyNode.NodeType.TERM, term);
		stack.push(node);
	}
	
	protected ComparatorType parseComparator() throws ParseException {
		Matcher matcher = getMatcher(TokenType.COMPARATOR);
		if (matcher.lookingAt()) {
			for (ComparatorType type : ComparatorType.values()) {
				if (type.toString().equals(matcher.group())) {
					cursor = matcher.end();
					return type;
				}
			}
			// we shouldn't reach this point, unless TokenType.COMPARATOR doesn't match ComparatorType
			error("Invalid comparator: " + matcher.group());
		} else {
			error("Can't find expected comparator token.");
		}
		return null;
	}
	
	protected String parseName() throws ParseException {
		Matcher matcher = getMatcher(TokenType.NAME);
		if (matcher.lookingAt()) {
			cursor = matcher.end();
			return matcher.group();
		} else {
			error("Can't find expected name token.");
		}
		return null;
	}
	
	private Object parseValue() throws ParseException {
		Matcher integerMatcher = getMatcher(TokenType.INTEGER);
		Matcher floatMatcher = getMatcher(TokenType.FLOAT);
		Matcher stringMatcher = getMatcher(TokenType.STRING);
		if (integerMatcher.lookingAt()) {
			cursor = integerMatcher.end();
			return Integer.valueOf(integerMatcher.group());
		} else if (floatMatcher.lookingAt()) {
			cursor = floatMatcher.end();
			return Float.valueOf(integerMatcher.group());
		} else if (stringMatcher.lookingAt()) {
			cursor = stringMatcher.end();
			return stringMatcher.group(1); // without surrounding quotes
		} else {
			error("Can't find expected value token.");
		}
		
		return null;
	}
	
	protected void parseAction() throws ParseException {
		PolicyAction.ActionType type = PolicyAction.ActionType.strToType(parseName());
		String content = null;
		if (peekToken(TokenType.LEFT_PAREN)) {
			skipToken(TokenType.LEFT_PAREN);
			content = (String) parseValue();
			skipToken(TokenType.RIGHT_PAREN);
		}
		PolicyAction action = new PolicyAction(type, content);
		PolicyNode node = new PolicyNode(PolicyNode.NodeType.ACTION, action);
		stack.push(node);
	}
	
	protected void skipWhiteSpace() {
		skipToken(TokenType.WHITESPACE);
	}
	
	protected boolean peekToken(TokenType type) {
		Matcher matcher = getMatcher(type);
		return matcher.lookingAt();
	}
	
	protected void consumeToken(TokenType type) throws ParseException {
		Matcher matcher = getMatcher(type);
		if (matcher.lookingAt()) {
			matcher.group();
			switch (type) {
			case ROOT:
				stack.push(new PolicyNode(NodeType.ROOT, null));
				break;
			case AND:
				stack.push(new PolicyNode(NodeType.AND, null));
				break;
			case OR:
				stack.push(new PolicyNode(NodeType.OR, null));
				break;
			case NOT:
				stack.push(new PolicyNode(NodeType.NOT, null));
				break;
			default:
				// just for safeguard, we are never supposed to reach this point
				error("Can't consume token of type: " + type.name());
			}
			cursor = matcher.end();
		} else {
			error("Can't find expected token of type: " + type.name());
		}
	}
	
	protected void skipToken(TokenType type) {
		Matcher matcher = getMatcher(type);
		if (matcher.lookingAt()) {
			cursor = matcher.end();
		}
	}
	
	protected Matcher getMatcher(TokenType type) {
		Matcher matcher = matchers.get(type);
		matcher.region(cursor, input.length());
		return matcher;
	}
	
	protected void makeBinaryTree() {
		PolicyNode right = stack.pop();
		PolicyNode parent = stack.pop();
		PolicyNode left = stack.pop();
		parent.setLeft(left);
		parent.setRight(right);
		stack.push(parent);
	}
	
	protected void makeUnaryTree() {
		PolicyNode left = stack.pop();
		PolicyNode parent = stack.pop();
		parent.setLeft(left);
		stack.push(parent);
	}
	
	protected void error(String reason) throws ParseException {
		throw new ParseException(reason, cursor);
	}
	
	public enum TokenType {
		ROOT		(":"),
		AND			("&"),
		OR			("\\|"),
		NOT			("!"),
		NAME		("[a-zA-Z_][\\w\\.]*"),
		COMPARATOR	("(?:[!><]=)|[=<>]"),
		STRING		("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\""), // allow escaping using \ (Friedl's: "unrolling-the-loop" technique)
		INTEGER		("\\d+"),
		FLOAT		("d+\\.d+"),
		LEFT_PAREN 	("\\("),
		RIGHT_PAREN ("\\)"),
		WHITESPACE	("\\s+");
		
		private final Pattern pattern;
		
		TokenType(String string) {
			this.pattern = Pattern.compile(string);
		}
		
		public Pattern getPattern() {
			return pattern;
		}
	}
}
