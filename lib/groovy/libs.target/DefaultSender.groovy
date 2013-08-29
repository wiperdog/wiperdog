import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import groovy.transform.Synchronized;
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.URIBuilder
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.ContentType.JSON
import java.text.MessageFormat
import com.gmongo.GMongo
import com.google.gson.Gson
import com.google.gson.GsonBuilder;
import com.mongodb.DB
import com.mongodb.MongoException;
import com.mongodb.DBObject
import com.mongodb.util.JSON

import org.apache.log4j.Logger;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;

import java.text.SimpleDateFormat
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue
import com.gmongo.internal.Patcher

/**
 * DefaultSender Send data to host or write out to console/file
 */
class DefaultSender {
	def mapMessage = [:]
	public listHttpSender = []
	public listMongoDBSender = []
	def properties = MonitorJobConfigLoader.getProperties()
	
	public DefaultSender(){
		File monitorjobSwapFolder = new File(properties.get(ResourceConstants.MONITORSWAP_DIRECTORY))
		monitorjobSwapFolder.mkdir()
		monitorjobSwapFolder.listFiles().each{folder->
			if(folder.isDirectory()){
				String fileName = folder.getName()
				HTTPSender httpSender = new HTTPSender(new String(Base64.decodeBase64(fileName)), fileName)
				folder.listFiles().each{file->
					if(file.getName().endsWith(".swp")){
						httpSender.queue.add(file.getName())
					}
				}
				synchronized(httpSender.queue) {
					httpSender.queue.notify()
				}
				listHttpSender.add(httpSender)
			}
		}
	}
	
	/**
	 * DefaultSender Send data to host or write out to console/file
	 * @param listDestination listDestination(host/file/stdout)
	 */
	@Synchronized
	public def mergeSender(listDestination, resultList) {
		List<Sender> senderListForEachJob = resultList
		if (senderListForEachJob == null) {
			senderListForEachJob = new ArrayList<Sender>()
		}

		if(listDestination != null) {
			listDestination.each{dest->
				if(dest.file != null){
					StdoutSender stdoutSender = new StdoutSender()
					stdoutSender.destination = dest.file
					senderListForEachJob.add(stdoutSender)
				}
				if(dest.http != null){
					boolean httpSenderExist = false
					def destination = dest.http
					if(!dest.http.contains("http://")){
						destination = "http://" + dest.http
					}
					if(listHttpSender.size() > 0){
						listHttpSender.each {httpSender->
							if(new String(Base64.decodeBase64(httpSender.alias)) == destination){
								httpSenderExist = true
								senderListForEachJob.add(httpSender)
							}
						}
					}
					if(!httpSenderExist){
						def alias = new String(Base64.encodeBase64(dest.http.getBytes()))
						HTTPSender httpSender = new HTTPSender(destination, alias)
						senderListForEachJob.add(httpSender)
						listHttpSender.add(httpSender)
						
						File hostFolder = new File(properties.get(ResourceConstants.MONITORSWAP_DIRECTORY) + "/$alias")
						hostFolder.mkdir()
					}
				}
				if(dest.https != null){
					// Check and create HTTPSender same as http
				}
				if(dest.mongoDB != null){
					boolean mongoDBSenderExist = false
					def destination = dest.mongoDB
					if(listMongoDBSender.size() > 0){
						listMongoDBSender.each{mongoSender->
							if(mongoSender.destination == destination){
								mongoDBSenderExist = true;
								senderListForEachJob.add(mongoSender)
							}
						}
					}
					if(!mongoDBSenderExist){
						def mongoSender = new MongoDBSender(destination)
						senderListForEachJob.add(mongoSender)
						listMongoDBSender.add(mongoSender)
					}
				}
			}
		} else	{
			// write data to console or output file
			StdoutSender defaultsender = new StdoutSender()
			senderListForEachJob.add(defaultsender)
			return
		}
		
		return senderListForEachJob
	}
}

/**
 * Sender Interface send data to host or write data
 */
public interface Sender<T> {
	public void send(data);
}

/**
 * StdoutSender implements Sender<>
 * StdoutSender Write data to console or output file
 */
public class StdoutSender implements Sender<String>{
	def destination
	@Override
	public void send(resultData) {
		def output = destination
		// joutput : Gson to covnert data into json type
		Gson gson = new GsonBuilder()
						 .serializeNulls()
						 .setPrettyPrinting()
						 .create();
		def resultData_json = gson.toJson(resultData)
		// Write data to console or into file output
		if(output == null || output == "stdout"){
			println resultData_json
		} else {
			try{
				def outputFile = new File(output)
				if((new File(output)).isAbsolute()){
					outputFile = new File(output)
				}else{
					def felix_home = System.getProperty("felix.home")
					if(felix_home != null){
						outputFile = new File(felix_home + "/" + output)
					}else{
						if(System.getProperty("bin_home") != null){
							File bin_home = new File(System.getProperty("bin_home"))
							def felix = bin_home.getParent()
							outputFile = new File(felix + "/" + output)
						}else{
							outputFile = null
						}
					}
				}
				if(outputFile != null){
					outputFile.write(resultData_json)
					println "--Done output file--"
				}
			}catch(FileNotFoundException fnfex){
				println "Can't find specific output file to store following data:"
				println resultData_json;
			}
		}
	}
}

