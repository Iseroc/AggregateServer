/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) 2009-2010 Prosys PMS Ltd., <http://www.prosysopc.com>. 
 * All rights reserved.
 */
package fi.opc.ua.client;

import java.util.HashMap;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredDataItemListener;

import fi.opc.ua.server.TargetServer;

/**
 * A sampler listener for monitored data changes.
 */
public class ASMonitoredDataItemListener implements MonitoredDataItemListener {
	private final AggregateServerConsoleClient client;

	/**
	 * @param client
	 */
	public ASMonitoredDataItemListener(AggregateServerConsoleClient client) {
		super();
		this.client = client;
	}
	
	
	private AggregateServerConsoleClient internalClient;
	private HashMap<Integer, TargetServer> clientMap = new HashMap<Integer, TargetServer>(100);
	private HashMap<NodeId, NodeId> IdMap = new HashMap<NodeId, NodeId>(1000);

	/*
	 * Whenever a subscribed value is changed, this function writes the value to the corresponding aggregated node.
	 */
	@Override
	public void onDataChange(MonitoredDataItem sender, DataValue prevValue, DataValue value) {
		AggregateServerConsoleClient.println(client.dataValueToString(sender.getNodeId(), sender.getAttributeId(), value));
		NodeId aggregatingId = IdMap.get(sender.getNodeId());
		System.out.println("Found aggregating id: " + aggregatingId);
		if (aggregatingId != null) {
			try {
				if (sender.getAttributeId() != null && value != null) {
					synchronized (this) {
						internalClient.client.writeAttribute(aggregatingId, sender.getAttributeId(), value);
					}
				}
			} catch (ServiceException | StatusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void storeInternalClient(AggregateServerConsoleClient intClient) {
		internalClient = intClient;
	}
	
	public void updateIdMap(TargetServer ts) {
		System.out.println("### Updating IDMap ###");
		int ns = ts.getNodeManager().getNamespaceIndex();
		if (clientMap.get(ns) == null) {
			clientMap.put(ns, ts);
			System.out.println("### Updating IDMap complete ###");
		}
	}
	
	public void storeMonitoredIdPair(NodeId remoteId, NodeId aggId) {
		System.out.println("### Stored a monitored ID pair ###");
		IdMap.put(remoteId, aggId);
	}
	
	public void removeMonitoredIdPair(NodeId remoteId) {
		IdMap.remove(remoteId);
	}
	
	public HashMap<NodeId, NodeId> getIdMap() {
		return IdMap;
	}
	
};
