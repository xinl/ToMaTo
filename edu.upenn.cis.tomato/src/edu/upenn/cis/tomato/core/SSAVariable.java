package edu.upenn.cis.tomato.core;

public class SSAVariable{
	private String methodName;
	private int variableNumber;
	
	public SSAVariable(String methodName, int variableNumber) {
		super();
		this.methodName = methodName;
		this.variableNumber = variableNumber;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public int getVariableNumber() {
		return variableNumber;
	}

	public void setVariableNumber(int variableNumber) {
		this.variableNumber = variableNumber;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SSAVariable other = (SSAVariable) obj;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (variableNumber != other.variableNumber)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return (this.methodName + " " + this.variableNumber).hashCode();
	}

	@Override
	public String toString() {
		return "[CGNode] " + this.methodName + "\t[Variable Number] " + this.variableNumber;
	}
	
}
