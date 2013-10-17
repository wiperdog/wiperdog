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


public class WPDInstallerGroovy{
    public static void main(String[] installerParam) throws Exception{
        def params = [:]
        def userConfirm = ""   
        String wiperdogHome = installerParam[0]
        while (userConfirm.length() == 0 || !userConfirm.toLowerCase().equalsIgnoreCase("y") ) {
            //-------------------------------------- Parse configuration XML ------------------------------------//
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
            factory.setValidating(true)
            factory.setIgnoringElementContentWhitespace(true)
            DocumentBuilder docBuilder = factory.newDocumentBuilder()
            docBuilder.setErrorHandler(new XMLErrorHandler())
            Document doc = docBuilder.parse(InstallerUtil.class.getResourceAsStream("/extractor.xml"))
            InstallerUtil.parseXml(doc.getDocumentElement())
            println "Welcome to the installation of " +InstallerXML.getInstance().getAppName()  + " - Version: " + InstallerXML.getInstance().getAppVersion() 
            println InstallerXML.getInstance().getWelcomeMsg()
            println "installAsService? " + InstallerXML.getInstance().getInstallAsOSService() 
        
            try {   
                InputStreamReader converter = new InputStreamReader(System.in);
                BufferedReader inp = new BufferedReader(converter);
                
                InstallerGroovyUtil.prepareParameters(params, wiperdogHome)
                println "\n"
                println "Please CONFIRM The following configuration are correct:"
                println "Wiperdog Home:"+ params['WIPERDOGHOME']
                println "Server Port:"+ params['jetty.port']
                println "Database address:"+ params['monitorjobfw.mongodb.host']
                println "Database port:"+ params['monitorjobfw.mongodb.port']
                println "Database name:"+ params['monitorjobfw.mongodb.dbName']
                println "\n"
                println "Your input are correct(Y|y|N|n):"
                userConfirm = inp.readLine().trim();        
            } catch (Exception ignore) {
                println ignore
            }
        }//-- END while
        //------------------------------------ CONFIGURE WIPERDOG ------------------------------------------//               
         //-- 1. replace  wiperdog home in bin/wiperdog
         String installAsService = InstallerXML.getInstance().getInstallAsOSService()
         
         //-- 2. Configure system.properties for jetty.port
        def configFile = new File("etc/system.properties")
        def fileText = configFile.text 
        
        def newFileText = ""
        String macherPattern = "(jetty.port=)((?:(?!\\n).)*)"
        Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(fileText);
        while(matcher.find())
        {
            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + params['jetty.port'])
        }                
        configFile.write(newFileText)
        
