package fi.opc.ua.iotticket;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.EUInformation;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoringMode;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.Range;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.transport.security.SecurityMode;
import org.xml.sax.SAXException;

import com.iotticket.api.v1.IOTAPIClient;
import com.iotticket.api.v1.exception.ValidAPIParamException;
import com.iotticket.api.v1.model.Datanode.DatanodeWriteValue;
import com.iotticket.api.v1.model.Device;
import com.iotticket.api.v1.model.Device.DeviceDetails;
import com.iotticket.api.v1.model.DeviceAttribute;
import com.iotticket.api.v1.model.PagedResult;
import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.MethodCallStatusException;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.ConnectException;
import com.prosysopc.ua.client.InvalidServerEndpointException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredDataItemListener;
import com.prosysopc.ua.client.MonitoredItem;
import com.prosysopc.ua.client.ServerListException;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaInstance;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.types.opcua.AnalogItemType;

public class IoTTicketClient {

	// static
	
	private static final String APP_NAME = "IoTTicketClient";
	private static String IOT_ADDRESS = "https://public1.wrm247.com/api/v1";
	private static String IOT_USERNAME = "";
	private static String IOT_PASSWORD = "";
	private static String UA_ADDRESS = "opc.tcp://localhost:52520/OPCUA/AggregateServer";
	private static String TARGET_NODE = "Vakola";
	
	
	
	// main for testing purposes
	
	public static void main(String[] args) throws Exception {
		// Load Log4j configurations from external file
		//PropertyConfigurator.configureAndWatch(IoTTicketClient.class.getResource("log.properties").getFile(), 5000);
		
		System.out.println("IoT Ticket client main started");
		
		IoTTicketClient client = new IoTTicketClient();

		System.out.println("UA client init starting");
		
		client.Initialize(UA_ADDRESS);
		
		client.mainMenu();

		System.out.println(APP_NAME + ": Closed");
	}

	private void mainMenu() throws ServerListException, URISyntaxException {
		// Try to connect to the server already at this point.

		System.out.println("mainMenu: begin");
		
		try {
			this.startCommunications();
		} catch (ServiceException e) {
			System.out.println("Error starting communications!");
			printErr(e);
		}

		try {
			this.startSubscriptions();
		} catch (Exception e) {	
			System.out.println("Error subscribing!");
			printErr(e);
		}
		
		System.out.println("mainMenu: startCommunications() done");
		
		/******************************************************************************/
		/* Wait for user command to execute next action. */
		do {
			printMenu(rootNodeId);

			try {
				switch (readInt()) {
				case -1:
					this.stopCommunications();
					break;
				case 0:
					reconnect();
					break;
				case 1:
					browse(rootNodeId);
					break;
				case 2:
					readValue(rootNodeId);
					break;
				case 3:
					subscribeMenuAction();
					break;
				case 4:
					registerDeviceAction();
					break;
				default:
					continue;
				}
			} catch (Exception e) {
				printErr(e);
			}

		} while (true);
		
		/******************************************************************************/
	}
	
	private static String readString() {
		return readInput(false);
	}
	
	private static int readInt() {
		return parseAction(readInput(true).toLowerCase());
	}
	
	private static int parseAction(String s) {
		if (s.equals("x"))
			return -1;
//		if (s.equals("b"))
//			return ACTION_BACK;
//		if (s.equals("r"))
//			return ACTION_ROOT;
//		if (s.equals("a"))
//			return ACTION_ALL;
//		if (s.equals("u"))
//			return ACTION_UP;
//		if (s.equals("t"))
//			return ACTION_TRANSLATE;
		return Integer.parseInt(s);
	}
	
