/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) 2009-2010 Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package fi.opc.ua.server;

//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.EnumSet;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.AggregateFilterResult;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoringFilter;
import org.opcfoundation.ua.core.MonitoringParameters;
import org.opcfoundation.ua.core.NodeAttributes;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.UserTokenType;
import org.opcfoundation.ua.core.ViewDescription;
import org.opcfoundation.ua.utils.NumericRange;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaReferenceType;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.MonitoredDataItem;
import com.prosysopc.ua.server.NodeManagerListener;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.Subscription;

/**
 * A sample implementation of a NodeManagerListener
 */
public class ASNodeManagerListener implements NodeManagerListener {
	
	
	public class DataItem {
		private NodeId dataType = Identifiers.Double;
		private final String name;
		private StatusCode status = new StatusCode(StatusCodes.Bad_WaitingForInitialData);
		private DateTime timestamp;
		private double value;

		/**
		 * @param name
		 * @param value
		 */
		public DataItem(String name) {
			super();
			this.name = name;
		}

		/**
		 * @return the dataType
		 */
		public NodeId getDataType() {
			return dataType;
		}

		/**
		 *
		 */
		public void getDataValue(DataValue dataValue) {
			dataValue.setValue(new Variant(getValue()));
			dataValue.setStatusCode(getStatus());
			dataValue.setServerTimestamp(DateTime.currentTime());
			dataValue.setSourceTimestamp(timestamp);
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the status
		 */
		public StatusCode getStatus() {
			return status;
		}

		/**
		 * The timestamp defined when the value or status changed.
		 * 
		 * @return the timestamp
		 */
		public DateTime getTimestamp() {
			return timestamp;
		}

		/**
		 * @return the value
		 */
		public double getValue() {
			return value;
		}

		/**
		 * @param dataType
		 *            the dataType to set
		 */
		public void setDataType(NodeId dataType) {
			this.dataType = dataType;
		}

		/**
		 * @param value
		 *            the value to set
		 */
		public void setValue(double value) {
			setValue(value, StatusCode.GOOD);
		}

		/**
		 * @param value
		 *            the value to set
		 * @param status
		 *            the status to set
		 */
		public void setValue(double value, StatusCode status) {
			if (status == null)
				status = StatusCode.BAD;
			if ((this.value != value) || !this.status.equals(status)) {
				this.value = value;
				this.status = status;
				this.timestamp = DateTime.currentTime();
			}
		}
	}
	
	//private final Map<String, Collection<MonitoredDataItem>> monitoredItems = new ConcurrentHashMap<String, Collection<MonitoredDataItem>>();
	//private List<DataItem> dataItems = new ArrayList<DataItem>();
	private HashMap<Integer, TargetServer> clientMap = new HashMap<Integer, TargetServer>(100);
	private HashMap<UaNode, EnumSet<AccessLevel>> nodeAccessLevels = new HashMap<UaNode, EnumSet<AccessLevel>>(1000);
	
	@Override
	public void onAddNode(ServiceContext serviceContext, NodeId parentNodeId,
			UaNode parent, NodeId nodeId, UaNode node, NodeClass nodeClass,
			QualifiedName browseName, NodeAttributes attributes,
			UaReferenceType referenceType, ExpandedNodeId typeDefinitionId,
			UaNode typeDefinition) throws StatusException {
		// Notification of a node addition request.
		// Note that NodeManagerTable#setNodeManagementEnabled(true) must be
		// called to enable these methods.
		// Anyway, we just check the user access.
		checkUserAccess(serviceContext);
	}

	@Override
	public void onAddReference(ServiceContext serviceContext,
			NodeId sourceNodeId, UaNode sourceNode,
			ExpandedNodeId targetNodeId, UaNode targetNode,
			NodeId referenceTypeId, UaReferenceType referenceType,
			boolean isForward) throws StatusException {
		// Notification of a reference addition request.
		// Note that NodeManagerTable#setNodeManagementEnabled(true) must be
		// called to enable these methods.
		// Anyway, we just check the user access.
		checkUserAccess(serviceContext);
	}

	@Override
	public void onAfterCreateMonitoredDataItem(ServiceContext serviceContext,
			Subscription subscription, MonitoredDataItem item) {

	}
	
	public void notifyMonitoredDataItems() {
		
	}
	
