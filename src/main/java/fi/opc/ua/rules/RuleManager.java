package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.List;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.AddressSpaceException;

public class RuleManager {
	
	private List<RuleSet> ruleSets = new ArrayList<RuleSet>();
	
	public RuleManager() {
	
	}
	
	public void ReadRuleFile(String filename) {
		RuleSet rs = new RuleSet("BoilerServer");
		Rule r = new Rule("[BoilerType]#1/[ControllerType]#2","#1/#2","Deep copy");
		Rule r2 = new Rule("[BoilerType]#1/[PipeType]/[FTType]#2/DataItem#3","#1/#3(DisplayName=#2@DisplayName)","Copy");
		Rule r3 = new Rule("[BoilerType]#1/[DrumType]/[LIType]#2/DataItem#3","#1/#3(DisplayName=#2@DisplayName)","Copy");
		rs.AddRule(r);
		rs.AddRule(r2);
		rs.AddRule(r3);
		ruleSets.add(rs);
	}
	
	public List<RuleSet> GetRuleSets() {
		return ruleSets;
	}
	
	public List<MatchingRule> MatchRules(NodeId nodeId, AddressSpace as) throws ServiceException, AddressSpaceException {
		//TODO: check if address space matches a rule set 
		RuleSet matchingSet = ruleSets.get(0);
		
		List<MatchingRule> matchingRules = new ArrayList<MatchingRule>();
		MatchingRule mRule = null;
		
		for(Rule rule : matchingSet.GetRuleList()) {
			mRule = MatchingRule.MatchRule(rule, nodeId, as);
			if(mRule != null)
				matchingRules.add(mRule);
		}
		
		return matchingRules;
	}
}
