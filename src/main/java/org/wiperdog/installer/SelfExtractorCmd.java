package org.wiperdog.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.text.SimpleDateFormat;

/**
 * Self-extractor main class of the installer, it help to peform all major tasks of the installation
 * such as: Self-extracting, run groovy for pre-configure.
 * @author nguyenvannghia
 * Email: nghia.n.v2007@gmail.com
 */
public class SelfExtractorCmd {
	public static String OUTPUT_FOLDER = "";
	
	public static final String LOG_FILE_NAME = System.getProperty("user.dir")+"/WiperdogInstaller.log";  
	public static SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.S");
	public static File loggingFile = new File(LOG_FILE_NAME);	
	public static FileOutputStream  fo = null;
	
	static void printInfoLog(String content) throws Exception{
		if(fo == null)
			fo = new FileOutputStream(loggingFile, true);		
		fo.write((content+ "\n").getBytes());
		System.out.println(content);
	}	
	public static void fileInfoLog(String content) throws Exception{
		if(fo == null)
			fo = new FileOutputStream(loggingFile, true);		
		fo.write((content+ "\n").getBytes());
	}	
	
	public static void main(String args[]) throws Exception{	
		try {
			// Create a new log file for each time user run the installer				
			fo = new FileOutputStream(loggingFile);
			String beginMessage = "Start the Wiperdog installation at " + df.format(new java.util.Date(System.currentTimeMillis())) + "\n";
			fo.write(beginMessage.getBytes());
			fo.flush();
			fo.close();
			fo = null;
		} catch (Exception e) {
			e.printStackTrace();
		}		
		//Argurments : -d (wiperdog home) ,-j(netty port),-r (restful server port),-m(mongodb host),-p(mongodb port),-n(database name),-u(user database),-pw(password database),-mp(mail policy),-s(install as OS service)
		//              -jd (job directory ) , -id (instances directory) , -cd (jobclass directory) 
		List<String> listParams = new ArrayList<String>();
		listParams.add("-d");
		listParams.add("-j");
		listParams.add("-r");
		listParams.add("-m");
		listParams.add("-jd");
		listParams.add("-id");
		listParams.add("-td");
		listParams.add("-cd");
		listParams.add("-p");
		listParams.add("-n");
		listParams.add("-u");
		listParams.add("-pw");
		listParams.add("-mp");
		listParams.add("-s");
		listParams.add("-ni");
		List<String> listArgs = Arrays.asList(args);
		InputStreamReader converter = new InputStreamReader(System.in);
	    BufferedReader inp = new BufferedReader(converter, 512);
	    
		try {			
			try{
            	Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            		public void run(){
            			try{
            				printInfoLog("Installer shutdown...");
            			}catch(Exception exx){
            				//Ignore exception for shutdown hook.
            			}
            		}
           	}));
        	}catch(Exception ex){
        		// In case CTRL + C were pressed
        		Thread.currentThread().sleep(100);
        	}
			// check command syntax to configure OUTPUT_FOLDER

			if (args.length == 0 || containParam(args, "-ni")) {
				if(args.length == 0){
 					printInfoLog("Press CTRL+C to quit. You can execute default installation with -ni option");
				}
				//Get current dir
				String currentDir = System.getProperty("user.dir");
				
				//Get jar file name, create install directory name
				String jarFileName = new java.io.File(SelfExtractorCmd.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
				String wiperdogDirName = "";
				if (jarFileName.endsWith(".jar")) {
					wiperdogDirName = jarFileName.substring(0, jarFileName.length() - 4);
				}
				if (wiperdogDirName.endsWith("-unix")) {
					wiperdogDirName = wiperdogDirName.substring(0, wiperdogDirName.length() - 5);
				}
				if (wiperdogDirName.endsWith("-win")) {
					wiperdogDirName = wiperdogDirName.substring(0, wiperdogDirName.length() - 4);
				}
				if (wiperdogDirName == "") {
					wiperdogDirName = "wiperdogHome";
				}
				//wiperdog home path
				String wiperdogPath = currentDir + File.separator + wiperdogDirName;
				
				//Check install or not
                                printInfoLog("You omitted to specify WIPERDOG HOME.");
			
	            String confirmStr = "";
	            if(containParam(args, "-ni")){	 
	            	if(containParam(args, "-d")){
	            		OUTPUT_FOLDER = getParamValue(args, "-d");
	            	}else
	            		OUTPUT_FOLDER = wiperdogPath;
	            }else{
	            	while (confirmStr!=null && (!confirmStr.toLowerCase().equalsIgnoreCase("y") && !confirmStr.toLowerCase().equalsIgnoreCase("n"))) {
		            	printInfoLog("Do you want to install wiperdog at " + wiperdogPath + " ? [y/n] :");
		            	confirmStr = inp.readLine().trim();
		            	if (confirmStr.toLowerCase().equalsIgnoreCase("y")) {
		            		OUTPUT_FOLDER = wiperdogPath;
		            	} else if (confirmStr.toLowerCase().equalsIgnoreCase("n")) {
		            		System.exit(0);
		            	}
	            	}
	            }
			} else if ((args.length < 2 && !containParam(args,"-ni")) || (!args[0].trim().equals("-d") && !containParam(args,"-ni")) ) {
				printInfoLog("Wrong parameter. Usage:\n \t\t java -jar [Installer Jar] \n \t\t or \n \t\t java -jar [Installer Jar] -d [INSTALL_PATH>] \n \t\t or \n \t\t java -jar [Installer Jar] -d [INSTALL_PATH] -j [nettyport] -m [mongodb host] -p [mongodb port] -n [mongodb database name] -u [mongodb user name] -pw [mongodb password] -mp [mail policy] -s [yes/no install as OS service] \n \t\t or \n \t\t java -jar [Installer Jar] -ni [ -d INSTALL_PATH] [-j nettyport] ... ]");				
				System.exit(0);
			}else {
				if(containParam(args,"-d"))
					OUTPUT_FOLDER = (String) args[1];
			}// end if (args.length ==0 || -ni option)
			
			//Prepare arguments for Groovy script running
			String strArgs = "";
			//Pass all arguments to Groovy class			
			for(int i = 0; i< args.length; i++ ){				
			   //Get netty port from argurment
				if(args[i].equals("-j")) {
					if( ( args.length > i+1) &&  (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						if( isNumeric(args[i+1])){
							strArgs += "-j " + args[i+1] + " ";
							i++;
						} else {
							printInfoLog( "Netty port must be number: " + args[i]);								
							return;
						}
					} else {							
						printInfoLog("Incorrect value of params: " + args[i]);							
						return;
					}
				}
			    //Get restful server port from argurment
				if(args[i].equals("-r")) {
					if( ( args.length > i+1) &&  (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						if( isNumeric(args[i+1])){
							strArgs += "-r " + args[i+1] + " ";
							i++;
						} else {
							printInfoLog( "Restful server port must be numeric: " + args[i]);								
							return;
						}
					} else {							
						printInfoLog("Incorrect value of params: " + args[i]);							
						return;
					}
				}
				// Get job directory from argurment
				if (args[i].equals("-jd")) {
					if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
						strArgs += "-jd " + args[i + 1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);							
						return;
					}
				}

				// Get intances directory from argurment
				if (args[i].equals("-id")) {
					if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
						strArgs += "-id " + args[i + 1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);							
						return;
					}
				}
				
				// Get job class directory from argurment
				if (args[i].equals("-cd")) {
					if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
						strArgs += "-cd " + args[i + 1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				
				// Get trigger directory from argurment
				if (args[i].equals("-td")) {
					if ((args.length > i + 1) && (args[i + 1].trim() != "") && (!listParams.contains(args[i + 1].trim()))) {
						strArgs += "-td " + args[i + 1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);							
						return;
					}
				}
				//Get Mongodb Host from argurment
				if(args[i].equals("-m")) {
					if(( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						strArgs += "-m " + args[i+1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				//Get Mongodb Port from argurment
				if(args[i].equals("-p")) {
					if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						if(isNumeric(args[i+1])){
							strArgs += "-p " + args[i+1] + " ";
							i++;
						} else {
							printInfoLog("Mongodb port must be number: " + args[i]);
							return;
						}
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				//Get Mongodb database name from argurment
				if(args[i].equals("-n")) {
					if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						strArgs += "-n " + args[i+1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				//Get user connect to database
				if(args[i].equals("-u")) {
					if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						strArgs += "-u " + args[i+1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				//Get password connect to database
				//String pattern = "[a-zA-Z0-9]+";
				if(args[i].equals("-pw")) {
					if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim())) ){
						strArgs += "-pw " + args[i+1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				//Get mail send to policy
				if(args[i].equals("-mp")) {
					if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						strArgs += "-mp " + args[i+1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				//Get install wiperdog as service
				if(args[i].equals("-s")) {
					if( ( args.length > i+1) && (args[i+1].trim() != "") && (!listParams.contains(args[i+1].trim()))){
						strArgs += "-s " + args[i+1] + " ";
						i++;
					} else {
						printInfoLog("Incorrect value of params: " + args[i]);
						return;
					}
				}
				//Get -ni option
				if(args[i].equals("-ni")) {						
					strArgs += args[i] + " ";
				}
			} 
			
			File outputDir = new File(OUTPUT_FOLDER);
			//check if wiperdog home params is not an absolute path
		    if(!outputDir.isAbsolute()) {
       			String userDir = System.getProperty("user.dir");
				OUTPUT_FOLDER = new File (userDir, OUTPUT_FOLDER).getAbsolutePath();
			}			
			printInfoLog("Wiperdog will be install to directory: "
					+ OUTPUT_FOLDER);
			String jarPath = new URI(SelfExtractorCmd.class.getProtectionDomain()
					.getCodeSource().getLocation().getFile()).getPath();
			//-- Stopping service 						
			if(System.getProperty("os.name").toLowerCase().indexOf("win") != -1){
				System.out.println("");				
				printInfoLog("Stop wiperdog service: Start");				
				stopService();
				printInfoLog("Stop wiperdog service: End");				
			}
			
			unZip(jarPath, OUTPUT_FOLDER);			
			String newJarPath = (System.getProperty("os.name").toLowerCase()
					.indexOf("win") != -1) ? jarPath.substring(1, jarPath
					.length()) : jarPath;
			try {
				fo.flush();
				fo.close();
			} catch (Exception e) {
				fo.flush();
				fo.close();
				e.printStackTrace();
			}
			/**
			 Before running Groovy script:
			 1. Set log file name for groovy because groovy will have user.dir as WIPERDOG_HOME which is different from 
			 SelfExtractor user.dir
			 2. Setup extractor.xml for installation mode based on -d option (args.length = 0 -> without -d option).
			 We need to change extractor xm schema to define new INSTALLATION_MODE(see element installMode in extractor.xml)
			 */
			String logFilePath = LOG_FILE_NAME.replaceAll("\\\\", "/");		
			try
	        {
		        File file = new File(OUTPUT_FOLDER + "/extractor.xml");
		        BufferedReader reader = new BufferedReader(new FileReader(file));
		        String line = "", oldtext = "";
		        while((line = reader.readLine()) != null)
		        {
		            oldtext += line + "\n";
		        }
		        reader.close();	        		
		        String tempText = oldtext.replaceAll("INSTALLER_LOG_PATH", logFilePath);	        
		        String newText = tempText.replaceAll("INSTALL_MODE", (strArgs != null && strArgs.indexOf("-ni") != -1)?"interactive":"silient");
		        FileWriter writer = new FileWriter(OUTPUT_FOLDER + "/extractor.xml");
		        writer.write(newText);
		        writer.close();
		    }
		    catch (IOException ioe)
		    {
		        ioe.printStackTrace();
		    }
		    // run pre-configuration groovy script
			runGroovyInstaller(newJarPath,strArgs);
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
	static void runGroovyInstaller(String jarPath,String strArgs)throws Exception{		
		//- risk when user choose the output directory in another volume, which is different from current volume
		String logFilePath = LOG_FILE_NAME.replaceAll("\\\\", "/");
		
		try
        {
	        File file = new File(OUTPUT_FOLDER + "/extractor.xml");
	        BufferedReader reader = new BufferedReader(new FileReader(file));
	        String line = "", oldtext = "";
	        while((line = reader.readLine()) != null)
	            {
	            oldtext += line + "\n";
	        }
	        reader.close();	        		
	        String newtext = oldtext.replaceAll("INSTALLER_LOG_PATH", logFilePath);	        
	        FileWriter writer = new FileWriter(OUTPUT_FOLDER + "/extractor.xml");
	        writer.write(newtext);
	        writer.close();
	    }
	    catch (IOException ioe)
	        {
	        ioe.printStackTrace();
	    }
		
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
		
		//Run java process, e.g: java -jar lib/java/bundle/groovy-all-2.2.1.jar installer/installer.groovy
		String runInstallerSyntax =  InstallerXML.getInstance().getRunInstallerSyntax();
		
		// runInstallerSyntax += " "+OUTPUT_FOLDER  + " " + strArgs;
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
						} else {
							listCmd.add(cmdArray[i]);
						}
					}
					listCmd.add(OUTPUT_FOLDER);
					if (strArgs != null && !strArgs.equals("")) {
					    cmdArray = strArgs.split(" ");
					    for(int i = 0; i < cmdArray.length; i++){
							listCmd.add(cmdArray[i]);
						}
					}
					/*for(String s:listCmd){
						System.out.print(s + " ");
					}*/
					ProcessBuilder builder = new ProcessBuilder(listCmd);
                    builder.directory(workDir);
					builder.redirectErrorStream(true);					
					Process p = builder.start();
					InputStream procOut  = p.getInputStream();
		            OutputStream procIn = p.getOutputStream();

		            new Thread(new Redirector("Output", procOut, System.out)).start();		            
		            new Thread(new Redirector("Input",System.in, procIn)).start();
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
    public static void unZip(String zipFile, String outputFolder) throws Exception{    
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
	    	   printInfoLog("Wiperdog installer, unzip to file : "+ newFile.getAbsolutePath());	           
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
	            if (fileName.startsWith("bin") || fileName.endsWith(".sh")) {
	            	newFile.setExecutable(true);
	            }
    		}
            ze2 = zis2.getNextEntry();
    	}
 
        zis2.closeEntry();
    	zis2.close();
    	printInfoLog("Self-extracting done!");    	
    }catch(IOException ex){
       ex.printStackTrace(); 
    }
   }
    public static String getParamValue(String[] args, String key){
    	String ret = null;
    	for(int i=0;i<args.length;i++){
    		if(args[i] != null &&  args[i].equals(key) && ((i+1) < args.length)){
    			ret = args[i+1];
    			break;
    		}
    	}
    	return ret;
    }
    public static boolean containParam(String[] args, String key){
    	boolean ret = false;
    	for(String s:args){
    		if(s != null &&  s.equals(key)){
    			ret = true;    			
    			break;
    		}
    	}
    	return ret;
    }
   	public static boolean isNumeric(String string){
	  return string.matches("-?\\d+(\\.\\d+)?");  
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
    	listCmd.add("wiperdog_service*");
    	
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
    String name = "";
    public Redirector(String name, InputStream in, OutputStream out) {
    	this.name = name;
        this.in = in;
        this.out = out;
    }
    public Redirector(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }    
    public void run() {
    	synchronized(in){
		try {
			byte[] buf = new byte[1];
			while ( in.read(buf) >= 0) {			
				out.write(buf);				
				out.flush();				
			}
		} catch (IOException e) {
            	//e.printStackTrace();
		}     
	}//- end sync
    }
}



	 
