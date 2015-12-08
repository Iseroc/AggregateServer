package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.List;

import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.client.AddressSpace;

public class RuleManager {
	
	private List<RuleSet> ruleSets = new ArrayList<RuleSet>();
	
	public RuleManager() {
	
	}
	
	public void ReadRuleFile(String filename) {
		RuleSet rs = new RuleSet("BoilerServer");
		Rule r = new Rule("[BoilerType]#1/[ControllerType]","#1/","Deep copy");
		Rule r2 = new Rule("[BoilerType]#1/[PipeType]/[SensorType]#2/DataItem#3","#1/#2","Copy");
		Rule r3 = new Rule("[BoilerType]#1/[DrumType]/[SensorType]#2/DataItem","#1/#3{DisplayName=#2@DisplayName}","Copy");
		rs.AddRule(r);
		rs.AddRule(r2);
		rs.AddRule(r3);
		ruleSets.add(rs);
	}
	
	public List<RuleSet> GetRuleSets() {
		return ruleSets;
	}
	
	public List<MatchingRule> MatchRules(NodeId nodeId, AddressSpace as) {
		//TODO: check if address space matches a rule set 
		RuleSet matchingSet = ruleSets.get(0);
		
		List<MatchingRule> matchingRules = new ArrayList<MatchingRule>();
		
		for(Rule r : matchingSet.GetRuleList()) {
			String[] nodes = r.LHS.split("/");
			
			List<RuleNode> ruleNodes = new ArrayList<RuleNode>();
			for(String s : nodes) {
				ruleNodes.add(new RuleNode(s));
			}
			
		}
		
		return matchingRules;
	}
	
	/*
	 * Read mappable types from the rule file. Agenda-groups and mappable type Ids are paired, so that when any mappable node
	 * is found, only the correct agenda-group is fired. It is possible to define multiple mappable Ids to the same agenda-
	 * group in the rulefile as separate lines. The "general" agenda-groups are used to fire certain rules on every server,
	 * regardless of what namespaceuris the server contains.
	 *
	public List<MappableType> ReadRuleFile(String filename, String[] uriArray) throws Exception {
		List<MappableType> resultList = new ArrayList<MappableType>();
		List<String> nameSpaceUris = Arrays.asList(uriArray);
		List<String> agendaGroups = new ArrayList<String>();

		try {
			//Read mappable types from file
			String filePath = new File("").getAbsolutePath();
			filePath = filePath.concat(filename);
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String sCurrentLine;
			boolean typesFound = false;
			List<NodeId> typesToBeMapped = new ArrayList<NodeId>();
			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.startsWith("//Mappable types:")) {
					typesFound = true;
					sCurrentLine = br.readLine();
				}
				if (sCurrentLine.startsWith("//End of mappable types")) {
					break;
				}
				if (typesFound == true) { //THIS LINE CHANGED FROM = TO == *** TEST IT
					sCurrentLine = sCurrentLine.replaceFirst("//", "");
					String[] idStrings = sCurrentLine.split(",");
					int nsindex = Integer.parseInt(idStrings[0]);
					int id = Integer.parseInt(idStrings[1]);
					String nsuri = idStrings[2];
					if (nameSpaceUris.contains(nsuri) || nsuri.equals("general") || nsuri.equals("general2") || nsuri.equals("general3")) {
						NodeId mappableId = new NodeId(nsindex,id);
						typesToBeMapped.add(mappableId);
						agendaGroups.add(nsuri);
					}
				}
			}
			br.close();
			for (int i = 0; i < typesToBeMapped.size(); i++) {
				MappableType mtype = new MappableType(typesToBeMapped.get(i), agendaGroups.get(i));
				resultList.add(mtype);
			}
		} catch (Exception e) {
			throw e;
		}
		
		return resultList;
	}
	*/
}
