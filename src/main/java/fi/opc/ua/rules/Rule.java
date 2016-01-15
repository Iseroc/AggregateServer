package fi.opc.ua.rules;

public class Rule {
	
	public Rule(String lhs, String rhs, String type) {
		this.LHS = lhs;//.replaceAll("\\s","");
		this.RHS = rhs;//.replaceAll("\\s","");
		this.Type = type;
	}
	
	public String LHS = null;
	public String RHS = null;
	public String Type = null;
	public String NodeClass = null;
}
