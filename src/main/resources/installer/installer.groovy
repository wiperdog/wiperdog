import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream
import org.wiperdog.installer.internal.InstallerUtil
import org.wiperdog.installer.internal.InstallerXML
import org.wiperdog.installer.internal.XMLErrorHandler

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.DocumentBuilder
import org.w3c.dom.Document

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.Scanner

public class WPDInstallerGroovy{
    public static void main(String[] installerParam) throws Exception{
        def params = [:]
        def userConfirm = null
		String wiperdogHome = installerParam[0]
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader inp = new BufferedReader(converter, 512);
            //-------------------------------------- Parse configuration XML ------------------------------------//
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
            factory.setValidating(true)
            factory.setIgnoringElementContentWhitespace(true)
            DocumentBuilder docBuilder = factory.newDocumentBuilder()
            docBuilder.setErrorHandler(new XMLErrorHandler())
            Document doc = docBuilder.parse(InstallerUtil.class.getResourceAsStream("/extractor.xml"))
            InstallerUtil.parseXml(doc.getDocumentElement())
            //replace  wiperdog home in bin/wiperdog
         	String installAsService = InstallerXML.getInstance().getInstallAsOSService()
			println "----------------------------------------------------------------"
			println "------------------    INSTALLATION     -------------------------"
			println "----------------------------------------------------------------"
            println "Welcome to the installation of " +InstallerXML.getInstance().getAppName()  + " - Version: " + InstallerXML.getInstance().getAppVersion() 
            println InstallerXML.getInstance().getWelcomeMsg()
            try {
				if(installerParam.length == 1){
						while (userConfirm == null || userConfirm.length() == 0 || !userConfirm.toLowerCase().equalsIgnoreCase("y") ) {
							//-- prompt for user input parameters
							//InstallerGroovyUtil.prepareParameters(params,wiperdogHome)
					        print "\nGetting input parameters for pre-configured wiperdog, press any key to continue... " 
					        def test = inp.readLine()           
					        params['WIPERDOGHOME'] = wiperdogHome
					        
					        println "Please input Netty port(default set to 13111):"
					        def tmpNettyPort = inp.readLine().trim();
					        params['netty.port'] = (tmpNettyPort != null && ! tmpNettyPort.equals(""))?tmpNettyPort:'13111';
					            
					        println "Please input database server (Mongodb) IP address (default set to 127.0.0.1):"
					        def tmpMonitorjobfwMongodbHost = inp.readLine().trim();
					        params['monitorjobfw.mongodb.host'] = (tmpMonitorjobfwMongodbHost != null && ! tmpMonitorjobfwMongodbHost.equals(""))?tmpMonitorjobfwMongodbHost:'127.0.0.1';
					        
					        println "Please input database server port, Mongodb port (default set to 27017):"    
					        def tmpMonitorjobfwMongodbPort = inp.readLine().trim();
					        params['monitorjobfw.mongodb.port'] = (tmpMonitorjobfwMongodbPort != null && ! tmpMonitorjobfwMongodbPort.equals(""))?tmpMonitorjobfwMongodbPort:'27017';
					            
					        println "Please input database name (default set to wiperdog):"
					        def tmpMonitorjobfwMongodbDbName = inp.readLine().trim();
					        params['monitorjobfw.mongodb.dbName'] = (tmpMonitorjobfwMongodbDbName != null && ! tmpMonitorjobfwMongodbDbName.equals(""))?tmpMonitorjobfwMongodbDbName:'wiperdog';
					        
					        println "Please input database server user name, (Mongodb user name, default set to empty):"    
					        def tmpMonitorjobfwMongodbUser = inp.readLine().trim();
					        params['monitorjobfw.mongodb.user'] = (tmpMonitorjobfwMongodbUser != null && ! tmpMonitorjobfwMongodbUser.equals(""))?tmpMonitorjobfwMongodbUser:'';
					        
					        println "Please input database server password, (Mongodb password, default set to empty):"    
					        def tmpMonitorjobfwMongodbPass = inp.readLine().trim();
					        params['monitorjobfw.mongodb.pass'] = (tmpMonitorjobfwMongodbPass != null && ! tmpMonitorjobfwMongodbPass.equals(""))?tmpMonitorjobfwMongodbPass:'';
					        
					        println "Please input mail send data policy, Mail Policy (default set to testmail@gmail.com):"    
					        def tmpMonitorjobfwMailPolicy = inp.readLine().trim();					        
					        params['monitorjobfw.mail.toMail'] = (tmpMonitorjobfwMailPolicy != null && ! tmpMonitorjobfwMailPolicy.equals(""))?tmpMonitorjobfwMailPolicy:'testmail@gmail.com';        
					        
						println "Do you want to install wiperdog as system service, default set to '"+installAsService+"' (type yes|no), enter for default value:" 
        					def tmpServiceFlag = inp.readLine().trim();
        					installAsService = (tmpServiceFlag == null || tmpServiceFlag == "")?installAsService:tmpServiceFlag;
        					
							println "\n"
							println "Please CONFIRM The following configuration are correct:"
							println "Wiperdog Home:"+ params['WIPERDOGHOME']
							println "Server Port:"+ params['netty.port']
							println "Database address:"+ params['monitorjobfw.mongodb.host']
							println "Database port:"+ params['monitorjobfw.mongodb.port']
							println "Database name:"+ params['monitorjobfw.mongodb.dbName']
							println "User name:"+ params['monitorjobfw.mongodb.user']
							println "Password:"+ params['monitorjobfw.mongodb.pass']
							println "Mail Policy:"+ params['monitorjobfw.mail.toMail']
							println "Install as OS service:"+ installAsService
							println "\n"
							println "Your input are correct(Y|y|N|n):"							
							userConfirm = inp.readLine().trim();        
						} //-- END while
				} else {
					if(installerParam.length > 2) {
						params['WIPERDOGHOME'] = wiperdogHome
						for(int i = 1; i< installerParam.length; i++ ){
						   //Get Netty port from argurment
							if(installerParam[i].equals('-j')) {
								params['netty.port'] = installerParam[i+1]
								i++
							} 
							//Get Mongodb Host from argurment
							if(installerParam[i].equals('-m')) {
								params['monitorjobfw.mongodb.host'] = installerParam[i+1]
								i++
							}
							//Get Mongodb Port from argurment
							if(installerParam[i].equals('-p')) {
								params['monitorjobfw.mongodb.port'] = installerParam[i+1]
								i++
							}
							//Get Mongodb database name from argurment
							if(installerParam[i].equals('-n')) {
								params['monitorjobfw.mongodb.dbName'] = installerParam[i+1]
								i++
							}
							//Get Mongodb user name from argurment
							if(installerParam[i].equals('-u')) {
								params['monitorjobfw.mongodb.user'] = installerParam[i+1]
								i++
							}
							//Get Mongodb password from argurment
							if(installerParam[i].equals('-pw')) {
								params['monitorjobfw.mongodb.pass'] = installerParam[i+1]
								i++
							}
							//Get mail policy from argurment
							if(installerParam[i].equals('-mp')) {
								params['monitorjobfw.mail.toMail'] = installerParam[i+1]
								i++
							}
							//Get 
							if(installerParam[i].equals('-s')) {
								installAsService = installerParam[i+1]
								i++
							}
						}
						if(params['monitorjobfw.mongodb.user'] == null){
							params['monitorjobfw.mongodb.user'] = ''
						}
						if(params['monitorjobfw.mongodb.pass'] == null){
							params['monitorjobfw.mongodb.pass'] = ''
						}
						println "----------------------------------------------------------------"												
						println "------------------    Wiperdog setting     -------------------------"
						println "Wiperdog Home:"+ params['WIPERDOGHOME']
						println "Server Port:"+ params['netty.port']
						println "Database address:"+ params['monitorjobfw.mongodb.host']
						println "Database port:"+ params['monitorjobfw.mongodb.port']
						println "Database name:"+ params['monitorjobfw.mongodb.dbName']
						println "User name:"+ params['monitorjobfw.mongodb.user']
						println "Password:"+ params['monitorjobfw.mongodb.pass']
						println "Mail Policy:"+ params['monitorjobfw.mail.toMail']
						println "Install as OS service:"+ installAsService
					} 
				}
            } catch (Exception ignore) {
                println ignore
            }
        //------------------------------------ CONFIGURE WIPERDOG ------------------------------------------//
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
        
        //def newFileText = fileText.replaceAll(/_netty.port/, params['netty.port'])                
        configFile.write(newFileText)
         /*
              3. Configure monitorjobfw.cfg for 
                monitorjobfw.mongodb.host
                monitorjobfw.mongodb.port
                monitorjobfw.mongodb.dbName
                monitorjobfw.mongodb.user
                monitorjobfw.mongodb.pass
                monitorjobfw.mail.toMail
          */
        //host name
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
            println "Start install application as a system service"
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
                
                println proc.err.text
                println proc.in.text
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
                println outputStr
                println errorStr
                
            }
            println "The installation has been completed successfully! \n Please use command 'net start/stop wiperdog'(Window) or 'service wiperdog start/stop'(Linux) to control the service."
            println "Thank you for choosing Wiperdog!"
            println ""
         }//-- END install as system service
         
    }//-- end main
}
