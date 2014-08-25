import groovy.json.JsonSlurper;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;
import java.util.jar.JarFile;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.osgi.framework.BundleContext;
import org.wiperdog.bundleextractor.BundleExtractor
class JobInstallController{
	def properties = MonitorJobConfigLoader.getProperties()
	String DEST_DIR_STR = properties.get(ResourceConstants.JOB_DIRECTORY)
	final static String TMP_BUNDLE_DIR = "tmp/bundle"
	final static String TMP_JOB_DIR = "tmp/jobs"
	BundleContext ctx;
	BundleExtractor extr;
	JobInstallController(BundleContext ctx){
		this.ctx = ctx;
		this.extr = ctx.getService(ctx.getServiceReference(BundleExtractor.class.getName()))
	}
	public List<Map<String,Object>> create(Request request, Response response){
		def listData = []
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		//Get list file from xwiki
		def dataReq =  (new ChannelBufferInputStream(request.getBody())).getText()
		JsonSlurper sluper = new JsonSlurper()
		def objectData = sluper.parseText(dataReq)

		if(objectData != null ){
			objectData.each{
				def copyStatus = null
				def returnData = [:]
				File sourceFile = new File(it["path"])
				File destFile = new File(DEST_DIR_STR +  File.separator  + sourceFile.getName())
				try{					
					copyStatus =  copyFile(sourceFile,destFile)
					returnData["status"] = copyStatus
					returnData["index"] = it["index"]
					returnData["file"] = destFile.getAbsolutePath()
					sourceFile.delete()
					listData.add(returnData)
				}catch(Exception ex){
					returnData["status"] = false
					returnData["index"] = it["index"]
					returnData["file"] = destFile.getAbsolutePath()
					returnData["message"] = ex.getMessage()
					listData.add(returnData)
					ex.printStackTrace();
				}
			}
		}
		return listData
	}
	public String update(Request request, Response response){
		return "update_METHOD"
	}

	// get  /bundle/groupid/artifactid/version
	public Map<String,Object> read(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		def responseData = [:]
		try{
			def bundleInfoMap = [:]
			bundleInfoMap["groupId"] = request.getUrlDecodedHeader("groupId")
			bundleInfoMap["artifactId"] = request.getUrlDecodedHeader("artifactId")
			bundleInfoMap["version"] = request.getUrlDecodedHeader("version")
			def bundleFileName = bundleInfoMap["artifactId"] + "-"+bundleInfoMap["version"]+".jar"
			bundleInfoMap["location"] = System.getProperty("felix.home") + File.separator  + TMP_BUNDLE_DIR + File.separator + bundleFileName
			bundleInfoMap["getit"] = true
			String location  = extr.processResource(bundleInfoMap)
			def bundleFile = new File(location) 
			if(!bundleFile.exists()){
				println "Can not get bundle: " + bundleFile.getName()
				responseData["status"] = "failed"
				responseData["message"] = "Can not get bundle : " + bundleFile.getName()
				return responseData
			}
			extr.extractPackage(bundleFile)
			JarFile jar = new JarFile(bundleFile);
			String destination = jar.getManifest().getMainAttributes().getValue("Destination");			
			File destDir =  new File(System.getProperty("felix.home")+ File.separator + destination)
			if(destDir.exists()){
				def listFilePath = []
				destDir.listFiles().each{
					listFilePath.add(it.getCanonicalPath());
				}
				
				responseData["status"] = "success"
				responseData["listFiles"] = listFilePath
			}else{
				responseData["status"] = "failed"
				responseData["message"] = "Destination directory not found "
			}
			bundleFile.delete()

		}catch(Exception ex){
			responseData["status"] = "failed"
			responseData["message"] = ex.getMessage()
			ex.printStackTrace();
		}
		return responseData
	}
	public String delete(Request request, Response response){
		return "delete_METHOD"
	}
	public boolean copyFile(File sourceFile,File dest){

		if(!sourceFile.exists()) {
			println "File not found to copy: " + sourceFile
			return false
		}
		if(sourceFile.isDirectory()){
			//if directory not exists, create it
			if(!dest.exists()){
				dest.mkdir();
				System.out.println("Directory copied from "
						+ sourceFile + "  to " + dest);
			}

			//list all the directory contents
			String[] files = sourceFile.list();

			for (String file : files) {
				//construct the src and dest file structure
				File srcFile = new File(sourceFile, file);
				File destFile = new File(dest, file);
				//recursive copy
				copyFile(srcFile,destFile);
			}
			return true
		} else {

			InputStream ins = new FileInputStream(sourceFile)
			OutputStream outs = new FileOutputStream(dest)
			try {
				byte[] buffer = new byte[1024]
				int length;
				while((length = ins.read(buffer)) > 0){
					outs.write(buffer,0,length)
				}
				return true
			}catch(Exception ex){
				ex.printStackTrace();
				return false
			}finally{
				ins.close();
				outs.close();
			}
		}

	}
}