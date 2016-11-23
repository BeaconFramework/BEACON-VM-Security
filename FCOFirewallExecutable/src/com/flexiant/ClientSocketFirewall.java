package com.flexiant;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * This is just a rough draft of the executable jar that will be called by the
 * FCO trigger when a VM is created by a user with certain key.
 * 
 * Use case yet to be considered - handling multiple creation of VMs
 */
public class ClientSocketFirewall {
	
	private static final LogManager LOG_MANAGER = LogManager.getLogManager();
	private static final Logger LOGGER = Logger.getLogger("logger");

	// IP of the server hosting openvas
	private static final String SCANNER_IP = "109.231.126.132";
	private static final int PORT = 8341;
	private static final int SSHPort = 22;

	// Fetch the log configuration
	static {
		try {

			LOG_MANAGER.readConfiguration(new FileInputStream("/home/mramannavar/logging.properties"));
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error in loading Logger configuration", exception);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		
		//Two arguments, server IP and the key String
		if (args.length == 2) {
			String serverIP = args[0];
			String serverKey = args[1];
			KeyDetails keyDetails = new KeyDetails(serverIP, serverKey);
			sendDataOverSocket(keyDetails);

		} else {
			LOGGER.log(Level.SEVERE, "Number of arguments: " + args.length);
			LOGGER.log(Level.SEVERE, "Error - No arguments passed");
		}
		System.exit(0);
	}
	
	//Encrypt IP and key for extra security
	public static SealedObject sealDetails(KeyDetails details) {

		try {
			SecretKey key64 = new SecretKeySpec(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 },
					"Blowfish");
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, key64);
			SealedObject sealedObject = new SealedObject(details, cipher);

			return sealedObject;
		}

		catch (Exception e) {

			LOGGER.log(Level.SEVERE, "Error - Unable to encrypt details");
			return null;
		}
	}
	
	//Send the IP and key to the scanner listener
	static void sendDataOverSocket(KeyDetails details) {
		LOGGER.log(Level.FINE, "Attempt to send the file");
		Socket socket = null;
		ObjectOutputStream oos = null;

		try {
			String host = SCANNER_IP;
			socket = new Socket(host, PORT);
			SealedObject sealedDetails = sealDetails(details);
			
			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(sealedDetails);
			LOGGER.log(Level.INFO, "The data has been sent to the scanner VM");

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);

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
