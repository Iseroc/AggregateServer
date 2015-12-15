package fi.opc.ua.server;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.UnsignedLong;
import org.opcfoundation.ua.builtintypes.UnsignedShort;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.builtintypes.XmlElement;
import org.opcfoundation.ua.common.NamespaceTable;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.EUInformation;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.Range;
import org.opcfoundation.ua.core.UserTokenPolicy;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.SecurityMode;
import org.opcfoundation.ua.utils.EndpointUtil;
import org.xml.sax.SAXException;

import com.iotticket.api.v1.exception.ValidAPIParamException;
import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.CertificateValidationListener;
import com.prosysopc.ua.ModelException;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UaAddress;
import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.ServerListException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaObject;
import com.prosysopc.ua.nodes.UaProperty;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.server.FileNodeManager;
import com.prosysopc.ua.server.NodeBuilderConfiguration;
import com.prosysopc.ua.server.NodeBuilderException;
import com.prosysopc.ua.server.NodeManagerListener;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.UaInstantiationException;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.UaServerException;
import com.prosysopc.ua.server.UserValidator;
import com.prosysopc.ua.server.nodes.CacheVariable;
import com.prosysopc.ua.server.nodes.FileFolderType;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.types.opcua.AnalogItemType;
import com.prosysopc.ua.types.opcua.DataItemType;
import com.prosysopc.ua.types.opcua.FolderType;
import com.prosysopc.ua.types.opcua.server.BuildInfoTypeNode;
import com.prosysopc.ua.types.opcua.server.DataItemTypeNode;
import com.prosysopc.ua.types.opcua.server.FolderTypeNode;

import fi.opc.ua.client.AggregateServerConsoleClient;
import fi.opc.ua.iotticket.IoTTicketClient;

/**
 * A server class which implements all OPC UA Aggregate Server features.
 * Does not use any direct console commands.
 * Use AggregateServerConfigurator to configure the server.
 */
public class AggregateServer {
	
	// main
	public static void main(String[] args) {
		AggregateServer server = new AggregateServer();
		
		server.initialize();
		
		server.runSocketServer(4999, false);
		
		return;
	}
	
	
	
	// constructor
	public AggregateServer() {
		
	}

	public void initialize() {
		// Setup logger
		PropertyConfigurator.configureAndWatch(AggregateServer.class.getResource("log.properties").getFile(), 5000);
		
		// Initialize the server
		try {
			this.initInternal(52520, 52443, APP_NAME);
			this.initMappingEngine();
			this.createAddressSpace();
		} catch (Exception e) {
			System.out.println("Initialize failed!");
			printErr(e);
		}
	}

	private void initMappingEngine() {
		mappingEngine = new MappingEngine();
		mappingEngine.Initialize();
	}

