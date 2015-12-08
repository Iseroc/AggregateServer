package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.List;

import com.prosysopc.ua.nodes.UaNode;

public class MatchingRule {
	private Rule rule;
	List<RuleNode> LHSNodes;
	List<RuleNode> RHSNodes;
	
	MatchingRule() {
		LHSNodes = new ArrayList<RuleNode>();
		RHSNodes = new ArrayList<RuleNode>();
	}
	
	public void MatchNode(UaNode node) {
		
	}
}
