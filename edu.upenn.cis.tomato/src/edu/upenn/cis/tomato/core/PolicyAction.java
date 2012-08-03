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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
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
		PolicyAction other = (PolicyAction) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public enum ActionType {
		/*
		 * ActionTypes are ordered by priority, from low to high. Because
		 * applying multiple policies to one source bundle may result in
		 * multiple policies want to modify one same suspect site. This priority
		 * order will determine which policy's action shall prevail.
		 */
		CUSTOM("custom"),
		PROHIBIT("prohibit");

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

		@Override
		public String toString() {
			return string;
		}
	}
}
