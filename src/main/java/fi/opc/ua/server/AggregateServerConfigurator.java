package fi.opc.ua.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A client software to access a running AggregateServer.
 * Running the main uses a hardcoded server address localhost:4999
 */
public class AggregateServerConfigurator {
	
	// main
	
	public static void main(String[] args) {
		AggregateServerConfigurator client = new AggregateServerConfigurator();
    	System.out.println("Server configurator started. Connecting to server at localhost:4999");
		client.openConnection("localhost", 4999);
		
	    if (client.isConnected()) {
	    	System.out.println("Server connected.");
	    	try {
				System.out.println("IoT status: " + client.readFromInputStream());
			} catch (IOException e) {
				printErr(e);
				System.out.println("IoT status: unknown");
			}
	    	printMainMenu();
	    	
	    	String send = null;
	    	String receive = null;
	    	
	    	while(true) {
		    	send = null;
		    	receive = null;
		    	try {
			    	if(hasConsoleMessageToRead())
			    	{
			    		send = client.parseConsoleInput();
			    		if(send != null && !send.equals(AggregateServer.ERROR))
			    		{
			    			client.writeToOutputStream(send);
			    		
				    		while((receive = client.readFromInputStream()) != null)
				    		{
					    		if(receive != null && (receive.equals(AggregateServer.DONE) || receive.equals(AggregateServer.ERROR)))
					    			break;
					    		else
						    	   	System.out.println("Server: " + receive);
				    		}
			    		}
			    	}

			    	if(client.hasStreamMessageToRead()) {
			    		receive = client.readFromInputStream();
			    		if(receive != null)
			    			System.out.println("Server: " + receive);
			    	}

			    	if(send != null && send.equals(AggregateServer.SHUTDOWN)) {
				    	System.out.println("Shutdown initiated");
			    		break;
			    	}
			    	
			    	if(send != null && send.equals(AggregateServer.DISCONNECT)) {
				    	System.out.println("Disconnect initiated");
			    		break;
			    	}
			    	
			    	if(send != null)
			    	{
			    		printMainMenu();
			    	}
		    	} catch (Exception e) {
		    		printErr(e);
		    		System.out.println("Server has disconnected");
		    	}
	    	}
	    	
        	client.closeConnection();
	    }
	    
    	return;
	}
	
	/**
	 * Parse the given console command into a Server Configurator message
	 * @return
	 * @throws IOException
	 */
	private String parseConsoleInput() throws IOException {
		String action = null;
		String input = readConsoleInput();

		String param = null;
		
		switch(input) {
			case AggregateServer.DIAG:
				action = AggregateServer.DIAG;
				break;
			case AggregateServer.EVENT:
				action = AggregateServer.EVENT;
				break;
			case AggregateServer.LIST:
				System.out.println("Select a server to map");
				writeToOutputStream(AggregateServer.LIST);
				String receive = null;
				while((receive = readFromInputStream()) != null) {
					if(receive.equals(AggregateServer.DONE))
						break;
					else if(receive.equals(AggregateServer.ERROR)) {
						action = AggregateServer.ERROR;
						break;
					}
					else
						System.out.println(receive);
				}
				if(!(action == AggregateServer.ERROR))
				{
					System.out.println("No errors, accepting index input");
					param = readConsoleInput();
					try {
						//Check if the given input is a parseable int
						Integer.parseInt(param);
						action = param;
					} catch(NumberFormatException e) {
						System.out.println("The given input is not a number");
						action = AggregateServer.ERROR;
					}
				}
				System.out.println("list command done");
				break;
			case AggregateServer.INSERT:
				System.out.println("Enter server address");
				param = readConsoleInput();
				if(param != null && !param.isEmpty())
					action = AggregateServer.INSERT + "|" + param;
				else
					action = AggregateServer.ERROR;
				break;
			case "map":
				System.out.println("Enter server address");
				param = readConsoleInput();
				if(param != null && !param.isEmpty())
					action = AggregateServer.INSERTANDMAP + "|" + param;
				else
					action = AggregateServer.ERROR;
				break;
			case AggregateServer.IOTCLIENT:
				action = AggregateServer.IOTCLIENT;
				break;
			case AggregateServer.SHUTDOWN:
				action = AggregateServer.SHUTDOWN;
				break;
			case AggregateServer.DISCONNECT:
				action = AggregateServer.DISCONNECT;
				break;
			default:
				System.out.println("Invalid command");
				break;
		}
		
		return action;
	}

