package com.flexiant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
* This is just a rough draft of the executable jar that will be called by the FCO trigger 
* when a VM is created by a user with certain key.
* 
* Use case yet to be considered - handling multiple creation of VMs
*/
public class ClientSocket {

	private static final LogManager LOG_MANAGER = LogManager.getLogManager();
	private static final Logger LOGGER = Logger.getLogger("logger");
	
	//IP of the server hosting openvas
	private static final String SCANNER_IP = "";
	//Port used by scanner listener
	private static final int PORT = 8341;

	// Fetch the log configuration
	static{
		try {
			//this needs updated to load location from xml path instead
			//LOG_MANAGER.readConfiguration(new FileInputStream("/Users/dwhigham/Desktop/beaconscannerjar/logging.properties"));
			LOG_MANAGER.readConfiguration(new FileInputStream("/home/mramannavar/logging.properties"));
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error in loading Logger configuration", exception);
		}	
	}
	
	//static String SCANNER_IP = "";
	//static int PORT ;
	
	public static void main(String[] args) {
		LOGGER.log(Level.INFO, "This will be good to see");
	/*	
		// New V1 XML Load file for  details
		try{
		File fXmlFile = new File("details.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("details");
		
		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = nList.item(temp);
										
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;
				SCANNER_IP = eElement.getAttribute("ip");
				PORT =  Integer.parseInt(eElement.getAttribute("port"));
			}
		}
		}
		catch (Exception e) {
			e.printStackTrace();	
		}
		*/
		if (args.length > 0) {
			String serverUUID = args[0];
			String serverIP = args[1];
			String emailID = args[2];
			LOGGER.log(Level.INFO, "Execuatble has been passed with following args: " 
					+ serverUUID + " " + serverIP + " " + emailID);

			VMDetails details = new VMDetails(serverIP, serverUUID, emailID);
			
			try {
				// Added sleep here till the VM actually starts running
				// However this is not 100% reliable, maybe try SSH the VM TODO
				Thread.sleep(180000); // 3 minutes
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
			
			sendDataOverSocket(details);
		} else {
			LOGGER.log(Level.SEVERE, "Error - No arguments passed");
		}
	}
	
	static void sendDataOverSocket(VMDetails details) {
		LOGGER.log(Level.FINE, "Attempt to send the file");
		Socket socket = null;
		ObjectOutputStream oos = null;
		
		try {
		    String host = SCANNER_IP;
		    socket = new Socket(host, PORT);
		    oos = new ObjectOutputStream(socket.getOutputStream());
		    oos.writeObject(details);
		    LOGGER.log(Level.INFO, "The data has been sent to the scanner VM");
		
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e );
		
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
			    if (socket != null) {
			    	socket.close();
			    }
			} catch (IOException e) {
        		LOGGER.log(Level.SEVERE, "Failed to close connection", e);
			}
		}
	}
}

