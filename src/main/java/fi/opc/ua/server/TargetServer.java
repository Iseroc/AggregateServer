package fi.opc.ua.server;

import com.prosysopc.ua.client.AddressSpace;

import fi.opc.ua.client.AggregateServerConsoleClient;

public class TargetServer {
	
	public AggregateServerConsoleClient client;
	public ASNodeManager nm;
	
	public TargetServer (AggregateServerConsoleClient client, ASNodeManager nm) {
		this.client = client;
		this.nm = nm;
	}
	
	public AggregateServerConsoleClient getClient() {
		return this.client;
	}
	
	public ASNodeManager getNodeManager() {
		return this.nm;
	}
	
	public AddressSpace getTargetServerAddressSpace() {
		return this.client.client.getAddressSpace();
	}
}
