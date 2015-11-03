/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) 2009-2010 Prosys PMS Ltd., <http://www.prosysopc.com>. 
 * All rights reserved.
 */
package fi.opc.ua.client;

import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.Variant;

import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.MonitoredEventItemListener;

/**
 * A sampler listener for monitored event notifications.
 */
public class ASMonitoredEventItemListener implements MonitoredEventItemListener {
	private final AggregateServerConsoleClient client;
	private final QualifiedName[] requestedEventFields;

	/**
	 * @param client
	 * @param eventFieldNames
	 */
	public ASMonitoredEventItemListener(AggregateServerConsoleClient client,
			QualifiedName[] requestedEventFields) {
		super();
		this.requestedEventFields = requestedEventFields;
		this.client = client;
	}

	@Override
	public void onEvent(MonitoredEventItem sender, Variant[] eventFields) {
		AggregateServerConsoleClient.println(client.eventToString(sender.getNodeId(),
				requestedEventFields, eventFields));
	}
};
