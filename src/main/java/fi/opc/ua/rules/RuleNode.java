package fi.opc.ua.rules;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.nodes.UaNode;

public class RuleNode {
	private String raw = null;
	public String Name = null;
	public String Type = null;
	public String Reference = null;
	public Map<String, Object> Attributes = new HashMap<String, Object>();
	public NodeId matchingNodeId;
	
	//**Ctor**
	public RuleNode(String rule, boolean parse) {
		this.raw = rule;
		
		if(parse)
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
		//TODO: displayname? browsename?
		if(!node.getBrowseName().equals(Name))
			matches = false;
		
		//match type
		
		//match attributes
		
		return matches;
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
			nameEndIndex = raw.length();
		
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
