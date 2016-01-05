package fi.opc.ua.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeAttributes;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReferenceDescription;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaObjectType;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.nodes.BaseNode;
import com.prosysopc.ua.server.nodes.CacheVariable;
import com.prosysopc.ua.types.opcua.FolderType;
import com.prosysopc.ua.types.opcua.server.FolderTypeNode;

import fi.opc.ua.client.AggregateServerConsoleClient;
import fi.opc.ua.rules.MatchingRule;
import fi.opc.ua.rules.RHSRuleNode;
import fi.opc.ua.rules.RuleAttribute;
import fi.opc.ua.rules.RuleManager;
import fi.opc.ua.rules.LHSRuleNode;

public class MappingEngine {

	private static final String RULE_FILE = "/src/main/resources/rules/rulefile.txt";
	
	private RuleManager ruleManager;

	private NodeId hasComponentId = new NodeId(0,47);
	private NodeId OrganizesId = new NodeId(0,35);
	private NodeId hasTypeDefId = new NodeId(0,40);
	
	private List<NodeId> loopedIds = new ArrayList<NodeId>();
	
	public void Initialize() {
		ruleManager = new RuleManager();
		ruleManager.ReadRuleFile(RULE_FILE);
	}
	
	public void MapAddressSpace(TargetServer ts) throws ServiceException, StatusException, ServiceResultException {
		NodeId root = Identifiers.ObjectsFolder;
		
		List<NodeId> idList = new ArrayList<NodeId>();
		
		AddressSpace sourceAddressSpace = ts.getTargetServerAddressSpace();
		sourceAddressSpace.setReferenceTypeId(Identifiers.HierarchicalReferences);
		sourceAddressSpace.setBrowseDirection(BrowseDirection.Forward);
		
		NodeId myObjectsNodeId = null;
		List<ReferenceDescription> refs = sourceAddressSpace.browse(root);
		for(ReferenceDescription ref : refs) {
			if(ref.getBrowseName().getName().equals("MyObjects"))
				myObjectsNodeId = sourceAddressSpace.getNamespaceTable().toNodeId(ref.getNodeId());
		}
		
		if(myObjectsNodeId != null)
			browseAndMapNode(myObjectsNodeId, sourceAddressSpace, idList, ts);
		
		//Update IdMap for IOListener
		ts.getNodeManager().getCustomIOListener().updateIdMap(ts);
		//((ASNodeManagerListener) myNodeManagerListener).updateIdMap(ts);
		ts.client.updateIdMap(ts);
		
		System.out.println("Done mapping address space: " + ts.nm.getNamespaceUri());
		System.out.println();
	}
	
	//recursively browse and map node and all it's children
	private void browseAndMapNode(NodeId nodeId, AddressSpace sourceAddressSpace, List<NodeId> idList, TargetServer ts) {
		idList.add(nodeId);
		System.out.println("Browsing node " + nodeId.toString());
		try {
			//map current node
			mapNode(nodeId, sourceAddressSpace, ts);
			
			List<ReferenceDescription> references = sourceAddressSpace.browse(nodeId);
			
			//recur browse-and-map method for each children
			for(ReferenceDescription ref : references) {
				NodeId currentId = sourceAddressSpace.getNamespaceTable().toNodeId(ref.getNodeId());

				browseAndMapNode(currentId, sourceAddressSpace, idList, ts);
			}
		}
		catch (Exception e) {
			System.out.println("Problem mapping node with NodeId " + nodeId);
			System.err.println(e.toString());
			e.printStackTrace();
			if (e.getCause() != null)
				System.err.println("Caused by: " + e.getCause());
		}
	}
	
	private void mapNode(NodeId nodeId, AddressSpace sourceAddressSpace, TargetServer ts) throws ServiceException, AddressSpaceException, StatusException {
		List<MatchingRule> matchingRules = ruleManager.MatchRules(nodeId, sourceAddressSpace);
		System.out.println("Number of matching rules: " + matchingRules.size());
		for(MatchingRule mRule : matchingRules) {
			executeRule(mRule, ts);
		}
		
	}
	
