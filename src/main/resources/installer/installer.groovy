import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.json.JsonBuilder
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import org.w3c.dom.Document
import org.wiperdog.installer.SelfExtractorCmd
import org.wiperdog.installer.internal.InstallerUtil
import org.wiperdog.installer.internal.InstallerXML
import org.wiperdog.installer.internal.XMLErrorHandler
import static org.wiperdog.installer.SelfExtractorCmd.getParamValue
import static org.wiperdog.installer.SelfExtractorCmd.containParam	
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
/**
 * WPDInstallerGroovy is a Groovy class used by SelfExtractorCmd to perform pre-configuration 
 * after installation of wiperdog.
 * @author nguyenvannghia
 * Email: nghia.n.v2007@gmail.com
 */
public class WPDInstallerGroovy{
        static File loggingFile = null
	static void printInfoLog(String content) throws Exception{
		loggingFile.append(content + "\n")		
		println content
	}	
	static void fileInfoLog(String content) throws Exception{
		loggingFile.append(content + "\n")		
	}
	static final String NONE_INTERACTIVE_PARAM_KEY = "-ni"
    public static void main(String[] installerParam) throws Exception{			
        def params = [:]
        
        def userConfirm = null		
		String wiperdogHome = installerParam[0]
		InputStreamReader converter = new InputStreamReader(System.in)
		BufferedReader inp = new BufferedReader(converter, 512)	  
        //-------------------------------------- Parse configuration XML ------------------------------------//
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.setValidating(true)
        factory.setIgnoringElementContentWhitespace(true)
        DocumentBuilder docBuilder = factory.newDocumentBuilder()
        docBuilder.setErrorHandler(new XMLErrorHandler())
        Document doc = docBuilder.parse(new FileInputStream(new File("extractor.xml")))
        InstallerUtil.parseXml(doc.getDocumentElement())
        //replace  wiperdog home in bin/wiperdog
     	String installAsService = InstallerXML.getInstance().getInstallAsOSService()
	    try {		 
		 loggingFile = new File(InstallerXML.getInstance().getInstallLogPath());
		} catch (Exception e) {			
		 e.printStackTrace();
		}
	    WPDInstallerGroovy.printInfoLog("----------------------------------------------------------------")			
	    WPDInstallerGroovy.printInfoLog("------------------    INSTALLATION     -------------------------")			
	    WPDInstallerGroovy.printInfoLog("----------------------------------------------------------------")			
        WPDInstallerGroovy.printInfoLog("Welcome to the installation of " +InstallerXML.getInstance().getAppName()  + " - Version: "+ InstallerXML.getInstance().getAppVersion()) 
        WPDInstallerGroovy.printInfoLog(InstallerXML.getInstance().getWelcomeMsg() )
        //-- Parameter list
        List<String> listParams = new ArrayList<String>();
		listParams.add("-d")
		listParams.add("-j")
		listParams.add("-r")
		listParams.add("-m")
		listParams.add("-jd")
		listParams.add("-id")
		listParams.add("-td")
		listParams.add("-cd")
		listParams.add("-p")
		listParams.add("-n")
		listParams.add("-u")
		listParams.add("-pw")
		listParams.add("-mp")
		listParams.add("-s")
		listParams.add("-ni")
		List<String> listArgs = Arrays.asList(installerParam)		
		//Get default params not in arguments
		List<String> listEmptyParams = new ArrayList<String>()
		for(int i = 1 ; i < listParams.size() ; i++){
			if(!listArgs.contains(listParams.get(i))){
				listEmptyParams.add(listParams.get(i))
			}
		}
		def installMode = InstallerXML.getInstance().getInstallMode() 
        try {
			if(!containParam(installerParam, NONE_INTERACTIVE_PARAM_KEY)){//INTERACTIVE MODE
					params['WIPERDOGHOME'] = wiperdogHome														
					while (userConfirm == null || userConfirm.length() == 0 || !userConfirm.toLowerCase().equalsIgnoreCase("y") ) {
						userConfirm = null
						installAsService = InstallerXML.getInstance().getInstallAsOSService()
				        WPDInstallerGroovy.printInfoLog("\n")					        
			        	//-- prompt for user input parameters						
						WPDInstallerGroovy.printInfoLog("\nGetting input parameters for pre-configured wiperdog, press any key to continue... ")
						def test = inp.readLine()
						//-j
						def tmpNettyPort = getParamValue(installerParam,"-j")
						if(listEmptyParams.contains("-j")){
				        	WPDInstallerGroovy.printInfoLog("Please input Netty port(default set to 13111):")					        
				        	tmpNettyPort = inp.readLine().trim()
				    	}
				        params['netty.port'] = (tmpNettyPort != null && ! tmpNettyPort.equals(""))?tmpNettyPort:'13111'	
						//-r
						def tmpRestPort = getParamValue(installerParam,"-r")
						if(listEmptyParams.contains("-r")){
				        	WPDInstallerGroovy.printInfoLog("Please input Restful service port(default set to 8089):")					        
				        	tmpRestPort = inp.readLine().trim()
				    	}
				        params['rest.port'] = (tmpRestPort != null && ! tmpRestPort.equals(""))?tmpRestPort:'8089'		
				        		
				        //-jd
				        def tmpMonitorjobfwJobDir = getParamValue(installerParam,"-jd")
				        if(listEmptyParams.contains("-jd")){
							WPDInstallerGroovy.printInfoLog("Please input job directory: (default set to ${wiperdogHome}/var/job)")							
							tmpMonitorjobfwJobDir = inp.readLine().trim()
						}
						params['monitorjobfw.directory.job'] = (tmpMonitorjobfwJobDir != null && ! tmpMonitorjobfwJobDir.equals(""))?tmpMonitorjobfwJobDir:'${felix.home}/var/job'
						
						//-td
						def tmpMonitorjobfwTrgDir = getParamValue(installerParam,"-td")
						if(listEmptyParams.contains("-td")){
							WPDInstallerGroovy.printInfoLog("Please input trigger directory:(default set to ${wiperdogHome}/var/job)")
							tmpMonitorjobfwTrgDir = inp.readLine().trim()
						}
						params['monitorjobfw.directory.trigger'] = (tmpMonitorjobfwTrgDir != null && ! tmpMonitorjobfwTrgDir.equals(""))?tmpMonitorjobfwTrgDir:'${felix.home}/var/job'

						//-cd
						def tmpMonitorjobfwJobClsDir = getParamValue(installerParam,"-cd")
						if(listEmptyParams.contains("-cd")){
							WPDInstallerGroovy.printInfoLog("Please input job class directory:(default set to ${wiperdogHome}/var/job)")
							tmpMonitorjobfwJobClsDir = inp.readLine().trim()
						}
						params['monitorjobfw.directory.jobcls'] = (tmpMonitorjobfwJobClsDir != null && ! tmpMonitorjobfwJobClsDir.equals(""))?tmpMonitorjobfwJobClsDir:'${felix.home}/var/job'
						
						//-id
						def tmpMonitorjobfwJobInstDir = getParamValue(installerParam,"-id")
						if(listEmptyParams.contains("-id")){
							WPDInstallerGroovy.printInfoLog("Please input job instance directory:(default set to ${wiperdogHome}/var/job)")
							tmpMonitorjobfwJobInstDir = inp.readLine().trim()
						}
						params['monitorjobfw.directory.instances'] = (tmpMonitorjobfwJobInstDir != null && ! tmpMonitorjobfwJobInstDir.equals(""))?tmpMonitorjobfwJobInstDir:'${felix.home}/var/job'
				        
				        //-m
				        def tmpMonitorjobfwMongodbHost = getParamValue(installerParam,"-m")
				        if(listEmptyParams.contains("-m")){
					    	WPDInstallerGroovy.printInfoLog("Please input database server (Mongodb) IP address (default set to 127.0.0.1):")
				        	tmpMonitorjobfwMongodbHost = inp.readLine().trim()
				        }
				        params['monitorjobfw.mongodb.host'] = (tmpMonitorjobfwMongodbHost != null && ! tmpMonitorjobfwMongodbHost.equals(""))?tmpMonitorjobfwMongodbHost:'127.0.0.1'
				        
				        //-p
				        def tmpMonitorjobfwMongodbPort = getParamValue(installerParam,"-p")
				        if(listEmptyParams.contains("-p")){
				        	WPDInstallerGroovy.printInfoLog("Please input database server port, Mongodb port (default set to 27017):")    
				        	tmpMonitorjobfwMongodbPort = inp.readLine().trim()
				        }
				        params['monitorjobfw.mongodb.port'] = (tmpMonitorjobfwMongodbPort != null && ! tmpMonitorjobfwMongodbPort.equals(""))?tmpMonitorjobfwMongodbPort:'27017'
				            
				        //n
				        def tmpMonitorjobfwMongodbDbName = getParamValue(installerParam,"-n")
				        if(listEmptyParams.contains("-n")){
				        	WPDInstallerGroovy.printInfoLog("Please input database name (default set to wiperdog):")
				        	tmpMonitorjobfwMongodbDbName = inp.readLine().trim()
				    	}
				        params['monitorjobfw.mongodb.dbName'] = (tmpMonitorjobfwMongodbDbName != null && ! tmpMonitorjobfwMongodbDbName.equals(""))?tmpMonitorjobfwMongodbDbName:'wiperdog'
				        
				        // -u
				        def tmpMonitorjobfwMongodbUser = getParamValue(installerParam,"-u")
				        if(listEmptyParams.contains("-u")){
				        	WPDInstallerGroovy.printInfoLog("Please input database server user name, (Mongodb user name, default set to empty):")    
				        	tmpMonitorjobfwMongodbUser = inp.readLine().trim()
				        }
				        params['monitorjobfw.mongodb.user'] = (tmpMonitorjobfwMongodbUser != null && ! tmpMonitorjobfwMongodbUser.equals(""))?tmpMonitorjobfwMongodbUser:''
				        
				        //pw
				        def tmpMonitorjobfwMongodbPass = getParamValue(installerParam,"-pw")
				        if(listEmptyParams.contains("-pw")){
				        	WPDInstallerGroovy.printInfoLog("Please input database server password, (Mongodb password, default set to empty):") 
				        	tmpMonitorjobfwMongodbPass = inp.readLine().trim()
				        }
				        params['monitorjobfw.mongodb.pass'] = (tmpMonitorjobfwMongodbPass != null && ! tmpMonitorjobfwMongodbPass.equals(""))?tmpMonitorjobfwMongodbPass:''
				        
				        //mp
				        def tmpMonitorjobfwMailPolicy = getParamValue(installerParam,"-mp")
				        if(listEmptyParams.contains("-mp")){
				        	WPDInstallerGroovy.printInfoLog("Please input mail send data policy, Mail Policy (default set to testmail@gmail.com):")    
				        	tmpMonitorjobfwMailPolicy = inp.readLine().trim()
				        }
				        params['monitorjobfw.mail.toMail'] = (tmpMonitorjobfwMailPolicy != null && ! tmpMonitorjobfwMailPolicy.equals(""))?tmpMonitorjobfwMailPolicy:'testmail@gmail.com'
				        	
					    //s
						def tmpServiceFlag = getParamValue(installerParam,"-s")
						if(listEmptyParams.contains("-s")){
	        				while(tmpServiceFlag != "" && tmpServiceFlag != "yes" && tmpServiceFlag != "no"){
	        					WPDInstallerGroovy.printInfoLog("Do you want to install wiperdog as system service, default set to '"+installAsService+"' (type yes|no), enter for default value:") 
	        				    tmpServiceFlag = inp.readLine().trim()
	        				}
        				}
    					installAsService = (tmpServiceFlag == null || tmpServiceFlag == "")?installAsService:tmpServiceFlag
    					WPDInstallerGroovy.printInfoLog("\n")
						WPDInstallerGroovy.printInfoLog("Please CONFIRM The following configuration are correct:")
						WPDInstallerGroovy.printInfoLog("Wiperdog Home:"+ params['WIPERDOGHOME'])
						WPDInstallerGroovy.printInfoLog("Server Port:"+ params['netty.port'])
						WPDInstallerGroovy.printInfoLog("Restful Port:"+ params['rest.port'])
						WPDInstallerGroovy.printInfoLog("Job directory:"+ params['monitorjobfw.directory.job'])
						WPDInstallerGroovy.printInfoLog("Trigger directory:"+ params['monitorjobfw.directory.trigger'])
						WPDInstallerGroovy.printInfoLog("Job class directory:"+ params['monitorjobfw.directory.jobcls'])
						WPDInstallerGroovy.printInfoLog("Job instances directory:"+ params['monitorjobfw.directory.instances'])
						WPDInstallerGroovy.printInfoLog("Database address:"+ params['monitorjobfw.mongodb.host'])
						WPDInstallerGroovy.printInfoLog("Database port:"+ params['monitorjobfw.mongodb.port'])
						WPDInstallerGroovy.printInfoLog("Database name:"+ params['monitorjobfw.mongodb.dbName'])
						WPDInstallerGroovy.printInfoLog("User name:"+ params['monitorjobfw.mongodb.user'])
						WPDInstallerGroovy.printInfoLog("Password:"+ params['monitorjobfw.mongodb.pass'])
						WPDInstallerGroovy.printInfoLog("Mail Policy:"+ params['monitorjobfw.mail.toMail'])
						WPDInstallerGroovy.printInfoLog("Install as OS service:"+ installAsService)
						WPDInstallerGroovy.printInfoLog("\n")
						while(userConfirm == null || (!userConfirm.toLowerCase().equalsIgnoreCase("y") && !userConfirm.toLowerCase().equalsIgnoreCase("n"))){
							WPDInstallerGroovy.printInfoLog("Your input are correct(Y|y|N|n):")							
							userConfirm = inp.readLine().trim()
						}							
						WPDInstallerGroovy.fileInfoLog("User confirm input: "+userConfirm)
					} //-- END while
				//-- END INTERACTIVE MODE
				} else {//-- NONE INTERACTIVE MODE
										 
					params['WIPERDOGHOME'] = wiperdogHome
					   //Get Netty port from argurment
					if(containParam(installerParam,"-j")){ 
						params['netty.port'] = getParamValue(installerParam,"-j")
					}else{
						params['netty.port'] = '13111'
					}

					   //Get Restful port from argurment
					if(containParam(installerParam,"-r")){ 
						params['rest.port'] = getParamValue(installerParam,"-r")
					}else{
						params['rest.port'] = '8089'
					}
												
					//Get Job directory config from argurment
					if(containParam(installerParam,"-jd")){
						params['monitorjobfw.directory.job'] = getParamValue(installerParam,"-jd")						
					}else{
						params['monitorjobfw.directory.job'] = '${felix.home}/var/job'
					}
						

					//Get Trigger directory from argurment
					if(containParam(installerParam,'-td')){
						params['monitorjobfw.directory.trigger'] = SelfExtractorCmd.getParamValue(installerParam,"-td")
					}else{
						params['monitorjobfw.directory.trigger'] = '${felix.home}/var/job'
					}
						
					//Get job class directory from argurment
					if(containParam(installerParam,'-cd')){
						params['monitorjobfw.directory.jobcls'] = SelfExtractorCmd.getParamValue(installerParam,"-cd")
					}else{
						params['monitorjobfw.directory.jobcls'] = '${felix.home}/var/job'
					}						
						
					//Get instances directory from argurment
					if(containParam(installerParam,'-id')){
						params['monitorjobfw.directory.instances'] = SelfExtractorCmd.getParamValue(installerParam,"-id")
					}else{
						params['monitorjobfw.directory.instances'] = '${felix.home}/var/job'
					}
						
					//Get Mongodb Host from argurment
					if(containParam(installerParam,'-m')){
						params['monitorjobfw.mongodb.host'] = getParamValue(installerParam,"-m")
					}else{
						params['monitorjobfw.mongodb.host'] = '127.0.0.1'
					}
						
					//Get Mongodb Port from argurment
					if(containParam(installerParam,'-p')) {
						params['monitorjobfw.mongodb.port'] = getParamValue(installerParam,"-p")
					}else{
						params['monitorjobfw.mongodb.port'] = '27017'
					}
					//Get Mongodb database name from argurment
					if(containParam(installerParam, '-n')) {
						params['monitorjobfw.mongodb.dbName'] = getParamValue(installerParam,"-n")
					}else{
						params['monitorjobfw.mongodb.dbName'] = 'wiperdog'
					}						
						
					//Get Mongodb user name from argurment
					if(containParam(installerParam,'-u')) {
						params['monitorjobfw.mongodb.user'] = getParamValue(installerParam,"-u")
					}else{
						params['monitorjobfw.mongodb.user'] = ''
					}
					//Get Mongodb password from argurment
					if(containParam(installerParam,'-pw')) {
						params['monitorjobfw.mongodb.pass'] = getParamValue(installerParam,"-pw")
					}else{
						params['monitorjobfw.mongodb.pass'] = ''
					}
					//Get mail policy from argurment
					if(containParam(installerParam,'-mp')) {
						params['monitorjobfw.mail.toMail'] = getParamValue(installerParam,"-mp")
					}else{
						params['monitorjobfw.mail.toMail'] = 'testmail@gmail.com'
					}
					//Get 
					if(containParam(installerParam,'-s')) {
						installAsService = getParamValue(installerParam,"-s")
					}else{
						installAsService = 'yes'
					}
					WPDInstallerGroovy.printInfoLog("-----------------------------------------------------------------")
					WPDInstallerGroovy.printInfoLog("------------------    Wiperdog setting     ----------------------")
					WPDInstallerGroovy.printInfoLog("-----------------------------------------------------------------")
					WPDInstallerGroovy.printInfoLog("Wiperdog Home:"+ params['WIPERDOGHOME'])
					WPDInstallerGroovy.printInfoLog("Server Port:"+ params['netty.port'])
					WPDInstallerGroovy.printInfoLog("Restful Port:"+ params['rest.port'])
					WPDInstallerGroovy.printInfoLog("Job directory:"+ params['monitorjobfw.directory.job'])
					WPDInstallerGroovy.printInfoLog("Trigger directory:"+ params['monitorjobfw.directory.trigger'])
					WPDInstallerGroovy.printInfoLog("Job class directory:"+ params['monitorjobfw.directory.jobcls'])
					WPDInstallerGroovy.printInfoLog("Job instances directory:"+ params['monitorjobfw.directory.instances'])
					WPDInstallerGroovy.printInfoLog("Database address:"+ params['monitorjobfw.mongodb.host'])
					WPDInstallerGroovy.printInfoLog("Database port:"+ params['monitorjobfw.mongodb.port'])
					WPDInstallerGroovy.printInfoLog("Database name:"+ params['monitorjobfw.mongodb.dbName'])
					WPDInstallerGroovy.printInfoLog("User name:"+ params['monitorjobfw.mongodb.user'])
					WPDInstallerGroovy.printInfoLog("Password:"+ params['monitorjobfw.mongodb.pass'])
					WPDInstallerGroovy.printInfoLog("Mail Policy:"+ params['monitorjobfw.mail.toMail'])
					WPDInstallerGroovy.printInfoLog("Install as OS service:"+ installAsService)
				}//END IF NONE INTERACTIVE MODE
            } catch (Exception ignore) {
				WPDInstallerGroovy.printInfoLog("Error:"+ ignore)                
            }
        //------------------------------------ CONFIGURE WIPERDOG ------------------------------------------//
		//Write coresponding MongoDB information to default.params
		def defaultParams = new File("var/conf/default.params")
		def shell = new GroovyShell()
		def defParamsObject = shell.evaluate(defaultParams)
		if(defParamsObject != null )  {
			def dest = defParamsObject["dest"]
			if(dest != null) {
				dest = dest.collect {
					if(it.getAt("mongoDB") != null) {
						it = ["mongoDB" : "${params['monitorjobfw.mongodb.host']}:${params['monitorjobfw.mongodb.port']}/${params['monitorjobfw.mongodb.dbName']}"]
					} else {
						it
					}
				}
				defParamsObject['dest'] = dest
			}
			def builder = new JsonBuilder(defParamsObject)
			def stringToFile = builder.toPrettyString().replaceAll("\\{\\s*\\}","[:]").replace("{","[").replace("}","]")
			defaultParams.setText(stringToFile)
		}
         // Configure system.properties for netty.port
        def configFile = new File("etc/system.properties")
        def fileText = configFile.text 
        
        def newFileText = ""
        String macherPattern = "(netty.port=)((?:(?!\\n).)*)"
        Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['netty.port'])
        }                
        configFile.write(newFileText)
        
         // Configure system.properties for rest.port
        configFile = new File("etc/system.properties")
        fileText = configFile.text 
        
        newFileText = ""
        macherPattern = "(rest.port=)((?:(?!\\n).)*)"
        pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['rest.port'])
        }                
        configFile.write(newFileText)
         /*
              3. Configure monitorjobfw.cfg for 
	        monitorjobfw.directory.job
		monitorjobfw.directory.trigger
		monitorjobfw.directory.jobcls
		monitorjobfw.directory.instances
                monitorjobfw.mongodb.host
                monitorjobfw.mongodb.port
                monitorjobfw.mongodb.dbName
                monitorjobfw.mongodb.user
                monitorjobfw.mongodb.pass
                monitorjobfw.mail.toMail
          */
		//write job directory config to file
		configFile = new File("etc/monitorjobfw.cfg")
		fileText = configFile.text
		macherPattern = "(monitorjobfw.directory.job=)((?:(?!\\n).)*)"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		matcher = pattern.matcher(fileText);
		while(matcher.find())
		{
			newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.directory.job'])
		}
		configFile.write(newFileText)

		//write trigger directory config to file
		configFile = new File("etc/monitorjobfw.cfg")
		fileText = configFile.text
		macherPattern = "(monitorjobfw.directory.trigger=)((?:(?!\\n).)*)"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		matcher = pattern.matcher(fileText);
		while(matcher.find())
		{
			newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.directory.trigger'])
		}
		configFile.write(newFileText)

		//write job class directory config to file
		configFile = new File("etc/monitorjobfw.cfg")
		fileText = configFile.text
		macherPattern = "(monitorjobfw.directory.jobcls=)((?:(?!\\n).)*)"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		matcher = pattern.matcher(fileText);
		while(matcher.find())
		{
			newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.directory.jobcls'])
		}
		configFile.write(newFileText)

		//write job instances directory config to file
		configFile = new File("etc/monitorjobfw.cfg")
		fileText = configFile.text
		macherPattern = "(monitorjobfw.directory.instances=)((?:(?!\\n).)*)"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		matcher = pattern.matcher(fileText);
		while(matcher.find())
		{
			newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.directory.instances'])
		}
		configFile.write(newFileText)

		//write mongo host config to file
        configFile = new File("etc/monitorjobfw.cfg")
        fileText = configFile.text 
        macherPattern = "(monitorjobfw.mongodb.host=)((?:(?!\\n).)*)"
        pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.mongodb.host'])
        }                
        configFile.write(newFileText)
        //port number
        configFile = new File("etc/monitorjobfw.cfg")
        fileText = configFile.text 
        macherPattern = "(monitorjobfw.mongodb.port=)((?:(?!\\n).)*)"
        pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.mongodb.port'])
        }                
        configFile.write(newFileText)
        //fileText = newFileText.replaceAll(/_monitorjobfw.mongodb.port/, params['monitorjobfw.mongodb.port'])
        //database name
        configFile = new File("etc/monitorjobfw.cfg")
        fileText = configFile.text 
        macherPattern = "(monitorjobfw.mongodb.dbName=)((?:(?!\\n).)*)"
        pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.mongodb.dbName'])
        }                
        configFile.write(newFileText)
        //user name
        configFile = new File("etc/monitorjobfw.cfg")
        fileText = configFile.text 
        macherPattern = "(monitorjobfw.mongodb.user=)((?:(?!\\n).)*)"
        pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.mongodb.user'])
        } 
        configFile.write(newFileText)
        //password
        configFile = new File("etc/monitorjobfw.cfg")
        fileText = configFile.text 
        macherPattern = "(monitorjobfw.mongodb.pass=)((?:(?!\\n).)*)"
        pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.mongodb.pass'])
        }                
        configFile.write(newFileText)
        //mail policy
        configFile = new File("etc/monitorjobfw.cfg")
        fileText = configFile.text 
        macherPattern = "(monitorjobfw.mail.toMail=)((?:(?!\\n).)*)"
        pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['monitorjobfw.mail.toMail'])
        }                
        configFile.write(newFileText)
        
        //---WIPERDOG_HOME---//
        String osName = System.getProperty("os.name").toLowerCase() 
		if(osName.indexOf("win") == -1){//-- LINUX
            configFile = new File("bin/wiperdog")
            fileText = configFile.text
            def newHome = wiperdogHome.replaceAll(/\\+/, '/')                
            newFileText = ""
            macherPattern = "(WIPERDOGHOME=)((?:(?!\\n).)*)"
            pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
            matcher = pattern.matcher(fileText);
            while(matcher.find())
            {
                newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + "\"" + newHome + "\"")
            }              
            def crlfFixFileText = newFileText.replaceAll("\r","");
            //write WIPERDOG_HOME to file bin/wiperdog
            configFile.write(crlfFixFileText)
		}
        //------------------------------------ INSTALL WIPERDOG AS SERVICE ------------------------------------------//
        if(installAsService != null && installAsService.equalsIgnoreCase("yes")){
            WPDInstallerGroovy.printInfoLog("Start install application as a system service")
            //-- configure Wiperdog Home in service script            
            if(osName.indexOf("win") == -1){//-- LINUX
                /** Replace CRLF to LF to fit Unix file format **/
                
                /* Install service script */
                FileOutputStream fos = new FileOutputStream(new File("install_service.sh"))
                StringBuffer sBuff = new StringBuffer(512)
                sBuff.append("#!/bin/bash\n#\n#\n#\n")            
                
                sBuff.append("isUbuntu=`uname -a | grep buntu`\n")
               	sBuff.append("if [ ! -n \"\$isUbuntu\" ]\n")  // If is not ubuntu
                sBuff.append("then\n")
    		sBuff.append(". ~/.bash_profile\n")
                sBuff.append("	service_cmd=`which service`\n")
                sBuff.append("	chkconfig_cmd=`which chkconfig`\n")
                sBuff.append("	if [ ! -n \$service_cmd ] || [ ! -x \$service_cmd ] || [ ! -n \$chkconfig_cmd ] || [ ! -x \$chkconfig_cmd ]\n")
                sBuff.append("	then\n")
                sBuff.append("    echo Service commands are not found on this system.\n")
                sBuff.append("    echo Cannot install wiperdog as system service unless you install chkconfig and service command!\n")
                sBuff.append("    exit 1\n")
                sBuff.append("	fi\n") 
                
                sBuff.append("	\$service_cmd wiperdog stop \n")
                sBuff.append("	/bin/cp \""+wiperdogHome+"/bin/wiperdog\" /etc/init.d/\n")
                sBuff.append("	/bin/chmod 755 /etc/init.d/wiperdog\n")
                sBuff.append("	\$chkconfig_cmd --del wiperdog\n")
                sBuff.append("	\$chkconfig_cmd --add wiperdog\n")
                sBuff.append("	\$chkconfig_cmd --level 2 wiperdog on\n")
                sBuff.append("	\$chkconfig_cmd --level 3 wiperdog on\n")
                sBuff.append("	\$chkconfig_cmd --level 4 wiperdog on\n")
                sBuff.append("	\$chkconfig_cmd --level 5 wiperdog on\n")
                sBuff.append("	\$chkconfig_cmd --list wiperdog\n")
                
                sBuff.append("else \n") // If is ubuntu
                sBuff.append("	service_cmd=`which service`\n")
                sBuff.append("	chkconfig_cmd=`which update-rc.d`\n")
                sBuff.append("	if [ ! -n \$service_cmd ] || [ ! -x \$service_cmd ] || [ ! -n \$chkconfig_cmd ] || [ ! -x \$chkconfig_cmd ]\n")
                sBuff.append("	then\n")
                sBuff.append("    echo Service commands are not found on this system.\n")
                sBuff.append("    echo Cannot install wiperdog as system service unless you install update-rc.d and service command!\n")
                sBuff.append("    exit 1\n")
                sBuff.append("	fi\n") 
                
                sBuff.append("	\$service_cmd wiperdog stop \n")
                sBuff.append("	/bin/cp \""+wiperdogHome+"/bin/wiperdog\" /etc/init.d/\n")
                sBuff.append("	/bin/chmod 755 /etc/init.d/wiperdog\n")
                sBuff.append("	\$chkconfig_cmd -f wiperdog remove\n")
                sBuff.append("	\$chkconfig_cmd wiperdog defaults\n")
                sBuff.append("	\$chkconfig_cmd wiperdog start 20 2 3 4 5 .\n")
        		
                sBuff.append("fi\n")
                
                sBuff.append("/bin/chmod 755 \""+ wiperdogHome +"\"/bin/* \n")
                sBuff.append("/bin/rm -f \""+ wiperdogHome +"/var/run/wiperdog.lck\" \n")
                sBuff.append("vim_cmd=`which vi`\n")
                sBuff.append("if [ ! -n \$vim_cmd ] || [ ! -x \$vim_cmd ]\n")
                sBuff.append("then\n")
                sBuff.append("    echo Vim editor is not install or not valid in this system,\n")
                sBuff.append("    you need to change file format in  "+ wiperdogHome +"/bin/* manually\n")
                sBuff.append("else\n")
                //-- Set file format for executable unix shell script in 'bin' folder                
                sBuff.append("\$vim_cmd +':w ++ff=unix' +':q' \""+ wiperdogHome +"/bin/clearjobdata\" \n")
                sBuff.append("\$vim_cmd +':w ++ff=unix' +':q' \""+ wiperdogHome +"/bin/gendbpasswd.sh\" \n")
                sBuff.append("\$vim_cmd +':w ++ff=unix' +':q' \""+ wiperdogHome +"/bin/groovy\" \n")
                sBuff.append("\$vim_cmd +':w ++ff=unix' +':q' \""+ wiperdogHome +"/bin/jobrunner.sh\" \n")
                sBuff.append("\$vim_cmd +':w ++ff=unix' +':q' \""+ wiperdogHome +"/bin/startGroovy\" \n")
                sBuff.append("\$vim_cmd +':w ++ff=unix' +':q' \""+ wiperdogHome +"/bin/startWiperdog.sh\"  \n")
                sBuff.append("\$vim_cmd +':w ++ff=unix' +':q' \""+ wiperdogHome +"/bin/wiperdog\" \n")
                sBuff.append("fi\n")
                
                fos.write(sBuff.toString().getBytes());
                fos.flush();
                fos.close();
                def listCmd = []
                listCmd.add("/bin/chmod")
                listCmd.add("755")
                listCmd.add("install_service.sh")
                ProcessBuilder builder = new ProcessBuilder(listCmd);
                builder.redirectErrorStream(true);					
                Process proc = builder.start();
                
                listCmd = []
                listCmd.add("/bin/sh")
                listCmd.add("install_service.sh")
                builder = new ProcessBuilder(listCmd);
                builder.redirectErrorStream(true);					
                proc = builder.start();
                
                WPDInstallerGroovy.printInfoLog(proc.err.text)
                WPDInstallerGroovy.printInfoLog(proc.in.text)
            }else{//-- Window
                def listCmd = []
                
                listCmd.add("\"" + wiperdogHome +"/service/javaservice/create_wiperdog_service.bat" + "\"")
                listCmd.add("\"" + wiperdogHome + "\"")                
                File workDir = new File(wiperdogHome +"/service/javaservice");
                ProcessBuilder builder = new ProcessBuilder(listCmd);
                builder.directory(workDir);
                builder.redirectErrorStream(true);					
                Process proc = builder.start();
                //proc.waitFor() //-- cause error if wiperdog_service.exe has been used by another process
                
                def errorStr = proc.err.text
                def outputStr = proc.in.text
                WPDInstallerGroovy.printInfoLog(outputStr)
                WPDInstallerGroovy.printInfoLog(errorStr)
                
            }
            WPDInstallerGroovy.printInfoLog("The installation has been completed successfully! \nPlease use command 'net start/stop wiperdog'(Window) or 'service wiperdog start/stop'(Linux) to control the service.")
	    WPDInstallerGroovy.printInfoLog("For detail installation log, please view the log file at: " + InstallerXML.getInstance().getInstallLogPath())
            WPDInstallerGroovy.printInfoLog("Thank you for choosing Wiperdog!")
         }//-- END install as system service
         WPDInstallerGroovy.printInfoLog("Finish the Wiperdog installation at " + SelfExtractorCmd.df.format(new java.util.Date(System.currentTimeMillis())));
	 WPDInstallerGroovy.printInfoLog("\n\n")
    }//-- end main
}
