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
	public List<LHSRuleNode> LHSNodes;
	public List<RHSRuleNode> RHSNodes;
	
	//**Ctor**
	MatchingRule() {
		LHSNodes = new ArrayList<LHSRuleNode>();
		RHSNodes = new ArrayList<RHSRuleNode>();
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
			for(LHSRuleNode rNode : mRule.LHSNodes) {
				rNode.ClearMatchingNodeId();
			}
		}
		
		return null;
	}
	
	private void parseRuleLHS(Rule rule) {
		this.rule = rule;
		String[] nodes = rule.LHS.split("/");
		
		LHSNodes = new ArrayList<LHSRuleNode>();
		for(String s : nodes) {
			LHSNodes.add(new LHSRuleNode(s));
		}
	}
	
	private void parseRuleRHS(){
		String[] nodes = rule.RHS.split("/");
		
		RHSNodes = new ArrayList<RHSRuleNode>();
		for(String s : nodes) {
			RHSNodes.add(new RHSRuleNode(s));
		}
		
		//connect references from LHS to RHS if they exist
		for(RHSRuleNode rhsNode : RHSNodes) {
			for(LHSRuleNode lhsNode : LHSNodes) {
				if(rhsNode.Reference.equals(lhsNode.Reference)) {
					rhsNode.MatchingNodeId = lhsNode.MatchingNodeId;
					break;
				}
			}
		}
	}
	
	private boolean matchWithNode(UaNode node, int index) throws ServiceException, AddressSpaceException {
		//reached the end of LHSNodes rule list
		if(index >= LHSNodes.size())
			return true;

		//does the given node match the node at LHSNodes size-index
		if(this.LHSNodes.get(LHSNodes.size() - index - 1).MatchWithUaNode(node)) {
			
			//get source parent node
			UaNode sourceParentNode = node.getReference(hasComponentId, true).getSourceNode();
			
			return matchWithNode(sourceParentNode, index+1);
		}
		
		//this node does not match rule
		return false;
	}
}