	private void executeRule(MatchingRule mRule, TargetServer ts) throws StatusException, ServiceException, AddressSpaceException {
		FolderType entryNode = ts.getNodeManager().getServer().getNodeManagerRoot().getObjectsFolder();
		
		UaNode currentNode = entryNode;
		
		//find or build the tree of nodes from RHSNodes
		for(RHSRuleNode rNode : mRule.RHSNodes) {
			System.out.println("*** Mapping RHSNode [" + rNode.Type + "]" + rNode.Name + "#" + rNode.Reference + " ***");
			
			//get all component children of current node, check if any one of them matches the rNode
			UaNode matchingNode = findChildNode(rNode, currentNode, ts);
			
			if(matchingNode != null) {
				currentNode = matchingNode;
			}
			else {
				System.out.println("Creating a new object node with a reference: " + rNode.MatchingNodeId);
				//create a node to match rNode
				UaNode sourceNode = ts.getTargetServerAddressSpace().getNode(rNode.MatchingNodeId);
				
				UaNode mappedNode = copyNode(rNode, sourceNode, currentNode, ts);
				
		        currentNode = mappedNode;
			}
	        System.out.println("*** Node mapped ***");
	        System.out.println();
		}
		
		//If the rule is a "deep copy", create all children nodes of the last rule node
		if(mRule.OriginalRule.Type.toLowerCase().equals("deep copy")) {
			System.out.println("*** Mapping children nodes for Deep Copy ***");
			
			LHSRuleNode lastRuleNode = mRule.LHSNodes.get(mRule.LHSNodes.size() - 1);
			UaNode sourceNode = ts.getTargetServerAddressSpace().getNode(lastRuleNode.MatchingNodeId);
			
			deepCopyNodes(sourceNode, currentNode, ts);
			
			System.out.println("*** Mapping children nodes done ***");
	        System.out.println();
		}
	}
	
	//recursive method for deep copying from 'sourceNode' to 'parent'
	private UaNode deepCopyNodes(UaNode sourceNode, UaNode parentNode, TargetServer ts) throws StatusException, ServiceException, AddressSpaceException {
		UaNode mappedNode = null;
		
		UaNode[] components = sourceNode.getComponents();
		
		for(UaNode component : components) {
			mappedNode = copyNode(new RHSRuleNode(""), component, parentNode, ts);
			deepCopyNodes(component, mappedNode, ts);
		}
		
		return mappedNode;
	}
	
	private UaNode findChildNode(RHSRuleNode rNode, UaNode parentNode, TargetServer ts) throws StatusException, ServiceException, AddressSpaceException {
		UaNode refNode, matchingNode = null;
		UaReference[] references = parentNode.getReferences();
		
		for(UaReference ref : references) {
			refNode = ref.getOppositeNode(parentNode);
			if(rNode.MatchWithUaNode(refNode, ts)) {
				System.out.println("Found matching node: " + refNode.getBrowseName().getName());
				matchingNode = refNode;
				break;
			}
		}
		
		return matchingNode;
	}
	
