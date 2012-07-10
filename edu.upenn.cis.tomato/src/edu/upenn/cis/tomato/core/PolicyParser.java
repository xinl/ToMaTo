package edu.upenn.cis.tomato.core;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.upenn.cis.tomato.core.PolicyNode.NodeType;
import edu.upenn.cis.tomato.core.PolicyTerm.ComparatorType;

/**
 * A Parser for converting policy string into a binary tree of PolicyNodes.
 * 
 * Policy string grammar:
 * 
 * <pre>
 * <policy> := <disjunction> ":" <action>
 * 
 * <disjunction> := <conjunction> {"|" <disjunction>}
 * <conjunction> := <negation> {"&" <conjunction>}
 * <negation> := <atom> | "!" <negation>
 * <atom> := "(" <disjunction> ")" | <term>
 * 
 * <term> := <name> <comparator> <value>
 * <comparator> := "=" | "!=" | ">" | "<" | ">=" | "<="
 * <value> := <float> | <integer> | <string>
 * 
 * <action> := <name> {"(" <string> ")"}
 * </pre>
 * 
 * Priority of logic operators: ! > & > |
 * 
 * @author Xin Li
 * 
 */
public class PolicyParser {
	protected String input;
	protected PolicyNode root;
	protected Stack<PolicyNode> stack = new Stack<PolicyNode>();
	protected int cursor;
	protected Map<TokenType, Matcher> matchers = new HashMap<TokenType, Matcher>();

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
		parseDisjunction();
		consumeToken(TokenType.ROOT);
		parseAction();
		skipWhiteSpace();
		if (cursor != input.length()) {
			error("Exccesive text at the end.");
		}
		makeBinaryTree();
	}

	protected void parseDisjunction() throws ParseException {
		skipWhiteSpace();
		parseConjunction();
		skipWhiteSpace();
		if (peekToken(TokenType.OR)) {
			consumeToken(TokenType.OR);
			parseDisjunction();
			makeBinaryTree();
		}
	}

	protected void parseConjunction() throws ParseException {
		skipWhiteSpace();
		parseNegation();
		skipWhiteSpace();
		if (peekToken(TokenType.AND)) {
			consumeToken(TokenType.AND);
			parseConjunction();
			makeBinaryTree();
		}
	}

	protected void parseNegation() throws ParseException {
		skipWhiteSpace();
		if (peekToken(TokenType.NOT)) {
			consumeToken(TokenType.NOT);
			parseNegation();
			makeUnaryTree();
		} else {
			parseAtom();
		}
	}

	protected void parseAtom() throws ParseException {
		skipWhiteSpace();
		if (peekToken(TokenType.LEFT_PAREN)) {
			skipToken(TokenType.LEFT_PAREN);
			parseDisjunction();
			if (peekToken(TokenType.RIGHT_PAREN)) {
				skipToken(TokenType.RIGHT_PAREN);
			} else {
				error("Unclosed parenthesis.");
			}
		} else {
			parseTerm();
		}
	}

	protected void parseTerm() throws ParseException {
		PolicyTerm term = null;
		String name = parseName();
		ComparatorType comp = parseComparator();
		Object value = parseValue();
		try {
			term = new PolicyTerm(name, comp, value);
		} catch (IllegalArgumentException e) {
			error("Illegal term format: " + name);
		}
		PolicyNode node = new PolicyNode(PolicyNode.NodeType.TERM, term);
		stack.push(node);
	}

	protected ComparatorType parseComparator() throws ParseException {
		skipWhiteSpace();
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
			error("Can't find expected comparator token at " + cursor);
		}
		return null;
	}

	protected String parseName() throws ParseException {
		skipWhiteSpace();
		Matcher matcher = getMatcher(TokenType.NAME);
		if (matcher.lookingAt()) {
			cursor = matcher.end();
			return matcher.group();
		} else {
			error("Can't find expected name token at " + cursor);
		}
		return null;
	}

	protected Object parseValue() throws ParseException {
		skipWhiteSpace();
		Matcher integerMatcher = getMatcher(TokenType.INTEGER);
		Matcher floatMatcher = getMatcher(TokenType.FLOAT);
		Matcher stringMatcher = getMatcher(TokenType.STRING);
		if (floatMatcher.lookingAt()) {
			cursor = floatMatcher.end();
			return Float.valueOf(floatMatcher.group());
		} else if (integerMatcher.lookingAt()) {
			cursor = integerMatcher.end();
			return Integer.valueOf(integerMatcher.group());
		} else if (stringMatcher.lookingAt()) {
			cursor = stringMatcher.end();
			return stringMatcher.group(1); // without surrounding quotes
		} else {
			error("Can't find a valid value token.");
		}

		return null;
	}

	protected void parseAction() throws ParseException {
		skipWhiteSpace();
		PolicyAction.ActionType type = PolicyAction.ActionType.strToType(parseName());
		if (type == null) {
			error("Invalid action name.");
		}
		String content = null;
		if (peekToken(TokenType.LEFT_PAREN)) {
			skipToken(TokenType.LEFT_PAREN);
			Object value = parseValue();
			if (!(value instanceof String)) {
				error("Parameter of action must be a string");
			}
			content = (String) value;
			skipToken(TokenType.RIGHT_PAREN);
		}
		PolicyAction action = new PolicyAction(type, content);
		PolicyNode node = new PolicyNode(PolicyNode.NodeType.ACTION, action);
		stack.push(node);
	}

	protected void skipWhiteSpace() {
		skipToken(TokenType.WHITESPACE);
	}

	/**
	 * Test whether next token is of a given type. This method does not move the
	 * cursor and does not consume any token.
	 * 
	 * @param type
	 *            the expected type of next token
	 * @return True if next token is of the given type, otherwise, False.
	 */
	protected boolean peekToken(TokenType type) {
		Matcher matcher = getMatcher(type);
		return matcher.lookingAt();
	}

	/**
	 * Turn the first ROOT, AND, OR, NOT token after cursor point into a
	 * PolicyNode and put it on stack.
	 * 
	 * @param type
	 *            the type of the token to consume
	 * @throws ParseException
	 */
	protected void consumeToken(TokenType type) throws ParseException {
		skipWhiteSpace();
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
			error("Can't find expected token of type: " + type.name() + " at " + cursor);
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
		ROOT(":"),
		AND("&"),
		OR("\\|"),
		NOT("!"),
		NAME("[a-zA-Z_][\\w\\.]*"),
		COMPARATOR("(?:[!><]=)|[=<>]"),
		STRING("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\""), // allow escaping using \ (Friedl's: "unrolling-the-loop" technique)
		INTEGER("-?\\d+"),
		FLOAT("-?\\d+\\.\\d+"),
		LEFT_PAREN("\\("),
		RIGHT_PAREN("\\)"),
		WHITESPACE("\\s+");

		private final Pattern pattern;

		TokenType(String string) {
			this.pattern = Pattern.compile(string);
		}

		public Pattern getPattern() {
			return pattern;
		}
	}
}
