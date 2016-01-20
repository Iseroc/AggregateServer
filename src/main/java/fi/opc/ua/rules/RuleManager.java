package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.List;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.common.NamespaceTable;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.AddressSpaceException;

public class RuleManager {
	
	private List<RuleSet> ruleSets = new ArrayList<RuleSet>();
	
	public RuleManager() {
	
	}
	
	public void ReadRuleFile(String filename) {
		RuleSet rs = new RuleSet("BoilerServer");
		
		//Boiler rules
//		Rule r = new Rule("[BoilerType]#1/[ControllerType]#2", "#1/#2", "Deep copy");
//		Rule r2 = new Rule("[BoilerType]#1/[PipeType]/[FTType]#2/DataItem#3", "#1/#3(BrowseName=#2@BrowseName,DisplayName=#2@DisplayName)", "Copy");
//		Rule r3 = new Rule("[BoilerType]#1/[DrumType]/[LIType]#2/DataItem#3", "#1/#3(BrowseName=#2@BrowseName,DisplayName=#2@DisplayName)", "Copy");
//		Rule r2 = new Rule("[BoilerType]#1/[PipeType]/[FTType]#2/DataItem#3", "#1/#3(BrowseName={#2@BrowseName}.{#3@BrowseName},DisplayName={#2@DisplayName}.{#3@DisplayName})", "Copy");
//		Rule r3 = new Rule("[BoilerType]#1/[DrumType]/[LIType]#2/DataItem#3", "#1/#3(BrowseName={#2@BrowseName}.{#3@BrowseName},DisplayName={#2@DisplayName}.{#3@DisplayName})", "Copy");
		
		//Transformed Boiler rules
		Rule r = new Rule("[BoilerType]#1/[ControllerType]#2", "#1/#2", "Deep copy");
		Rule r2 = new Rule("[BoilerType]#1/FT1001#2", "#1/[PipeType]Pipe1001/[FTType]FT1001/#2(BrowseName=DataItem,DisplayName=DataItem)", "Copy");
		Rule r3 = new Rule("[BoilerType]#1/LI1001#2", "#1/[DrumType]Drum1001/[LIType]LI1001/#2(BrowseName=DataItem,DisplayName=DataItem)", "Copy");
		Rule r4 = new Rule("[BoilerType]#1/LI1001#2", "#1/[PipeType]Pipe1002/[FTType]FT1002/#2(BrowseName=DataItem,DisplayName=DataItem)", "Copy");
		
		rs.AddRule(r);
		rs.AddRule(r2);
		rs.AddRule(r3);
		rs.AddRule(r4);
		
		ruleSets.add(rs);

		//ISOUS rules
		RuleSet rs2 = new RuleSet("UaDemoserver");
		Rule r5 = new Rule("[ISOBUSDeviceType]#1/[ISOBUSDeviceElementType]#2/ParameterSet/#3", "Tractors/#1/#2/#3(DisplayName={#2@DisplayName}.{#3@DisplayName} {#3@EngineeringUnits})", "Copy");
		rs2.AddRule(r5);
		
		ruleSets.add(rs2);
	}
	
	public List<RuleSet> GetRuleSets() {
		return ruleSets;
	}
	
	public List<MatchingRule> MatchRules(NodeId nodeId, AddressSpace as) throws ServiceException, AddressSpaceException {
		//TODO: check if address space matches a rule set 
		RuleSet matchingSet = new RuleSet("");
		
		NamespaceTable table = as.getNamespaceTable();
		String uri = table.toArray()[1];
		
		for(RuleSet set : ruleSets) {
			if(uri.contains(set.ServerUri)) {
				matchingSet = set; 
				break;
			}
		}
		
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