	private UaNode copyNode(RHSRuleNode rNode, UaNode sourceNode, UaNode parentNode, TargetServer ts) throws StatusException, ServiceException, AddressSpaceException {
		UaNode mappedNode = null;
		ASNodeManager nm = ts.getNodeManager();

		String browseName = rNode.Name;
		String displayName = rNode.Name;
		String typeName = rNode.Type;
		
		//if this has a source node, fill out the missing information
		if(sourceNode != null) {
			if(browseName == null)
				browseName = sourceNode.getBrowseName().getName();
			
			if(displayName == null)
				displayName = sourceNode.getDisplayName().getText();
			
			//get or create the node type
			UaReference typeReference = sourceNode.getReference(hasTypeDefId, false);
			if(typeName == null && typeReference != null)
				typeName = typeReference.getTargetNode().getBrowseName().getName();
		}
		
		UaObjectType nodeType = createOrGetObjectTypeNode(typeName, ts);

		//if this has a source node, create a copy of the source node
		if(sourceNode != null) {
			//map object node
			if(sourceNode.getNodeClass() == NodeClass.Object) {
				mappedNode = nm.CreateComponentObjectNode(browseName, displayName, nodeType, parentNode);
				nm.InsertMappedNode(mappedNode.getNodeId(), sourceNode.getNodeId());
			}
			
			//map variable node
			if(sourceNode.getNodeClass() == NodeClass.Variable) {
				UaVariable varNode = (UaVariable)sourceNode;
				mappedNode = nm.CreateComponentVariableNode(browseName, displayName, nodeType, varNode.getDataTypeId(), varNode.getValue(), parentNode);
				nm.InsertMappedNode(mappedNode.getNodeId(), sourceNode.getNodeId());
			}

			//copy attribute table
			NodeAttributes nodeAttrs = sourceNode.getAttributes().clone();
			mappedNode.setAttributes(nodeAttrs);
		}
		else { //create a completely new node - always an object node
			mappedNode = nm.CreateComponentObjectNode(browseName, displayName, nodeType, parentNode);
		}
		
		//add rest of the attributes and other additional values from rNode
		for(RuleAttribute rAttr : rNode.Attributes) {
			//figure out the attribute value
			Object value = rAttr.Value;
			if(rAttr.Reference != null) {
				UaNode attributeSourceNode = ts.getTargetServerAddressSpace().getNode(rAttr.MatchingNodeId);
				switch(rAttr.ReferenceAttributeName) {
					case("DisplayName"):
						value = attributeSourceNode.getDisplayName().getText();
						break;
					case("BrowseName"):
						value = attributeSourceNode.getBrowseName().getName();
						break;
					default:
						break;
				}
			}
			
			switch(rAttr.AttributeName) {
				case("DisplayName"):
					mappedNode.setDisplayName(new LocalizedText((String)value, Locale.ENGLISH));
					break;
				case("BrowseName"):
					mappedNode.setBrowseName(new QualifiedName(nm.getNamespaceIndex(), (String)value));
					break;
				default:
					break;
			}
		}
        
		return mappedNode;
	}
	
	private UaObjectType createOrGetObjectTypeNode(String name, TargetServer ts) throws StatusException {
		ASNodeManager nm = ts.getNodeManager();

		UaObjectType type = nm.ContainsObjectType(name);
		
		if(type == null)
			type = nm.CreateObjectTypeNode(name);
		
		return type;
	}
	
	private UaVariable createVariableNode(UaNode sourceNode, UaNode targetParent, ASNodeManager nm) throws StatusException {
		NodeId newId = new NodeId(nm.getNamespaceIndex(), UUID.randomUUID());
		
		LocalizedText displayName = new LocalizedText((sourceNode.getDisplayName().getText()), Locale.ENGLISH);
		UaVariable mappedNode = new CacheVariable(nm,
				newId,
				new QualifiedName(nm.getNamespaceIndex(), sourceNode.getBrowseName().getName()),
				displayName);
		mappedNode.setAttributes(sourceNode.getAttributes());
		
		//TODO: set properties
		
		((BaseNode) mappedNode).initNodeVersion();
		
		//add a references from the target parent node to the new target variable node
		nm.addNodeAndReference(targetParent, mappedNode, Identifiers.Organizes);
		
		return mappedNode;
	}
	
	//*** OLD ***
	
