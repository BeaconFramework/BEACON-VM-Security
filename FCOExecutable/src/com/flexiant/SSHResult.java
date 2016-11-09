package com.flexiant;

	public enum SSHResult {
		AUTH_FAIL(3), REFUSED(2), SUCCESS(1);

		private final int result;

		private SSHResult(int result) {

			this.result = result;
		}
	}