/**
 * HTTPSender implements Sender
 * HTTPSender Send data to host
 */
public class HTTPSender implements Sender<Map>{
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	def properties = MonitorJobConfigLoader.getProperties()
	def messageMap = [:]
	def destination
	def alias
	def sendingStatus
	def senderThread
	BlockingQueue queue = new LinkedBlockingQueue()
	JsonOutput output = new JsonOutput()
	JsonSlurper slurper = new JsonSlurper()
	
	public HTTPSender(destination, alias) {
		this.destination = destination
		this.alias = alias
		def messageFile = new File(properties.get(ResourceConstants.MESSAGE_FILE_DIRECTORY) + "/message.properties");
		def mapMessage = [:]
		messageFile.each {
			def toArray = it.split(" = ")
			mapMessage[toArray[0]] = toArray[1]
		}
		this.messageMap = mapMessage;
		senderThread = new HTTPSenderThread(properties)
		(new Thread(senderThread)).start()
	}
	
	@Override
	public void send(data2send){
		if(checkCountandSize()){
			// Write data to swap file
			def jobname = (data2send.instanceName == null) ? data2send.sourceJob : (data2send.sourceJob + "_" + data2send.instanceName)
			def sequence = HTTPSender.getFileName(jobname)
			def swapFile = new File(properties.get(ResourceConstants.MONITORSWAP_DIRECTORY) + "/$alias/$sequence" + ".swp")
			logger.debug("Write data of " + data2send.sourceJob + " to file " + swapFile.getName() + "\n into folder $alias")
			def dataJson = output.toJson(data2send)
			swapFile.setText(dataJson)
			// put to queue
			this.queue.add(swapFile.getName())
			synchronized(this.queue) {
				this.queue.notify()
			}
		}else{
			logger.info("Reach max allowed number of files or size")
		}
	}
	
	public boolean checkCountandSize(){
		final int MAX_NUMBER_OF_FILE_SWAP = Integer.valueOf(properties.get(ResourceConstants.MAX_NUMBER_SWAP_FILE))
		final long MAX_SWAP_FOLDER_SIZE_MB = Long.valueOf(properties.get(ResourceConstants.MAX_SWAP_DIRECTORY_SIZE))
		if(MAX_NUMBER_OF_FILE_SWAP < 0 && MAX_SWAP_FOLDER_SIZE_MB < 0){
			return true
		}
		boolean isValid = false;
		int countFile = 0
		def countSize = 0
		def monitorSwapFolder = new File(properties.get(ResourceConstants.MONITORSWAP_DIRECTORY))
		monitorSwapFolder.mkdir()
		monitorSwapFolder.listFiles().each {hostFolder->
			if(hostFolder.isDirectory()){
				hostFolder.listFiles().each{swapFile->
					if(swapFile.getName().endsWith(".swp")){
						countFile++
						countSize += swapFile.length()
					}
				}
			}
		}
		if(MAX_NUMBER_OF_FILE_SWAP > 0 && MAX_SWAP_FOLDER_SIZE_MB > 0){
			if(countFile < MAX_NUMBER_OF_FILE_SWAP && countSize < (MAX_SWAP_FOLDER_SIZE_MB * 1024 * 1024)){
				isValid = true
			}
		}
		if(MAX_NUMBER_OF_FILE_SWAP < 0){
			if(countSize < (MAX_SWAP_FOLDER_SIZE_MB * 1024 * 1024)){
				isValid = true
			}
		}
		if(MAX_SWAP_FOLDER_SIZE_MB < 0){
			if(countFile < MAX_NUMBER_OF_FILE_SWAP){
				isValid = true
			}
		}
		return isValid
	}
	
	@Synchronized
	public static String getFileName(sourceJob){
		def sequence = new Date().format("yyyyMMddHHmmss.SSS")
		return sourceJob + "_" + sequence
	}
	
	class HTTPSenderThread implements Runnable {
		def properties
		
		public HTTPSenderThread(properties){
			this.properties = properties
		}
		@Override
		public void run() {
			logger.debug("$alias thread is running")
			while(true){
				synchronized(queue) {
				    while (queue.isEmpty())
				        queue.wait(); //wait for the queue to become not empty
				}
				// Browse queue and send data
				if(sendingStatus != null && sendingStatus == 'NG'){
					long sleep_time = Long.valueOf(this.properties.get(ResourceConstants.SLEEP_TIME_MS))
					logger.debug("[" + destination + "] bad sending status! sleep $sleep_time ms before resend")
					Thread.currentThread().sleep(sleep_time)
				}
				String fileName = queue.peek()
				String fullFilePath = getFilePath(alias, fileName)
				if(fullFilePath != null){
					File swapFile = new File(fullFilePath)
					if(swapFile.exists()){
						FileInputStream fileIn = new FileInputStream(swapFile)
						InputStreamReader reader = new InputStreamReader(fileIn)
						def data = slurper.parse(reader)
						fileIn.close()
						reader.close()
						boolean isSuccess = HTTPsend(data)
						if(isSuccess){
							// Send successfully -> delete file
							sendingStatus = 'OK'
							queue.remove()
							if(!swapFile.delete()){
								logger.debug(swapFile.getName() + " was sent to $destination but wasn't deleted by some resons. Deleting manually needed")
							}
						}else{
							sendingStatus = 'NG'
						}
					}
				}
			}
		}
		
