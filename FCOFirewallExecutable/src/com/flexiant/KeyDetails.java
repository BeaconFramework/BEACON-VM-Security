package com.flexiant;

import java.io.Serializable;

public class KeyDetails implements Serializable {
	private static final long serialVersionUID = 1L;
	private String serverIP;
	private String key;

	public KeyDetails(String serverIP, String key) {
		this.serverIP = serverIP;
		this.key  = key;
	}

}