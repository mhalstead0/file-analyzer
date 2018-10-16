package com.matthalstead.fileanalyzer.filefetch;

public class LoginProvider {

	private String systemName;
	private boolean attempted;
	private boolean failed;
	private String username;
	private String password;
	
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public boolean isAttempted() {
		return attempted;
	}
	public void setAttempted(boolean attempted) {
		this.attempted = attempted;
	}
	public boolean isFailed() {
		return failed;
	}
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	public String getSystemName() {
		return systemName;
	}
	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}
}
