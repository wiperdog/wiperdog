package org.wiperdog.installer.internal;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Installer util
 * @author nguyenvannghia
 *
 */
public class InstallerUtil {
	/**
	 * Parsing the root node of installer XML file
	 * @param elt Root node
	 */
	public static void parseXml(org.w3c.dom.Element elt){
		String welcomeMsg = "";
        String appName = "";
        String appVersion = "";	
        String installAsOSService = "";
        String runInstallerSyntax = "";
        String readmePath = "";
        String licensePath = "";
        String installLogPath = "";
        String installMode = "";

		NodeList nodes = elt.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			// skips non-element nodes
			if (node instanceof org.w3c.dom.Element) {
				org.w3c.dom.Element nodeElt = (org.w3c.dom.Element) node;
				// parses the node data according to each type of node
				if (nodeElt.getTagName().equals("welcome")) {
					// the welcome message data, which may contain several
					// paragraphs of text, both plain and HTML-formatted
					NodeList welcomeNodes = nodeElt.getChildNodes();
					// cycles through the paragraphs
					for (int j = 0; j < welcomeNodes.getLength(); j++) {
						org.w3c.dom.Element paragraph = (org.w3c.dom.Element) welcomeNodes.item(j);
						Text textNode = (Text) paragraph.getFirstChild();
						//						if ((textData = textNode.getData()) != null) {
						// needs to disregard text null text nodes, such as from empty
						// paragraphs
						if (textNode != null) {
							welcomeMsg = welcomeMsg + textNode.getData().trim();
						}
						// add newline, even if empty text node, for such nodes may
						// specify blank lines
						if (j != welcomeNodes.getLength() - 1) {
							welcomeMsg = welcomeMsg + "\n";
						}
					}
				} else if (nodeElt.getTagName().equals("appName")) {
					// package name
					NodeList appNameNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) appNameNodes.item(0);
					Text textNode = (Text) path.getFirstChild();
					if (textNode != null) {
						appName = textNode.getData().trim();
					}
				} else if (nodeElt.getTagName().equals("appVersion")) {
					// package name
					NodeList packageNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) packageNodes.item(0);
					Text textNode = (Text) path.getFirstChild();					
					if (textNode != null) {
						appVersion = textNode.getData().trim();		
					}
				} else if (nodeElt.getTagName().equals("runAppStr")) {
					// location of the output dir for the command to run an application
					// after extraction; any paths are relative to the installation location
					NodeList logoNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
					Text textNode = (Text) path.getFirstChild();
					if (textNode != null) {
						runInstallerSyntax = textNode.getData().trim();
					}
				} else if (nodeElt.getTagName().equals("readmePath")) {
					// location of the output dir for the path to a readme
					// to display after extraction; any paths are relative to the 
					// installation loc
					NodeList logoNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
					Text textNode = (Text) path.getFirstChild();
					if (textNode != null) {
						readmePath = textNode.getData().trim();
					}
				} else if (nodeElt.getTagName().equals("licensePath")) {
					// location of the output dir for the path to the license file
					NodeList logoNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
					Text textNode = (Text) path.getFirstChild();
					if (textNode != null) {
						licensePath = textNode.getData().trim();
					}
				} else if (nodeElt.getTagName().equals("installAsOSService")) {
					// location of the output dir for the path to the license file
					NodeList logoNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
					Text textNode = (Text) path.getFirstChild();
					if (textNode != null) {
						installAsOSService = textNode.getData().trim();
					}
				}else if (nodeElt.getTagName().equals("installLogPath")) {
					// location of the output dir for the path to the license file
					NodeList logoNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
					Text textNode = (Text) path.getFirstChild();
					if (textNode != null) {
						installLogPath = textNode.getData().trim();
					}
				}else if (nodeElt.getTagName().equals("installMode")) {
					// location of the output dir for the path to the license file
					NodeList logoNodes = nodeElt.getChildNodes();
					org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
					Text textNode = (Text) path.getFirstChild();
					if (textNode != null) {
						installMode = textNode.getData().trim();
					}
				}
			}
		}
		InstallerXML installerXml = InstallerXML.getInstance();
		installerXml.setAppName(appName);
		installerXml.setWelcomeMsg(welcomeMsg);
		installerXml.setAppVersion(appVersion);
		installerXml.setInstallAsOSService(installAsOSService);
		installerXml.setRunInstallerSyntax(runInstallerSyntax);
		installerXml.setReadmePath(readmePath);
		installerXml.setLicensePath(licensePath);
		installerXml.setInstallLogPath(installLogPath);
		installerXml.setInstallMode(installMode);
	}
}