	/*
	 * Remove subscription and return the aggregating nodes access level to what it was before the subscription
	 */
	@Override
	public void onAfterDeleteMonitoredDataItem(ServiceContext serviceContext,
			Subscription subscription, MonitoredDataItem item) {
		
		UaNode node = item.getNode();
		int ns = node.getNodeId().getNamespaceIndex();
		TargetServer ts = clientMap.get(ns);
		
		if (ts != null) {
			
			NodeId remoteId = ts.getNodeManager().getIdMap().get(node.getNodeId());
			if (remoteId != null) {
				
				try {
					ts.client.removeSubscription(remoteId, node.getNodeId());
				} catch (ServiceException | StatusException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (node.supportsAttribute(Attributes.AccessLevel)) {
					((UaVariable) node).setAccessLevel(nodeAccessLevels.get(node));
				}
			}
		}
	}

	@Override
	public void onAfterModifyMonitoredDataItem(ServiceContext serviceContext,
			Subscription subscription, MonitoredDataItem item) {
		//
	}

	@Override
	public boolean onBrowseNode(ServiceContext serviceContext,
			ViewDescription view, NodeId nodeId, UaNode node,
			UaReference reference) {
		// Perform custom filtering, for example based on the user
		// doing the browse. The method is called separately for each reference.
		// Default is to return all references for everyone
		return true;
	}

	/*
	 * Relays a subscription from the aggregating node to the underlying node
	 */
	@Override
	public void onCreateMonitoredDataItem(ServiceContext serviceContext,
			Subscription subscription, UaNode node,
			UnsignedInteger attributeId, NumericRange indexRange,
			MonitoringParameters params, MonitoringFilter filter,
			AggregateFilterResult filterResult) throws StatusException {
		// Notification of a monitored item creation request
		int ns = node.getNodeId().getNamespaceIndex();
		TargetServer ts = clientMap.get(ns);
		
		System.out.println("### onCreateMonitoredDataItem called for node " + node + " ###");
		
		if (ts != null) {
			NodeId remoteId = ts.getNodeManager().getIdMap().get(node.getNodeId());
			if (remoteId != null) {
				ts.client.relaySubscription(remoteId, attributeId);
				ts.client.storeMonitoredIdPair(remoteId, node.getNodeId());
				if (node.supportsAttribute(Attributes.AccessLevel)) {
					nodeAccessLevels.put(node, ((UaVariable)node).getAccessLevel());
					((UaVariable) node).setAccessLevel(AccessLevel.READWRITE);				
				}
				System.out.println("Subscription realyed to underlying node");
				
			}
		}

		// You may, for example start to monitor the node from a physical
		// device, only once you get a request for it from a client
	}

	@Override
	public void onDeleteMonitoredDataItem(ServiceContext serviceContext,
			Subscription subscription, MonitoredDataItem monitoredItem) {
		// Notification of a monitored item delete request
	}

	@Override
	public void onDeleteNode(ServiceContext serviceContext, NodeId nodeId,
			UaNode node, boolean deleteTargetReferences) throws StatusException {
		// Notification of a node deletion request.
		// Note that NodeManagerTable#setNodeManagementEnabled(true) must be
		// called to enable these methods.
		// Anyway, we just check the user access.
		checkUserAccess(serviceContext);
	}

	@Override
	public void onDeleteReference(ServiceContext serviceContext,
			NodeId sourceNodeId, UaNode sourceNode,
			ExpandedNodeId targetNodeId, UaNode targetNode,
			NodeId referenceTypeId, UaReferenceType referenceType,
			boolean isForward, boolean deleteBidirectional)
			throws StatusException {
		// Notification of a reference deletion request.
		// Note that NodeManagerTable#setNodeManagementEnabled(true) must be
		// called to enable these methods.
		// Anyway, we just check the user access.
		checkUserAccess(serviceContext);
	}

	@Override
	public void onGetReferences(ServiceContext serviceContext,
			ViewDescription viewDescription, NodeId nodeId, UaNode node,
			List<UaReference> references) {
		// Add custom references that are not defined in the nodes here.
		// Useful for non-UaNode-based node managers - or references.
	}

	@Override
	public void onModifyMonitoredDataItem(ServiceContext serviceContext,
			Subscription subscription, MonitoredDataItem item, UaNode node,
			MonitoringParameters params, MonitoringFilter filter,
			AggregateFilterResult filterResult) {
		// Notification of a monitored item modification request
	}

	private void checkUserAccess(ServiceContext serviceContext)
			throws StatusException {
		// Do not allow for anonymous users
		if (serviceContext.getSession().getUserIdentity().getType()
				.equals(UserTokenType.Anonymous))
			throw new StatusException(StatusCodes.Bad_UserAccessDenied);
	}
	
	public void updateIdMap(TargetServer ts) {
		int ns = ts.getNodeManager().getNamespaceIndex();
		if (clientMap.get(ns) == null) {
			clientMap.put(ns, ts);
		}
	}
};
