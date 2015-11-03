package fi.opc.ua.iotticket;

import org.opcfoundation.ua.core.MonitoringMode;

public class IoTTicketConfigurationNode {
	
	public IoTTicketConfigurationNode(String name, MonitoringMode subType) {
		this.name = name;
		this.subscriptionType = subType;
	}
	
	String name;
	public String getName() {
		return this.name;
	}
	
	MonitoringMode subscriptionType;
	public MonitoringMode getSubscriptionType() {
		return this.subscriptionType;
	}
	
	public enum SubscriptionType {
		Reporting,
	}
}
