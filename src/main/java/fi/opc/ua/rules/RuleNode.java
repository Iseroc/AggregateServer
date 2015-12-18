package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;

public class RuleNode {
	protected String raw = null;
	public String Name = null;
	public String Type = null;
	public String Reference = null;
	public List<RuleAttribute> Attributes = new ArrayList<RuleAttribute>();
	public NodeId MatchingNodeId;

	protected void parseRAW() throws IllegalArgumentException{
		//parse type - [Type]
		if(raw.contains("[") && raw.contains("]")) {
			this.Type = raw.substring(raw.indexOf("[") + 1, raw.indexOf("]"));
		}
		
		//parse name - Name
		int nameStartIndex = raw.indexOf("]") + 1;
		int nameEndIndex = raw.indexOf("(");
		if(nameEndIndex == -1)
			nameEndIndex = raw.indexOf("#");
		if(nameEndIndex == -1)
			nameEndIndex = raw.length();
		
		if(nameStartIndex != nameEndIndex)
			this.Name = raw.substring(nameStartIndex, nameEndIndex);

		//parse attributes - (@Attribute = value, @Attribute2 = value2)
		String attrRaw = "";
		if(raw.contains("(") && raw.contains(")"))
			attrRaw = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")"));
		
		//TODO: parse attributes - DisplayName = #2@DisplayName
		String[] attrs = attrRaw.split(",");
		for(String attr : attrs) {
			this.Attributes.add(new RuleAttribute(attr));
		}
		
		//parse reference #Reference
		if(raw.contains("#")) {
			this.Reference = raw.substring(raw.indexOf("#") + 1, raw.length());
		}
	}
}