		public String getFilePath(String host, String fileName){
			String filePath
			if(fileName != null){
				filePath = this.properties.get(ResourceConstants.MONITORSWAP_DIRECTORY) + "/$host/$fileName"
			}
			return filePath
		}
		
		public boolean HTTPsend(data2send) {
			def http
			//Serializer date data before send
			def serializerData = serializeDateToSend(data2send)
			def isSuccess = false
			try {
				http = new HTTPBuilder(destination)
				http.auth.basic("administrator", "insight")
				http.request(PUT,groovyx.net.http.ContentType.JSON){req->
						req.getParams().setParameter("http.connection.timeout", new Integer(5000));
						req.getParams().setParameter("http.socket.timeout", new Integer(5000));
						body = serializerData
					 	response.success = { resp ->
					 		isSuccess = true
					  	}
				}
			} catch (Exception e) {
				logger.info ("Fail to send data to $destination")
			}
			return isSuccess
		}
		
		private String serializeDateToSend(data2send) {
			CustomSerializerFactory sfactory = new CustomSerializerFactory();
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializerFactory(sfactory);
			sfactory.addGenericMapping(java.util.Date.class, new DateSerializer());
			return mapper.writeValueAsString(data2send)
		}
	}
}

public class MongoDBSender implements Sender<Map>{
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	def properties = MonitorJobConfigLoader.getProperties()
	def destination //String destination
	def mapDetailDestination = [:] //Map [host:a, port:b, db:c]
	GMongo mongo;
	long sleep_time = Long.valueOf(this.properties.get(ResourceConstants.SLEEP_TIME_MS))
	
	public MongoDBSender(String destination){
		try{
			this.destination = destination
			mapDetailDestination['host'] = destination.substring(0, destination.indexOf(":") != -1 ? destination.indexOf(":") : destination.indexOf("/"))
			if(destination.indexOf(":") != -1 && destination.indexOf("/") != -1){
				mapDetailDestination['port'] = destination.substring(destination.indexOf(":") + 1, destination.indexOf("/"))
			}	
			mapDetailDestination['db'] = destination.substring(destination.indexOf("/") + 1)
		}catch(IndexOutOfBoundsException oobex){
			logger.debug("Destination string is in a wrong format!\nFormat must be host:port/db or host/db")
		}
	}

	def createConnection(mapDetailDestination){
		logger.debug("Try to connect to mongoDB with $mapDetailDestination")
		try{
			def port = mapDetailDestination['port']
			def host = mapDetailDestination['host']
			def dbName = mapDetailDestination['db']
			if(host == "localhost" && port == null){
				mongo = new GMongo()
			}else if(host == "localhost" && port != null){
				mongo = new GMongo(host + ":" + port)
			}else if(host != null && port != null){
				mongo = new GMongo(host, Integer.valueOf(port))
			}
		}catch(Exception ex){
			mongo = null
		}
		return mongo
	}
	
	@Override
	public void send(Object data) {
	    def data_serialzeDate = serializeDateToSend(data)
	   	DB db
		DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(data_serialzeDate)
		for(int i = 0; i < 20; i++){
			try{
				mongo = createConnection(mapDetailDestination)
				if(mongo != null){
					db = mongo.getDB(mapDetailDestination['db'])
					def jobName = data.sourceJob
					def istIid = data.istIid
					def col = db.getCollection(jobName + "." + istIid)
					col.insert(dbObject)
					mongo.close()
					println "-Done send data to mongo DB at ${mapDetailDestination['host']}-"
					break;
				}
			}catch(MongoException mex){
				logger.debug("Could not connect to mongoDB! Sleep before retrying...")
				Thread.currentThread().sleep(sleep_time)
			}catch(Exception ex){
				logger.debug(ex)
			}
		}
	}

	private String serializeDateToSend(data2send) {
		CustomSerializerFactory sfactory = new CustomSerializerFactory();
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializerFactory(sfactory);
		sfactory.addGenericMapping(java.util.Date.class, new DateSerializer());
		return mapper.writeValueAsString(data2send)
	}	
	
	
}

/**
 * Serializer date data before send
 */
class DateSerializer extends JsonSerializer<Date> {
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	def dateFormat = new SimpleDateFormat(ResourceConstants.DATEFORMAT)
	
   	public void serialize(Date inputDate, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		try {
   			jgen.writeString(dateFormat.format(inputDate));
		} catch (Exception e) {
			logger.info (e.toString())
		}
	}
}

