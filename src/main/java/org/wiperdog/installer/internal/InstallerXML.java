package org.wiperdog.installer.internal;


/**
 * Installer XML Bean for reference
 * @author nguyenvannghia
 *
 */
public class InstallerXML {
	public static InstallerXML instance;
	public static InstallerXML getInstance(){
		if(instance == null ) instance = new InstallerXML();
		return instance;
	}
	
	private String welcomeMsg = "";
	private String appName = "";
	private String appVersion = "";	
	private String installAsOSService = "";
	private String runInstallerSyntax = "";
	private String readmePath = "";
	private String licensePath = "";
	public InstallerXML(){}
	
	public String getWelcomeMsg() {
		return welcomeMsg;
	}
	public void setWelcomeMsg(String welcomeMsg) {
		this.welcomeMsg = welcomeMsg;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getAppVersion() {
		return appVersion;
	}
	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}
	
	public String getInstallAsOSService() {
		return installAsOSService;
	}
	public void setInstallAsOSService(String installAsOSService) {
		this.installAsOSService = installAsOSService;
	}
	public String getRunInstallerSyntax() {
		return runInstallerSyntax;
	}
	public void setRunInstallerSyntax(String runInstallerSyntax) {
		this.runInstallerSyntax = runInstallerSyntax;
	}
	public String getReadmePath() {
		return readmePath;
	}
	public void setReadmePath(String readmePath) {
		this.readmePath = readmePath;
	}
	public String getLicensePath() {
		return licensePath;
	}
	public void setLicensePath(String licensePath) {
		this.licensePath = licensePath;
	}	
}
