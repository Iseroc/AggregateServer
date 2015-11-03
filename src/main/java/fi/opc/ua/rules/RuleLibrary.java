package fi.opc.ua.rules;

import java.util.Locale;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.core.Identifiers;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaProperty;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.nodes.BaseNode;
import com.prosysopc.ua.server.nodes.CacheProperty;
import com.prosysopc.ua.server.nodes.CacheVariable;
import com.prosysopc.ua.types.opcua.server.FolderTypeNode;

import fi.opc.ua.server.ASNodeManager;
import fi.opc.ua.server.DUaNode;

public class RuleLibrary { // TODO: static class possible in java? function class thing?
	
	// Constructor
	
	public RuleLibrary() {
		
	}
	
	
	
	// Public static methods
	
	/**
	 * 
	 * @param nm
	 * @param dispName
	 * @param entryNode
	 * @return
	 * @throws StatusException
	 */
	public static FolderTypeNode CreateFolderTypeNode(ASNodeManager nm, UaNode parentNode, LocalizedText newNodeName) throws StatusException {
		System.out.println("New folder type node, " + newNodeName);
		
		NodeId newId = new NodeId(nm.getNamespaceIndex(), UUID.randomUUID());
        FolderTypeNode mappedNode = nm.createInstance(FolderTypeNode.class, newNodeName.getText(), newId);
        nm.addNodeAndReference(parentNode, mappedNode, Identifiers.Organizes);
        return mappedNode;
	}
	
	/**
	 * 
	 * @param nm
	 * @param af
	 * @param n
	 * @param dispName
	 * @throws StatusException
	 */
	public static void AddVariable(ASNodeManager nm, FolderTypeNode af, DUaNode n, LocalizedText dispName) throws StatusException {
		NodeId newId = new NodeId(nm.getNamespaceIndex(), UUID.randomUUID());
        UaVariable mappedNode = new CacheVariable(nm, newId, new QualifiedName(nm.getNamespaceIndex(),n.getBrowseName().getName()), n.getDisplayName());
        mappedNode.setAttributes(n.getAttributes());
        mappedNode.getValue().setSourceTimestamp(((UaVariable) n.getUaNode()).getValue().getSourceTimestamp());
        ((BaseNode) mappedNode).initNodeVersion();
        nm.addNodeAndReference(af, mappedNode, Identifiers.Organizes);
	}
	
	/**
	 * 
	 * @param nm
	 * @param af
	 * @param n
	 * @param orgref
	 * @param hasComponentId
	 * @param hasTypeDefId
	 * @return
	 * @throws StatusException
	 */
	public static NodeId CreateNodeAndFlatted(ASNodeManager nm, FolderTypeNode af, DUaNode n, UaReference orgref, NodeId hasComponentId, NodeId hasTypeDefId) throws StatusException {
		UaNode parentElement = orgref.getSourceNode();
		
		//Parse grandparents displayname
		UaNode grandParentElement = parentElement.getReference(hasComponentId, true).getSourceNode();
		String parentElementDisplayName = grandParentElement.getDisplayName().getText();
		
		//Parse displayname from additional parent elements
		UaNode grandParentElementN = grandParentElement.getReference(hasComponentId, true).getSourceNode();
		while (grandParentElementN.getReference(hasTypeDefId, false).getTargetNode().getDisplayName().getText().equals("ISOBUSDeviceElementType")) {
			parentElementDisplayName = grandParentElementN.getDisplayName().getText() + "." + parentElementDisplayName;
			grandParentElementN = grandParentElementN.getReference(hasComponentId, true).getSourceNode();
		}
		
		//parse engineering unit
		QualifiedName engUnitName = new QualifiedName(0, "EngineeringUnits");
		UaProperty engUnitProperty = n.getProperty(engUnitName);
		String engUnit = "";
		if (engUnitProperty != null) {
			DataValue testi = engUnitProperty.getValue();
			int alku = testi.toString().indexOf("(en)");
			int loppu = testi.toString().indexOf("\n", alku);
			engUnit = testi.toString().substring(alku+5, loppu);
		}
		
		NodeId newId = new NodeId(nm.getNamespaceIndex(), UUID.randomUUID());
		
		LocalizedText displayName = new LocalizedText((parentElementDisplayName + "." + n.getDisplayName().getText() + ", " + engUnit), Locale.ENGLISH);
		UaVariable mappedNode = new CacheVariable(nm, newId, new QualifiedName(nm.getNamespaceIndex(), n.getBrowseName().getName()), displayName);
		mappedNode.setAttributes(n.getAttributes());
		
		//Copy required properties for the new variable. NOTE: ID:s are not currently mapped for properties since the values are unlikely to change
		QualifiedName EURangeName = new QualifiedName(0, "EURange");
		UaProperty EURangeProperty = n.getProperty(EURangeName);
		if (EURangeProperty != null) {
			NodeId EURpropertyId = new NodeId(nm.getNamespaceIndex(),UUID.randomUUID());
			UaProperty newEURProperty = new CacheProperty(nm, EURpropertyId, new QualifiedName(nm.getNamespaceIndex(),EURangeProperty.getBrowseName().getName()), EURangeProperty.getDisplayName());
			newEURProperty.setAttributes(EURangeProperty.getAttributes());
			nm.addNodeAndReference(mappedNode, newEURProperty, Identifiers.HasProperty);
		}
		
		if (engUnitProperty != null) {
			NodeId EUpropertyId = new NodeId(nm.getNamespaceIndex(),UUID.randomUUID());
			UaProperty newEUProperty = new CacheProperty(nm, EUpropertyId, new QualifiedName(nm.getNamespaceIndex(),engUnitProperty.getBrowseName().getName()), engUnitProperty.getDisplayName());
			newEUProperty.setAttributes(engUnitProperty.getAttributes());
			nm.addNodeAndReference(mappedNode, newEUProperty, Identifiers.HasProperty);
		}
		((BaseNode) mappedNode).initNodeVersion();
		nm.addNodeAndReference(af, mappedNode, Identifiers.Organizes);
		
		return newId;
	}