        //def newFileText = fileText.replaceAll(/_jetty.port/, params['jetty.port'])                
        configFile.write(newFileText)
         /*
              3. Configure monitorjobfw.cfg for 
                monitorjobfw.mongodb.host
                monitorjobfw.mongodb.port
                monitorjobfw.mongodb.dbName
          */
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
        
        
        //------------------------------------ INSTALL WIPERDOG AS SERVICE ------------------------------------------//
        if(installAsService != null && installAsService.equalsIgnoreCase("yes")){
            println "Start install application as a system service"
             String osName = System.getProperty("os.name").toLowerCase()    
            //-- configure Wiperdog Home in service script            
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
                    newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + newHome)
                }              
                def crlfFixFileText = newFileText.replaceAll("\r","");
                configFile.write(crlfFixFileText)
                /** Replace CRLF to LF to fit Unix file format **/
                
                /* Install service script */
                FileOutputStream fos = new FileOutputStream(new File("install_service.sh"))
                StringBuffer sBuff = new StringBuffer(512)
                sBuff.append("#!/bin/sh\n#\n#\n#\n")
                sBuff.append("/sbin/service wiperdog stop\n")
                sBuff.append("/bin/cp "+wiperdogHome+"/bin/wiperdog /etc/rc.d/init.d/\n")
                sBuff.append("/bin/chmod 755 /etc/rc.d/init.d/wiperdog\n")
                sBuff.append("/sbin/chkconfig --del wiperdog\n")
                sBuff.append("/sbin/chkconfig --add wiperdog\n")
                sBuff.append("/sbin/chkconfig --level 2 wiperdog on\n")
                sBuff.append("/sbin/chkconfig --level 3 wiperdog on\n")
                sBuff.append("/sbin/chkconfig --level 4 wiperdog on\n")
                sBuff.append("/sbin/chkconfig --level 5 wiperdog on\n")
                sBuff.append("/sbin/chkconfig --list wiperdog\n")
                sBuff.append("/bin/chmod 755 "+ wiperdogHome +"/bin/* \n")
                sBuff.append("/bin/rm -f "+ wiperdogHome +"/var/run/wiperdog.lck \n")
                
                //-- Set file format for executable unix shell script in 'bin' folder                
                sBuff.append("/bin/vi +':w ++ff=unix' +':q' "+ wiperdogHome +"/bin/clearjobdata \n")
                sBuff.append("/bin/vi +':w ++ff=unix' +':q' "+ wiperdogHome +"/bin/gendbpasswd.sh \n")
                sBuff.append("/bin/vi +':w ++ff=unix' +':q' "+ wiperdogHome +"/bin/groovy \n")
                sBuff.append("/bin/vi +':w ++ff=unix' +':q' "+ wiperdogHome +"/bin/jobrunner.sh \n")
                sBuff.append("/bin/vi +':w ++ff=unix' +':q' "+ wiperdogHome +"/bin/startGroovy \n")
                sBuff.append("/bin/vi +':w ++ff=unix' +':q' "+ wiperdogHome +"/bin/startWiperdog.sh  \n")
                sBuff.append("/bin/vi +':w ++ff=unix' +':q' "+ wiperdogHome +"/bin/wiperdog \n")
                
                fos.write(sBuff.toString().getBytes());
                fos.flush();
                fos.close();
                def proc = "/bin/chmod 755 install_service.sh".execute()
                println proc.err.text
                println proc.in.text
                
                proc = "/bin/sh install_service.sh".execute()
                println proc.err.text
                println proc.in.text
            }else{//-- Window
                String javaHome = System.getProperty("java.home")
                //File jvmDllFile = new File(javaHome + "/bin/client/jvm.dll") //-- check if user use JRE                
                def commandString = wiperdogHome +"/service/javaservice/create_wiperdog_service.bat \"" + wiperdogHome + "\""
                def proc = commandString.execute()
                //proc.waitFor() //-- cause error if wiperdog_service.exe has been used by another process
                def errorStr = proc.err.text
                def outputStr = proc.in.text
                println outputStr
                println errorStr
                
            }
         }//-- END install as system service
         println "The installation has been completed successfully!, thank you for choosing Wiperdog!"
    }//-- end main
}
class InstallerGroovyUtil{
    static prepareParameters(paramsMap, wiperdogHome){
        InputStreamReader converter = new InputStreamReader(System.in);
        BufferedReader inp = new BufferedReader(converter);
        println "\n"        
        paramsMap['WIPERDOGHOME'] = wiperdogHome
        
        println "Please input Jetty Port(default set to 13110):"
        def tmpJettyPort = inp.readLine().trim();
        paramsMap['jetty.port'] = (tmpJettyPort != null && ! tmpJettyPort.equals(""))?tmpJettyPort:'13111';
            
        println "Please input database server (Mongodb) IP address (default set to 127.0.0.1):"
        def tmpMonitorjobfwMongodbHost = inp.readLine().trim();
        paramsMap['monitorjobfw.mongodb.host'] = (tmpMonitorjobfwMongodbHost != null && ! tmpMonitorjobfwMongodbHost.equals(""))?tmpMonitorjobfwMongodbHost:'127.0.0.1';
        
        
        println "Please input database server port, Mongodb port (default set to 27017):"    
        def tmpMonitorjobfwMongodbPort = inp.readLine().trim();
        paramsMap['monitorjobfw.mongodb.port'] = (tmpMonitorjobfwMongodbPort != null && ! tmpMonitorjobfwMongodbPort.equals(""))?tmpMonitorjobfwMongodbPort:'27017';
            
        println "Please input database name (default set to wiperdog):"
        def tmpMonitorjobfwMongodbDbName = inp.readLine().trim();
        paramsMap['monitorjobfw.mongodb.dbName'] = (tmpMonitorjobfwMongodbDbName != null && ! tmpMonitorjobfwMongodbDbName.equals(""))?tmpMonitorjobfwMongodbDbName:'wiperdog';
    }
 }