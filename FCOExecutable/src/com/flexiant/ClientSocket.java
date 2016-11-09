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
public class ClientSocket {

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

	private static void trySSHConnection(String username, String password, String serverIP, int sshPort)
			throws JSchException {

		JSch jsch = new JSch();
		Session session = jsch.getSession(username, serverIP, sshPort);
		session.setPassword(password);

		session.setConfig("StrictHostKeyChecking", "no");
		LOGGER.log(Level.INFO, "Trying SSH Connection");

		session.connect();
		LOGGER.log(Level.INFO, "Connection Successful");
	}

	private static Enum<SSHResult> getSSHConnectionResult(String username, String password, String serverIP,
			int sshPort) throws InterruptedException {

		int sshTries = 6;
		Enum<SSHResult> result;

		for (int i = 0; i <= sshTries; i++) {

			try {
				//Try to SSH to the VM in order to check if it is scannable
				trySSHConnection(username, password, serverIP, sshPort);
				result = SSHResult.SUCCESS;
				return result;

			} catch (Exception e) {
				//Bad credentials
				if (e.getMessage().contains("Auth fail")) {

					result = SSHResult.AUTH_FAIL;
					return result;
				}
			}

			Thread.sleep(30000); // 30 Seconds
			i++;
		}

		result = SSHResult.REFUSED;
		return result;
	}

	public static void main(String[] args) throws InterruptedException {

		//No credentials
		if (args.length == 3) {
			String serverUUID = args[0];
			String serverIP = args[1];
			String emailID = args[2];

			LOGGER.log(Level.INFO,
					"Executable has been passed with following args: " + serverUUID + " " + serverIP + " " + emailID);

			VMDetails details = new VMDetails(serverIP, serverUUID, emailID, null, null);
			sendDataOverSocket(details);
		}
		
		//4 args may indicate a username but no password, or vice versa
		else if (args.length == 4) {
			String serverUUID = args[0];
			String serverIP = args[1];
			String emailID = args[2];
			String usernameOrPassword = args[3];

			LOGGER.log(Level.INFO, "Executable has been passed with following args: " + serverUUID + " " + serverIP
					+ " " + emailID + " " + usernameOrPassword);
			Enum<SSHResult> connection = getSSHConnectionResult(usernameOrPassword, null, serverIP, SSHPort);
			
			parseEnumOutput(connection,serverUUID,serverIP,emailID,usernameOrPassword,null);
		}

		//VM Details + credentials
		else if (args.length == 5) {
			String serverUUID = args[0];
			String serverIP = args[1];
			String emailID = args[2];
			String username = args[3];
			String password = args[4];

			LOGGER.log(Level.INFO, "Executable has been passed with following args: " + serverUUID + " " + serverIP
					+ " " + emailID + " " + username + " " + password);

			Enum<SSHResult> connection = getSSHConnectionResult(username, password, serverIP, SSHPort);

			parseEnumOutput(connection,serverUUID,serverIP,emailID,username,password);

		} else {
			LOGGER.log(Level.SEVERE, "Number of arguments: " + args.length);
			LOGGER.log(Level.SEVERE, "Error - No arguments passed");
		}
		System.exit(0);
	}

	public static boolean parseEnumOutput(Enum<SSHResult> enumResult, String serverUUID, String serverIP, String emailID,
			String username, String password) {

		if (enumResult.equals(SSHResult.SUCCESS)) {
			//Credentials are correct
			VMDetails details = new VMDetails(serverIP, serverUUID, emailID, username, password);
			sendDataOverSocket(details);
			return true;
		}

		else if (enumResult.equals(SSHResult.AUTH_FAIL)) {

			//Credentials are incorrect, continue scan without them
			VMDetails details = new VMDetails(serverIP, serverUUID, emailID, "nil", "nil");
			sendDataOverSocket(details);
			return true;
		}

		else if (enumResult.equals(SSHResult.REFUSED)) {
			//VM unresponsive, cancel scan
			LOGGER.log(Level.SEVERE, "Unable to connect to VM, aborting");
			return false;
		}
		
		else
			return false;

	}

	//Returns an encrypted version of VMDetails
	public static SealedObject sealDetails(VMDetails details) {

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

	static void sendDataOverSocket(VMDetails details) {
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

			// VMDetails detailsUnpacked = decryptSealedObject(sealedDetails);
			// LOGGER.log(Level.INFO, "Decrypted details:");
			// LOGGER.log(Level.INFO, detailsUnpacked.getServerUUID());
			// LOGGER.log(Level.INFO, detailsUnpacked.getIP());
			// LOGGER.log(Level.INFO, detailsUnpacked.getEmailID());
			// LOGGER.log(Level.INFO, detailsUnpacked.getUsername());
			// LOGGER.log(Level.INFO, detailsUnpacked.getPassword());

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
