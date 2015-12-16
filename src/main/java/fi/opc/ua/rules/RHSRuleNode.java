package fi.opc.ua.rules;

import org.opcfoundation.ua.core.Identifiers;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;

import fi.opc.ua.server.ASNodeManager;
import fi.opc.ua.server.TargetServer;

public class RHSRuleNode extends RuleNode {

	//**Ctor**
	public RHSRuleNode(String rule) {
		this.raw = rule;
		
		this.parseRAW();
	}
	

	public boolean MatchWithUaNode(UaNode node, TargetServer ts) throws StatusException, ServiceException, AddressSpaceException {
		String name = this.Name;
		String type = this.Type;
		//TODO: attribute comparison too
		
		if(this.MatchingNodeId != null) {
			UaNode matchingNode = ts.getTargetServerAddressSpace().getNode(this.MatchingNodeId);
			
			if(name == null)
				name = matchingNode.getBrowseName().getName();
			
			if(type == null) {
				UaReference typeRef = matchingNode.getReference(Identifiers.HasTypeDefinition, false);
				if(typeRef != null) {
					UaNode typeNode = typeRef.getTargetNode();
					type = typeNode.getBrowseName().getName();
				}
			}
		}
		
		if(!name.equals(node.getBrowseName().getName())) {
			return false;
		}
		
		UaReference typeRef = node.getReference(Identifiers.HasTypeDefinition, false);
		if(typeRef != null) {
			UaNode typeNode = typeRef.getTargetNode();
			if(!type.equals(typeNode.getBrowseName().getName()))
				return false;
		}
		
		return true;
	}
	
}
