package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;

public class MatchingRule {
	private NodeId hasComponentId = new NodeId(0,47);
	
	private Rule rule;
	public List<LHSRuleNode> LHSNodes;
	public List<RHSRuleNode> RHSNodes;
	public Map<String, NodeId> LHSNodeReferences;
	public Rule OriginalRule;
	
	//**Ctor**
	MatchingRule() {
		LHSNodes = new ArrayList<LHSRuleNode>();
		LHSNodeReferences = new HashMap<String, NodeId>();
		RHSNodes = new ArrayList<RHSRuleNode>();
	}
	
	//**Public methods**
	public static MatchingRule MatchRule(Rule rule, NodeId nodeId, AddressSpace as) throws ServiceException, AddressSpaceException {
		MatchingRule mRule = new MatchingRule();
		mRule.OriginalRule = rule;
		mRule.parseRuleLHS(rule);
		
		boolean debug = false;
		
		if(nodeId.toString().equals("ns=2;i=50056") || nodeId.toString().equals("ns=2;i=50057") || nodeId.toString().equals("ns=2;i=50058")) {
			System.out.println();
			System.out.println("Matching node: " + nodeId);
			System.out.println("Matching with: " + rule.LHS + "=" + rule.RHS);
			debug = true;
		}
		
		UaNode sourceNode = as.getNode(nodeId);
		if(mRule.matchWithNode(sourceNode, 0, debug)) {
			mRule.parseRuleRHS();
			return mRule;
		}
		else {
			//Did not match, clearing matchingNodeIds
			for(LHSRuleNode lNode : mRule.LHSNodes) {
				lNode.ClearMatchingNodeId();
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
			rhsNode.MatchingNodeId = LHSNodeReferences.get(rhsNode.Reference);
			
			//TODO: find attribute references
			for(RuleAttribute rAttr : rhsNode.Attributes) {
				if(rAttr.Reference != null) {
					rAttr.MatchingNodeId = new NodeId[rAttr.Reference.length];
					for(int i = 0; i < rAttr.Reference.length; i++) {
						rAttr.MatchingNodeId[i] = LHSNodeReferences.get(rAttr.Reference[i]);
					}
				}
				
			}
		}
	}
	
	private boolean matchWithNode(UaNode node, int index, boolean debug) throws ServiceException, AddressSpaceException {
		LHSRuleNode lrn = this.LHSNodes.get(LHSNodes.size() - index - 1);
		
		if(debug) {
			System.out.println("## matchWithNode: " + node);
			System.out.println("## matchWithNode: " + lrn.raw);
		}
		
		//does the given node match the node at LHSNodes size-index?
		if(lrn.MatchWithUaNode(node, debug)) {
			
			//node matches, add possible references to reference map
			if(lrn.Reference != null)
				this.LHSNodeReferences.put(lrn.Reference, node.getNodeId());
			
			//reached the end of LHSNodes rule list
			if(index + 1 >= LHSNodes.size())
				return true;
			
			//get source parent node, if none found, does not match the rule
			UaReference parentRef = node.getReference(hasComponentId, true);
			if(parentRef == null)
				return false;
			
			UaNode sourceParentNode = parentRef.getSourceNode();
			
			return matchWithNode(sourceParentNode, index+1, debug);
		}
		
		//this node does not match rule
		return false;
	}
}
