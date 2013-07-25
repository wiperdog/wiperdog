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
// job loader

import com.insight_tec.pi.directorywatcher.Listener
import org.osgi.framework.ServiceReference
import org.osgi.util.tracker.ServiceTracker
import org.osgi.util.tracker.ServiceTrackerCustomizer
import com.insight_tec.pi.jobmanager.JobFacade
import org.apache.log4j.Logger
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import com.insight_tec.pi.event.*

/**
 * job 専用loader
 * (未実装）
 * job の記述が簡潔になるよう、いろいろコチラ側で処理してあげる。
 *  IST_HOME/var/jobs/*.job を監視してロードする。
 */
class JobLoader implements Listener, ServiceTrackerCustomizer {
	def shell
	def dir
	def interval
	def consumner
	def context
	def trackerObj
	def jobdsl
	def jobfacade
	def properties
	
	String isManager = System.getProperty("pi.role.manager")
	String isAgent = System.getProperty("pi.role.agent")

	def repoTableIns
	def resourceNameInformation = [:]
	def resourcePropertyInformation = []
	def SYS_CDFInformation = [:]
	def prevJobDetailList = []

	public JobLoader(BundleContext ctx, GroovyShell shell) {
		this.shell = shell
		this.context = ctx
		MonitorJobConfigLoader configLoader = new MonitorJobConfigLoader(context)
		properties = configLoader.getProperties();
		dir = properties.get(ResourceConstants.JOB_DIRECTORY)
		interval = 5000
		trackerObj = new ServiceTracker(context, JobFacade.class.getName(), this)
		trackerObj.open()
		if("true".equals(isManager)){
			def RepoTable = shell.getClassLoader().loadClass("RepoTable")
			repoTableIns = RepoTable.newInstance(context, shell, prevJobDetailList)
		}
	}

	public boolean filterFile(File file) {
		return file.getName().endsWith(".job") || file.getName().endsWith(".cls") || file.getName().endsWith(".trg") || file.getName().endsWith(".instances");
	}

	public String getDirectory() {
		return dir;
	}

	public long getInterval() {
		return interval;
	}

	public boolean notifyAdded(File target) throws IOException {
		if("true".equals(isManager)){
			return processFile(target);
		}
		if (jobfacade == null) {
			// falseを返せばこのファイルは未処理としてマークされる。
			return false
		}
		return processFile(target);
	}

	public boolean notifyDeleted(File target) throws IOException {
		return false;
	}

	public boolean notifyModified(File target) throws IOException {
		if (jobfacade == null) {
			// falseを返せばこのファイルは未処理としてマークされる。
			return false
		}
		return processFile(target);
	}