	private void initInternal(int port, int httpsPort, String applicationName) throws SecureIdentityException, IOException, UaServerException {
		// *** Create the server
		uaServer = new UaServer();

		// Use PKI files to keep track of the trusted and rejected client
		// certificates...
		final PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator();
		uaServer.setCertificateValidator(validator);
		// ...and react to validation results with a custom handler
		validator.setValidationListener(validationListener);

		// *** Application Description is sent to the clients
		ApplicationDescription appDescription = new ApplicationDescription();
		appDescription.setApplicationName(new LocalizedText(applicationName, Locale.ENGLISH));
		
		// 'localhost' (all lower case) in the URI is converted to the actual
		// host name of the computer in which the application is run
		appDescription.setApplicationUri("urn:localhost:OPCUA:" + applicationName);
		appDescription.setProductUri("urn:prosysopc.com:OPCUA:" + applicationName);

		// *** Server Endpoints
		// TCP Port number for the UA Binary protocol
		uaServer.setPort(Protocol.OpcTcp, port);
		// TCP Port for the HTTPS protocol
		uaServer.setPort(Protocol.Https, httpsPort);

		// optional server name part of the URI (default for all protocols)
		uaServer.setServerName("OPCUA/" + applicationName);

		// Optionally restrict the InetAddresses to which the server is bound.
		// You may also specify the addresses for each Protocol.
		// This is the default:
		uaServer.setBindAddresses(EndpointUtil.getInetAddresses());

		// *** Certificates

		File privatePath = new File(validator.getBaseDir(), "private");

		// Define a certificate for a Certificate Authority (CA) which is used
		// to issue the keys. Especially
		// the HTTPS certificate should be signed by a CA certificate, in order
		// to make the .NET applications trust it.
		//
		// If you have a real CA, you should use that instead of this sample CA
		// and create the keys with it.
		// Here we use the IssuerCertificate only to sign the HTTPS certificate
		// (below) and not the Application Instance Certificate.
		KeyPair issuerCertificate = ApplicationIdentity.loadOrCreateIssuerCertificate(
				"ProsysSampleCA",
				privatePath,
				"opcua",
				3650,
				false);

		// If you wish to use big certificates (4096 bits), you will need to
		// define two certificates for your application, since to interoperate
		// with old applications, you will also need to use a small certificate
		// (up to 2048 bits).

		// Also, 4096 bits can only be used with Basic256Sha256 security
		// profile, which is currently not enabled by default, so we will also
		// leave the the keySizes array as null. In that case, the default key
		// size defined by CertificateUtils.getKeySize() is used.
		int[] keySizes = null;

		// Use 0 to use the default keySize and default file names as before
		// (for other values the file names will include the key size).
		// keySizes = new int[] { 0, 4096 };

		// *** Application Identity

		// Define the Server application identity, including the Application
		// Instance Certificate (but don't sign it with the issuerCertificate as
		// explained above).
		final ApplicationIdentity identity = ApplicationIdentity.loadOrCreateCertificate(
				appDescription,
				"Sample Organisation",
				/* Private Key Password */"opcua",
				/* Key File Path */privatePath,
				/* Issuer Certificate & Private Key */null,
				/* Key Sizes for instance certificates to create */keySizes,
				/* Enable renewing the certificate */true);

		// Create the HTTPS certificate bound to the hostname.
		// The HTTPS certificate must be created, if you enable HTTPS.
		String hostName = ApplicationIdentity.getActualHostName();
		identity.setHttpsCertificate(ApplicationIdentity.loadOrCreateHttpsCertificate(
				appDescription,
				hostName,
				"opcua",
				issuerCertificate,
				privatePath,
				true));

		uaServer.setApplicationIdentity(identity);

		// *** Security settings
		// Define the security modes to support for the Binary protocol -
		// ALL is the default
		uaServer.setSecurityModes(SecurityMode.ALL);
		
		// The TLS security policies to use for HTTPS
		uaServer.getHttpsSettings().setHttpsSecurityPolicies(HttpsSecurityPolicy.ALL);

		// Number of threads to reserve for the HTTPS server, default is 10
		// server.setHttpsWorkerThreadCount(10);

		// Define a custom certificate validator for the HTTPS certificates
		uaServer.getHttpsSettings().setCertificateValidator(validator);
		// client.getHttpsSettings().setCertificateValidator(...);

		// Or define just a validation rule to check the hostname defined for
		// the certificate; ALLOW_ALL_HOSTNAME_VERIFIER is the default
		// client.getHttpsSettings().setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		// Define the supported user Token policies
		uaServer.addUserTokenPolicy(UserTokenPolicy.ANONYMOUS);
		uaServer.addUserTokenPolicy(UserTokenPolicy.SECURE_USERNAME_PASSWORD);
		uaServer.addUserTokenPolicy(UserTokenPolicy.SECURE_CERTIFICATE);
		
		// Define a validator for checking the user accounts
		uaServer.setUserValidator(userValidator);

		// Register on the local discovery server (if present)
		try {
			UaAddress discoveryAddress = new UaAddress(discoveryServerUrl);
			uaServer.setDiscoveryServerAddress(discoveryAddress);
		} catch (URISyntaxException e) {
			logger.error("DiscoveryURL is not valid", e);
		}

		// server.setDiscoveryEndpointEnabled(false);

		// *** init() creates the service handlers and the default endpoints
		// according to the above settings
		uaServer.init();

		initBuildInfo();

		// "Safety limits" for ill-behaving clients
		uaServer.getSessionManager().setMaxSessionCount(500);
		uaServer.getSessionManager().setMaxSessionTimeout(3600000); // one hour
		uaServer.getSubscriptionManager().setMaxSubscriptionCount(500);

		// You can do your own additions to server initializations here
	}

	
	
	// variables
	protected static final String DONE = "done";
	protected static final String ERROR = "error";
	
	protected static final String EVENT = "event";
	protected static final String DIAG = "diag";
	protected static final String LIST = "list";
	protected static final String INSERT = "insert";
	protected static final String INSERTANDMAP = "insert_map";
	protected static final String SHUTDOWN = "shutdown";
	protected static final String DISCONNECT = "disconnect";
	protected static final String IOTCLIENT = "iot";
	
	private static String APP_NAME = "AggregateServer";
	private static String discoveryServerUrl = "opc.tcp://localhost:4840";
	
	private DataInputStream is;
	private PrintStream os;

	private static Logger logger = Logger.getLogger(AggregateServer.class);
	
	private static AggregateServerConsoleClient internalClient = new AggregateServerConsoleClient();
	private static NodeManagerListener myNodeManagerListener = new ASNodeManagerListener();
	private static ASIoManagerListener MyIOListener = new ASIoManagerListener();
	private static ASHistorian myHistorian = new ASHistorian();
	private final CertificateValidationListener validationListener = new ASCertificateValidationListener();
	private final UserValidator userValidator = new ASUserValidator();

	protected NodeManagerUaNode complianceNodeManager;
	protected int complianceNamespaceIndex;
	private FolderType staticVariableFolder;
	private FolderType staticArrayVariableFolder;
	private FolderType dataItemFolder;
	private FolderType analogItemFolder;
	private FolderType analogItemArrayFolder;
	private FolderType deepFolder;
	private FileNodeManager fileNodeManager;
	
	private static ASNodeManager myNodeManager;
	private static UaServer uaServer;
	
	private static List<TargetServer> clientList = new ArrayList<TargetServer>();
	
	private IoTTicketClient iotClient;
	private MappingEngine mappingEngine;
	
	
    // Actions
    
	//TODO: untested
	private String enableDiagnostics() {
		try {
			final UaProperty enabledFlag = uaServer.getNodeManagerRoot().getServerData().getServerDiagnosticsNode().getEnabledFlagNode();
			boolean newValue = !((Boolean) enabledFlag.getValue().getValue().getValue());
			enabledFlag.setValue(Boolean.valueOf(newValue));
			System.out.println("Server Diagnostics " + (newValue ? "Enabled" : "Disabled"));
			return DONE;
		} catch (StatusException e) {
			printErr(e);
		}
		return ERROR;
	}

	//TODO: untested
	private String sendEvent() {
		myNodeManager.sendEvent();
		return DONE;
	}
	
