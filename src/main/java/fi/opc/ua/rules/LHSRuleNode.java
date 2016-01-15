package fi.opc.ua.rules;

import org.opcfoundation.ua.core.Identifiers;

import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;

public class LHSRuleNode extends RuleNode {
	
	//**Ctor**
	public LHSRuleNode(String rule) {
		this.raw = rule;
		
		this.parseRAW();
	}
	
	//**Public methods**
	public boolean MatchWithUaNode(UaNode node, boolean debug) {
		//match name
		if(Name != null && Name != "" && !node.getBrowseName().getName().equals(Name)) {
			if(debug)
				System.out.println("Name match failed: " + Name + " - " + node.getBrowseName().getName());
			
			return false;
		}
		
		//match type
		UaReference typeRef = node.getReference(Identifiers.HasTypeDefinition, false);
		if(Type != null && Type != "" && typeRef != null && !typeRef.getTargetNode().getBrowseName().getName().equals(Type)) {
			if(debug)
				System.out.println("Type match failed: " + Type + " - " + typeRef != null ? typeRef.getTargetNode().getBrowseName().getName() : "(no typeRef)");
			
			return false;
		}
		
		//TODO: match attributes and properties
		
		//matches, save matching node id
		this.MatchingNodeId = node.getNodeId();
		
		return true;
	}
	
	public void ClearMatchingNodeId() {
		this.MatchingNodeId = null;
	}
}
