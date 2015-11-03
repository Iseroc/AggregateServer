package fi.opc.ua.iotticket;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.opcfoundation.ua.core.MonitoringMode;

public class IoTTicketConfigurationParser {
	
	// constructor
	
	public IoTTicketConfigurationParser() {
	
	}
	
	public void parseConfigurationFile(String filename) throws ParserConfigurationException, SAXException, IOException {
		// initialize
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setFeature("http://xml.org/sax/features/namespaces", false);
		factory.setFeature("http://xml.org/sax/features/validation", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputStream stream = new FileInputStream(filename);
		Document document = builder.parse(stream);
		
		// create a new list for nodes
		configurationNodes = new ArrayList<IoTTicketConfigurationNode>();
		
		// parse the document
		NodeList nodeList = document.getDocumentElement().getElementsByTagName("node");
		
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				String nodeName = element.getTextContent();
				MonitoringMode subType = MonitoringMode.valueOf(element.getAttribute("subscriptionType"));

				IoTTicketConfigurationNode confNode = new IoTTicketConfigurationNode(nodeName, subType);
				configurationNodes.add(confNode);
				
				System.out.println("Created node " + nodeName);
			}
		}
	}
	
	List<IoTTicketConfigurationNode> configurationNodes;
	public List<IoTTicketConfigurationNode> getConfigurationNodes()
	{
		return this.configurationNodes;
	}
}