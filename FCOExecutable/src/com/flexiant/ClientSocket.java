package com.flexiant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Properties;
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

	// FLEX(8341),
	// OPEN_STACK (8342),
	// OPEN_NEBULA (8343);

	private static final LogManager LOG_MANAGER = LogManager.getLogManager();
	private static final Logger LOGGER = Logger.getLogger("logger");

	// IP of the server hosting openvas
	private static String SCANNER_IP = "";
	private static int PORT = 0;
	private static final int SSHPort = 22;

	// Fetch the log configuration
	static {
		try {

			LOG_MANAGER.readConfiguration(new FileInputStream("/home/mramannavar/logging.properties"));
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error in loading Logger configuration", exception);
		}
	}

	// Try to SSH to the VM to check if is accepting connections
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

		// Number of times to try connection
		int sshTries = 6;
		Enum<SSHResult> result;
		for (int i = 0; i <= sshTries; i++) {

			try {
				// Try to SSH to the VM in order to check if it is scannable
				trySSHConnection(username, password, serverIP, sshPort);
				result = SSHResult.SUCCESS;
				return result;

			} catch (Exception e) {
				// Bad credentials
				if (e.getMessage().contains("Auth fail")) {
					
					result = SSHResult.AUTH_FAIL;
					return result;
				}
			}
			//Wait before reattempting the connection
			Thread.sleep(30000); // 30 Seconds
			i++;
		}
		
		result = SSHResult.REFUSED;
		return result;
	}
	
	//Get execution path in order to get config file path
	public static String getPath() {
		String decodedPath = new java.io.File(
				ClientSocket.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
		decodedPath = decodedPath.substring(0, decodedPath.lastIndexOf("."));
		decodedPath = decodedPath + System.getProperty("java.class.path");

		int index = decodedPath.lastIndexOf('/');
		decodedPath = decodedPath.substring(0, index);

		decodedPath = decodedPath + "/FCOExecutable.properties";
		return decodedPath;
	}

	//Load credentials from config file
	public static void loadConfig() throws URISyntaxException {

		Properties prop = new Properties();
		InputStreamReader in = null;
		try {

			String decodedPath = getPath();
			LOGGER.log(Level.INFO, "Path: " + decodedPath);

			if (decodedPath.equals("."))
				decodedPath = "/FCOExecutable.properties";

			in = new InputStreamReader(new FileInputStream(decodedPath), "UTF-8");
			prop.load(in);
			
			//Load parameters from the file
			SCANNER_IP = prop.getProperty("scannerIP");
			PORT = Integer.parseInt(prop.getProperty("port"));

			LOGGER.log(Level.INFO, "Scanner IP: " + SCANNER_IP);
			LOGGER.log(Level.INFO, "Port: " + PORT);

		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "Error loading config");
			//If config can't be loaded, abort
			System.exit(0);
		}
	}

	public static void main(String[] args) throws InterruptedException, URISyntaxException {

		//Load config file before attempting
		loadConfig();

		if (SCANNER_IP.equals("") || PORT == 0)
			System.exit(0);

		// No credentials
		if (args.length == 3) {
			String serverUUID = args[0];
			String serverIP = args[1];
			String emailID = args[2];

			LOGGER.log(Level.INFO,
					"Executable has been passed with following args: " + serverUUID + " " + serverIP + " " + emailID);

			VMDetails details = new VMDetails(serverIP, serverUUID, emailID, null, null);
			sendDataOverSocket(details);
		}

		// 4 args may indicate a username but no password, or vice versa
		else if (args.length == 4) {
			String serverUUID = args[0];
			String serverIP = args[1];
			String emailID = args[2];
			String usernameOrPassword = args[3];

			LOGGER.log(Level.INFO, "Executable has been passed with following args: " + serverUUID + " " + serverIP
					+ " " + emailID + " " + usernameOrPassword);
			Enum<SSHResult> connection = getSSHConnectionResult(usernameOrPassword, null, serverIP, SSHPort);

			parseEnumOutput(connection, serverUUID, serverIP, emailID, usernameOrPassword, null);
		}

		// VM Details + credentials
		else if (args.length == 5) {
			String serverUUID = args[0];
			String serverIP = args[1];
			String emailID = args[2];
			String username = args[3];
			String password = args[4];

			LOGGER.log(Level.INFO, "Executable has been passed with following args: " + serverUUID + " " + serverIP
					+ " " + emailID + " " + username + " " + password);

			Enum<SSHResult> connection = getSSHConnectionResult(username, password, serverIP, SSHPort);

			parseEnumOutput(connection, serverUUID, serverIP, emailID, username, password);

		} else {
			LOGGER.log(Level.SEVERE, "Number of arguments: " + args.length);
			LOGGER.log(Level.SEVERE, "Error - Bad arguments");
		}
		System.exit(0);
	}

	public static boolean parseEnumOutput(Enum<SSHResult> enumResult, String serverUUID, String serverIP,
			String emailID, String username, String password) {

		if (enumResult.equals(SSHResult.SUCCESS)) {
			// Credentials are correct
			VMDetails details = new VMDetails(serverIP, serverUUID, emailID, username, password);
			sendDataOverSocket(details);
			return true;
		}

		else if (enumResult.equals(SSHResult.AUTH_FAIL)) {

			// Credentials are incorrect, continue scan without them
			VMDetails details = new VMDetails(serverIP, serverUUID, emailID, "nil", "nil");
			sendDataOverSocket(details);
			return true;
		}

		else if (enumResult.equals(SSHResult.REFUSED)) {
			// VM unresponsive, cancel scan
			LOGGER.log(Level.SEVERE, "Unable to connect to VM, aborting");
			return false;
		}

		else
			return false;

	}

	// Returns an encrypted version of VMDetails
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
