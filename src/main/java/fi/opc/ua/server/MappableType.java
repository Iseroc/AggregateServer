package fi.opc.ua.server;

import org.opcfoundation.ua.builtintypes.NodeId;

public class MappableType {
	
	public NodeId type;
	public String agenda;
	
	public MappableType(NodeId typeId, String agendagroup) {
		this.type = typeId;
		this.agenda = agendagroup;
	}

	public NodeId getType() {
		return this.type;
	}
	
	public String getAgenda() {
		return this.agenda;
	}
	
}
