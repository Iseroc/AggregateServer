package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.List;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.nodes.UaNode;

public class MatchingRule {
	private NodeId hasComponentId = new NodeId(0,47);
	
	private Rule rule;
	public List<RuleNode> LHSNodes;
	public List<RuleNode> RHSNodes;
	
	//**Ctor**
	MatchingRule() {
		LHSNodes = new ArrayList<RuleNode>();
		RHSNodes = new ArrayList<RuleNode>();
	}
	
	//**Public methods**
	public static MatchingRule MatchRule(Rule rule, NodeId nodeId, AddressSpace as) throws ServiceException, AddressSpaceException {
		MatchingRule mRule = new MatchingRule();
		mRule.parseRuleLHS(rule);
		
		UaNode sourceNode = as.getNode(nodeId);
		if(mRule.matchWithNode(sourceNode, 0)) {
			mRule.parseRuleRHS();
			return mRule;
		}
		
		return null;
	}
	
	private void parseRuleLHS(Rule rule) {
		this.rule = rule;
		String[] nodes = rule.LHS.split("/");
		
		LHSNodes = new ArrayList<RuleNode>();
		for(String s : nodes) {
			LHSNodes.add(new RuleNode(s, true));
		}
	}
	
	private void parseRuleRHS(){
		
	}
	
	private boolean matchWithNode(UaNode nodeId, int index) throws ServiceException, AddressSpaceException {
		
		
		if(this.LHSNodes.get(LHSNodes.size() - index - 1).MatchesWithUaNode(sourceNode)) {
			//this node matches the node at LHSNodes size-index
			
			//get source parent node
			UaNode sourceParentNode = sourceNode.getReference(hasComponentId, true).getSourceNode();
			
			return matchWithNode()
		}
		
		//this node does not match rule
		return false;
	}
}
