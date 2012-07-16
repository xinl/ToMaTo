package edu.upenn.cis.tomato.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import edu.upenn.cis.tomato.core.PolicyTerm.PropertyName;

public class FunctionInvocationSuspect extends Suspect {
	protected SSAVariable funcVar;
	public SSAVariable getFuncVar() {
		return funcVar;
	}

	public void setFuncVar(SSAVariable funcVar) {
		this.funcVar = funcVar;
	}

	public FunctionInvocationSuspect(SourcePosition callSitePos, SSAVariable funcVar) {
		super(callSitePos);
		this.type = SuspectType.FUNCTION_INVOCATION_SUSPECT;
		this.funcVar = funcVar;
	}
	
	public SourcePosition getCallerPosition() {
		return super.sitePos;
	}
	
	public Set<Suspect> getAliases() {
		//TODO: return the alias group for callee
		return null;
	}

	public String toSignatureString() {
		return "[Suspect Signature] [Position] " + sitePos + "\t[Function Variable] " + funcVar + "\n";
	}
	
	@Override
	public String toString() {
		
		String result = this.toSignatureString() + "===== Attribute List =====\n";
		Iterator<Entry<PropertyName, Object>> iter_attr = attributes.entrySet().iterator();
		while (iter_attr.hasNext()) {
			Entry<PropertyName, Object> entry = iter_attr.next();
			PropertyName name = entry.getKey();
			Object value = entry.getValue();
			if (!name.equals(PropertyName.ALIAS_SUSPECT)) {
				result = result + "[" + name + "] " + value + "\n";
			} 
		}
		
		HashSet<FunctionInvocationSuspect> aliasSet = (HashSet<FunctionInvocationSuspect>) attributes.get(PropertyName.ALIAS_SUSPECT);
		if(aliasSet != null) {
			result = result + "===== Alias Suspect =====\n";
			Iterator<FunctionInvocationSuspect> iter_as = aliasSet.iterator();
			while (iter_as.hasNext()) {
				result = result + iter_as.next().toSignatureString();
			}
		}		
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((funcVar == null) ? 0 : funcVar.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionInvocationSuspect other = (FunctionInvocationSuspect) obj;
		if (funcVar == null) {
			if (other.funcVar != null)
				return false;
		} else if (!funcVar.equals(other.funcVar))
			return false;
		return true;
	}
}
