package fi.opc.ua.rules;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.Identifiers;

import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;

public class RuleNode {
	private String raw = null;
	public String Name = null;
	public String Type = null;
	public String Reference = null;
	public Map<String, Object> Attributes = new HashMap<String, Object>();
	public NodeId matchingNodeId;
	
	//**Ctor**
	public RuleNode(String rule) {
		this.raw = rule;
		
		parseRAW();
	}
	
	public RuleNode(String name, String type, Map<String,Object> attributes, NodeId matchingNodeId) {
		this.Name = name;
		this.Type = type;
		this.Attributes = attributes;
		this.matchingNodeId = matchingNodeId;
	}
	
	//**Public methods**
	public boolean MatchesWithUaNode(UaNode node) {
		boolean matches = true;
		
		//match name
		if(Name != null && Name != "" && !node.getBrowseName().getName().equals(Name))
			matches = false;
		
		//match type
		UaReference typeRef = node.getReference(Identifiers.HasTypeDefinition, false);
		if(Type != null && Type != "" && typeRef != null && !typeRef.getTargetNode().getBrowseName().getName().equals(Type))
			matches = false;
		
		//match attributes
		String compType = null;
		if(typeRef != null){
			compType = typeRef.getTargetNode().getBrowseName().getName();
		}
		
		System.out.println("Comparing RuleNode: [" + Type + "]" + Name);
		System.out.println("Comparing to Node:  [" + compType + "]" + node.getBrowseName().getName());
		
		this.matchingNodeId = node.getNodeId();
		
		return matches;
	}
	
	public boolean MatchesWithRHSUaNode(UaNode node) {
		boolean matches = true;
		
		System.out.println("Matching nodeId: " + matchingNodeId);
		System.out.println("Compare nodeId:  " + node.getNodeId());
		System.out.println("CompareTo value: " + this.matchingNodeId.compareTo(node.getNodeId()));
		
		if(this.matchingNodeId != null && this.matchingNodeId.compareTo(node.getNodeId()) != 0)
			matches = false;
		
		return matches;
	}
	
	public void ClearMatchingNodeId() {
		this.matchingNodeId = null;
	}
	
	//**Private methods**
	private void parseRAW(){
		//parse type [Type]
		if(raw.contains("[") && raw.contains("]")) {
			this.Type = raw.substring(raw.indexOf("[") + 1, raw.indexOf("]"));
		}
		
		//parse name
		int nameStartIndex = raw.indexOf("]") + 1;
		int nameEndIndex = raw.indexOf("(");
		if(nameEndIndex == -1)
			nameEndIndex = raw.indexOf("#");
		if(nameEndIndex == -1)
			nameEndIndex = raw.length();
		
		if(nameStartIndex != nameEndIndex)
			this.Name = raw.substring(nameStartIndex, nameEndIndex);

		//parse attributes (@Attribute = value, @Attribute2 = value2)
		String attrString = "";
		if(raw.contains("(") && raw.contains(")"))
			attrString = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")"));
		
		//TODO: parse attributes
		
		//parse reference #Reference
		if(raw.contains("#")) {
			this.Reference = raw.substring(raw.indexOf("#") + 1, raw.length());
		}
	}
}
