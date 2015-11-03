package fi.opc.ua.rules;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DdopParser {
	
	private static String Filename = "DemoDDOP_15";
	
	public DdopParser() {
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		DdopParser dp = new DdopParser();
		
		try {
			Document dom = dp.parseXMLFile(new File("ddop\\"+Filename+".xml"));
			//Huom että document element on (luulis olevan) kaikissa Device
			Element device = dom.getDocumentElement();
			
			List<byte[]> ddopElementsAsByteArrays = new ArrayList<byte[]>();
			
			//Get bytes for device and add it to the list
			byte[] deviceBytes = dp.deviceToBinary(device);
			ddopElementsAsByteArrays.add(deviceBytes);
			
			//Get bytes for each device element and add them to the list
			NodeList deviceElements = dp.getElementsByTag(device, "DET");
			for (int i = 0; i < deviceElements.getLength(); i++) {
				Node currentElement = deviceElements.item(i);
				byte[] currentElementAsBytes = dp.deviceElementToBinary(currentElement);
				ddopElementsAsByteArrays.add(currentElementAsBytes);
				//System.out.println("ele löydetty");
			}
			System.out.println("Kaikki DET:t muunnettu ja lisätty listaan");
			
			//Get Bytes for each device process data element and add them to the list
			NodeList deviceProcessData = dp.getElementsByTag(device, "DPD");
			for (int i = 0; i < deviceProcessData.getLength(); i++) {
				Node currentProcessData = deviceProcessData.item(i);
				byte[] currentProcessDataAsBytes = dp.deviceProcessDataToBinary(currentProcessData);
				ddopElementsAsByteArrays.add(currentProcessDataAsBytes);
				//System.out.println("pdp löydetty");
			}
			System.out.println("Kaikki DPD:t muunnettu ja lisätty listaan");
			
			//Get bytes for each device property element and add them to the  list
			NodeList deviceProperties = dp.getElementsByTag(device, "DPT");
			for (int i = 0; i < deviceProperties.getLength(); i++) {
				Node currentProperty = deviceProperties.item(i);
				byte[] currentPropertyAsBytes = dp.devicePropertyToBinary(currentProperty);
				ddopElementsAsByteArrays.add(currentPropertyAsBytes);
				//System.out.println("dpt löydetty");
			}
			System.out.println("Kaikki DPT:t muunnettu ja lisätty listaan");
			
			//Get bytes for each device value presentation and add the to the list
			NodeList deviceValuePresentations = dp.getElementsByTag(device, "DVP");
			for (int i = 0; i < deviceValuePresentations.getLength(); i++) {
				Node currentValuePresentation = deviceValuePresentations.item(i);
				byte[] currentValuePresentationAsBytes = dp.deviceValuePresentationToBinary(currentValuePresentation);
				ddopElementsAsByteArrays.add(currentValuePresentationAsBytes);
				//System.out.println("dvp löydetty");
			}
			System.out.println("Kaikki DVP:t muunnettu ja lisätty listaan");
			
			int totalSize = 0;
			for (byte[] currentArrayE : ddopElementsAsByteArrays) {
				totalSize = totalSize + currentArrayE.length;
			}
			
			byte[] ddopAsByteArray = new byte[totalSize];
			
			int currentLength = 0;
			for (byte[] currentArray : ddopElementsAsByteArrays) {
			System.arraycopy(currentArray, 0, ddopAsByteArray, currentLength, currentArray.length);
			currentLength += currentArray.length;
			}
			
			System.out.println("Ddop parsittu ja muunnettu bittijonoksi, jonka koko on " + ddopAsByteArray.length + " tavua");
			
			System.out.println("Koko paskan eka tavu: " + ddopAsByteArray[0]);
			System.out.println("Koko paskan toka tavu: " + ddopAsByteArray[1]);
			System.out.println("Koko paskan kolmas tavu: " + ddopAsByteArray[2]);
			
			Path path = Paths.get("Z:\\workspace\\DroolsProto\\"+Filename);
		    Files.write(path, ddopAsByteArray); //creates, overwrites
		    
		    System.out.println("Filu kirjotettu");
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private Document parseXMLFile(File filename) throws ParserConfigurationException, SAXException, IOException{
		
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		//Using factory get an instance of document builder
		DocumentBuilder db = dbf.newDocumentBuilder();
		//parse using builder to get DOM representation of the XML file
		Document dom = db.parse(filename);
		System.out.println("Dokkari parsittu objekteiks");
		return dom;
	
	}
	
	//Huomioi sitte että pitää pystyä ottamaan myös ainoastaan tietyn elementin lapsielementit. Onnistunee ku syöttää tälle docElen sijaan jonku deviceElen
	private NodeList getElementsByTag(Element element, String tagname) {
		NodeList nl = element.getElementsByTagName(tagname);
		System.out.println("Elementistä: " + element.getTagName() + " parsittu elementit tag namella: " + tagname + ". NodeList length: " + nl.getLength());
		return nl;	
	}
	
	private byte[] deviceToBinary(Element device) { //Ensin jokanen extractoiden stringiks -> Stringi oikeeks datatypeks -> oikee datatyyppi biteiks -> yhdistä bitit
		List<byte[]> byteArrays = new ArrayList<byte[]>();
		
		String tableId_1 = "DVC";//device.getAttribute("A");
		byte[] tableId_1_B = tableId_1.getBytes(); //TODO: Mikä charset? Ei määritelty taulukossa muuten ku String
		byteArrays.add(tableId_1_B);
		
		//Integer objectId_2 = 0;
		byte[] objectId_2_B = new byte[2];
		objectId_2_B[0] = (byte)0;//objectId_2.byteValue();
		objectId_2_B[1] = (byte)0;
		byteArrays.add(objectId_2_B);
		
		String deviceDesignator_4 = device.getAttribute("B");
		byte[] deviceDesignator_4_B = deviceDesignator_4.getBytes(Charset.forName("UTF-8")); //TODO: tarkista utf-8 no bom
		
		
		int numOfDesBytes_3 = deviceDesignator_4.getBytes(Charset.forName("UTF-8")).length; //TODO: tarkista utf-8 no bom
		byte [] numOfDesBytes_3_B = new byte[1]; //Numero 1 byte
		numOfDesBytes_3_B[0] = ((Integer) numOfDesBytes_3).byteValue();
		byteArrays.add(numOfDesBytes_3_B);
		byteArrays.add(deviceDesignator_4_B);
		
		String softwareVersion_6 = device.getAttribute("C");
		byte[] softwareVersion_6_B = softwareVersion_6.getBytes(Charset.forName("UTF-8")); //TODO: tarkista utf-8 no bom
		
		
		int numOfSoftVerBytes_5 = softwareVersion_6.getBytes(Charset.forName("UTF-8")).length; //TODO: tarkista utf-8 no bom
		byte[] numOfSoftVerBytes_5_B = new byte[1];  //Numero 1 byte
		numOfSoftVerBytes_5_B[0] = ((Integer) numOfSoftVerBytes_5).byteValue();
		byteArrays.add(numOfSoftVerBytes_5_B);
		byteArrays.add(softwareVersion_6_B);
		
		String workingSetMasterNAME_7 = device.getAttribute("D");
		byte[] workingSetMasterNAME_7_B = hexStringToByteArray(workingSetMasterNAME_7);
		byteArrays.add(workingSetMasterNAME_7_B);
		
		String serialNumber_9 = device.getAttribute("E");
		byte[] serialNumber_9_B = serialNumber_9.getBytes(Charset.forName("UTF-8")); //TODO: tarkista utf-8 no bom
		
		int numOfSerNumBytes_8 = serialNumber_9.getBytes(Charset.forName("UTF-8")).length; //TODO: tarkista utf-8 no bom
		byte[] numOfSerNumBytes_8_B = new byte[1];  //Numero 1 byte
		numOfSerNumBytes_8_B[0] = ((Integer) numOfSerNumBytes_8).byteValue();
		byteArrays.add(numOfSerNumBytes_8_B);
		byteArrays.add(serialNumber_9_B);
		
		String deviceStructureLabel_10 = device.getAttribute("F"); //TODO: BUGI, xml-filussa todnäk väärässä muodossa, kato bugilistasta tarkemmin
		byte[] deviceStructureLabel_10_B = hexStringToByteArray(deviceStructureLabel_10);//((Integer) deviceStructureLabelint).byteValue();
		byteArrays.add(deviceStructureLabel_10_B);
		//System.out.println(deviceStructureLabel_10_B[0] + " " + deviceStructureLabel_10_B[1] + " " + deviceStructureLabel_10_B[deviceStructureLabel_10_B.length-2] + " " + deviceStructureLabel_10_B[deviceStructureLabel_10_B.length-1]);
		
		String deviceLocalizationLabel_11 = device.getAttribute("G");
		byte[] deviceLocalizationLabel_11_B = hexStringToByteArray(deviceLocalizationLabel_11);
		byteArrays.add(deviceLocalizationLabel_11_B);
		//System.out.println(deviceLocalizationLabel_11_B[0] + " " + deviceLocalizationLabel_11_B[1] + " " + deviceLocalizationLabel_11_B[deviceLocalizationLabel_11_B.length-2] + " " + deviceLocalizationLabel_11_B[deviceLocalizationLabel_11_B.length-1]);
		
		byte[] numOfExtStrucLabelBytes_12_B = new byte[1]; //Numero 1 byte //TODO: varmista toimiiko näin (voi laittaa minkä tahansa intin ja tulostus näkyy inttinä myös byteValue():lla) ja että onko yleensä tarpeellinen (ISOBUS Versio?)
		numOfExtStrucLabelBytes_12_B[0] = (byte)0;
		byteArrays.add(numOfExtStrucLabelBytes_12_B);
		
		/*
		System.out.println("Device: tableId (A) = " + tableId_1 + ", objectId = 0, number of designator bytes = " + 
		numOfDesBytes_3 + ", deviceDesignator = " + deviceDesignator_4 + ", software version length = " + numOfSoftVerBytes_5
		+ ", software version = " + softwareVersion_6 + ", \n NAME = " + workingSetMasterNAME_7 + ", serialNumberByteSize = " + numOfSerNumBytes_8
		+ ", serial number = " + serialNumber_9 + ", structure label = " + deviceStructureLabel_10 + ", loc.label = " + deviceLocalizationLabel_11
		+ " \n number of extended structurelabel bytes = " + numOfExtStrucLabelBytes_12_B);
		
		System.out.println("\nDevice: tableId (A) = " + tableId_1_B + ", objectId = 0, number of designator bytes = " + 
				numOfDesBytes_3_B + ", deviceDesignator = " + deviceDesignator_4_B + ", software version length = " + numOfSoftVerBytes_5_B
				+ ", software version = " + softwareVersion_6_B + ", \n NAME = " + workingSetMasterNAME_7_B + ", serialNumberByteSize = " + numOfSerNumBytes_8_B
				+ ", serial number = " + serialNumber_9_B + ", structure label = " + deviceStructureLabel_10_B + ", loc.label = " + deviceLocalizationLabel_11_B
				+ " \n number of extended structurelabel bytes = " + numOfExtStrucLabelBytes_12_B);
		*/
		
		/*
		byte testi = (byte) 21;
		int testi2 = testi & (0xff);
		System.out.println("\n" + testi + " " + testi2 + "\n");
		*/
		
		/*
		for (int i = 0; i < deviceStructureLabel_10_B.length; i++) {
			System.out.println(deviceStructureLabel_10_B[i]);
		}
		*/
		
		byte[] result = new byte[tableId_1_B.length + objectId_2_B.length + numOfDesBytes_3_B.length + deviceDesignator_4_B.length
		                         + numOfSoftVerBytes_5_B.length + softwareVersion_6_B.length + workingSetMasterNAME_7_B.length + numOfSerNumBytes_8_B.length + serialNumber_9_B.length + 
		                         deviceStructureLabel_10_B.length + deviceLocalizationLabel_11_B.length + numOfExtStrucLabelBytes_12_B.length];
		
		
		int currentLength = 0;
		for (byte[] currentArray : byteArrays) {
		System.arraycopy(currentArray, 0, result, currentLength, currentArray.length);
		currentLength += currentArray.length;
		}
		return result;
	}
	
	private byte[] deviceElementToBinary(Node det) {
		List<byte[]> byteArrays = new ArrayList<byte[]>();
		
		NamedNodeMap detAttributes = det.getAttributes();
		
		
		String tableId_1 = "DET";//det.getAttribute("A");
		byte[] tableId_1_B = tableId_1.getBytes(); //TODO: Mikä charset? Ei määritelty taulukossa muuten ku String
		byteArrays.add(tableId_1_B);
		
		int objectId_2 = Integer.parseInt(detAttributes.getNamedItem("B").getTextContent());
		byte[] objectId_2_B = new byte[2];
		objectId_2_B[0] = (byte) (objectId_2 & 0xFF); //Numero 2 bytes
		objectId_2_B[1] = (byte) ((objectId_2 >>> 8) & 0xFF);
		byteArrays.add(objectId_2_B);
		/*
		System.out.println(objectId_2_B[0] + " " + objectId_2_B[1]);
		System.out.println(((int)objectId_2_B[1] << 8) | ((int)objectId_2_B[0] & 0xFF)); //Tää palauttaa oikeen luvun, alunperin noi oli toisin päin
		*/
		
		int devEleType_3 = Integer.parseInt(detAttributes.getNamedItem("C").getTextContent());
		byte[] devEleType_3_B = new byte[1]; //Numero 1 byte
		devEleType_3_B[0] = ((Integer) devEleType_3).byteValue();
		byteArrays.add(devEleType_3_B);
		
		byte [] numOfDesBytes_4_B = new byte[1];
		byte[] deviceEleDesignator_5_B = null;
		if (detAttributes.getNamedItem("D") != null) {
			String deviceEleDesignator_5 = detAttributes.getNamedItem("D").getTextContent();
			deviceEleDesignator_5_B = deviceEleDesignator_5.getBytes(Charset.forName("UTF-8")); //TODO: tarkista utf-8 no bom
			int numOfDesBytes_4 = deviceEleDesignator_5.getBytes(Charset.forName("UTF-8")).length; //TODO: tarkista utf-8 no bom
			//byte [] numOfDesBytes_4_B = new byte[1]; //Numero 1 byte
			numOfDesBytes_4_B[0] = ((Integer) numOfDesBytes_4).byteValue();
			byteArrays.add(numOfDesBytes_4_B);
			byteArrays.add(deviceEleDesignator_5_B);
		} else {
			//byte [] numOfDesBytes_4_B = new byte[1]; //Numero 1 byte
			numOfDesBytes_4_B[0] = (byte)0;
			byteArrays.add(numOfDesBytes_4_B);
		}
		
		
		int devEleNumber_6 = Integer.parseInt(detAttributes.getNamedItem("E").getTextContent());
		byte[] devEleNumber_6_B = new byte[2];
		devEleNumber_6_B[0] = (byte) (devEleNumber_6 & 0xFF); //Numero 2 bytes
		devEleNumber_6_B[1] = (byte) ((devEleNumber_6 >>> 8) & 0xFF);
		byteArrays.add(devEleNumber_6_B);
		
		int parentObjId_7 = Integer.parseInt(detAttributes.getNamedItem("F").getTextContent());
		byte[] parentObjId_7_B = new byte[2];
		parentObjId_7_B[0] = (byte) (parentObjId_7 & 0xFF); //Numero 2 bytes
		parentObjId_7_B[1] = (byte) ((parentObjId_7 >>> 8) & 0xFF);
		byteArrays.add(parentObjId_7_B);
		
		List<Node> detObjReferences = new ArrayList<Node>();
		NodeList detChildren = det.getChildNodes();
		for (int j = 0; j < detChildren.getLength(); j++) {
			Node currentNode = detChildren.item(j);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				detObjReferences.add(currentNode);
			}
		}
		
		int numberOfDORObjects_8 = detObjReferences.size();
		byte[] numberOfDORObjects_8_B = new byte[2];
		numberOfDORObjects_8_B[0] = (byte) (numberOfDORObjects_8 & 0xFF); //Numero 2 bytes
		numberOfDORObjects_8_B[1] = (byte) ((numberOfDORObjects_8 >>> 8) & 0xFF);
		byteArrays.add(numberOfDORObjects_8_B);
		
		for (Node DOR : detObjReferences) {
			NamedNodeMap DORAttribute = DOR.getAttributes();
			int DORObjId_9 = Integer.parseInt(DORAttribute.getNamedItem("A").getTextContent());
			byte[] DORObjId_9_B = new byte[2];
			DORObjId_9_B[0] = (byte) (DORObjId_9 & 0xFF); //Numero 2 bytes
			DORObjId_9_B[1] = (byte) ((DORObjId_9 >>> 8) & 0xFF);
			byteArrays.add(DORObjId_9_B);
			//System.out.println(DORObjId_9);
		}
		/*
		System.out.println("DeviceEle: tableId (A) = " + tableId_1 + ", objectId = " + objectId_2 +", device ele type = " + 
				devEleType_3 + ", number of designator bytes = " + numOfDesBytes_4 + ", device designator = " + deviceEleDesignator_5
				+ ", device ele number = " + devEleNumber_6 + ", \n parent object id = " + parentObjId_7 + ", number of DORs = " + numberOfDORObjects_8 + "\n");
		
		System.out.println("DeviceEle: tableId (A) = " + tableId_1_B + ", objectId = " + objectId_2_B +", device ele type = " + 
				devEleType_3_B + ", number of designator bytes = " + numOfDesBytes_4_B + ", device designator = " + deviceEleDesignator_5_B
				+ ", device ele number = " + devEleNumber_6_B + ", \n parent object id = " + parentObjId_7_B + ", number of DORs = " + numberOfDORObjects_8_B + "\n\n\n");
		*/
		byte[] result = null;
		if (deviceEleDesignator_5_B != null)
			result = new byte[tableId_1_B.length + objectId_2_B.length + devEleType_3_B.length + numOfDesBytes_4_B.length + deviceEleDesignator_5_B.length + devEleNumber_6_B.length + parentObjId_7_B.length + numberOfDORObjects_8_B.length + detObjReferences.size()*2];
		else
			result = new byte[tableId_1_B.length + objectId_2_B.length + devEleType_3_B.length + numOfDesBytes_4_B.length + devEleNumber_6_B.length + parentObjId_7_B.length + numberOfDORObjects_8_B.length + detObjReferences.size()*2];

		int currentLength = 0;
		for (byte[] currentArray : byteArrays) {
		System.arraycopy(currentArray, 0, result, currentLength, currentArray.length);
		currentLength += currentArray.length;
		}
		return result;
	}
	
	private byte[] deviceProcessDataToBinary(Node dpd) {
		
		List<byte[]> byteArrays = new ArrayList<byte[]>();
		NamedNodeMap dpdAttributes = dpd.getAttributes();
		
		String tableId_1 = "DPD";//det.getAttribute("A");
		byte[] tableId_1_B = tableId_1.getBytes(); //TODO: Mikä charset? Ei määritelty taulukossa muuten ku String
		byteArrays.add(tableId_1_B);
		
		int objectId_2 = Integer.parseInt(dpdAttributes.getNamedItem("A").getTextContent());
		byte[] objectId_2_B = new byte[2];
		objectId_2_B[0] = (byte) (objectId_2 & 0xFF); //Numero 2 bytes
		objectId_2_B[1] = (byte) ((objectId_2 >>> 8) & 0xFF);
		byteArrays.add(objectId_2_B);
		
		String DDI_3 = dpdAttributes.getNamedItem("B").getTextContent();
		if (DDI_3.length() == 2) {
			DDI_3 = "00"+DDI_3;
		}
		if (DDI_3.length() == 1) {
			DDI_3 = "000"+DDI_3;
		}
		if (DDI_3.length() == 3) {
			DDI_3 = "0"+DDI_3;
		}
		byte[] DDI_3_B = hexStringToByteArray(DDI_3);
		byteArrays.add(DDI_3_B);
		
		/*
		byte[] DDI_B_ALT = DatatypeConverter.parseHexBinary(DDI);
		byteArrays.add(DDI_B);
		System.out.println(DDI + " " + DDI.length() + " " + DDI_B.length);
		System.out.println(DDI_B[0] + " " + DDI_B[1]);
		System.out.println(DDI_B_ALT[0] + " " + DDI_B_ALT[1]);
		System.out.println(((int)DDI_B[0] << 8) | ((int)DDI_B[1] & 0xFF)); //TODO: Tää tulostaa dfff:n negatiivisena koska liian iso. -8193 + 32767 + 32767 = 57341, pitäis olla 57343. Ilmesesti menee kuitenki oikein koska DatatypeConverter antaa ihan samat.
		System.out.println(((int)DDI_B_ALT[0] << 8) | ((int)DDI_B_ALT[1] & 0xFF));
		System.out.println("");
		*/
		
		int dpdProperties_4 = Integer.parseInt(dpdAttributes.getNamedItem("C").getTextContent());
		byte[] dpdProperties_4_B = new byte[1]; //Numero 1 byte
		dpdProperties_4_B[0] = ((Integer) dpdProperties_4).byteValue();
		byteArrays.add(dpdProperties_4_B);
		
		int dpdTriggerMethods_5 = Integer.parseInt(dpdAttributes.getNamedItem("D").getTextContent());
		byte[] dpdTriggerMethods_5_B = new byte[1]; //Numero 1 byte
		dpdTriggerMethods_5_B[0] = ((Integer) dpdTriggerMethods_5).byteValue();
		byteArrays.add(dpdTriggerMethods_5_B);
		
		byte[] dpdDesignator_7_B = null;
		byte [] numOfDesBytes_6_B = new byte[1]; //Numero 1 byte
		if (dpdAttributes.getNamedItem("E") != null) {
			String dpdDesignator_7 = dpdAttributes.getNamedItem("E").getTextContent();
			dpdDesignator_7_B = dpdDesignator_7.getBytes(Charset.forName("UTF-8")); //TODO: tarkista utf-8 no bom
			int numOfDesBytes_6 = dpdDesignator_7.getBytes(Charset.forName("UTF-8")).length; //TODO: tarkista utf-8 no bom
			//byte [] numOfDesBytes_6_B = new byte[1]; //Numero 1 byte
			numOfDesBytes_6_B[0] = ((Integer) numOfDesBytes_6).byteValue();
			byteArrays.add(numOfDesBytes_6_B);
			byteArrays.add(dpdDesignator_7_B);
		} else {
			numOfDesBytes_6_B[0] = (byte)0;
			byteArrays.add(numOfDesBytes_6_B);
		}
		
		
		Node dvpObjIdNode = dpdAttributes.getNamedItem("F");
		byte[] deviceValuePresentationObjId_8_B = new byte[2];
		if (dvpObjIdNode != null) {
			int deviceValuePresentationObjId_8 = Integer.parseInt(dvpObjIdNode.getTextContent());
			deviceValuePresentationObjId_8_B[0] = (byte) (deviceValuePresentationObjId_8 & 0xFF); //Numero 2 bytes
			deviceValuePresentationObjId_8_B[1] = (byte) ((deviceValuePresentationObjId_8 >>> 8) & 0xFF);
		} else {
			String nullId = "FFFF";
			deviceValuePresentationObjId_8_B = hexStringToByteArray(nullId);
		}
		byteArrays.add(deviceValuePresentationObjId_8_B);
		
		/*
		System.out.println("DeviceProcessData: tableId (A) = " + tableId_1 + ", objectId = " + objectId_2 +", DDI = " + 
				DDI_3 + ", properties = " + dpdProperties_4 + ", avail. trigger methods = " + dpdTriggerMethods_5
				+ ", number of des.bytes = " + numOfDesBytes_6 + ", \n designator = " + dpdDesignator_7 + ", dev.val.pres.obj.id = "
				+ (((int)deviceValuePresentationObjId_8_B[1] << 8) | ((int)deviceValuePresentationObjId_8_B[0] & 0xFF)) + "\n");
		
		System.out.println("DeviceProcessData: tableId (A) = " + tableId_1_B + ", objectId = " + objectId_2_B +", DDI = " + 
				DDI_3_B + ", properties = " + dpdProperties_4_B + ", avail. trigger methods = " + dpdTriggerMethods_5_B
				+ ", number of des.bytes = " + numOfDesBytes_6_B + ", \n designator = " + dpdDesignator_7_B + ", dev.val.pres.obj.id = "
				+ (((int)deviceValuePresentationObjId_8_B[1] << 8) | ((int)deviceValuePresentationObjId_8_B[0] & 0xFF)) + "\n");
		*/
		byte[] result = null;
		if (dpdDesignator_7_B != null) {
			result = new byte[tableId_1_B.length + objectId_2_B.length + DDI_3_B.length + dpdProperties_4_B.length + 
		                         dpdTriggerMethods_5_B.length + numOfDesBytes_6_B.length + dpdDesignator_7_B.length + 
		                         deviceValuePresentationObjId_8_B.length];
		} else {
			result = new byte[tableId_1_B.length + objectId_2_B.length + DDI_3_B.length + dpdProperties_4_B.length + 
			                         dpdTriggerMethods_5_B.length + numOfDesBytes_6_B.length + 
			                         deviceValuePresentationObjId_8_B.length];
		}
		int currentLength = 0;
		for (byte[] currentArray : byteArrays) {
		System.arraycopy(currentArray, 0, result, currentLength, currentArray.length);
		currentLength += currentArray.length;
		}
		return result;
	}
	
	
	private byte[] devicePropertyToBinary (Node dpt) {
		
		List<byte[]> byteArrays = new ArrayList<byte[]>();
		NamedNodeMap dptAttributes = dpt.getAttributes();
		
		String tableId_1 = "DPT";//det.getAttribute("A");
		byte[] tableId_1_B = tableId_1.getBytes(); //TODO: Mikä charset? Ei määritelty taulukossa muuten ku String
		byteArrays.add(tableId_1_B);
		
		int objectId_2 = Integer.parseInt(dptAttributes.getNamedItem("A").getTextContent());
		byte[] objectId_2_B = new byte[2];
		objectId_2_B[0] = (byte) (objectId_2 & 0xFF); //Numero 2 bytes
		objectId_2_B[1] = (byte) ((objectId_2 >>> 8) & 0xFF);
		byteArrays.add(objectId_2_B);
		
		String DDI_3 = dptAttributes.getNamedItem("B").getTextContent();
		if (DDI_3.length() == 2) {
			DDI_3 = "00"+DDI_3;
		}
		byte[] DDI_3_B = hexStringToByteArray(DDI_3);
		byteArrays.add(DDI_3_B);
		
		int propValue_4 = Integer.parseInt(dptAttributes.getNamedItem("C").getTextContent());
		byte[] propValue_4_B = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(propValue_4).array(); //TODO: Numero 4 tavua Huom 4 tavua (SIGNED 32 bit) ja Bytebuffer.order!
		byteArrays.add(propValue_4_B);
		//System.out.println(propValue_4 + " " + propValue_4_B[0] + " " + propValue_4_B[1] + " " + propValue_4_B[2] + " " + propValue_4_B[3]);
		
		byte[] dptDesignator_6_B = null;
		byte [] numOfDesBytes_5_B = new byte[1]; //Numero 1 byte
		if (dptAttributes.getNamedItem("D") != null) {
			String dptDesignator_6 = dptAttributes.getNamedItem("D").getTextContent();
			dptDesignator_6_B = dptDesignator_6.getBytes(Charset.forName("UTF-8")); //TODO: tarkista utf-8 no bom
			int numOfDesBytes_5 = dptDesignator_6.getBytes(Charset.forName("UTF-8")).length; //TODO: tarkista utf-8 no bom
			//byte [] numOfDesBytes_5_B = new byte[1]; //Numero 1 byte
			numOfDesBytes_5_B[0] = ((Integer) numOfDesBytes_5).byteValue();
			byteArrays.add(numOfDesBytes_5_B);
			byteArrays.add(dptDesignator_6_B);
		} else {
			numOfDesBytes_5_B[0] = (byte)0;
			byteArrays.add(numOfDesBytes_5_B);
		}
		
		
		Node dvpObjIdNode = dptAttributes.getNamedItem("E");
		byte[] deviceValuePresentationObjId_7_B = new byte[2];
		if (dvpObjIdNode != null) {
			int deviceValuePresentationObjId_7 = Integer.parseInt(dvpObjIdNode.getTextContent());
			deviceValuePresentationObjId_7_B[0] = (byte) (deviceValuePresentationObjId_7 & 0xFF); //Numero 2 bytes
			deviceValuePresentationObjId_7_B[1] = (byte) ((deviceValuePresentationObjId_7 >>> 8) & 0xFF);
		} else {
			String nullId = "FFFF";
			deviceValuePresentationObjId_7_B = hexStringToByteArray(nullId);
		}
		byteArrays.add(deviceValuePresentationObjId_7_B);
		
		/*
		System.out.println("DeviceProperty: tableId (A) = " + tableId_1 + ", objectId = " + objectId_2 +", DDI = " + 
				DDI_3 + ", propertyvalue = " + propValue_4 + ", number of des bytes = " + numOfDesBytes_5
				+ ", designators = " + dptDesignator_6 + ", \n dev.val.pres.obj.id =" 
				+ (((int)deviceValuePresentationObjId_7_B[1] << 8) | ((int)deviceValuePresentationObjId_7_B[0] & 0xFF)) + "\n");
		
		System.out.println("DeviceProperty: tableId (A) = " + tableId_1_B + ", objectId = " + objectId_2_B +", DDI = " + 
				DDI_3_B + ", propertyvalue = " + propValue_4_B + ", number of des bytes = " + numOfDesBytes_5_B
				+ ", designators = " + dptDesignator_6_B + ", \n dev.val.pres.obj.id =" 
				+ (((int)deviceValuePresentationObjId_7_B[1] << 8) | ((int)deviceValuePresentationObjId_7_B[0] & 0xFF)) + "\n");
		*/
		byte[] result = null;
		if (dptDesignator_6_B != null) {
			result = new byte[tableId_1_B.length + objectId_2_B.length + DDI_3_B.length + propValue_4_B.length + 
		                         numOfDesBytes_5_B.length + dptDesignator_6_B.length + deviceValuePresentationObjId_7_B.length];
		} else {
			result = new byte[tableId_1_B.length + objectId_2_B.length + DDI_3_B.length + propValue_4_B.length + 
			                         numOfDesBytes_5_B.length + deviceValuePresentationObjId_7_B.length];
		}
		
		int currentLength = 0;
		for (byte[] currentArray : byteArrays) {
		System.arraycopy(currentArray, 0, result, currentLength, currentArray.length);
		currentLength += currentArray.length;
		}
		return result;
	}
	
	private byte[] deviceValuePresentationToBinary(Node dvp) {
		
		List<byte[]> byteArrays = new ArrayList<byte[]>();
		NamedNodeMap dvpAttributes = dvp.getAttributes();
		
		String tableId_1 = "DVP";//det.getAttribute("A");
		byte[] tableId_1_B = tableId_1.getBytes(); //TODO: Mikä charset? Ei määritelty taulukossa muuten ku String
		byteArrays.add(tableId_1_B);
		
		int objectId_2 = Integer.parseInt(dvpAttributes.getNamedItem("A").getTextContent());
		byte[] objectId_2_B = new byte[2];
		objectId_2_B[0] = (byte) (objectId_2 & 0xFF); //Numero 2 bytes Näin päin on little endian!!!!
		objectId_2_B[1] = (byte) ((objectId_2 >>> 8) & 0xFF);
		byteArrays.add(objectId_2_B);
		//System.out.println("2 byte object id: " + objectId_2 + "  " + objectId_2_B[0] + "  " + objectId_2_B[1]);
		//System.out.println("Sama id byteista: " + (((objectId_2_B[1] & 0xff) << 8) | (objectId_2_B[0] & 0xff))); //näin päin tulostaa little-endianin
		
		int offset_3 = Integer.parseInt(dvpAttributes.getNamedItem("B").getTextContent());
		byte[] offset_3_B = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(offset_3).array(); //Numero 4 tavua Huom 4 tavua (SIGNED 32 bit) ja Bytebuffer.order!
		byteArrays.add(offset_3_B);
		//System.out.println("4 byte offset: " + offset_3 + " " + offset_3_B[0] + " " + offset_3_B[1] + " " + offset_3_B[2] + " " + offset_3_B[3] );
		//System.out.println(offset_3 + " " + offset_3_B[0] + " " + offset_3_B[1] + " " + offset_3_B[2] + " " + offset_3_B[3]);
		
		Float scale_4 = Float.parseFloat(dvpAttributes.getNamedItem("C").getTextContent());
		//byte[] scale_4_B = ByteBuffer.allocate(4).putFloat(scale_4).array(); //Numero 4 tavua Huom 4 tavua FLOAT ja (LITTLE)ENDIANNESS ja Bytebuffer.order!
		byte[] scale_4_B = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(scale_4).array(); //Numero 4 tavua Huom 4 tavua FLOAT ja (LITTLE)ENDIANNESS ja Bytebuffer.order!
		//System.out.println(scale_4 + " " + scale_4_B[0] + " " + scale_4_B[1] + " " + scale_4_B[2] + " " + scale_4_B[3]);
		//float f = ByteBuffer.wrap(scale_4_B).order(ByteOrder.LITTLE_ENDIAN).getFloat(); //TODO: Tää toimii BIGGInä jos ei oo putFloatissa määritelty, pitäis ehkä kuitenki olla LITTLE
		//System.out.println(f);
		byteArrays.add(scale_4_B);
		
		int numOfDecimals_5 = Integer.parseInt(dvpAttributes.getNamedItem("D").getTextContent());
		byte[] numOfDecimals_5_B = new byte[1]; //Numero 1 byte
		numOfDecimals_5_B[0] = ((Integer) numOfDecimals_5).byteValue();
		byteArrays.add(numOfDecimals_5_B);
		
		byte[] unitDesignator_7_B = null;
		String unitDesignator_7 = "";
		Node unitDesignator_7_node = dvpAttributes.getNamedItem("E");
		
		byte [] numOfDesBytes_6_B = new byte[1]; //Numero 1 byte
		if (unitDesignator_7_node != null) {
			unitDesignator_7 = unitDesignator_7_node.getTextContent();
			unitDesignator_7_B = unitDesignator_7.getBytes();
			
			int deslength = unitDesignator_7.getBytes(Charset.forName("UTF-8")).length;
			numOfDesBytes_6_B[0] = ((Integer) deslength).byteValue();
			
			byteArrays.add(numOfDesBytes_6_B);
			byteArrays.add(unitDesignator_7_B);
		} else {
			numOfDesBytes_6_B[0] = (byte)0;
			byteArrays.add(numOfDesBytes_6_B);
		}
		
		System.out.println(numOfDesBytes_6_B[0]);
		
		/*
		System.out.println("DeviceValuePres.: tableId (A) = " + tableId_1 + ", objectId = " + objectId_2 +", Offset = " + 
				offset_3 + ", scale = " + scale_4 + ", number of decimals = " + numOfDecimals_5
				+ ", unitdesignator = " + unitDesignator_6 + "\n");
		
		System.out.println("DeviceValuePres.: tableId (A) = " + tableId_1_B + ", objectId = " + objectId_2_B +", Offset = " + 
				offset_3_B + ", scale = " + scale_4_B + ", number of decimals = " + numOfDecimals_5_B
				+ ", unitdesignator = " + unitDesignator_6_B + "\n");
		*/
		
		byte[] result = null;
		if (unitDesignator_7_B != null) {
			result = new byte[tableId_1_B.length + objectId_2_B.length + offset_3_B.length + scale_4_B.length + 
			                         numOfDecimals_5_B.length + numOfDesBytes_6_B.length + unitDesignator_7_B.length];
			System.out.println("ei null");
		} else {
			result = new byte[tableId_1_B.length + objectId_2_B.length + offset_3_B.length + scale_4_B.length + 
			                         numOfDecimals_5_B.length + numOfDesBytes_6_B.length];
			System.out.println("null");
		}
		

		int currentLength = 0;
		for (byte[] currentArray : byteArrays) {
		System.arraycopy(currentArray, 0, result, currentLength, currentArray.length);
		currentLength += currentArray.length;
		}
		return result;
	}
	
	
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	/*
	public static byte[] intStringToByteArray(String s) { //TODO: TÄÄ JA TOI STRUCTURE LABEL KUSEE. Toteuta niin, että varsinainen luku käännetään normisti ja sit lisätään leading zerot
	    int len = s.length();
	    byte[] data = new byte[len];
	    for (int i = 0; i < len; i += 1) {
	        char current = s.charAt(i);
	        Integer currentint = Character.getNumericValue(current); //TODO: SHORT?
	        data[i] = currentint.byteValue();
	    }
	    return data;
	}
	*/
	
	
	

}
