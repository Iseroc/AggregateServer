package fi.opc.ua.rules;

import org.opcfoundation.ua.core.Identifiers;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;

import fi.opc.ua.server.TargetServer;

public class RHSRuleNode extends RuleNode {

	//**Ctor**
	public RHSRuleNode(String rule) {
		this.raw = rule;
		
		this.parseRAW();
	}
	
	public boolean MatchWithUaNode(UaNode node, TargetServer ts) throws StatusException, ServiceException, AddressSpaceException {
		String browseName = this.Name;
		String displayName = null;
		String type = this.Type;
		//TODO: attribute and property comparison required too
		
		if(this.MatchingNodeId != null) {
			UaNode matchingNode = ts.getTargetServerAddressSpace().getNode(this.MatchingNodeId);
			
			if(browseName == null)
				browseName = matchingNode.getBrowseName().getName();

			if(displayName == null)
				displayName = matchingNode.getDisplayName().getText();
			
			if(type == null) {
				UaReference typeRef = matchingNode.getReference(Identifiers.HasTypeDefinition, false);
				if(typeRef != null) {
					UaNode typeNode = typeRef.getTargetNode();
					if(typeNode != null)
						type = typeNode.getBrowseName().getName();
				}
			}
		}

		if(!browseName.equals(node.getBrowseName().getName())) {
			return false;
		}
		
		if(displayName != null && !displayName.equals(node.getDisplayName().getText())) {
			return false;
		}
		
		UaReference typeRef = node.getReference(Identifiers.HasTypeDefinition, false);
		if(type != null && typeRef != null) {
			UaNode typeNode = typeRef.getTargetNode();
			if(!type.equals(typeNode.getBrowseName().getName()))
				return false;
		}
		
		return true;
	}
	
}
