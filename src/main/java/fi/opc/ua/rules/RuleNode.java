package fi.opc.ua.rules;

import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaType;

public class RuleNode {
	
	public RuleNode(String ruleString) {
		/*
		 * Example ruleString
		 *
		 * [NodeType]NodeName#1
		 * TYPE       between []
		 * NAME:      after the type ] or if no type, the beginning
		 * REFERENCE: #1 makes this the reference with name 1
		 * 
		 * Reference is not required. Either type or name is required for the rule to work
		 */
		
		if(ruleString.indexOf("[") == 0) {
			int endIndex = ruleString.indexOf("]");
			this.Type = ruleString.substring(1, endIndex - 1);
		}
		
		int nameStartIndex = 0;
		if(ruleString.indexOf("]") > 0) {
			nameStartIndex = ruleString.indexOf("]") + 1;
		}
		
		int referenceIndex = ruleString.indexOf("#");
		
		if(nameStartIndex < ruleString.length()) {
			if(referenceIndex != -1)
				this.Name = ruleString.substring(nameStartIndex, referenceIndex - 1);
			else
				this.Name = ruleString.substring(nameStartIndex, ruleString.length() - 1);
		}
		
		if(referenceIndex != 1) {
			this.Reference = ruleString.substring(referenceIndex + 1, ruleString.length() - 1);
		}
	}
	
	public String RuleSide = "LHS";
	public String Type = null;
	public String Name = null;
	public String Reference = null;
	
	public boolean MatchesUaNode(UaNode node, UaType type) throws Exception {
		if(RuleSide == "RHS")
			throw new Exception("Invalid rule side matching.");
		if(node == null)
			throw new IllegalArgumentException("Argument node is null");
		
		if(Type != null) {
			if(!type.getDisplayName().equals(Type))
				return false;
		}
		
		if(Name != null) {
			if(!node.getDisplayName().equals(Name))
				return false;
		}
		
		return true;
	}
}
