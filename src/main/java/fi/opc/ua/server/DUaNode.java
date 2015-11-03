package fi.opc.ua.server;

import com.prosysopc.ua.nodes.UaNode;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.core.NodeAttributes;
import com.prosysopc.ua.nodes.UaReference;
import org.opcfoundation.ua.builtintypes.NodeId;
import com.prosysopc.ua.nodes.UaProperty;

public class DUaNode {
	
	public UaNode node;
	public String typeDef;
	public int timesProcessed = 0;
	
	public DUaNode (UaNode node, String typeDef) {
		this.node = node;
		this.typeDef = typeDef;
	}
	
	void setUaNode(UaNode UAnode) {
		this.node = UAnode;
	}
	
	void setTypeDef(String typeString) {
		this.typeDef = typeString;
	}
	
	void setTimesProcessed(int tp) {
		this.timesProcessed = tp;
	}
	
	public UaNode getUaNode() {
		return this.node;
	}
	
	public String getTypeDef() {
		return this.typeDef;
	}
	
	int getTimesProcessed() {
		return this.timesProcessed;
	}
	
	public LocalizedText getDisplayName() {
		return this.node.getDisplayName();
	}
	
	public QualifiedName getBrowseName() {
		return this.node.getBrowseName();
	}
	
	public LocalizedText getDescription() {
		return this.node.getDescription();
	}
	
	public NodeAttributes getAttributes() {
		return this.node.getAttributes();
	}
	
	public UaReference[] getForwardReferences(NodeId id) {
		return this.node.getForwardReferences(id);
	}
	
	public UaReference getReference(NodeId id, boolean bool) {
		return this.node.getReference(id, bool);
	}

	public UaReference[] getReferences(NodeId id, boolean isInverse) {
		return this.node.getReferences(id, isInverse);
	}
	
	public NodeId getNodeId() {
		return this.node.getNodeId();
	}
	
	public UaProperty getProperty(QualifiedName browsename) {
		return this.node.getProperty(browsename);
	}
}
