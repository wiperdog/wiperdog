package org.wiperdog.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.wiperdog.installer.internal.InstallerUtil;
import org.wiperdog.installer.internal.InstallerXML;
import org.wiperdog.installer.internal.XMLErrorHandler;

/**
 * Self-extractor main class
 * @author nguyenvannghia
 *
 */
public class SelfExtractorCmd {	 
	public static String OUTPUT_FOLDER = "";
	public static void main(String args[]){
		String userDir = System.getProperty("user.dir");	
		try {
			if (args == null || args.length == 0 || args[0] == null
					|| args[0].equals("")) {
				System.out
						.println("Wrong parameter. Usage: java -jar <Installer Jar> <INSTALL_PATH>");
				System.exit(0);
			}
			OUTPUT_FOLDER = (String) args[0];
			File outputDir = new File(OUTPUT_FOLDER);
			if(!outputDir.isAbsolute()) {
				OUTPUT_FOLDER = new File (userDir, OUTPUT_FOLDER).getAbsolutePath();
			}
			System.out.println("Wiperdog will be install to directory: "
					+ OUTPUT_FOLDER);
			String jarPath = SelfExtractorCmd.class.getProtectionDomain()
					.getCodeSource().getLocation().getFile();
			//-- Stopping service 						
			if(System.getProperty("os.name").toLowerCase().indexOf("win") != -1){
				System.out.println("");				
				System.out.println("Stop wiperdog service: Start");
				stopService();
				System.out.println("Stop wiperdog service: End");
			}
			
			unZip(jarPath, OUTPUT_FOLDER);			
			String newJarPath = (System.getProperty("os.name").toLowerCase()
					.indexOf("win") != -1) ? jarPath.substring(1, jarPath
					.length()) : jarPath;
			runGroovyInstaller(newJarPath);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Run the installer script written in Groovy
	 * @param jarPath path to this Jar file, used in classpath for the java process
	 * @throws Exception any exception
	 */
	static void runGroovyInstaller(String jarPath)throws Exception{		
		//- risk when user choose the output directory in another volume, which is different from current volume
		File workDir = new File(OUTPUT_FOLDER);		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		docBuilder.setErrorHandler(new XMLErrorHandler());
		Document doc = docBuilder.parse(InstallerUtil.class.getResourceAsStream("/extractor.xml"));
		InstallerUtil.parseXml(doc.getDocumentElement());
		
		if(InstallerXML.getInstance().getRunInstallerSyntax() == null || InstallerXML.getInstance().getRunInstallerSyntax().equals(""))
			throw new Exception("Cannot run configuration for newly installed Wiperdog");
		
		//Run java process, e.g: java -jar lib/java/bundle/groovy-all-2.0.2-beta-2.jar installer/installer.groovy
		String runInstallerSyntax =  InstallerXML.getInstance().getRunInstallerSyntax();
		
		runInstallerSyntax += " "+OUTPUT_FOLDER;
			if (runInstallerSyntax != null && !runInstallerSyntax.equals("")) {
				String[] cmdArray = runInstallerSyntax.split(" ");
				List<String> listCmd = new LinkedList<String>();
				
				if (cmdArray.length > 0) {					
					if (cmdArray[0].equals("java")) {
						cmdArray[0] = System.getProperty("java.home") 
							+ File.separator + "bin" + File.separator + "java";
					}
					for(int i=0; i<cmdArray.length;i++){						
						if(i==2){
							String claspathSeparator =  (System.getProperty("os.name").toLowerCase().indexOf("win")!=-1)?";":":";
							String newCmd = (System.getProperty("os.name").toLowerCase().indexOf("win")==-1)?
									(OUTPUT_FOLDER +File.separator+ cmdArray[i] + claspathSeparator + jarPath)
									:(cmdArray[i] + claspathSeparator + jarPath);
							listCmd.add(newCmd);
						}else 
							listCmd.add(cmdArray[i]);
					}
					ProcessBuilder builder = new ProcessBuilder(listCmd);
                    builder.directory(workDir);
					builder.redirectErrorStream(true);					
					Process p = builder.start();
					InputStream procOut  = p.getInputStream();
		            OutputStream procIn = p.getOutputStream();

		            new Thread(new Redirector(procOut, System.out)).start();
		            new Thread(new Redirector(System.in, procIn)).start();		            
		            p.waitFor();					
				}
				
			}		
	}
	/**
	 * Extract the jar file
	 * 
	 * @param zipFile input zip file
	 * @param outputFolder zip file output folder
	 */
    public static void unZip(String zipFile, String outputFolder){    
     byte[] buffer = new byte[1024];
 
     try{
 
    	// create output directory is not exists
    	File folder = new File(outputFolder);
    	if(!folder.exists()){
    		folder.mkdir();
    	}   	
    	
    	//------------------------------------ 
    	ZipInputStream zis2 = new ZipInputStream(new FileInputStream(zipFile));
    	// get the zipped file list entry
    	ZipEntry ze2 = zis2.getNextEntry();
    	while(ze2!=null ){
    		String fileName = ze2.getName();
    		if(!ze2.isDirectory() 
    				&& ! fileName.endsWith(".java") 
    				&& !fileName.endsWith(".class") 
    				&& !fileName.toLowerCase().endsWith(".mf")
    				&& !fileName.toLowerCase().endsWith("pom.xml")
    				&& !fileName.toLowerCase().endsWith("pom.properties")
    				){
	    	   File newFile = new File(outputFolder + System.getProperty("file.separator") + fileName);	 
	           System.out.println("Wiperdog installer, unzip to file : "+ newFile.getAbsolutePath());	 
	            // create all non exists folders
	            // else you will hit FileNotFoundException for compressed folder
	           String parentPath = newFile.getParent();
	           File parentFolder = new File(parentPath);           
	           if(! parentFolder.exists())
	        	   parentFolder.mkdirs();
	           FileOutputStream fos = new FileOutputStream(newFile);
	           int len;
	           while ((len = zis2.read(buffer)) > 0) {
	            	fos.write(buffer, 0, len);
	           }
	            fos.flush();
	            fos.close();   
    		}
            ze2 = zis2.getNextEntry();
    	}
 
        zis2.closeEntry();
    	zis2.close();
    	System.out.println("Self-extracting done!");
    }catch(IOException ex){
       ex.printStackTrace(); 
    }
   }
    public static void stopService() throws Exception{
    	
    	File workDir = new File(System.getProperty("user.dir"));
    	List<String> listCmd = new LinkedList<String>();
    	listCmd.add("net");
    	listCmd.add("stop");
    	listCmd.add("wiperdog");
    	ProcessBuilder builder = new ProcessBuilder(listCmd);
		builder.redirectErrorStream(true);
		builder.directory(workDir);
		Process p = builder.start();	
		InputStream procOut  = p.getInputStream();
        OutputStream procIn = p.getOutputStream();

        new Thread(new Redirector(procOut, System.out)).start();
        new Thread(new Redirector(System.in, procIn)).start();
		p.waitFor();
		
		//-- kill process
		listCmd = new LinkedList<String>();
		listCmd.add("taskkill");
    	listCmd.add("/F");
    	listCmd.add("/IM");
    	listCmd.add("wiperdog_service.exe");
    	
    	builder = new ProcessBuilder(listCmd);
		builder.redirectErrorStream(true);
		builder.directory(workDir);
		p = builder.start();	
		procOut  = p.getInputStream();
        procIn = p.getOutputStream();

        new Thread(new Redirector(procOut, System.out)).start();
        new Thread(new Redirector(System.in, procIn)).start();
		p.waitFor();

		//-- Wait
		listCmd = new LinkedList<String>();
		listCmd.add("cmd.exe");    	    	
		listCmd.add("/c");
		listCmd.add("sleep");
		listCmd.add("3");
		builder = new ProcessBuilder(listCmd);
		builder.redirectErrorStream(true);
		builder.directory(workDir);
		p = builder.start();	
		procOut  = p.getInputStream();
        procIn = p.getOutputStream();

        new Thread(new Redirector(procOut, System.out)).start();
        new Thread(new Redirector(System.in, procIn)).start();
		p.waitFor();
    }
}

/**
 * Standard input/output redirector thread
 * @author nguyenvannghia
 *
 */
class Redirector implements Runnable {
    InputStream in;
    OutputStream out;
    public Redirector(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }
    @Override
    public void run() {
        byte [] buf = new byte[1];
        try {
            while (in.read(buf) >= 0) {
                out.write(buf);
                out.flush();
            }
        } catch (IOException e) {

        }
    }
}