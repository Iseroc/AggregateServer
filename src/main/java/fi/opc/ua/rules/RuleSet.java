package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.List;

public class RuleSet {
	
	public RuleSet(String serverUri) {
		this.ServerUri = serverUri;
		this.ruleList = new ArrayList<Rule>();
	}
	
	public String ServerUri = null;
	private List<Rule> ruleList = null;
	
	public List<Rule> GetRuleList() {
		return ruleList;
	}
	
	public void AddRule(Rule rule) {
		ruleList.add(rule);
	}
}
