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
	def static listMongodbConnectInfo = []
	public listHttpSender = []
	public listMongoDBSender = []
	def properties = MonitorJobConfigLoader.getProperties()
	
	public DefaultSender(){
		// List sender
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

		// Mongo db password information
		File mongodbPassFile = new File(properties.get(ResourceConstants.MONGODB_PASS_CONFIG))
		mongodbPassFile.eachLine {line ->
			def mapMongodbInfo = [:]
			def lstPassInfo = line.split(",")
			if(lstPassInfo.size() >= 3) {
				mapMongodbInfo['host'] = lstPassInfo[0].trim()
				mapMongodbInfo['user'] = lstPassInfo[1].trim()
				mapMongodbInfo['pass'] = lstPassInfo[2].trim()
			} else if(lstPassInfo.size() == 2) {
				mapMongodbInfo['host'] = lstPassInfo[0].trim()
				mapMongodbInfo['user'] = lstPassInfo[1].trim()
				mapMongodbInfo['pass'] = ''
			} else if(lstPassInfo.size() == 1) {
				mapMongodbInfo['host'] = lstPassInfo[0].trim()
				mapMongodbInfo['user'] = ''
				mapMongodbInfo['pass'] = ''
			}
			listMongodbConnectInfo.add(mapMongodbInfo)
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
						def mongoSender = new MongoDBSender(destination, listMongodbConnectInfo)
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
			def jobname = (data2send.instanceName == null) ? data2send.sourceJob : (data2send.sourceJob + "_" + data2send.instanceName)
			def sequence = HTTPSender.getFileName(jobname)
			// check queue is not empty or not
			if(!this.queue.isEmpty()) {
				// write data to swap file
				writeData(data2send, sequence)
			} else {
				// send data to http
				HashMap mapData2Send = new HashMap(data2send)
				def isSuccess = senderThread.HTTPsend(mapData2Send)
				if(!isSuccess) {
					// write data to swap file
					writeData(data2send, sequence)
				}
			}
		}else{
			logger.info("Reach max allowed number of files or size")
		}
	}
	
	/**
	 * Write data to swap file and add to queue
	 * @param data2send data send to http
	 * @param sequence name of swap file corresponding to data
	 */
	public void writeData(data2send, sequence) {
		def swapFile = new File(properties.get(ResourceConstants.MONITORSWAP_DIRECTORY) + "/$alias/$sequence" + ".swp")
		logger.debug("Write data of " + data2send.sourceJob + " to file " + swapFile.getName() + "\n into folder $alias")
		def dataJson = output.toJson(data2send)
		// write data to swap file
		swapFile.setText(dataJson)
		// put to queue
		this.queue.add(swapFile.getName())
		synchronized(this.queue) {
			this.queue.notify()
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
				http.request(PUT,groovyx.net.http.ContentType.JSON) {req->
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
	GMongo gmongo;
	DB db;
	long sleep_time = Long.valueOf(this.properties.get(ResourceConstants.SLEEP_TIME_MS))
	
	public MongoDBSender(String destination, listMongodbInfo) {
		try{
			if(destination.contains(",") && destination.split(",").size() == 2) {
				def lstDestination = destination.split(",")
				this.destination = lstDestination[0]
				mapDetailDestination['user'] = lstDestination[1]
			} else {
				this.destination = destination
				mapDetailDestination['user'] = ""
			}
			mapDetailDestination['host'] = this.destination.substring(0, this.destination.indexOf(":") != -1 ? this.destination.indexOf(":") : this.destination.indexOf("/"))
			if(this.destination.indexOf(":") != -1 && this.destination.indexOf("/") != -1){
				mapDetailDestination['port'] = this.destination.substring(this.destination.indexOf(":") + 1, this.destination.indexOf("/"))
			}	
			mapDetailDestination['db'] = this.destination.substring(this.destination.indexOf("/") + 1)
			listMongodbInfo.each {eMongoConnect ->
				if(this.destination == eMongoConnect['host'] && mapDetailDestination['user'] == eMongoConnect['user']) {
					mapDetailDestination['pass'] = eMongoConnect['pass']
				}
			}
			
			//Get Connection
			getConnection()
		} catch(IndexOutOfBoundsException oobex) {
			logger.debug("Destination string is in a wrong format!\nFormat must be <host:port/db>, <host/db> or <host/db,username>")
		}
	}
	
	@Override
	public void send(Object data) {
	    def data_serialzeDate = serializeDateToSend(data)
		DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(data_serialzeDate)
		if((gmongo == null) || (db == null)) {
			//Get Connection
			getConnection()
		}
		try {
			def jobName = data.sourceJob
			def istIid = data.istIid
			def col = db.getCollection(jobName + "." + istIid)
			col.insert(dbObject)
			println "-Done send data to mongo DB at ${mapDetailDestination['host']}-"
		} catch(Exception ex) {
			logger.info("Can not save data to MongoDB in ${mapDetailDestination['host']}")
			logger.info(ex)
		}
	}
	
	private void getConnection() {
		for(int i = 0; i < 20; i++) {
			try {
				def mongoDBConnectionObj = new MongoDBConnection()
				def mapMongoDb = mongoDBConnectionObj.createConnection(mapDetailDestination)
				gmongo = mapMongoDb['gmongo']				
				db = mapMongoDb['db']
				if((gmongo != null) && (db != null)) {
					break;
				} else {
					logger.info("MongoDBSender: Can't connect to MongoDB, retrying... !")
					Thread.currentThread().sleep(sleep_time)
				}
			} catch(MongoException mex) {
				logger.info("Could not connect to mongoDB! Sleep before retrying...")
				Thread.currentThread().sleep(sleep_time)
			}
		}
		if((gmongo == null) || (db == null)) {
			logger.info ("After 20 times try to connect to MongoDB in " + mapDetailDestination['host'] + ". Cannot connect!")
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