package edu.upenn.cis.tomato.core;

public class PolicyAction {
	ActionType type;
	String content;

	public PolicyAction(ActionType type, String content) {
		this.type = type;
		this.content = content;
	}
	
	public ActionType getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		if (content != null) {
			return "" + type + "(\"" + content + "\")";
		} else {
			return "" + type;
		}
	}

	public enum ActionType {
		PROHIBIT("prohibit"),
		CUSTOM("custom");

		private final String string;

		ActionType(String string) {
			this.string = string;
		}

		public static ActionType strToType(String str) {
			for (ActionType type : ActionType.values()) {
				if (str.equals(type.toString())) {
					return type;
				}
			}
			return null;
		}

		public String toString() {
			return string;
		}
	}
}
