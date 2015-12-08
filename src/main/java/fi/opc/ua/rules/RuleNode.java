package fi.opc.ua.rules;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;

public class RuleNode {
	public RuleNode(String rule, boolean parse) {
		this.raw = rule;
		
		if(parse)
			parseRAW();
	}
	
	private String raw = null;
	private String Name = null;
	private String Type = null;
	private Map<String, Object> Attributes = new HashMap<String, Object>();
	private NodeId matchingNodeId;
	
	private void parseRAW(){
		//parse type [Type]
		if(raw.contains("[") && raw.contains("]")) {
			Type = raw.substring(raw.indexOf("[") + 1, raw.indexOf("]"));
		}
		
		//parse name
		int nameStartIndex = raw.indexOf("]") + 1;
		int nameEndIndex = raw.indexOf("(");
		if(nameEndIndex == -1)
			nameEndIndex = raw.length();
		
		Name = raw.substring(nameStartIndex, nameEndIndex);
		
		String attrString = "";
		if(raw.contains("(") && raw.contains(")"))
			attrString = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")"));
		
		//parse attributes (@Attribute = value, @Attribute2 = value2)
		//TODO: parse attributes
	}
}
