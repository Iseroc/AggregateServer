package fi.opc.ua.rules;

import java.util.ArrayList;
import java.util.List;

public class RuleManager {
	
	private List<RuleSet> rules = new ArrayList<RuleSet>();
	
	public RuleManager() {
	
	}
	
	public void ReadRuleFile(String filename) {
		RuleSet rs = new RuleSet("URI");
		Rule r = new Rule("LHS","RHS","Deep copy");
		rs.AddRule(r);
		rules.add(rs);
	}
	
	public List<Rule> GetRules() {
		return rules;
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