	private String listServers() {
		System.out.println("Sending server list to client");
		System.out.println("    -Server list size: " + clientList.size());
		
		if(clientList.size() == 0) {
			writeToOutputStream("Server list empty");
			return ERROR;
		}
		
		int i = 1;
		for (TargetServer server : clientList) {
			try {
				writeToOutputStream(i + ": " + ((UaProperty)server.getClient().client.getAddressSpace().getNode(new NodeId(0,2254))).getValue().getValue().toString() + "(Namespaceindex " + server.getNodeManager().getNamespaceIndex() + ")");
			} catch (ServiceException | AddressSpaceException e) {
				printErr(e);
				writeToOutputStream("Error printing server list");
				return ERROR;
			}
			i += 1;
		}

		try{
			writeToOutputStream(DONE);
	
			System.out.println("Waiting client input");
			
			String input = readFromInputStream();
			
			System.out.println("Client: " + input);
			
			int ind = Integer.parseInt(input) - 1;
			//TODO: add new rule manager input here
			//List<MappableType> ruleFileResults = readRuleFile(clientList.get(ind).getClient());
			
			//loopedIds.clear();
			mappingEngine.deleteNodesByNameSpaceIndex(uaServer, clientList.get(ind).getNodeManager(), internalClient, Identifiers.RootFolder);
	
			//loopedIds.clear();
			//copyAddressSpace(Identifiers.RootFolder, ruleFileResults, clientList.get(ind));
			
			System.out.println("Sending server list to client done");
			
			return DONE;
		} catch (Exception e) {
			printErr(e);
		}
			
		return ERROR;
	}

	private String insertServer(String address) {
		AggregateServerConsoleClient newClient = new AggregateServerConsoleClient();
		
		String[] clientargs = address.split(" ");
		for (TargetServer ts : clientList) {
			if (clientargs[clientargs.length-1].equals(ts.getNodeManager().getNamespaceUri())) {
				writeToOutputStream("Server already added, remap from server list if necessary");
				return DONE;
			}
		}

		newClient.parseCmdLineArgs(clientargs);
		try {
			newClient.initialize(clientargs);
			newClient.storeInternalClient(internalClient);
			newClient.connect();
			if (newClient.client.isConnected()) {
				UaProperty serverArray = newClient.client.getAddressSpace().getNode(new NodeId(0,2253)).getProperty(new QualifiedName(0,"ServerArray"));
				uaServer.addToServerArray(serverArray.getValue().getValue().toString());
				ASNodeManager newNodeManager = createNodeManager(serverArray.getValue().getValue().toString());
				TargetServer newServer = new TargetServer(newClient, newNodeManager);
				clientList.add(newServer);
				writeToOutputStream("Server added successfully");
				return DONE;
			} else {
				newClient = null;
				writeToOutputStream("Could not connect to server");
				return ERROR;
			}
		} catch (URISyntaxException | SecureIdentityException | IOException
				| ServerListException | StatusException | AddressSpaceException | ServiceException e) {
			printErr(e);
		}

		writeToOutputStream("Adding the server failed");
		return ERROR;
	}
	
	private String insertAndMapServer(String address) {
		String status = insertServer(address);
		
		if(status != ERROR)
		{
			writeToOutputStream("Mapping server address space...");
			try{
				mappingEngine.MapAddressSpace(clientList.get(clientList.size()-1));
				writeToOutputStream("Server address space mapped");
			}
			catch(Exception e) {
				status = ERROR;
				writeToOutputStream("Failed to map server");
			}
		}
		
		return status;
	}

	private String toggleIoTClient() {
		String status = DONE;
		
		if(iotClient == null) {
			writeToOutputStream("Starting IoT client...");
			
			if(iotClient == null)
				iotClient = new IoTTicketClient();
			
			try {
				iotClient.automaticInitializeAndStart("opc.tcp://localhost:52520/OPCUA/AggregateServer");
				writeToOutputStream("IoT client started");
			} catch (ValidAPIParamException | URISyntaxException
					| ParserConfigurationException | SAXException | IOException
					| ServiceException | StatusException | ServiceResultException
					| AddressSpaceException e) {
				System.out.println("Starting IoT client failed");
				writeToOutputStream("Starting IoT client failed");
				printErr(e);
				status = ERROR;
			}
		} else {
			writeToOutputStream("Closing IoT client...");
			
			try {
				iotClient.shutdown();
				iotClient = null;
			} catch (ServiceException e) {
				System.out.println("Closing IoT client failed");
				writeToOutputStream("Closing IoT client failed");
				printErr(e);
				status = ERROR;
			}
		}

		return status;
	}
	

    
	// public methods
	
