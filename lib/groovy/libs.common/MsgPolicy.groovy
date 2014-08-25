import com.gmongo.GMongo
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

import java.text.SimpleDateFormat

import groovy.json.*

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.lang.GroovyShell

import java.util.*;
import java.text.*;

class MsgPolicy {
	def create(Request request, Response response){
		return "CREATE";
	}
	def update(Request request, Response response){
		return "UPDATE";
	}
	def delete(Request request, Response response){
		return "DELETE";
	}
	def read(Request request, Response response){
		//def gmongoObject = services.MongoDBConnection
		//gmongoObject.getConnection(services.WiperdogConfig.getDataFromConfig())
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")

		def dbConn = MongoDBConnection.getWiperdogConnection()['db']

		//def slurper = new JsonSlurper()
		//def builder
		try {
			//response.setContentType('text')

			def action = request.getUrlDecodedHeader("action")
			if(action == "INITSCREEN") {
				def lstJobMongo = dbConn.getCollectionNames()
				def mapFinal = [:]
				def lstJobName = []
				def lstIntIid = []
				for(eCollection in lstJobMongo){
					if(eCollection != "system.indexes" && eCollection != "policy_message") {
						if(eCollection.contains(".")) {
							def jobName = eCollection.substring(0, eCollection.lastIndexOf("."))
							if(jobName != null && jobName != "") {
								if(!lstJobName.contains(jobName)){
									lstJobName.add(jobName)
								}
							}
							def istIid = eCollection.substring(eCollection.lastIndexOf(".") + 1, eCollection.size())
							if(istIid != null && istIid != "") {
								if(!lstIntIid.contains(istIid)){
									lstIntIid.add(istIid)
								}
							}
						} else {
							lstJobName.add(eCollection)
						}
					}
				}
				mapFinal['lstJobName'] = lstJobName
				mapFinal['lstIntIid'] = lstIntIid
				mapFinal['status'] = 'success'
				return mapFinal
			} else if(action == "GETDATA") {
				// Data response
				def mapFinal = [:]
				def dataMongo = dbConn['policy_message'].find().sort( fetchedAt_bin : -1 ).limit(100)
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
				//sdf.setTimeZone(TimeZone.getTimeZone("GMT+7"));
				def milis
				Date date
				mapFinal['lstCollections'] = []
				for(data in dataMongo){
					milis = ((Long)data.fetchedAt_bin) * 1000
					String formatted = sdf.format(new Date(milis))
					data.fetchedAt_bin = formatted
					mapFinal['lstCollections'].add(data)
				}

				mapFinal['status'] = 'success'
				//mapFinal['lstCollections'] = dataMongo
				return mapFinal
				mapFinal = null
				dataMongo = null
			}

		} catch(Exception ex) {
			def mapErrorMsg = ['status': 'fail', 'message': ex.message]
			return mapErrorMsg
			mapErrorMsg = null
		} finally {
//			if(gmongoObject != null){
//				gmongoObject.closeConnection()
//			}
		}

		// For better garbage collector
		slurper = null
		builder = null

	}
}
