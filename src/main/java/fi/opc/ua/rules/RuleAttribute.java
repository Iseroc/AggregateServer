package fi.opc.ua.rules;

import org.opcfoundation.ua.builtintypes.NodeId;

public class RuleAttribute {
	private String raw = null;
	public String AttributeName = null;
	public Object Value = null;
	public String[] Reference = null;
	public String[] ReferenceAttributeName = null;
	
	public NodeId[] MatchingNodeId = null;
	
	public RuleAttribute(String raw) throws IllegalArgumentException {
		this.raw = raw;
		parseRAW();
	}
	
	private void parseRAW() throws IllegalArgumentException {
		//Name = #Refrence@ReferenceAttributeName
		String[] parts = raw.split("=");

		if(parts.length != 2)
			throw new IllegalArgumentException("Could not parse rule attributes");

		//LHS
		AttributeName = parts[0];//.replaceAll("\\s","");
		
		//RHS
		if(this.raw.contains("{")) {
			//DisplayName = {#2@DisplayName}.{#3@DisplayName} {#3@EngineeringUnits}
			String[] attrs = parts[1].split("#");
			this.Reference = new String[attrs.length - 1];
			this.ReferenceAttributeName = new String[attrs.length - 1];
			for(int i = 1; i < attrs.length; i++) {
				//2@DisplayName}.{
				System.out.println("## " + attrs[i]);
				this.Reference[i - 1] = attrs[i].substring(0, attrs[i].indexOf("@"));
				this.ReferenceAttributeName[i - 1] = attrs[i].substring(attrs[i].indexOf("@") + 1, attrs[i].indexOf("}"));
			}
		}
		else {
			//DisplayName = #2@DisplayName
			if(parts[1].contains("#")) {
				this.Reference = new String[1];
				this.Reference[0] = parts[1].substring(parts[1].indexOf("#") + 1, parts[1].indexOf("@"));
				this.ReferenceAttributeName = new String[1];
				this.ReferenceAttributeName[0] = parts[1].substring(parts[1].indexOf("@") + 1, parts[1].length());
			}
			else {
				this.Value = parts[1];
			}
		}
	}
	
	public String BuildRHSString(String[] referenceValues) {
//		System.out.print("## Build RHS String from refs: ");
//		for(String s : referenceValues) {
//			System.out.print(s + " ");
//		}
//		System.out.println();
		
		String rhsString = "";
		
		String rhsRaw = this.raw.substring(this.raw.indexOf("=") + 1, this.raw.length());
//		System.out.println("## Raw: " + rhsRaw);
		if(rhsRaw.contains("{")) {
			//DisplayName = {#2@DisplayName}.{#3@DisplayName} {#3@EngineeringUnits}
			String[] attrs = rhsRaw.split("\\{");
			for(int i = 1; i < attrs.length; i++) {
//				System.out.println("## ## " + attrs[i]);
				//#2@DisplayName}.
				String[] split = attrs[i].split("\\}");
				
				rhsString += referenceValues[i - 1];
				if(split.length == 2)
					rhsString += split[1];
			}
		}
		else {
			//DisplayName = #2@DisplayName
			if(rhsRaw.contains("#")) {
				rhsString = referenceValues[0];
			}
			else {
				rhsString = (String)this.Value;
			}
		}
		
		return rhsString;
	}
}