	/**
	 * Run the server with java sockets open for communication
	 * @param socket
	 * @throws UaServerException 
	 */
	public void runSocketServer(int socket, boolean enableSessionDiagnostics) {
		try {
			uaServer.start();
		} catch (UaServerException e) {
			printErr(e);
			System.out.println("Unable to start UA server. Halting startup process.");
			return;
		}
		
		initHistory();
		if (enableSessionDiagnostics)
			uaServer.getNodeManagerRoot().getServerData().getServerDiagnosticsNode().setEnabled(true);
		
		// initialize the internal client which is used to browse the address space of the aggregating server, as well as to delete and write to nodes
		String[] internalargs = new String[1];
		
		
		internalargs[0] = "opc.tcp://localhost:52520/OPCUA/AggregateServer";
		internalClient.parseCmdLineArgs(internalargs);
		try {
			internalClient.initialize(internalargs);
		} catch (SessionActivationException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (SecureIdentityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServerListException e) {
			e.printStackTrace();
		}
		internalClient.connect();
	
		// initialize socket communications
		ServerSocket serverSocket = null;
        String input;
        String output;
        Socket clientSocket = null;
        
        System.out.println("Server: AggregateServer started");

        
        
        //TODO: TEMPORARY TESTING AUTOMAP
		insertAndMapServer("opc.tcp://Rickenbacker2:52510/OPCUA/BoilerServer");
		
        
        
        try {
        	serverSocket = new ServerSocket(socket);
        }
        catch (IOException e) {
        	System.out.println(e);
        }
        
        while(true) {
	        try {
		        clientSocket = serverSocket.accept();
	            System.out.println("Server: client connected");
	            
		        is = new DataInputStream(clientSocket.getInputStream());
		        os = new PrintStream(clientSocket.getOutputStream());
		        
		        writeToOutputStream(iotClient != null ? "connected" : "disconnected");
		        
		        while (true) {
		            System.out.println("Server: waiting for client input");
					input = readFromInputStream();
	
					System.out.println("Client: " + input);
					
					// process client input
					output = parseClientInput(input);
	
					if(output != null)
						writeToOutputStream(output);
					
					if(output != null && output.equals(DISCONNECT)) {
						// current client disconnected
						System.out.println("Server: Current client disconnected");
						closeConnectionToClient(clientSocket);
						break;
					}
					
					// shutdown command
					if(output != null && output.equals(SHUTDOWN)) {
						shutdown(serverSocket, clientSocket);
						return;
					}
	            }
		    }   
	        catch (IOException | ServiceException e) {
	            printErr(e);
	            System.out.println("Server: Lost connection to client");
	            if(clientSocket != null && !clientSocket.isClosed()) {
	            	try {
	            		System.out.println("DEBUG: Lost connection force disconnect");
						closeConnectionToClient(clientSocket);
	            	} catch(Exception e2) {
	            		printErr(e2);
	            	}
	            }
	        }
        }
	}

	private String parseClientInput(String message) {
		String ret = null;

		if(message != null)
		{
			String[] params = message.split("\\|");
			
			switch(params[0]) {
				case DIAG:
					ret = enableDiagnostics();
					break;
				case EVENT:
					ret = sendEvent();
					break;
				case LIST: // list servers to the client, and map the selected server
					ret = listServers();
					break;
				case INSERT: // insert the given server to the server list
					if(params.length>1)
						ret = insertServer(params[1]);
					else
						ret = ERROR;
					break;
				case INSERTANDMAP: // insert the server to the server list and map instantly
					if(params.length>1)
						ret = insertAndMapServer(params[1]);
					else
						ret = ERROR;
					break;
				case IOTCLIENT:
					ret = toggleIoTClient();
					break;
				case SHUTDOWN:
					ret = SHUTDOWN;
					break;
				case DISCONNECT:
					ret = DISCONNECT;
					break;
				default:
					writeToOutputStream("Invalid server command!");
					ret = ERROR;
					break;
			}
		}
		
		return ret;
	}
	
	private void shutdown(ServerSocket serverSocket, Socket clientSocket) throws IOException, ServiceException {
		System.out.println("Server: shutdown initiated from client side");
        
        serverSocket.close();
        
		iotClient.shutdown();
		
		// Notify the clients about a shutdown, with a 5 second delay
		uaServer.shutdown(5, new LocalizedText("Closed by user", Locale.ENGLISH));
		
		System.out.println("Server: shutdown");
	}
	
	private void closeConnectionToClient(Socket clientSocket) throws IOException {
        // end communications
        is.close();
        is = null;
        os.close();
        os = null;
        clientSocket.close();
        clientSocket = null;
	}
	

	
	// TODO: possibly obsolete methods. Copied from the original AggregateServerConsoleServer

	protected void initHistory() {
		for (UaVariableNode v : myNodeManager.getHistorizableVariables())
			myHistorian.addVariableHistory(v);
		for (UaObjectNode o : myNodeManager.getHistorizableEvents())
			myHistorian.addEventHistory(o);
	}

	/**
	 * Initialize the information to the Server BuildInfo structure
	 */
	private void initBuildInfo() {
		// Initialize BuildInfo - using the version info from the SDK
		// You should replace this with your own build information
		final BuildInfoTypeNode buildInfo = uaServer.getNodeManagerRoot().getServerData().getServerStatusNode().getBuildInfoNode();

		// Fetch version information from the package manifest
		final Package sdkPackage = UaServer.class.getPackage();
		final String implementationVersion = sdkPackage.getImplementationVersion();
		if (implementationVersion != null) {
			int splitIndex = implementationVersion.lastIndexOf(".");
			final String softwareVersion = implementationVersion.substring(0, splitIndex);
			String buildNumber = implementationVersion.substring(splitIndex + 1);

			buildInfo.setManufacturerName(sdkPackage.getImplementationVendor());
			buildInfo.setSoftwareVersion(softwareVersion);
			buildInfo.setBuildNumber(buildNumber);
		}

//		final URL classFile = UaServer.class.getResource("fi/opc/ua/server/AggregateServer.class");
		final URL classFile = UaServer.class.getResource("/com/prosysopc/ua/samples/server/SampleConsoleServer.class");
		if (classFile != null) {
			final File mfFile = new File(classFile.getFile());
			GregorianCalendar c = new GregorianCalendar();
			c.setTimeInMillis(mfFile.lastModified());
			buildInfo.setBuildDate(new DateTime(c));
		}
	}
	
	/**
	 * Create a sample address space with a new folder, a device object, a level
	 * variable, and an alarm condition.
	 * <p>
	 * The method demonstrates the basic means to create the nodes and
	 * references into the address space.
	 * <p>
	 * Simulation of the level measurement is defined in
	 * {@link #startSimulation()}
	 *
	 * @throws StatusException
	 *             if the referred type nodes are not found from the address
	 *             space
	 * @throws UaInstantiationException
	 * @throws NodeBuilderException
	 * @throws URISyntaxException
	 * @throws ModelException
	 * @throws IOException
	 * @throws SAXException
	 *
	 */
	protected void createAddressSpace() throws StatusException, UaInstantiationException, NodeBuilderException {
		// Load the standard information models
		loadInformationModels();

		// My Node Manager
		myNodeManager = new ASNodeManager(uaServer, ASNodeManager.NAMESPACE);
		myNodeManager.createAddressSpace();

		myNodeManager.addListener(myNodeManagerListener);
		myNodeManager.storeNodeManagerListener(myNodeManagerListener);
		
		// My I/O Manager Listener
		
		myNodeManager.getIoManager().addListeners(MyIOListener);
		myNodeManager.storeCustomIOListener(MyIOListener);
		
		myNodeManager.getIoManager().addListeners(new ASIoManagerListener());

		// My HistoryManager
		myNodeManager.getHistoryManager().setListener(myHistorian);

		// More specific nodes to enable OPC UA compliance testing of more
		// advanced features
		createComplianceNodes();

		createFileNodeManager();

		logger.info("Address space created.");
	}

	/**
	 * Load information models into the address space. Also register classes, to
	 * be able to use the respective Java classes with
	 * NodeManagerUaNode.createInstance().
	 *
	 * See the codegen/Readme.md on instructions how to use your own models.
	 */
	protected void loadInformationModels() {
		// Uncomment to take the extra information models in use.

		// // Register generated classes
		// server.registerModel(com.prosysopc.ua.types.di.server.InformationModel.MODEL);
		// server.registerModel(com.prosysopc.ua.types.adi.server.InformationModel.MODEL);
		// server.registerModel(com.prosysopc.ua.types.plc.server.InformationModel.MODEL);
		//
		// // Load the standard information models
		// try {
		// server.getAddressSpace().loadModel(
		// UaServer.class.getResource("Opc.Ua.Di.NodeSet2.xml")
		// .toURI());
		// server.getAddressSpace().loadModel(
		// UaServer.class.getResource("Opc.Ua.Adi.NodeSet2.xml")
		// .toURI());
		// server.getAddressSpace().loadModel(
		// UaServer.class.getResource("Opc.Ua.Plc.NodeSet2.xml")
		// .toURI());
		// } catch (Exception e) {
		// throw new RuntimeException(e);
		// }
	}

	/**
	 * @throws NodeBuilderException
	 *
	 */
	private void createComplianceNodes() throws NodeBuilderException {
		try {
			// My Node Manager
			complianceNodeManager = new NodeManagerUaNode(uaServer,"http://www.prosysopc.com/OPCUA/ComplianceNodes");

			complianceNamespaceIndex = complianceNodeManager.getNamespaceIndex();

			// UA types and folders which we will use
			final UaObject objectsFolder = uaServer.getNodeManagerRoot().getObjectsFolder();

			final NodeId staticDataFolderId = new NodeId(complianceNamespaceIndex, "StaticData");
			FolderType staticDataFolder = complianceNodeManager.createInstance(FolderType.class, "StaticData", staticDataFolderId);

			objectsFolder.addReference(staticDataFolder, Identifiers.Organizes, false);

			// Folder for static test variables
			final NodeId staticVariableFolderId = new NodeId(complianceNamespaceIndex, "StaticVariablesFolder");
			staticVariableFolder = complianceNodeManager.createInstance(FolderTypeNode.class, "StaticVariables", staticVariableFolderId);

			complianceNodeManager.addNodeAndReference(staticDataFolder, staticVariableFolder, Identifiers.Organizes);

			createStaticVariable("Boolean", Identifiers.Boolean, true);
			createStaticVariable("Byte", Identifiers.Byte, UnsignedByte.valueOf(0));
			createStaticVariable("ByteString", Identifiers.ByteString, new byte[] { (byte) 0 });
			createStaticVariable("DateTime", Identifiers.DateTime,DateTime.currentTime());
			createStaticVariable("Double", Identifiers.Double, (double) 0);
			createStaticVariable("Float", Identifiers.Float, (float) 0);
			createStaticVariable("GUID", Identifiers.Guid, UUID.randomUUID());
			createStaticVariable("Int16", Identifiers.Int16, (short) 0);
			createStaticVariable("Int32", Identifiers.Int32, 0);
			createStaticVariable("Int64", Identifiers.Int64, (long) 0);
			createStaticVariable("SByte", Identifiers.SByte, (byte) 0);
			createStaticVariable("String", Identifiers.String, "testString");
			createStaticVariable("UInt16", Identifiers.UInt16, UnsignedShort.valueOf(0));
			createStaticVariable("UInt32", Identifiers.UInt32, UnsignedInteger.valueOf(0));
			createStaticVariable("UInt64", Identifiers.UInt64, UnsignedLong.valueOf(0));
			createStaticVariable("XmlElement", Identifiers.XmlElement, new XmlElement("<testElement />"));

			// Folder for static test array variables
			final NodeId staticArrayVariableFolderId = new NodeId(complianceNamespaceIndex, "StaticArrayVariablesFolder");
			staticArrayVariableFolder = complianceNodeManager.createInstance(FolderTypeNode.class, "StaticArrayVariables", staticArrayVariableFolderId);

			staticDataFolder.addReference(staticArrayVariableFolder, Identifiers.Organizes, false);

			createStaticArrayVariable("BooleanArray", Identifiers.Boolean,
					new Boolean[] { true, false, true, false, false });
			createStaticArrayVariable(
					"ByteArray",
					Identifiers.Byte,
					new UnsignedByte[] { UnsignedByte.valueOf(1),
							UnsignedByte.valueOf(2), UnsignedByte.valueOf(3),
							UnsignedByte.valueOf(4), UnsignedByte.valueOf(5) });
			createStaticArrayVariable("ByteStringArray",
					Identifiers.ByteString, new byte[][] {
							new byte[] { (byte) 1, (byte) 2, (byte) 3 },
							new byte[] { (byte) 2, (byte) 3, (byte) 4 },
							new byte[] { (byte) 3, (byte) 4, (byte) 5 },
							new byte[] { (byte) 4, (byte) 5, (byte) 6 },
							new byte[] { (byte) 5, (byte) 6, (byte) 7 } });
			createStaticArrayVariable(
					"DateTimeArray",
					Identifiers.DateTime,
					new DateTime[] { DateTime.currentTime(),
							DateTime.currentTime(), DateTime.currentTime(),
							DateTime.currentTime(), DateTime.currentTime() });
			createStaticArrayVariable("DoubleArray", Identifiers.Double,
					new Double[] { (double) 1, (double) 2, (double) 3,
							(double) 4, (double) 5 });
			createStaticArrayVariable("FloatArray", Identifiers.Float,
					new Float[] { (float) 1, (float) 2, (float) 3, (float) 4,
							(float) 5 });
			createStaticArrayVariable(
					"GUIDArray",
					Identifiers.Guid,
					new UUID[] { UUID.randomUUID(), UUID.randomUUID(),
							UUID.randomUUID(), UUID.randomUUID(),
							UUID.randomUUID() });
			createStaticArrayVariable("Int16Array", Identifiers.Int16,
					new Short[] { (short) 1, (short) 2, (short) 3, (short) 4,
							(short) 5 });
			createStaticArrayVariable("Int32Array", Identifiers.Int32,
					new Integer[] { 1, 2, 3, 4, 5 });
			createStaticArrayVariable("Int64Array", Identifiers.Int64,
					new Long[] { (long) 1, (long) 2, (long) 3, (long) 4,
							(long) 5 });
			createStaticArrayVariable("SByteArray", Identifiers.SByte,
					new Byte[] { (byte) 0, (byte) 15, (byte) 255, (byte) 15,
							(byte) 0 });
			createStaticArrayVariable("StringArray", Identifiers.String,
					new String[] { "testString1", "testString2", "testString3",
							"testString4", "testString5" });
			createStaticArrayVariable(
					"UInt16Array",
					Identifiers.UInt16,
					new UnsignedShort[] { UnsignedShort.valueOf(1),
							UnsignedShort.valueOf(2), UnsignedShort.valueOf(3),
							UnsignedShort.valueOf(4), UnsignedShort.valueOf(5) });
			createStaticArrayVariable(
					"UInt32Array",
					Identifiers.UInt32,
					new UnsignedInteger[] { UnsignedInteger.valueOf(1),
							UnsignedInteger.valueOf(2),
							UnsignedInteger.valueOf(3),
							UnsignedInteger.valueOf(4),
							UnsignedInteger.valueOf(5) });
			createStaticArrayVariable(
					"UInt64Array",
					Identifiers.UInt64,
					new UnsignedLong[] { UnsignedLong.valueOf(1),
							UnsignedLong.valueOf(2), UnsignedLong.valueOf(3),
							UnsignedLong.valueOf(4), UnsignedLong.valueOf(5) });
			createStaticArrayVariable("XmlElementArray",
					Identifiers.XmlElement, new XmlElement[] {
							new XmlElement("<testElement1 />"),
							new XmlElement("<testElement2 />"),
							new XmlElement("<testElement3 />"),
							new XmlElement("<testElement4 />"),
							new XmlElement("<testElement5 />") });

			// Folder for DataItem test variables
			final NodeId dataItemFolderId = new NodeId(complianceNamespaceIndex, "DataItemsFolder");
			dataItemFolder = complianceNodeManager.createFolder("DataItems", dataItemFolderId);
			staticDataFolder.addReference(dataItemFolder, Identifiers.Organizes, false);

			// createDataItem("Boolean", Identifiers.Boolean, true);
			createDataItem("Byte", Identifiers.Byte, UnsignedByte.valueOf(0));
			// createDataItem("ByteString", Identifiers.ByteString,
			// new byte[] { (byte) 0 });
			createDataItem("DateTime", Identifiers.DateTime, DateTime.currentTime());
			createDataItem("Double", Identifiers.Double, (double) 0);
			createDataItem("Float", Identifiers.Float, (float) 0);
			// createDataItem("GUID", Identifiers.Guid, UUID.randomUUID());
			createDataItem("Int16", Identifiers.Int16, (short) 0);
			createDataItem("Int32", Identifiers.Int32, 0);
			createDataItem("Int64", Identifiers.Int64, (long) 0);
			createDataItem("SByte", Identifiers.SByte, (byte) 0);
			createDataItem("String", Identifiers.String, "testString");
			createDataItem("UInt16", Identifiers.UInt16, UnsignedShort.valueOf(0));
			createDataItem("UInt32", Identifiers.UInt32, UnsignedInteger.valueOf(0));
			createDataItem("UInt64", Identifiers.UInt64, UnsignedLong.valueOf(0));

			// Folder for DataItem test variables
			final NodeId analogItemFolderId = new NodeId(complianceNamespaceIndex, "AnalogItemsFolder");
			analogItemFolder = complianceNodeManager.createFolder("AnalogItems", analogItemFolderId);
			staticDataFolder.addReference(analogItemFolder, Identifiers.Organizes, false);

			createAnalogItem("Byte", Identifiers.Byte, UnsignedByte.valueOf(0), analogItemFolder);
			createAnalogItem("Double", Identifiers.Double, (double) 0, analogItemFolder);
			createAnalogItem("Float", Identifiers.Float, (float) 0, analogItemFolder);
			createAnalogItem("Int16", Identifiers.Int16, (short) 0, analogItemFolder);
			createAnalogItem("Int32", Identifiers.Int32, 0, analogItemFolder);
			createAnalogItem("Int64", Identifiers.Int64, (long) 0, analogItemFolder);
			createAnalogItem("SByte", Identifiers.SByte, (byte) 0, analogItemFolder);
			createAnalogItem("UInt16", Identifiers.UInt16, UnsignedShort.valueOf(0), analogItemFolder);
			createAnalogItem("UInt32", Identifiers.UInt32, UnsignedInteger.valueOf(0), analogItemFolder);
			createAnalogItem("UInt64", Identifiers.UInt64, UnsignedLong.valueOf(0), analogItemFolder);

			// Folder for static test array variables
			final NodeId analogItemArrayFolderId = new NodeId(complianceNamespaceIndex, "AnalogItemArrayFolder");
			analogItemArrayFolder = complianceNodeManager.createFolder("AnalogItemArrays", analogItemArrayFolderId);
			staticDataFolder.addReference(analogItemArrayFolder, Identifiers.Organizes, false);

			createAnalogItemArray("Double", Identifiers.Double,
					new Double[] { (double) 1, (double) 2, (double) 3,
							(double) 4, (double) 5 }, analogItemArrayFolder);
			createAnalogItemArray("Float", Identifiers.Float, new Float[] {
					(float) 1, (float) 2, (float) 3, (float) 4, (float) 5 },
					analogItemArrayFolder);
			createAnalogItemArray("Int16", Identifiers.Int16, new Short[] {
					(short) 1, (short) 2, (short) 3, (short) 4, (short) 5 },
					analogItemArrayFolder);
			createAnalogItemArray("Int32", Identifiers.Int32, new Integer[] {
					1, 2, 3, 4, 5 }, analogItemArrayFolder);
			createAnalogItemArray(
					"UInt16",
					Identifiers.UInt16,
					new UnsignedShort[] { UnsignedShort.valueOf(1),
							UnsignedShort.valueOf(2), UnsignedShort.valueOf(3),
							UnsignedShort.valueOf(4), UnsignedShort.valueOf(5) },
					analogItemArrayFolder);
			createAnalogItemArray(
					"UInt32",
					Identifiers.UInt32,
					new UnsignedInteger[] { UnsignedInteger.valueOf(1),
							UnsignedInteger.valueOf(2),
							UnsignedInteger.valueOf(3),
							UnsignedInteger.valueOf(4),
							UnsignedInteger.valueOf(5) }, analogItemArrayFolder);

			// Folder for deep object chain
			final NodeId deepFolderId = new NodeId(complianceNamespaceIndex,
					"DeepFolder");
			deepFolder = complianceNodeManager.createFolder("DeepFolder",
					deepFolderId);
			staticDataFolder.addReference(deepFolder, Identifiers.Organizes,
					false);

			addDeepObject(deepFolder, 1, 20);

			// / COMPLIANCE TEST NODES END HERE ///

			logger.info("Compliance address space created.");
		} catch (StatusException e) {
			logger.error("Error occurred with creating compliance nodes: ", e);
		} catch (UaInstantiationException e) {
			logger.error("Error occurred with creating compliance nodes: ", e);
		}
	}

	protected UaVariableNode createStaticVariable(String dataTypeName, NodeId dataType, Object initialValue) throws StatusException {
		final NodeId nodeId = new NodeId(complianceNamespaceIndex, dataTypeName);
		UaType type = uaServer.getNodeManagerRoot().getType(dataType);
		UaVariableNode node = new CacheVariable(complianceNodeManager, nodeId, dataTypeName, Locale.ENGLISH);
		node.setDataType(type);
		node.setValue(new DataValue(new Variant(initialValue), StatusCode.GOOD, new DateTime(), new DateTime()));
		staticVariableFolder .addReference(node, Identifiers.HasComponent, false);
		return node;
	}

	private void createStaticArrayVariable(String dataTypeName, NodeId dataType, Object initialValue) throws StatusException {
		final NodeId nodeId = new NodeId(complianceNamespaceIndex, dataTypeName);
		UaType type = uaServer.getNodeManagerRoot().getType(dataType);
		UaVariableNode node = new CacheVariable(complianceNodeManager, nodeId, dataTypeName, Locale.ENGLISH);
		node.setDataType(type);
		node.setTypeDefinition(type);
		node.setValueRank(ValueRanks.OneDimension);
		node.setArrayDimensions(new UnsignedInteger[] { UnsignedInteger.valueOf(Array.getLength(initialValue)) });

		node.setValue(new DataValue(new Variant(initialValue), StatusCode.GOOD, new DateTime(), new DateTime()));
		staticArrayVariableFolder.addReference(node, Identifiers.HasComponent, false);
	}

	private void createDataItem(String dataTypeName, NodeId dataTypeId, Object initialValue) throws StatusException, UaInstantiationException {
		DataItemType node = complianceNodeManager.createInstance(DataItemTypeNode.class, dataTypeName + "DataItem");

		node.setDataTypeId(dataTypeId);
		node.setValue(new DataValue(new Variant(initialValue), StatusCode.GOOD, new DateTime(), new DateTime()));
		dataItemFolder.addReference(node, Identifiers.HasComponent, false);
	}

	/**
	 * @throws StatusException
	 *
	 */
	private void createFileNodeManager() throws StatusException {
		fileNodeManager = new FileNodeManager(uaServer, "http://prosysopc.com/OPCUA/FileTransfer", "Files");
		uaServer.getNodeManagerRoot().getObjectsFolder().addReference(fileNodeManager.getRootFolder(),Identifiers.Organizes, false);
		FileFolderType folder = fileNodeManager.addFolder("Folder");
		folder.setFilter("*");
	}

	private AnalogItemType createAnalogItem(String dataTypeName, NodeId dataTypeId, Object initialValue, UaNode folder) throws NodeBuilderException, StatusException {
		// Configure the optional nodes using a NodeBuilderConfiguration
		NodeBuilderConfiguration conf = new NodeBuilderConfiguration();

		// You can use NodeIds to define Optional nodes (good for standard UA
		// nodes as they always have namespace index of 0)
		conf.addOptional(Identifiers.AnalogItemType_EngineeringUnits);

		// You can also use ExpandedNodeIds with NamespaceUris if you don't know
		// the namespace index.
		conf.addOptional(new ExpandedNodeId(NamespaceTable.OPCUA_NAMESPACE, Identifiers.AnalogItemType_InstrumentRange.getValue()));

		// You can also use the BrowsePath from the type if you like (the type's
		// BrowseName is not included in the path, so this configuration will
		// apply to any type which has the same path)
		// You can use Strings for 0 namespace index, QualifiedNames for 1-step
		// paths and BrowsePaths for full paths
		// Each type interface has constants for it's structure (1-step deep)
		conf.addOptional(AnalogItemType.DEFINITION);

		// Use the NodeBuilder to create the node
		final AnalogItemType node = complianceNodeManager .createNodeBuilder(AnalogItemType.class, conf) .setName(dataTypeName + "AnalogItem").build();

		node.setDefinition("Sample AnalogItem of type " + dataTypeName);
		node.setDataTypeId(dataTypeId);
		node.setValueRank(ValueRanks.Scalar);

		node.setEngineeringUnits(new EUInformation("http://www.example.com", 3, new LocalizedText("kg", LocalizedText.NO_LOCALE), new LocalizedText("kilogram", Locale.ENGLISH)));

		node.setEuRange(new Range(0.0, 1000.0));
		node.setValue(new DataValue(new Variant(initialValue), StatusCode.GOOD, DateTime.currentTime(), DateTime.currentTime()));
		folder.addReference(node, Identifiers.HasComponent, false);
		return node;
	}

	private AnalogItemType createAnalogItemArray(String dataTypeName, NodeId dataType, Object initialValue, UaNode folder) throws StatusException, NodeBuilderException {
		AnalogItemType node = createAnalogItem(dataTypeName + "Array", dataType, initialValue, folder);
		node.setValueRank(ValueRanks.OneDimension);
		node.setArrayDimensions(new UnsignedInteger[] { UnsignedInteger .valueOf(Array.getLength(initialValue)) });
		return node;
	}

	private void addDeepObject(UaNode parent, int depth, int maxDepth) {
		if (depth <= maxDepth) {
			final String name = String.format("DeepObject%02d", depth);
			UaObjectNode newObject = new UaObjectNode(complianceNodeManager, new NodeId(complianceNamespaceIndex, name), name, Locale.ENGLISH);
			
			try {
				complianceNodeManager.addNodeAndReference(parent, newObject, Identifiers.Organizes);
			} catch (StatusException e) {
				
			}
			
			addDeepObject(newObject, depth + 1, maxDepth);
		}
	}

	
	
	// private methods
	
	private String readFromInputStream() throws IOException {
		return is.readUTF();
	}
	
	private void writeToOutputStream(String line) {
		if(os != null)
			os.println(line);
	}

	private static ASNodeManager createNodeManager(String ns) throws UaInstantiationException, StatusException {
		ASNodeManager newNodeManager = new ASNodeManager(uaServer, ns);
		newNodeManager.addListener(myNodeManagerListener);
		newNodeManager.storeNodeManagerListener(myNodeManagerListener);
		newNodeManager.getIoManager().addListeners(MyIOListener);
		newNodeManager.storeCustomIOListener(MyIOListener);
		newNodeManager.getHistoryManager().setListener(myHistorian);
		newNodeManager.setEntryNode(myNodeManager.getEntryNode());
		return newNodeManager;
	}

	private static void printErr(Exception e) {
		System.err.println(e.toString());
		if (e.getCause() != null)
			System.err.println("Caused by: " + e.getCause());
	}
}