	private static String readInput(boolean useCmdSequence) {
		// You can provide "commands" already from the command line, in which
		// case they will be kept in cmdSequence
		if (useCmdSequence && !cmdSequence.isEmpty()) {
			String cmd = cmdSequence.remove(0);
			try {
				// Negative int values are used to pause for n seconds
				int i = Integer.parseInt(cmd);
				if (i < 0) {
					try {
						TimeUnit.SECONDS.sleep(-i);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					return readInput(useCmdSequence);
				}
			} catch (NumberFormatException e) {
				// never mind
			}
			return cmd;
		}
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		do
			try {
				s = stdin.readLine();
			} catch (IOException e) {
				printErr(e);
			}
		while ((s == null) || (s.length() == 0));
		return s;
	}
	
	private void printMenu(NodeId nodeId) {
		System.out.println();
		if (uaClient.isConnected()) {
			System.out.println("*** Connected to: " + uaClient.getUri());
			System.out.println();
			if (nodeId != null)
				printNode(nodeId);
		} else {
			System.out.println("*** NOT connected to: " + uaClient.getUri());
			return;
		}

		System.out.println("-------------------------------------------------------");
		System.out.println("- Enter x to close client");
		System.out.println("-------------------------------------------------------");
		System.out.println("- Enter 0 to reconnect                                -");
		System.out.println("- Enter 1 to browse the server address space          -");
		System.out.println("- Enter 2 to read values                              -");
		System.out.println("- Enter 3 to subscribe                                -");
		System.out.println("- Enter 4 to register a device                        -");
		System.out.println("-------------------------------------------------------");
	}
	
	private void reconnect()
	{
		
	}
	
	private void printNode(NodeId nodeId) {
		if (uaClient.isConnected())
			// Find the node from the NodeCache
			try {
				UaNode node = uaClient.getAddressSpace().getNode(nodeId);

				if (node == null)
					return;
				String currentNodeStr = getNodeAsString(node);
				if (currentNodeStr != null) {
					System.out.println(currentNodeStr);
					System.out.println("");
				}
			} catch (ServiceException e) {
				printErr(e);
			} catch (AddressSpaceException e) {
				printErr(e);
			}
	}
	
	private String getNodeAsString(UaNode node) {
		String nodeStr = "";
		String typeStr = "";
		String analogInfoStr = "";
		nodeStr = node.getDisplayName().getText();
		UaType type = null;
		if (node instanceof UaInstance)
			type = ((UaInstance) node).getTypeDefinition();
		typeStr = (type == null ? nodeClassToStr(node.getNodeClass()) : type
				.getDisplayName().getText());

		// This is the way to access type specific nodes and their
		// properties, for example to show the engineering units and
		// range for all AnalogItems
		if (node instanceof AnalogItemType)
			try {
				AnalogItemType analogNode = (AnalogItemType) node;
				EUInformation units = analogNode.getEngineeringUnits();
				analogInfoStr = units == null ? "" : " Units="
						+ units.getDisplayName().getText();
				Range range = analogNode.getEuRange();
				analogInfoStr = analogInfoStr + (range == null ? "" : String.format(
								" Range=(%f; %f)", range.getLow(), range.getHigh()));
			} catch (Exception e) {
				printErr(e);
			}

		String currentNodeStr = String.format(
				"*** Current Node: %s: %s (ID: %s)%s", nodeStr, typeStr,
				node.getNodeId(), analogInfoStr);
		return currentNodeStr;
	}
	
	private String nodeClassToStr(NodeClass nodeClass) {
		return "[" + nodeClass + "]";
	}

	private void subscribeMenuAction() {
		System.out.println("Give node name:");
		String nodeName = readString();
		
		try {
			NodeId foundNode = findNode(rootNodeId, nodeName);
			if(foundNode != null)
				subscribe(foundNode, Attributes.Value);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceResultException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AddressSpaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void registerDeviceAction() throws ValidAPIParamException {
		System.out.println("Give device name:");
		String name = readString();
		
		deviceId = registerDevice(name, "PROJECT", "PC", "", new String[] {}, new String[] {});
		System.out.println("Active device id: " + deviceId);
	}
	
	
	
	// constructor
	
	public IoTTicketClient() {
		
	}

	
	
	// variables

	private final static List<String> cmdSequence = new ArrayList<String>();

	private UaClient uaClient;
    private NodeId rootNodeId = Identifiers.RootFolder;
	
	private static IOTAPIClient iotClient;
    private static String deviceId;
    private static Map<String, String> nodeIdToName = new HashMap<String, String>();

	
    private IoTTicketConfigurationParser configParser = new IoTTicketConfigurationParser();
	
    
    
    // public methods
    
	public void Initialize(String serverUri) throws ValidAPIParamException, URISyntaxException, ParserConfigurationException, SAXException, IOException {
		initUA(serverUri);
		initIoT();
	}

	public void automaticInitializeAndStart(String serverUri) throws ValidAPIParamException, URISyntaxException, ParserConfigurationException, SAXException, IOException, InvalidServerEndpointException, ConnectException, SessionActivationException, ServiceException, StatusException, ServiceResultException, AddressSpaceException {
		Initialize(serverUri);
		this.startCommunications();
		this.startSubscriptions();
	}
	
	public void shutdown() throws ServiceException {
		uaClient.removeSubscriptions(uaClient.getSubscriptions());
		
		stopCommunications();
	}
	
	public void startCommunications() throws InvalidServerEndpointException, ConnectException, SessionActivationException, ServiceException {
		uaClient.connect();
		
		if(uaClient.isConnected())
		{
			uaClient.getAddressSpace().setMaxReferencesPerNode(1000);
			uaClient.getAddressSpace().setReferenceTypeId(Identifiers.HierarchicalReferences);
		}
		
	}
	
	public void stopCommunications() {
		uaClient.disconnect();
	}
	
	public List<ReferenceDescription> browse(NodeId nodeId) throws ServiceException, StatusException, ServiceResultException, AddressSpaceException {
		List<ReferenceDescription> references = null;
		
		if(uaClient.isConnected())
			references = uaClient.getAddressSpace().browse(nodeId);

		NodeId targetNodeId = findNode(nodeId, TARGET_NODE);

		printNodeList(targetNodeId, 1);
		
		System.out.println("Done browsing.");
		
		return references;
	}
	
	public DataValue readAttribute(NodeId nodeId, UnsignedInteger attributeId) throws ServiceException, StatusException {
		DataValue value = null;
		
		if(uaClient.isConnected())
			value = uaClient.readAttribute(nodeId, attributeId);
		
		return value;
	}

	public DataValue readValue(NodeId nodeId) throws ServiceException, StatusException {
		DataValue value = null;
		
		if(uaClient.isConnected())
			value = uaClient.readValue(nodeId);
		
		return value;
	}
	
	public void subscribe(NodeId nodeId, UnsignedInteger attributeId) throws ServiceException, StatusException {
		Subscription subscription = new Subscription();
		MonitoredDataItem item = new MonitoredDataItem(nodeId, attributeId, MonitoringMode.Reporting);
		subscription.addItem(item);
		uaClient.addSubscription(subscription);
		item.setDataChangeListener(dataChangeListener);
	}

	public String registerDevice(String name, String manufacturer, String type, String description, String[] attributeKeys, String[] attributeValues) throws ValidAPIParamException {
		String id = null;
		
		if(attributeKeys.length != attributeValues.length) {
			printErr("Invalid attribute arrays");
			return id;
		}
		
		id = getRegisteredDevice(name);
		
		if (id == null) {
			Device device = new Device();
			
			device.setName(name);
			device.setDescription(description);
			device.setManufacturer(manufacturer);
			
			Collection<DeviceAttribute> attributes = device.getAttributes();
			for(int i = 0; i < attributeKeys.length; i++) {
				attributes.add(new DeviceAttribute(attributeKeys[i], attributeValues[i]));
			}
			device.setAttributes(attributes);
			
			DeviceDetails deviceDetails = iotClient.registerDevice(device);
			
			id = deviceDetails.getDeviceId();
		}
		
		return id;
	}

	
	
	// private methods

	private NodeId findNode(NodeId nodeId, String nodeName) throws ServiceException, StatusException, ServiceResultException, AddressSpaceException {
		NodeId foundNodeId = null;
		
		List<ReferenceDescription> references = uaClient.getAddressSpace().browse(nodeId);
		
		for (ReferenceDescription ref : references) {
			NodeId subNodeId = uaClient.getAddressSpace().getNamespaceTable().toNodeId(ref.getNodeId());
			if(ref.getDisplayName().getText().equals(nodeName))
			{
				foundNodeId = subNodeId;
				break;
			}
			
			foundNodeId = findNode(subNodeId, nodeName);
			if(foundNodeId != null)
				break;
		}
		
		return foundNodeId;
	}
	
	private void printNodeList(NodeId nodeId, int indent) throws ServiceException, StatusException, ServiceResultException, AddressSpaceException {
		for(int i = 1; i < indent; i++)
			System.out.print("  ");
		
		printNode(nodeId);

		UaNode node = uaClient.getAddressSpace().getNode(nodeId);
		UaType type = null;
		if (node instanceof UaInstance)
			type = ((UaInstance) node).getTypeDefinition();
		String typeStr = (type == null ? nodeClassToStr(node.getNodeClass()) : type
				.getDisplayName().getText());
		
		if(typeStr.equals("FolderType") || typeStr.equals("[FolderType]")) {
			List<ReferenceDescription> references = uaClient.getAddressSpace().browse(nodeId);
			
			for(ReferenceDescription desc : references)
			{
				NodeId subNodeId = uaClient.getAddressSpace().getNamespaceTable().toNodeId(desc.getNodeId());
				printNodeList(subNodeId, indent+1);
			}
		}
	}
	/*
	private String referenceToString(ReferenceDescription r) throws ServerConnectionException, ServiceException, StatusException {
		if (r == null)
			return "";
		String referenceTypeStr = null;
		try {
			// Find the reference type from the NodeCache
			UaReferenceType referenceType = (UaReferenceType) uaClient.getAddressSpace().getType(r.getReferenceTypeId());
			if ((referenceType != null)	&& (referenceType.getDisplayName() != null))
				if (r.getIsForward())
					referenceTypeStr = referenceType.getDisplayName().getText();
				else
					referenceTypeStr = referenceType.getInverseName().getText();
		} catch (AddressSpaceException e) {
			printErr(e);
			System.out.print(r.toString());
			referenceTypeStr = r.getReferenceTypeId().getValue().toString();
		}
		String typeStr;
		switch (r.getNodeClass()) {
		case Object:
		case Variable:
			try {
				// Find the type from the NodeCache
				UaNode type = uaClient.getAddressSpace().getNode(r.getTypeDefinition());
				if (type != null)
					typeStr = type.getDisplayName().getText();
				else
					typeStr = r.getTypeDefinition().getValue().toString();
			} catch (AddressSpaceException e) {
				printErr(e);
				System.out.print("type not found: " + r.getTypeDefinition().toString());
				typeStr = r.getTypeDefinition().getValue().toString();
			}
			break;
		default:
			typeStr = nodeClassToStr(r.getNodeClass());
			break;
		}
		return String.format("%s%s (ReferenceType=%s, BrowseName=%s%s)", r.getDisplayName().getText(), ": " + typeStr, referenceTypeStr,
				r.getBrowseName(), r.getIsForward() ? "" : " [Inverse]");
	}
	*/
	private String getRegisteredDevice(String name) {
		String id = null;
		
		PagedResult<DeviceDetails> devices = iotClient.getDeviceList(0, 100);
		Collection<DeviceDetails> deviceColl = devices.getResults();
		for(DeviceDetails device : deviceColl) {
			if(device.getName().equals(name))
				id = device.getDeviceId();
		}
		
		return id;
	}
	
	private static MonitoredDataItemListener dataChangeListener = new MonitoredDataItemListener() {
		@Override
		public void onDataChange(MonitoredDataItem sender, DataValue prevValue, DataValue value) {
			MonitoredItem item = sender;

			// write the data to IoT Ticket service
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

			String nodeName = nodeIdToName.get(item.getNodeId().toString());
			
			String[] pathParts = nodeName.split("\\.");
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < pathParts.length - 1; i++)
				builder.append("/" + pathParts[i]);
			
			String name = pathParts[pathParts.length - 1].split(",")[0].replace(" ", "");
			String path = builder.toString().replace(" ", "");
			
			if(path == null || path.isEmpty()) {
				path = "/Boiler";
			}
			
		    DatanodeWriteValue dnwrite = new DatanodeWriteValue();
		    dnwrite.setName(name);
		    dnwrite.setPath(path);
		    dnwrite.setUnit("");
		    dnwrite.setValue(value.getValue().toString());
		    dnwrite.setTimestampMiliseconds(cal.getTimeInMillis());
		
		    try {
//		    	WriteDataResponse writeResult = 
				iotClient.writeData(deviceId, dnwrite);
			} catch (ValidAPIParamException e) {
				System.out.println("Error sending data to IoT client");
				printErr(e);
			}
		    System.out.println("Name:  " + name);
		    System.out.println("Path:  " + path);
			System.out.println("Value: " + dataValueToString(item.getNodeId(), item.getAttributeId(), value));
			System.out.println();
		}
	};

    protected static String dataValueToString(NodeId nodeId, UnsignedInteger attributeId, DataValue value) {
		return value.getValue().toString();
	}
    
    private void initUA(String serverUri) throws URISyntaxException {
    	uaClient = new UaClient(serverUri);
    	
    	ApplicationDescription appDescription = new ApplicationDescription();
    	appDescription.setApplicationName(new LocalizedText("IoTTicketClient", Locale.ENGLISH));
    	// 'localhost' (all lower case) is converted to the actual host name in the URI
    	appDescription.setApplicationUri("urn:localhost:UA:SampleConsoleClient");
    	appDescription.setProductUri("urn:prosysopc.com:UA:SampleConsoleClient");
    	appDescription.setApplicationType(ApplicationType.Client);
    	
    	final ApplicationIdentity identity = new ApplicationIdentity();
    	identity.setApplicationDescription(appDescription);
    	
    	uaClient.setApplicationIdentity(identity);
    	uaClient.setSecurityMode(SecurityMode.NONE);
    	//uaClient.setUserIdentity(new UserIdentity("my_name", "my_password"));
    }
    
	private void initIoT() throws ValidAPIParamException {
        try {
            FileReader fileReader = new FileReader("pwd.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            IOT_USERNAME = bufferedReader.readLine();
            IOT_PASSWORD = bufferedReader.readLine();
            
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.out.println("Unable to open pwd.txt");                
        } catch (IOException e) {
			e.printStackTrace();
		}
		
		iotClient = new IOTAPIClient(IOT_ADDRESS, IOT_USERNAME, IOT_PASSWORD);
		
		//deviceId = registerDevice("Seed Drill Machine", "PROJECT", "Agricultural apparatus", "", new String[] {}, new String[] {});
		deviceId = registerDevice("Boiler", "PROJECT", "Agricultural apparatus", "", new String[] {}, new String[] {});
		System.out.println("Active device id: " + deviceId);
		
		try {
			//load subscription configuration
			configParser.parseConfigurationFile("src/main/resources/configuration.xml");
		} catch (Exception e) {
			System.out.println("Error in loading configuration file and subscriptions");
			printErr(e);
		}
    }
	
	private void startSubscriptions() throws ServiceException, StatusException, ServiceResultException, AddressSpaceException {
		for(IoTTicketConfigurationNode node : configParser.configurationNodes) {
			NodeId foundNode = findNode(rootNodeId, node.name);
			if(foundNode != null) {
				subscribe(foundNode, Attributes.Value);
				nodeIdToName.put(foundNode.toString(), node.name);
				System.out.println("Subscribed node " + foundNode + ":" + node.name);
			}
			else {
				System.out.println("Failed to subscribe node " + node.name);
			}
		}
	}

	private static void printErr(Exception e) {
		System.out.println(e.toString());
		if (e instanceof MethodCallStatusException) {
			MethodCallStatusException me = (MethodCallStatusException) e;
			final StatusCode[] results = me.getInputArgumentResults();
			if (results != null)
				for (int i = 0; i < results.length; i++) {
					StatusCode s = results[i];
					if (s.isBad()) {
						System.out.println("Status for Input #" + i + ": " + s);
						DiagnosticInfo d = me
								.getInputArgumentDiagnosticInfos()[i];
						if (d != null)
							System.out.println("  DiagnosticInfo:" + i + ": " + d);
					}
				}
		}
		if (e.getCause() != null)
			System.out.println("Caused by: " + e.getCause());
	}
	
    private static void printErr(String e) {
    	System.err.println(e);
    }

}