	/*
	 * This method browses recursively through the address space searching for nodes which are defined as mappable types. When a node is
	 * found, all child nodes of that node are separately grouped with groupDeviceChildElements. The resulting node set is then passed to the
	 * rule engine as many times as the number of agenda groups defined for that specific mappable type Id. GroupDeviceChildElements also returns
	 * a list of any additional mappable nodes that may be found from the children of the first mappable node. The children of these additional
	 * nodes are then grouped with groupDeviceChildElementsLocalList, which is basically the same as groupDeviceChildElements, but it uses a local
	 * IdMap to avoid mix ups. All of these additional groups are then passed to the rule engine in the same manner as the first group returned by
	 * groupDeviceChildElements.
	 */
	protected void copyAddressSpace(NodeId nodeId, List<MappableType> mappableTypes, TargetServer ts) {
		AddressSpace sourceAddressSpace = ts.getTargetServerAddressSpace();
		sourceAddressSpace.setReferenceTypeId(Identifiers.HierarchicalReferences);
		sourceAddressSpace.setBrowseDirection(BrowseDirection.Forward);
		loopedIds.add(nodeId);
		ReferenceDescription ref;
		List<NodeId> mappedAdditionals = new ArrayList<NodeId>();
		List<NodeId> loopedIdsL = new ArrayList<NodeId>();
		List<NodeId> types = new ArrayList<NodeId>();
		for (int a = 0; a < mappableTypes.size(); a++) {
			types.add(mappableTypes.get(a).getType());
		}
		Stack<NodeId> additionalMappableIds = new Stack<NodeId>();
		
		try {
			List<ReferenceDescription> references = sourceAddressSpace.browse(nodeId);
			for (int i = 0; i < references.size(); i++)
			{
				ref = references.get(i);
				try {
					NodeId currentId = sourceAddressSpace.getNamespaceTable().toNodeId(ref.getNodeId());
					if (!loopedIds.contains(currentId))
					{
						loopedIds.add(currentId);
						//Check if the node's type definition is defined as mappable
						if(types.contains(sourceAddressSpace.getTypeDefinition(currentId)))
						{
							List<UaNode> childListArgument = new ArrayList<UaNode>();
							childListArgument.add(sourceAddressSpace.getNode(currentId));
							additionalMappableIds.clear();
							
							//Get child nodes for the current node and pass them all to the rule engine
							List<UaNode> devChildren = groupDeviceChildElements(sourceAddressSpace, currentId, childListArgument, additionalMappableIds, types, sourceAddressSpace.getTypeDefinition(currentId));
							for (MappableType current : mappableTypes) {
								System.out.println("current type Id: " + sourceAddressSpace.getTypeDefinition(currentId).getNamespaceIndex() + "," + sourceAddressSpace.getTypeDefinition(currentId).getValue().toString());
								System.out.println("mappableId: " + current.getType().getNamespaceIndex() + "," + current.getType().getValue().toString());
								if (current.getType().equals(sourceAddressSpace.getTypeDefinition(currentId))) {
									ts.getNodeManager().checkRulesForDevice(sourceAddressSpace, currentId, devChildren, current.getAgenda());
								}
							}
							
							devChildren.clear();
							
							//Check whether additional mappable nodes were found, group them and pass them to the rule engine
							while (!additionalMappableIds.empty()) {
								devChildren.clear();
								NodeId mid = additionalMappableIds.pop();
								mappedAdditionals.add(mid);
								System.out.println("Found additional mappable type: " + sourceAddressSpace.getNode(mid).getDisplayName().getText());
								
								childListArgument.clear();
								childListArgument.add(sourceAddressSpace.getNode(mid));
								loopedIdsL.clear();
								loopedIdsL.add(mid);
								devChildren = groupDeviceChildElementsLocalList(sourceAddressSpace, mid, childListArgument, additionalMappableIds, types, loopedIdsL, mappedAdditionals, sourceAddressSpace.getTypeDefinition(mid));
								loopedIdsL.clear();
								
								for (MappableType current : mappableTypes) {
									System.out.println("current type Id, additional: " + sourceAddressSpace.getTypeDefinition(mid).getNamespaceIndex() + "," + sourceAddressSpace.getTypeDefinition(mid).getValue().toString());
									System.out.println("mappableId: " + current.getType().getNamespaceIndex() + "," + current.getType().getValue().toString());
									if (current.getType().equals(sourceAddressSpace.getTypeDefinition(mid))) {
										ts.getNodeManager().checkRulesForDevice(sourceAddressSpace, mid, devChildren, current.getAgenda());
									}
								}					
							}
						}
						copyAddressSpace(currentId, mappableTypes, ts);
					}
				} catch (ServiceResultException e) {
					e.printStackTrace();
				} catch (AddressSpaceException e) {
					
				}
			}
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (StatusException e) {
			e.printStackTrace();
		}
		
		//Update IdMap for IOListener
		ts.getNodeManager().getCustomIOListener().updateIdMap(ts);
		//((ASNodeManagerListener) myNodeManagerListener).updateIdMap(ts);
		ts.client.updateIdMap(ts);
	}

	/*
	 * Groups child elements for nodes which have a type definition that is defined as mappable
	 */
	private List<UaNode> groupDeviceChildElements(AddressSpace AP, NodeId ID, List<UaNode> deviceChildren, Stack<NodeId> additionalMappingRequired, List<NodeId> allMappableTypes, ExpandedNodeId initialType) throws ServiceException, AddressSpaceException {
		AP.setReferenceTypeId(Identifiers.HierarchicalReferences);
		AP.setBrowseDirection(BrowseDirection.Forward);
		ReferenceDescription ref;
		try {
			List<ReferenceDescription> references = AP.browse(ID);
			for (int i = 0; i < references.size(); i++) {
				ref = references.get(i);
				NodeId currentId = AP.getNamespaceTable().toNodeId(ref.getNodeId());
				if (!loopedIds.contains(currentId))	{
					loopedIds.add(currentId);
					deviceChildren.add(AP.getNode(currentId));
				}
				if(allMappableTypes.contains(AP.getTypeDefinition(currentId)) && !AP.getTypeDefinition(currentId).equals(initialType)) {
					additionalMappingRequired.push(currentId);
				}		
				groupDeviceChildElements(AP, currentId, deviceChildren, additionalMappingRequired, allMappableTypes, initialType);
			}
		} catch (StatusException e) {
			e.printStackTrace();
		} catch (ServiceResultException e) {
			e.printStackTrace();
		}
		return deviceChildren;
	}

	/*
	 * Groups the children of additional mappable nodes that are found as children of the first mappable node found by copyAddressSpace
	 */
	private List<UaNode> groupDeviceChildElementsLocalList(AddressSpace AP, NodeId ID, List<UaNode> deviceChildren, Stack<NodeId> additionalMappingRequired, List<NodeId> allMappableTypes, List<NodeId> loopedIdsL, List<NodeId> alreadyMapped, ExpandedNodeId initialType) throws ServiceException, AddressSpaceException {	
		AP.setReferenceTypeId(Identifiers.HierarchicalReferences);
		AP.setBrowseDirection(BrowseDirection.Forward);
		ReferenceDescription ref;
		try {
			List<ReferenceDescription> references = AP.browse(ID);
			for (int i = 0; i < references.size(); i++) {
				ref = references.get(i);
				NodeId currentId = AP.getNamespaceTable().toNodeId(ref.getNodeId());
				if (!loopedIdsL.contains(currentId))	{
					loopedIdsL.add(currentId);
					deviceChildren.add(AP.getNode(currentId));
				}
				groupDeviceChildElementsLocalList(AP, currentId, deviceChildren, additionalMappingRequired, allMappableTypes, loopedIdsL, alreadyMapped, initialType);
			}
		} catch (StatusException e) {
			e.printStackTrace();
		} catch (ServiceResultException e) {
			e.printStackTrace();
		}
		return deviceChildren;
	}

	/*
	 * This is called whenever an aggregated server is mapped again from the server list to avoid duplicate nodes.
	 */
	protected void deleteNodesByNameSpaceIndex(UaServer uaServer, ASNodeManager nm, AggregateServerConsoleClient c, NodeId id) {
		loopedIds.clear();
		List<NodeId> nodesToBeDeleted = new ArrayList<NodeId>();
		getNodesToBeDeleted(nm, c, id, nodesToBeDeleted);
		for (NodeId deleteid : nodesToBeDeleted) {
			uaServer.getNodeManagerRoot().beginModelChange();
			try {
				nm.deleteNode(deleteid, true, false);
			} catch (StatusException e) {
				e.printStackTrace();
			}
			uaServer.getNodeManagerRoot().endModelChange();
		}	
	}

	/*
	 * This is called by deleteNodesByNameSpaceIndex to browse the address space for all nodes with the specified namespace-
	 * index, which are then deleted.
	 */
	protected void getNodesToBeDeleted(ASNodeManager nm, AggregateServerConsoleClient c, NodeId id, List<NodeId> nodesToBeDeleted) {
		int ns = nm.getNamespaceIndex();
		AddressSpace AP = c.client.getAddressSpace();
		ReferenceDescription ref;
		try {
			List<ReferenceDescription> references = AP.browse(id);
			for (int i = 0; i < references.size(); i++)	{
				ref = references.get(i);
				try {
					NodeId currentId = AP.getNamespaceTable().toNodeId(ref.getNodeId());
					if (!loopedIds.contains(currentId))	{
						loopedIds.add(currentId);
						if (currentId.getNamespaceIndex() == ns) {
							nodesToBeDeleted.add(currentId);
						}
						getNodesToBeDeleted(nm, c, currentId, nodesToBeDeleted);
					}			
				} catch (ServiceResultException e) {
					e.printStackTrace();
				}
			}
		} catch (ServiceException | StatusException e) {
			e.printStackTrace();
		}	
	}

}
