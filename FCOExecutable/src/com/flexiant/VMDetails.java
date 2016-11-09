package com.flexiant;

import java.io.Serializable;

public class VMDetails implements Serializable {
	private static final long serialVersionUID = 1L;
	private String ip;
	private String serverUUID;
	private String emailID;
	private String username;
	private String password;

	public VMDetails(String serverIP, String serverUUID, String emailID, String username, String password) {
		this.ip = serverIP;
		this.serverUUID = serverUUID;
		this.emailID = emailID;
		this.username = username;
		this.password = password;
	}

	public String getIP() {
		return ip;
	}

	public String getServerUUID() {
		return serverUUID;
	}

	public String getEmailID() {
		return emailID;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}