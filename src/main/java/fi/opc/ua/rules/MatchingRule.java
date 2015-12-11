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
		else {
			//Did not match, clearing matchingNodeIds
			for(RuleNode rNode : mRule.LHSNodes) {
				rNode.ClearMatchingNodeId();
			}
		}
		
		return null;
	}
	
	private void parseRuleLHS(Rule rule) {
		this.rule = rule;
		String[] nodes = rule.LHS.split("/");
		
		LHSNodes = new ArrayList<RuleNode>();
		for(String s : nodes) {
			LHSNodes.add(new RuleNode(s));
		}
	}
	
	private void parseRuleRHS(){
		String[] nodes = rule.RHS.split("/");
		
		RHSNodes = new ArrayList<RuleNode>();
		for(String s : nodes) {
			RHSNodes.add(new RuleNode(s));
			System.out.println("Create RHS from " + s + " to have ref " + RHSNodes.get(RHSNodes.size()-1).Reference);
		}
		
		//connect references from LHS to RHS if they exist
		for(RuleNode rhsNode : RHSNodes) {
			for(RuleNode lhsNode : LHSNodes) {
				if(rhsNode.Reference.equals(lhsNode.Reference)) {
					rhsNode.matchingNodeId = lhsNode.matchingNodeId;
					break;
				}
			}
		}
	}
	
	private boolean matchWithNode(UaNode node, int index) throws ServiceException, AddressSpaceException {
		//reached the end of LHSNodes rule list
		if(index >= LHSNodes.size())
			return true;
		
		if(this.LHSNodes.get(LHSNodes.size() - index - 1).MatchesWithUaNode(node)) {
			//this node matches the node at LHSNodes size-index
			
			//get source parent node
			UaNode sourceParentNode = node.getReference(hasComponentId, true).getSourceNode();
			
			return matchWithNode(sourceParentNode, index+1);
		}
		
		//this node does not match rule
		return false;
	}
}
