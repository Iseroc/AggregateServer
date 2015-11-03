/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) 2009-2010 Prosys PMS Ltd., <http://www.prosysopc.com>. 
 * All rights reserved.
 */
package fi.opc.ua.server;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.WriteAccess;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.io.IoManagerListener;

import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.utils.NumericRange;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * A sample implementation of a {@link IoManagerListener}
 */
public class ASIoManagerListener implements IoManagerListener {
	private static Logger logger = Logger.getLogger(ASIoManagerListener.class);

	private HashMap<Integer, TargetServer> clientMap = new HashMap<Integer, TargetServer>(100);

	@Override
	public EnumSet<AccessLevel> onGetUserAccessLevel(
			ServiceContext serviceContext, NodeId nodeId, UaVariable node) {
		// The AccessLevel defines the accessibility of the Variable.Value
		// attribute

		// Define anonymous access
		// if (serviceContext.getSession().getUserIdentity().getType()
		// .equals(UserTokenType.Anonymous))
		// return EnumSet.noneOf(AccessLevel.class);
		if (node.getHistorizing())
			return EnumSet.of(AccessLevel.CurrentRead,
					AccessLevel.CurrentWrite, AccessLevel.HistoryRead);
		else
			return EnumSet
					.of(AccessLevel.CurrentRead, AccessLevel.CurrentWrite);
	}

	@Override
	public Boolean onGetUserExecutable(ServiceContext serviceContext,
			NodeId nodeId, UaMethod node) {
		// Enable execution of all methods that are allowed by default
		return true;
	}

	@Override
	public EnumSet<WriteAccess> onGetUserWriteMask(
			ServiceContext serviceContext, NodeId nodeId, UaNode node) {
		// Enable writing to everything that is allowed by default
		// The WriteMask defines the writable attributes, except for Value,
		// which is controlled by UserAccessLevel (above)

		// The following would deny write access for anonymous users:
		// if
		// (serviceContext.getSession().getUserIdentity().getType().equals(
		// UserTokenType.Anonymous))
		// return EnumSet.noneOf(WriteAccess.class);

		return EnumSet.allOf(WriteAccess.class);
	}

	@Override
	public boolean onReadNonValue(ServiceContext serviceContext, NodeId nodeId,
			UaNode node, UnsignedInteger attributeId, DataValue dataValue)
			throws StatusException {
		return false;
	}
	
	
	/*
	 * If a variable node has a corresponding node in one of the aggregated servers, the value is read directly from the underlying node.
	 */
	@Override
	public boolean onReadValue(ServiceContext serviceContext, NodeId nodeId,
			UaVariable node, NumericRange indexRange,
			TimestampsToReturn timestampsToReturn, DateTime minTimestamp,
			DataValue dataValue) throws StatusException {
		
		int ns = nodeId.getNamespaceIndex();
		TargetServer ts = clientMap.get(ns);
		
		if(ts != null) {
			NodeId remoteId = ts.getNodeManager().getIdMap().get(nodeId);
			
			if (remoteId != null) {
				try {					
					DataValue readValue = ts.getClient().client.readValue(remoteId);
					dataValue.setSourceTimestamp(readValue.getSourceTimestamp());
					dataValue.setValue(readValue.getValue());
					node.setValue(dataValue);
					return true;
				} catch (ServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (logger.isDebugEnabled())
					logger.debug("onReadValue: nodeId=" + nodeId
							+ (node != null ? " node=" + node.getBrowseName() : ""));
			} else {
				return false;
			}
		} else {
			return false;
		}
		return false;
	}

	@Override
	public boolean onWriteNonValue(ServiceContext serviceContext,
			NodeId nodeId, UaNode node, UnsignedInteger attributeId,
			DataValue dataValue) throws StatusException {
		return false;
	}

	/*
	 * Relay write-operations from aggregating nodes to the underlying nodes, unless the aggregated node is currently being subscribed to
	 */
	@Override
	public boolean onWriteValue(ServiceContext serviceContext, NodeId nodeId,
			UaVariable node, NumericRange indexRange, DataValue dataValue)
			throws StatusException {	
		int ns = nodeId.getNamespaceIndex();
		TargetServer ts = clientMap.get(ns);
		
		if (!ts.getClient().getMonitoredPairs().containsValue(nodeId)) {
		
		if(ts != null) {
			NodeId remoteId = ts.getNodeManager().getIdMap().get(nodeId);
			if (remoteId != null) {
				try {
					ts.getClient().client.writeValue(remoteId, dataValue.getValue());
					return true;
				} catch (ServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
			}	
		}
		}		
		return false;
		
	}

	public void updateIdMap(TargetServer ts) {
		int ns = ts.getNodeManager().getNamespaceIndex();
		if (clientMap.get(ns) == null) {
			clientMap.put(ns, ts);
		}
	}
	
}