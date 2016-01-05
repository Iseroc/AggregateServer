package fi.opc.ua.rules;

import org.opcfoundation.ua.builtintypes.NodeId;

public class RuleAttribute {
	private String raw = null;
	public String AttributeName = null;
	public Object Value = null;
	public String Reference = null;
	public String ReferenceAttributeName = null;
	
	public NodeId MatchingNodeId = null;
	
	public RuleAttribute(String raw) throws IllegalArgumentException {
		this.raw = raw;
		parseRAW();
	}
	
	public RuleAttribute(String name, Object value, String reference) {
		this.AttributeName = name;
		this.Value = value;
		this.Reference = reference;
	}
	
	private void parseRAW() throws IllegalArgumentException {
		//DisplayName = #2@DisplayName
		//Name = #Refrence@ReferenceAttributeName
		String[] parts = raw.replaceAll("\\s","").split("=");
		
		if(parts.length != 2)
			throw new IllegalArgumentException("Could not parse rule attributes");
		
		//LHS
		AttributeName = parts[0];
		
		//RHS
		if(parts[1].contains("#")) {
			this.Reference = parts[1].substring(parts[1].indexOf("#") + 1, parts[1].indexOf("@"));
			this.ReferenceAttributeName = parts[1].substring(parts[1].indexOf("@") + 1, parts[1].length());
		}
		else {
			this.Value = parts[1];
		}
	}
}