	public static NodeId CreateNodeAndFolders(ASNodeManager nm, FolderTypeNode targetFolder, DUaNode n, UaReference orgref, NodeId hasComponentId, NodeId hasTypeDefId, NodeId organizesId) throws StatusException {
		System.out.println();
		System.out.println("RL: CreateNodeAndFolders: Start: " + n.getDisplayName().getText());
		
		UaNode parent = FindOrCreateFolderNodePath(nm, targetFolder, n.getReference(hasComponentId, true).getSourceNode(), hasComponentId, organizesId);

		//parse engineering unit
		QualifiedName engUnitName = new QualifiedName(0, "EngineeringUnits");
		UaProperty engUnitProperty = n.getProperty(engUnitName);
		String engUnit = "";
		if (engUnitProperty != null) {
			DataValue testi = engUnitProperty.getValue();
			int alku = testi.toString().indexOf("(en)");
			int loppu = testi.toString().indexOf("\n", alku);
			engUnit = testi.toString().substring(alku+5, loppu);
		}
		
		NodeId newId = new NodeId(nm.getNamespaceIndex(), UUID.randomUUID());
		
		LocalizedText displayName = new LocalizedText((n.getDisplayName().getText() + ", " + engUnit), Locale.ENGLISH);
		UaVariable mappedNode = new CacheVariable(nm, newId, new QualifiedName(nm.getNamespaceIndex(), n.getBrowseName().getName()), displayName);
		mappedNode.setAttributes(n.getAttributes());
		
		//Copy required properties for the new variable. NOTE: ID:s are not currently mapped for properties since the values are unlikely to change
		QualifiedName EURangeName = new QualifiedName(0, "EURange");
		UaProperty EURangeProperty = n.getProperty(EURangeName);
		if (EURangeProperty != null) {
			NodeId EURpropertyId = new NodeId(nm.getNamespaceIndex(),UUID.randomUUID());
			UaProperty newEURProperty = new CacheProperty(nm, EURpropertyId, new QualifiedName(nm.getNamespaceIndex(),EURangeProperty.getBrowseName().getName()), EURangeProperty.getDisplayName());
			newEURProperty.setAttributes(EURangeProperty.getAttributes());
			nm.addNodeAndReference(mappedNode, newEURProperty, Identifiers.HasProperty);
		}
		
		if (engUnitProperty != null) {
			NodeId EUpropertyId = new NodeId(nm.getNamespaceIndex(),UUID.randomUUID());
			UaProperty newEUProperty = new CacheProperty(nm, EUpropertyId, new QualifiedName(nm.getNamespaceIndex(),engUnitProperty.getBrowseName().getName()), engUnitProperty.getDisplayName());
			newEUProperty.setAttributes(engUnitProperty.getAttributes());
			nm.addNodeAndReference(mappedNode, newEUProperty, Identifiers.HasProperty);
		}
		((BaseNode) mappedNode).initNodeVersion();
		nm.addNodeAndReference(parent, mappedNode, Identifiers.Organizes);
		
		return null;
	}
	
	private static UaNode FindOrCreateFolderNodePath(ASNodeManager nm, FolderTypeNode root, UaNode n, NodeId hasComponentId, NodeId organizesId) throws StatusException {
		UaNode parentNode = n.getReference(hasComponentId, true).getSourceNode();
		
		if(parentNode.getDisplayName().getText().equals(root.getDisplayName().getText())) {
			return root;
		}
		else {
			parentNode = FindOrCreateFolderNodePath(nm, root, parentNode, hasComponentId, organizesId);
		}
		
		System.out.println("RL: Create Folder: Parent folder: " + parentNode.getDisplayName().getText());
		
		UaReference[] children = parentNode.getReferences(organizesId, false);
//		UaNode[] children2 = parentNode.getComponents();
		
		UaNode candidate = null;
		
		for(int i = 0; i < children.length; i++) {
			candidate = children[i].getTargetNode();
			System.out.println("RL: Create Folder: Looking for " + n.getDisplayName().getText() + " from node: source: " + children[i].getSourceNode().getDisplayName().getText() + ", target: " + candidate.getDisplayName().getText());
			//System.out.println("RL: Create Folder:  Candidate " + i + ": " + candidate.getDisplayName().getText() + ", lookin for " + n.getDisplayName().getText());
			if(candidate.getDisplayName().getText().equals(n.getDisplayName().getText())) {
				return candidate;
			}
		}

		return CreateFolderTypeNode(nm, parentNode, n.getDisplayName());
	}
	
}