	//////
	private boolean processCls(File target) {
		//
		// use custommized Reader, then compile it
		// shell.parse(reader, pseudofilename)
		//
		return false;
	}
	private boolean processJob(File target) {
		def cPolicy = null
		def listEvent = null
		def objectPolicy
		def binding = shell.getContext()
		try {
			shell.evaluate(target)
		} catch (MissingPropertyException e) {
			//If missing parameter properties, continue
		} catch (Exception ex) {
			return false
		}
		// Get closure policy from job
		if (binding.hasVariable(ResourceConstants.DEF_POLICY)) {
			cPolicy = binding.getVariable(ResourceConstants.DEF_POLICY)
		}

		// Get list event from job
		if(binding.hasVariable(ResourceConstants.DEF_ACCEPTS)) {	
			listEvent = binding.getVariable(ResourceConstants.DEF_ACCEPTS)
		}
		
		// Gennerate object Policy
		if(cPolicy != null && listEvent != null) {
				
				def ClosureEvaluatableWrapperImpl = shell.getClassLoader().loadClass("ClosureEvaluatableWrapperImpl")
				objectPolicy = ClosureEvaluatableWrapperImpl.newInstance(cPolicy,listEvent)
		} else {
			return false
		}

		def LoadPolicyScript = shell.getClassLoader().loadClass("LoadPolicyScript")
		def loadPolicy = LoadPolicyScript.newInstance(objectPolicy)
		context.registerService("com.insight_tec.pi.event.EvaluatableProvider", loadPolicy, null)

		// -- Begin auto create table process --
		def isCreate = false
		def RESOURCEID = null
		def REPOSITORYDEF = null
		def CHARTDEF = null
		
		if(repoTableIns.repoAdmin == null){
			def RepoTable = shell.getClassLoader().loadClass("RepoTable")
			repoTableIns = RepoTable.newInstance(context, shell, prevJobDetailList)
		}
		if(repoTableIns.repoAdmin != null){
		if(binding.hasVariable(ResourceConstants.RESOURCEID)) {
			RESOURCEID = binding.getVariable(ResourceConstants.RESOURCEID)
		}
		if(binding.hasVariable(ResourceConstants.REPOSITORYDEF)) {
			REPOSITORYDEF = binding.getVariable(ResourceConstants.REPOSITORYDEF)
		}
		if(binding.hasVariable(ResourceConstants.CHARTDEF)) {
			CHARTDEF = binding.getVariable(ResourceConstants.CHARTDEF)
		}
		// check update job and update list prevJobDetailList
		def jobDetail = [:]
		jobDetail["JobName"] = binding.getVariable(ResourceConstants.DEF_JOB)[ResourceConstants.DEF_JOB_NAME]
		jobDetail[ResourceConstants.REPOSITORYDEF] = REPOSITORYDEF
		jobDetail[ResourceConstants.CHARTDEF] = CHARTDEF
		def isUpdateRepo = repoTableIns.checkUpdateRepositoryDef(prevJobDetailList,jobDetail)
		def isUpdateChart = repoTableIns.checkUpdateChartDef(prevJobDetailList,jobDetail)
		def isNew = repoTableIns.isFirstLoad(target.getName())
		repoTableIns.updateListPrevJob(prevJobDetailList, jobDetail)

		//get information and insert data to table ResourceName and ResourceProperty
		if (isNew || isUpdateRepo || isUpdateChart) {
			if (RESOURCEID != null && RESOURCEID != '' && REPOSITORYDEF != null && isUpdateRepo) {
				def prevShortRSN = repoTableIns.getShortResourceNameFromDB(RESOURCEID)
				def prevDataId = repoTableIns.getDataID(RESOURCEID)
				def resource = [:]
				resource["DATA_ID"] = prevDataId
				resource["RESOURCE_NAME"] = RESOURCEID
				//delete old data
				repoTableIns.deleteResource("ResourceName", resource)
				repoTableIns.deleteResource("ResourceProperty", resource)
				repoTableIns.dopTable(prevShortRSN)
				//insert new data
				resourceNameInformation = repoTableIns.getResourceNameInformation(REPOSITORYDEF,RESOURCEID)
				resourcePropertyInformation = repoTableIns.getResourcePropInformation(REPOSITORYDEF,RESOURCEID, resourceNameInformation)
				isCreate = repoTableIns.insertData("ResourceName", resourceNameInformation)
				resourcePropertyInformation.each{
					isCreate = repoTableIns.insertData("ResourceProperty", it)
				}
				//update dataid for SYS_CDF
				repoTableIns.updateDataID(resourceNameInformation["DATA_ID"], RESOURCEID)
			}
			
			if (RESOURCEID != null && RESOURCEID != '' && CHARTDEF != null && isUpdateChart) {
				def SYS_CDFInfor = repoTableIns.getSYS_CDFFromDB(RESOURCEID) 
				repoTableIns.deleteResource("SYS_CDF", SYS_CDFInfor)
				//insert new data
				SYS_CDFInformation = repoTableIns.getSYSCDFInformation(CHARTDEF,RESOURCEID)
				repoTableIns.insertData("SYS_CDF", SYS_CDFInformation)
			}

			// insert data to ResourceName or ResourceProperty
			if (isNew && RESOURCEID != null && RESOURCEID != '' && REPOSITORYDEF != null) {
				resourceNameInformation = repoTableIns.getResourceNameInformation(REPOSITORYDEF,RESOURCEID)
				resourcePropertyInformation = repoTableIns.getResourcePropInformation(REPOSITORYDEF,RESOURCEID, resourceNameInformation)
				isCreate = repoTableIns.insertData("ResourceName", resourceNameInformation)
				resourcePropertyInformation.each{
					isCreate = repoTableIns.insertData("ResourceProperty", it)
				}

				//insert data to SYSDEF
				if (CHARTDEF != null && CHARTDEF != '') {
					SYS_CDFInformation = repoTableIns.getSYSCDFInformation(CHARTDEF,RESOURCEID)
					repoTableIns.insertData("SYS_CDF", SYS_CDFInformation)
				}
			}		
		}
		//if insert data successfull then create table
		if (isCreate) {
			def resourceMap = [:]
			resourceMap["DATA_ID"] = resourceNameInformation["DATA_ID"].toString()
			resourceMap["SHORT_RESOURCE_NAME"] = resourceNameInformation["SHORT_RESOURCE_NAME"]
			resourceMap["DATA_VER"] = resourceNameInformation["DATA_VER"].toString()
			try {
				repoTableIns.database.createTableForJob(resourceMap)
				println "-------Create table successful--------"
			}catch(Exception e){
				repoTableIns.logger.error(e.toString())
				return false
			}
		}
		// write prev job to file
		repoTableIns.writePrevJob(target)
		}else{
			println "Rerun " + target.getName() + " to load RepoAdmin"
		}
		// -- End auto create table --
		
		return true;
	}
	private boolean processSchedule(File target) {
		//
		// use custommized Reader, then compile it
		// shell.parse(reader, pseudofilename)
		//
		return false;
	}

	private boolean processFile(File target) {
		if("true".equals(isManager)){
			if (target.getName().endsWith(".job")) {
				return this.processJob(target)
			}
		}else{
			if (jobdsl != null) {
				if (target.getName().endsWith(".job")) {
					return jobdsl.processJob(target)
				} else if (target.getName().endsWith(".cls")) {
					return jobdsl.processCls(target)
				} else if (target.getName().endsWith(".trg")) {
					return jobdsl.processTrigger(target)
				} else if (target.getName().endsWith(".instances")) {
					return jobdsl.processInstances(target)
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	/**
	 * ServiceTrackerCustomizer.addingService
	 * 以下は、ServiceTrackerCustomizerの実装部
	 *  (http://www.osgi.org/javadoc/r4v42/org/osgi/util/tracker/ServiceTrackerCustomizer.html)
	 */
	public Object addingService(ServiceReference reference) {
		def oservice = context.getService(reference);
		if (oservice instanceof JobFacade) {
			jobfacade = oservice
			// prepare jobdsl object here.
			if("false".equals(isManager)){
				//this.class.getClassLoader().addClasspath(System.getProperty("felix.home") + '/' + "lib/groovy/libs.target/JobDsl.groovy")
				def JobDsl = shell.getClassLoader().loadClass("JobDsl")
				jobdsl = JobDsl.newInstance(shell, jobfacade, context)
			}
		}
		return oservice
	}

	/**
	 * ServiceTrackerCustormizer.modifiedService
	 */
	public void modifiedService(ServiceReference reference, Object service) {
	}

	/**
	 * ServiceTrackerCustomizer.removedService
	 */
	public void removedService(ServiceReference reference, Object service)  {
		if (service == jobfacade) {
			jobfacade = null
			jobdsl = null
		}
	}
}


