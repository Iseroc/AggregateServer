package fi.opc.ua.rules;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;

public class RuleNode {
	protected String raw = null;
	public String Name = null;
	public String Type = null;
	public String Reference = null;
	public Map<String, Object> Attributes = new HashMap<String, Object>();
	public NodeId MatchingNodeId;

	protected void parseRAW(){
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