	private static void printMainMenu() {
		// 60x -
    	System.out.println();
    	System.out.println("[-----------------Server Configurator-----------------]");
    	System.out.println();
    	System.out.println("Input menu action:");
    	System.out.println("add        - Add a new node");
    	System.out.println("delete     - Delete a node");
    	System.out.println("diag       - Enable/Disable diagnostics");
    	System.out.println("event      - Send an event");
    	System.out.println("list       - List all available servers");
    	System.out.println("insert     - Insert a new server to the list");
    	System.out.println("map        - Insert a new server and map automatically");
    	System.out.println("iot        - Start IoT Ticket data transfer");
    	System.out.println("disconnect - Disconnect from the server");
    	System.out.println("shutdown   - Shutdown the connected server");
    	System.out.println();
	}

	
	
	// variables

    static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    
    private Socket serverSocket;  
    private DataOutputStream os;
    private BufferedReader is;
	
    
	
	// constructor
	
	public AggregateServerConfigurator() {
		
	}
	
	
	
	// public methods

	/**
	 * True if the connection is currently open
	 * @return
	 */
	public boolean isConnected() {
		return serverSocket != null && os != null && is != null;
	}
	
	/**
	 * Open a connection to the given host and socket number
	 * @param hostname
	 * @param socket
	 */
	public void openConnection(String hostname, int socket) {
		serverSocket = null;  
		os = null;
		is = null;

		try {
			serverSocket = new Socket(hostname, socket);
			os = new DataOutputStream(serverSocket.getOutputStream());
			is = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        } catch (UnknownHostException e) {
        	printErr("Don't know about host: hostname");
        } catch (IOException e) {
        	printErr("Couldn't get I/O for the connection to: hostname");
        }
	}
	
	/**
	 * Terminate the connection if it's open
	 */
	public void closeConnection() {
        try {
			os.close();
			os = null;
	        is.close();
	        is = null;
	        serverSocket.close();
	        serverSocket = null;
		} catch (IOException e) {
			printErr(e);
		}
	}
	
	
	
	// private methods

	/**
	 * Send a message over the connection if it's open
	 * @param message
	 * @return
	 */
	private void writeToOutputStream(String line) {
		try {
			os.writeUTF(line); //os.writeBytes(message);
			//System.out.println("DEBUG writeMessage: " + message);
		} catch (IOException e) {
			printErr(e);
			System.out.println("Error sending a message to the server");
		}
	}

	/**
	 * Returns true if the input stream has a message to read
	 * @return
	 * @throws IOException 
	 */
	private boolean hasStreamMessageToRead() throws IOException {
		return is.ready();
	}
	
	/**
	 * Read a message from the connection if it's open
	 * @return
	 * @throws IOException 
	 */
	private String readFromInputStream() throws IOException {
    	return is.readLine();
	}
	
	private static void printErr(Exception e) {
		System.err.println(e.toString());
		if (e.getCause() != null)
			System.err.println("Caused by: " + e.getCause());
	}

	private static void printErr(String e) {
		System.err.println(e);
	}

	private static boolean hasConsoleMessageToRead() {
		boolean b = false;
		
		try {
			b = stdin.ready();
		} catch (IOException e) {
			printErr(e);
		}
		
		return b;
	}
	
	private static String readConsoleInput() {
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
}
