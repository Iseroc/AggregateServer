package fi.opc.ua.rules;

import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;
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
	public boolean MatchWithUaNode(UaNode node) {
		//match name
		if(Name != null && Name != "" && !node.getBrowseName().getName().equals(Name))
			return false;
		
		//match type
		UaReference typeRef = node.getReference(Identifiers.HasTypeDefinition, false);
		if(Type != null && Type != "" && typeRef != null && !typeRef.getTargetNode().getBrowseName().getName().equals(Type))
			return false;
		
		//TODO: match attributes
		
		//matches!
		this.MatchingNodeId = node.getNodeId();
		
		return true;
	}
	
	public void ClearMatchingNodeId() {
		this.MatchingNodeId = null;
	}
}
