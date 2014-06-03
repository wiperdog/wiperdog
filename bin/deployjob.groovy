@Grapes(
	@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.1')
)

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream
import groovy.json.*
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.entity.StringEntity 

def getTextFromStream = { inputStream ->
	BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
	StringBuffer result = new StringBuffer();
	String line = "";
	while ((line = rd.readLine()) != null) {
		result.append(line);
	}
	inputStream.close()
	return result.toString()
}
BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
println "Please start wiperdog before run this tools !"
println "**To configuring for maven repository ,please configure in wiperdog_home/etc/maven_repositories.cfg"
println "Please input data for deploy job: "
println "---------------------------"
def groupId 
def artifactId
def version
while(groupId == null || groupId.trim().equals("")) {
	print "Enter bundle groupId: " ;
	groupId = reader.readLine()
}
while(artifactId == null || artifactId.trim().equals("")) {
	print "Enter bundle artifactId: " ;
	artifactId = reader.readLine()
}
while(version == null || version.trim().equals("")) {
	print "Enter bundle version: " ;
	version = reader.readLine()
}
print "Enter wiperdog host (default is : \"localhost\"): " ;
def host = reader.readLine()
if(host == null || host.trim().equals("")) {
	host = "localhost"
}
print "Enter Rest post (default is : 8089): " ;
def port = reader.readLine()
if(port == null || port.trim().equals("")) {
	port = "8089"
}
HttpClient client = HttpClientBuilder.create().build();
String url = "http://${host}:${port}/bundle/${groupId}/${artifactId}/${version}"
HttpGet request = new HttpGet(url);
println "Processing..."
HttpResponse response = client.execute(request);
def responseData = getTextFromStream(response.getEntity().getContent())
def listFileDisplay = []
if(responseData != null && !responseData.trim().equals("")) {
	try{
		def slurper = new JsonSlurper()
		def objData = slurper.parseText(responseData)
		if(objData.status.equals("success")) {
			println "List file from bundle: "
			objData.listFiles.eachWithIndex{ item,index ->
				println "${index}. ${item}"
				def mapFileDisplay = [:]
				mapFileDisplay["status"] = "Not installed"
				mapFileDisplay["index"] = index
				mapFileDisplay["file"] = item
				listFileDisplay.add(mapFileDisplay)
			}
			while(true) {
				println "---------------------------------------------------------------------------------"
				println "Select index number for install specific file, seperate by space (EX : 1 2 3 4 5 )"
				print "Enter 'all' for install all files:  Left empty for exit : "
				def listInstallFile = []
				def stringInput = reader.readLine()
				if(stringInput == null || stringInput.trim().equals("")) {
					break;
				}
				if(stringInput.trim().equals("all")) {
					// install all file
					objData.listFiles.eachWithIndex{ item,index ->
							def tmpMap = [:]
							tmpMap["index"] = index
							tmpMap["path"] = item
							listInstallFile.add(tmpMap)
					}
				} else {
					// install selected file
					def tmpList = stringInput.split(" ")
					tmpList.each{
						try{

							def number = Integer.parseInt(it)
							def tmpMap = [:]
							tmpMap["index"] = number
							tmpMap["path"] = objData.listFiles.getAt(number)
							if(tmpMap["path"] != null) {
								listInstallFile.add(tmpMap)
							} else {
								println "Can not select file at index : ${number}"
							}
							
						} catch(NumberFormatException ex) {
							ex.printStackTrace()
						} 
					}

				}
				url = "http://${host}:${port}/bundle/install"
				if(listInstallFile != []) {
					def builder = new JsonBuilder(listInstallFile)
					HttpPost post = new HttpPost(url);
					post.setHeader("Content-type", "application/json");
					post.addHeader("Connection", "close");
					StringEntity se = new StringEntity(builder.toString()); 
					post.setEntity(se);
					response = client.execute(post);
			        responseData = getTextFromStream(response.getEntity().getContent())
			        def objectData2 = slurper.parseText(responseData)
			        def iter = listFileDisplay.listIterator()
			        while(iter.hasNext()){
			        	def mapItem = iter.next()
			        	objectData2.each{
			        		if(it["index"].equals(mapItem["index"])) {
			        			mapItem["status"] = "Installed"	
			        			iter.set(mapItem)
			        		}
			        	}
			        }
			        listFileDisplay.each{
			        	println "${it.index}. ${it.file} - ${it.status}"
			        }
			       
				}
			}
		} else {
			println "Failed to get bundle: ${artifactId}-${version}" 
		}

	}catch(Exception ex){	
		ex.printStackTrace()
	}
}

