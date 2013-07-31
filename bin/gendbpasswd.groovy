/*
 *  Copyright 2013 Insight technology,inc. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;

public class AES {
	static final String COMMON_UTIL_FILE = "/lib/groovy/libs.target/CommonUltis.groovy"
	static final String ORACLE_PASSWD_DIR = "/var/conf/"
	
	public static void main(String[] args) throws Exception {
		def DBType = null
		def username = null		
		def hostId = null
		def sid = null
		def password = null
		def encryptedString = null
		// Get args
		if(args[0] == '-t') {
			DBType = args[1]
		}
		if(args[2] == '-u') {
			username = args[3]
		}
		if((args.length >= 6) && (args[4] == '-h')) {
			hostId = args[5]
		}
		if((args.length == 8) && (args[6] == '-s')) {
			sid = args[7]
		} else 	if((args.length == 6) && (args[4] == '-s')) {
			sid = args[5]
		}
		
		println "DBType:" + DBType
		println "Username:" + username
		println "HostId:" + hostId
		println "Sid:" + sid
		
		if ((DBType == null) || (username == null)) {
			println "Incorrect parameters !!!"
			println "Correct format of commmand: gendbpasswd -t DBType -u username [-h hostId] [-s sid]"
			return
		}
		
		def DBTypeList = ['@ORA' , '@MYSQL' ,'@PGSQL' ,'@MSSQL']
		if (!DBTypeList.contains(DBType)) {
			println "DBType is incorrect. DBType may accept following value: @ORA , @MYSQL ,@PGSQL ,@MSSQL"
			return
		}
		
		try {
			println "------------------------------"
			println ""
			// creates a console object
			Console cnsl = System.console();
			// if console is not null
			if (cnsl != null) {
				// get password from user input
				def passwordArray = cnsl.readPassword("Password: ");
				password = new String(passwordArray)
				
				//get CommonUtils.groovy file to parse
				def felix_home = getFelixHome()
				def commonUtilFile = new File( felix_home + COMMON_UTIL_FILE)
				//Parse file
				GroovyClassLoader gcl = new GroovyClassLoader();
				Class commonClass = gcl.parseClass(commonUtilFile);
				Object commonUtil_obj = commonClass.newInstance()
				
				//encrypt password
				String encryptedPasswd = commonUtil_obj.encrypt(password);
				
				//get password file
				def passwdFilename = commonUtil_obj.getPasswdFileName(DBType, hostId, sid)
				updatePwdFile(passwdFilename, username, encryptedPasswd)
				println "Update password successfully in password file " + passwdFilename
			}
		} catch (Exception ex){
        	// if any error occurs
        	println ex
		}
	}
	
	public static String getFelixHome(){
		//	get felix home
		File currentDir = new File(System.getProperty("bin_home"))
		def felix_home = currentDir.getParent()
		return felix_home
	}
	
	public static void updatePwdFile(passwdFilename, username, encryptedPassword){
		def mapPasswd = [:]
		def felix_home = getFelixHome()
		try {
			def pwdFile = new File(felix_home + ORACLE_PASSWD_DIR + passwdFilename)
			if (!pwdFile.exists()) {
				pwdFile.createNewFile()
			}
			
			// Write encrypted password into password file			
			def tempArray
			pwdFile.eachLine {
				tempArray = it.split(":")
				mapPasswd[tempArray[0]] = tempArray[1].trim()
			}
			mapPasswd[username] = encryptedPassword
			StringBuilder strBuilder = new StringBuilder()
			mapPasswd.each{
				strBuilder.append(it.key + ":" + it.value + "\n")
			}
			if (pwdFile != null) {
				pwdFile.setText(strBuilder.toString())
			}
		} catch (Exception ex) {
        	println ex
		}
	}
}
