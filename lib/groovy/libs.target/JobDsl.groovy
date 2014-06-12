// JobDsl
import org.wiperdog.jobmanager.JobFacade
import org.wiperdog.jobmanager.JobClass
import java.io.File
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext

class JobDsl implements JobDSLService {
	def shell
	def jobfacade
	def loader
	def lstTriggerWaitJob = []  //list trigger waiting for reading job
	def lstJobWaitJobClass = [] // list job waiting for reading job class
	def lstTriggerWaitAll = []  // list trigger waiting because job waiting for job class
	def bundleContext
	def mapInstancesWaitJob = [:] // map [jobName:<list instances>] instances waiting for reading job
	def mapJobDefaultSchedule = [:] // map [jobName:<schedule in trigger file>] schedule can be used for jobInstances
	// Define a map to store <jobName:listInstances >
	def mapJobListInstances = [:]
	// Define a map to store <jobName: listJob>
	def mapJobJobFile = [:] // map [jobName: <jobfile>], jobfile: directory of job file
	def defaultSchedule // Store schedule in trigger file. It can be used in processInstances
	//Store list schedule corresponding to trigger file
	def mapTrgFileListTrgs = [:]
	//Store list instance corresponding to instances file
	def mapInstFileListInsts = [:]
	def mapJCFileListJC = [:]
	def mapJobInCls = [:]
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	DefaultSender sender = new DefaultSender()
	
	private long toMilliSec(sec) {
		return sec * 1000
	}

	private JobClass getJobClass(name) {
		if (name == null) {
			return null;
		}
		def jc = jobfacade.getJobClass(name)
		/*
		if (jc == null) {
			jc = jobfacade.createJobClass(name)
		}*/
		return jc
	}

	public JobDsl(shell, JobFacade jobfacade, context) {
		this.shell = shell
		this.jobfacade = jobfacade
		this.loader = shell.getClassLoader()
		this.bundleContext = context
	}

	public boolean processJob(File jobfile) {
		removeJob(jobfile)
		try {
			loader.clearCache()
			def clsJob = loader.parseClass(jobfile)
			def scheduledJob = new GroovyScheduledJob(jobfile.absolutePath, clsJob, sender)
			def jobName = scheduledJob.getJobName()
			def jobClassName = scheduledJob.getJobClassName()
			jobfacade.createJob(scheduledJob)
			// Store jobfile to global
			mapJobJobFile[jobName] = jobfile
			// If have instances file waiting job then process that instances file
			try {
				File fileInstancesWaitJob = mapInstancesWaitJob[jobName]
				if(fileInstancesWaitJob != null) {
					processInstances(fileInstancesWaitJob)
				}
			} catch(Exception ex) {
				println "error file: " + ex
			}

			def lstRemoveTrigger = []
			
			// Check if job belongs to a jobClass or not 
			if (jobClassName != null && jobClassName != "") {
				if(mapJobInCls[jobClassName] == null ){
					mapJobInCls[jobClassName] = []
				}
				mapJobInCls[jobClassName].add(jobName)
				def jc = getJobClass(jobClassName)
				if (jc == null) {
					//if job waiting jobclass then add to list lstJobWaitJobClass
					def jobWaitClass = [:]
					jobWaitClass["jobClass"] = jobClassName
					jobWaitClass["jobName"] = jobName
					lstJobWaitJobClass.add(jobWaitClass)
					//if job waiting for reading jobclass, add trigger, job, jobclass to list lstTriggerWaitAll
					lstTriggerWaitJob.each {
						if (it["jobName"] == jobName) {
							def mapTriggerWaitAll = [:]
							mapTriggerWaitAll["trigger"] = it["trigger"]
							mapTriggerWaitAll["jobName"] = jobName
							mapTriggerWaitAll["jobClass"] = jobClassName
							lstTriggerWaitAll.add(mapTriggerWaitAll)
						}
					}	
				} else {
					//if jobclass has read then create schedule for for trigger that waiting for job
					jc.addJob(jobfacade.jobKeyForName(scheduledJob.getJobName()))
					lstTriggerWaitJob.each {
						if (it["jobName"] == jobName) {
							jobfacade.scheduleJob(jobfacade.getJob(jobName), it["trigger"])
							lstRemoveTrigger.add(it)
						}
					}
					lstTriggerWaitJob.removeAll(lstRemoveTrigger)
				}
			} else { // if job hasn't got jobclass then create schedule for trigger that waiting for job
				lstTriggerWaitJob.each {
					if (it["jobName"] == jobName) {
						jobfacade.scheduleJob(jobfacade.getJob(jobName), it["trigger"])
						lstRemoveTrigger.add(it)
					}
				}
				lstTriggerWaitJob.removeAll(lstRemoveTrigger)
				lstRemoveTrigger = []
			    lstTriggerWaitAll.each {
					if (it["jobName"] == jobName) {
			      		jobfacade.scheduleJob(jobfacade.getJob(jobName), it["trigger"])
			      		lstRemoveTrigger.add(it)
			     	}
			    }
			    lstTriggerWaitAll.removeAll(lstRemoveTrigger)
			    
			    lstRemoveTrigger = []
			    lstJobWaitJobClass.each {
					if (it["jobName"] == jobName) {
			      		lstRemoveTrigger.add(it)
			     	}
			    }
			    lstJobWaitJobClass.removeAll(lstRemoveTrigger)
			}
		} catch (NullPointerException e) {
			logger.info("[" + jobfile.getName() + "]: " + "[" + e.toString() + "]")
		} catch(IllegalArgumentException e) {
			logger.info("[" + jobfile.getName() + "]: " + "[" + e.toString() + "]")
		} catch(MissingPropertyException e) {
			logger.info("[" + jobfile.getName() + "]: " + "[" + e.toString() + "]")
		} catch(MultipleCompilationErrorsException e) {
			logger.info("[" + jobfile.getName() + "]: " + "[" + e.toString() + "]")
		} catch(Exception e) {
			logger.debug("[" + jobfile.getName() + "]: " + "[" + e.toString() + "]")
		}
		return true
	}

	/**
	 * Process create jobClass and add defined jobs to class.
	 * Return true to stop notifying .cls file
	 * @param clsfile cls file
	 * @return true
	 */
	public boolean processCls(File clsfile) {
		removeJobCls(clsfile)
		def clsFileName = clsfile.getName()
		mapJCFileListJC[clsFileName] = []
		try{
			clsfile.eachLine { aline -> 
				def clsdef = shell.evaluate( "[" + aline + "]")	
				def clsname = clsdef[ResourceConstants.DEF_CLS_NAME]
				def concurrency = clsdef[ResourceConstants.DEF_CLS_CONCURRENCY]
				def maxrun = clsdef[ResourceConstants.DEF_CLS_MAXRUN]
				def maxwait = clsdef[ResourceConstants.DEF_CLS_MAXWAIT]
				if (clsname != null && clsname != "") {
					mapJCFileListJC[clsFileName].add(clsname)
					def jobcls = getJobClass(clsname)
					def lstRemoveJobClass = []
					def lstRemoveTrigger = []
					if (jobcls == null) {
						jobcls = jobfacade.createJobClass(clsname)
					}
					if (jobcls != null) {
						if (concurrency != null) {
							jobcls.setConcurrency(concurrency)
						}
						if (maxrun != null) {
							jobcls.setMaxRunTime(toMilliSec(maxrun))
						}
						if (maxwait != null) {
							jobcls.setMaxWaitTime(toMilliSec(maxwait))
						}
					}
					//add job into jobclass
					lstJobWaitJobClass.each {
						if (it["jobClass"] == clsname) {
							jobcls.addJob(jobfacade.jobKeyForName(it["jobName"]))
							lstRemoveJobClass.add(it)
						}
					}
					lstJobWaitJobClass.removeAll(lstRemoveJobClass)
					
					//create schedule for trigger that waiting for job beacause job waiting for reading jobclass
					lstTriggerWaitAll.each {
						if (it["jobClass"] == clsname) {
							jobfacade.scheduleJob(jobfacade.getJob(it["jobName"]), it["trigger"])
							lstRemoveTrigger.add(it)
						}
					}
					lstTriggerWaitAll.removeAll(lstRemoveTrigger)
				} else {
					logger.info("[" + clsfile.getName() + "]: FILE CONTENT IS INCORRECT !!!")
				}
			}
		} catch(IllegalArgumentException e) {
			logger.info("[" + clsfile.getName() + "]: " + "[" + e.toString() + "]")
		} catch(MultipleCompilationErrorsException e) {
			logger.info("[" + clsfile.getName() + "]: " + "[" + e.toString() + "]")
		} catch(Exception e) {
			logger.debug("[" + clsfile.getName() + "]: " + "[" + e.toString() + "]")
		}
		return true;
	}

	/**
	 * Process file trg to create and schedule trigger jobs. Create default schedule for instances.
	 * Return true to stop notify trg file
	 * @param trgfile trg file
	 * @return true
	 */
	public boolean processTrigger(File trgfile) {
		removeTrigger(trgfile)
		def trgFileName = trgfile.getName()
		mapTrgFileListTrgs[trgFileName] = []
		try {
			trgfile.eachLine { aline -> 
				def trg = shell.evaluate( "[" + aline + "]")	
				if (trg != null) {
					def jobname = trg[ResourceConstants.DEF_TRIGGER_JOB]
					mapTrgFileListTrgs[trgFileName].add(jobname)
					defaultSchedule = trg[ResourceConstants.DEF_TRIGGER_SCHEDULE]
						mapJobDefaultSchedule[jobname] = defaultSchedule
						try {
							if (jobname != null && jobname != "" && defaultSchedule != null && defaultSchedule != "") {
								//def job = jobfacade.getJob(jobname)
								/*
								if (job == null) {
									// specified job is not presented yet or name of job is mis-spelled.
									// returning "false" will results this trigger be re-consulted after.
									return false
								}*/
								// Create trigger and job original
								def trigger_ori = processCreateTrigger(defaultSchedule , jobname)
								def job_ori = jobfacade.getJob(jobname)
								// Create trigger and schedule for instances job which doesn't had schedule parameter
								try {
									def listInstNotSchedule = mapJobListInstances[jobname]
									if(listInstNotSchedule != null && listInstNotSchedule != []) {
										listInstNotSchedule.each {element_inst_not_schedule ->
											if(element_inst_not_schedule.schedule == null) {
												def triggerInstNotSchedule = processCreateTrigger(defaultSchedule , jobname + "_" + element_inst_not_schedule.instancesName)
												def jobInstNotSchedule = jobfacade.getJob(jobname + "_" + element_inst_not_schedule.instancesName)
												if(jobInstNotSchedule != null) {
													def isWait = false
													lstJobWaitJobClass.each {
														if (jobname + "_" + element_inst_not_schedule.instancesName == it["jobName"]) {
															def mapTriggerWaitAll = [:]
															mapTriggerWaitAll["trigger"] = triggerInstNotSchedule
															mapTriggerWaitAll["jobName"] = jobname + "_" + element_inst_not_schedule.instancesName
															mapTriggerWaitAll["jobClass"] = it["jobClass"]
															lstTriggerWaitAll.add(mapTriggerWaitAll)
															isWait = true
														}
													}
													// if job hasn't to waiting for reading jobclass then create schedule 
													if (!isWait) {
														// Create schedule for instances job
														jobfacade.scheduleJob(jobInstNotSchedule, triggerInstNotSchedule)
													}
												}
											}
										}
									}
								} catch(Exception ex) {
									println "JobInstances doesn't had schedule: " + ex
								}

								if (trigger_ori != null && defaultSchedule != 'delete') {
									if (job_ori == null) {
										//if job hasn't read then add trigger to list lstTriggerWaitJob
										def mapTriggerWaitJob = [:]
										mapTriggerWaitJob["trigger"] = trigger_ori
										mapTriggerWaitJob["jobName"] = jobname
										lstTriggerWaitJob.add(mapTriggerWaitJob)
									} else {
										//if job had read but job waiting for reading jobclass then add trigger, job, jobclass to list lstTriggerWaitAll
										def isWait = false
										lstJobWaitJobClass.each {
											if (jobname == it["jobName"]) {
												def mapTriggerWaitAll = [:]
												mapTriggerWaitAll["trigger"] = trigger_ori
												mapTriggerWaitAll["jobName"] = jobname
												mapTriggerWaitAll["jobClass"] = it["jobClass"]
												lstTriggerWaitAll.add(mapTriggerWaitAll)
												isWait = true
											}
										}
										// if job hasn't to waiting for reading jobclass then create schedule 
										if (!isWait) {
											// Create schedule for original job
											jobfacade.scheduleJob(job_ori, trigger_ori)
										}
									}
								}
							} else {
								logger.info("[" + trgfile.getName() + "]: FILE CONTENT IS INCORRECT !!!")
							}
						} catch (Exception ex) {
							logger.info("[" + trgfile.getName() + "]: " + "[" + ex.toString() + "]")
						}
				}
			}
		} catch(MultipleCompilationErrorsException e) {
			logger.info("[" + trgfile.getName() + "]: " + "[" + e.toString() + "]")
		} catch(Exception e) {
			logger.debug("[" + trgfile.getName() + "]: " + "[" + e.toString() + "]")
		}
		return true;
	}

	/**
	 * Process instances file and create schedue for instances job.
	 * Return true to stop notify file .instances
	 * @param instfile Instances file
	 * @return true 
	 */
	public boolean processInstances(File instfile) {
		removeInstances(instfile)
		def instfilename = instfile.getName()
		mapInstFileListInsts[instfilename] = []
		try {
			loader.clearCache()
			def listInstances = []
			def lstRemoveTrigger = []
			
			// Evaluate file .instances
			def instEval = shell.evaluate(instfile)
			def jobName = instfile.getName().substring(0, instfile.getName().indexOf(".instances"))
			/*def jobFileName = jobName + ".job"
			def jobfile = new File(System.getProperty("felix.home") + "/var/job/" + jobFileName)*/
			def jobfile = mapJobJobFile[jobName]
			def textParsed = ""
			
			// Process to get list instances of job
			instEval.each {
				def mapInstances = [:]
				def instancesName
				def schedule
				def parmas
				mapInstances['instancesName'] = it.key
				mapInstances['schedule'] = it.value.schedule
				mapInstances['params'] = it.value.params
				listInstances.add(mapInstances)
			}

			// Add list instances to map mapJobListInstances
			mapJobListInstances[jobName] = listInstances
			
			// If .job file is loaded then create text from .job file to parse class later
			// Else add .instances file to map waiting job and return to stop notify .instances file 
			if(jobfile != null) {
				jobfile.eachLine { line_job ->
					textParsed += line_job + "\n"
				}
			} else {
				mapInstancesWaitJob[jobName] = instfile
				return true
			}
			
			// create instances of jobs
			listInstances.each {element_listinst ->
				def triggerInstances
				def isNotSchedule = true
				def job_inst = jobName + "_" + element_listinst.instancesName
				def tmpTextParesd = textParsed.trim()
				mapInstFileListInsts[instfilename].add(job_inst)
				// tmpTextParesd != null means .job file is loaded
				if(tmpTextParesd != null) {
					// Parse class and create job instances
					if(tmpTextParesd.contains(jobName)) {
						tmpTextParesd = tmpTextParesd.replace(jobName,job_inst)
					}
					def clsJob = loader.parseClass(tmpTextParesd, job_inst)
					def scheduledJob = new GroovyScheduledJob(jobfile.absolutePath, clsJob, element_listinst.params, jobName, element_listinst.instancesName, sender)
					try {
						jobfacade.createJob(scheduledJob)

					} catch (Exception e) {
						println "Error createJob: " + e
					}
					
					// Check if job instances is belongs to a jobClass or not
					def className = scheduledJob.getJobClassName()
					if(className != null && className != "") {
						if(mapJobInCls[className] == null) {
							mapJobInCls[className] = []
						}
						mapJobInCls[className].add(job_inst)
						def jc = getJobClass(className)							
						// jobClass has not been loaded yet
						if(jc == null) {
							//if job waiting jobclass then add to list lstJobWaitJobClass
							def jobWaitClass = [:]
							jobWaitClass["jobClass"] = className
							jobWaitClass["jobName"] = job_inst
							lstJobWaitJobClass.add(jobWaitClass)
							
							//Check schedule, if has data, add to list trg wait job
							if(element_listinst.schedule != null) {
								triggerInstances = processCreateTrigger(element_listinst.schedule, job_inst)

							} else {
								mapJobDefaultSchedule.each {
									if(it.key == jobName){
										try{
											def jobInstancesDefaultSchedule = jobfacade.getJob(job_inst)
											triggerInstances = processCreateTrigger(it.value, job_inst)
										}catch(Exception ex){
											pritnln "ex:" + ex
										}
									}
								}
							}
							if (triggerInstances != null) {
								def mapTriggerWaitJob = [:]
								mapTriggerWaitJob["trigger"] = triggerInstances
								mapTriggerWaitJob["jobName"] = job_inst
								lstTriggerWaitJob.add(mapTriggerWaitJob)
								
								//Because job waiting for reading jobclass, add trigger, job, jobclass to list lstTriggerWaitAll
								lstTriggerWaitJob.each {
									if (it["jobName"] == job_inst) {
										def mapTriggerWaitAll = [:]
										mapTriggerWaitAll["trigger"] = it["trigger"]
										mapTriggerWaitAll["jobName"] = job_inst
										mapTriggerWaitAll["jobClass"] = className
										lstTriggerWaitAll.add(mapTriggerWaitAll)
									}
								}
							}
						} else {
							jc.addJob(jobfacade.jobKeyForName(scheduledJob.getJobName()))
							// Execute job by instances schedule
							if(element_listinst.schedule != null) {
								triggerInstances = processCreateTrigger(element_listinst.schedule, job_inst)
								def jobInstances = jobfacade.getJob(job_inst)
								jobfacade.scheduleJob(jobInstances, triggerInstances)
								
								//If has in lstTriggerWaitJob, remove it
								lstRemoveTrigger = []
								lstTriggerWaitJob.each {
									if (it["jobName"] == job_inst) {
										lstRemoveTrigger.add(it)
									}
								}
								lstTriggerWaitJob.removeAll(lstRemoveTrigger)
							} else {
								mapJobDefaultSchedule.each {
									if(it.key == jobName){
										try{
											def jobInstancesDefaultSchedule = jobfacade.getJob(job_inst)
											def triggerInstancesDefault = processCreateTrigger(it.value, job_inst)
											jobfacade.scheduleJob(jobInstancesDefaultSchedule, triggerInstancesDefault)
											
											//If has in lstTriggerWaitJob, remove it
											lstRemoveTrigger = []
											lstTriggerWaitJob.each {
												if (it["jobName"] == job_inst) {
													lstRemoveTrigger.add(it)
												}
											}
											lstTriggerWaitJob.removeAll(lstRemoveTrigger)
										}catch(Exception ex){
											println "ex:" + ex
										}
									}
								}
							}
						}
					} else {
						if(element_listinst.schedule != null) {
							triggerInstances = processCreateTrigger(element_listinst.schedule, job_inst)
							def jobInstances = jobfacade.getJob(job_inst)
							jobfacade.scheduleJob(jobInstances, triggerInstances)
							isNotSchedule = false
							
							//If has in lstTriggerWaitJob, remove it
							lstRemoveTrigger = []
							lstTriggerWaitJob.each {
								if (it["jobName"] == job_inst) {
									lstRemoveTrigger.add(it)
								}
							}
							lstTriggerWaitJob.removeAll(lstRemoveTrigger)
						} else {
							mapJobDefaultSchedule.each {
								if(it.key == jobName){
									try{
										def jobInstancesDefaultSchedule = jobfacade.getJob(job_inst)
										def triggerInstancesDefault = processCreateTrigger(it.value, job_inst)
										jobfacade.scheduleJob(jobInstancesDefaultSchedule, triggerInstancesDefault)
										isNotSchedule = false
										
										//If has in lstTriggerWaitJob, remove it
										lstRemoveTrigger = []
										lstTriggerWaitJob.each {
											if (it["jobName"] == job_inst) {
												lstRemoveTrigger.add(it)
											}
										}
										lstTriggerWaitJob.removeAll(lstRemoveTrigger)
									}catch(Exception ex){
										println "ex:" + ex
									}
								}
							}
						}
						
					    if (isNotSchedule) {
						    lstTriggerWaitJob.each {
								if (it["jobName"] == jobName) {
									jobfacade.scheduleJob(jobfacade.getJob(jobName), it["trigger"])
									lstRemoveTrigger.add(it)
								}
							}
							lstTriggerWaitJob.removeAll(lstRemoveTrigger)
							
							lstRemoveTrigger = []
						    lstTriggerWaitAll.each {
								if (it["jobName"] == jobName) {
						      		jobfacade.scheduleJob(jobfacade.getJob(jobName), it["trigger"])
						      		lstRemoveTrigger.add(it)
						     	}
						    }
						    lstTriggerWaitAll.removeAll(lstRemoveTrigger)
					    }
					    
					    lstRemoveTrigger = []
					    lstJobWaitJobClass.each {
							if (it["jobName"] == jobName) {
					      		lstRemoveTrigger.add(it)
					     	}
					    }
					    lstJobWaitJobClass.removeAll(lstRemoveTrigger)
					}
				}
			}
		} catch (Exception ex) {
			logger.info("[" + instfile.getName() + "]: " + "[" + ex.toString() + "]")
		}
		return true;
	}

	/**
	* ProcessCreateTrigger: create o trigger for corresponding schedule and jobname
	* schedule: Schedule need to set for job
	* jobname: Name of job
	*/
	public Trigger processCreateTrigger(String schedule, String jobname) {
		def trigger
		if (schedule ==~ /[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+/ ||
			schedule ==~ /[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+/ ) {
			// create trigger.
			// created trigger will be registered automatically.
			trigger = jobfacade.createTrigger(jobname, schedule)
		} else if (schedule == "now" || schedule == "NOW") {
			// create trigger.
			// created trigger will be registered automatically.
			trigger = jobfacade.createTrigger(jobname, 0)
		} else if (schedule.endsWith('i')) {
			long interval = Long.parseLong(schedule.substring(0,schedule.lastIndexOf('i')))*1000
			trigger = jobfacade.createTrigger(jobname, 0, interval)
		}else if (schedule == 'delete'){
			trigger = jobfacade.getTrigger(jobname)
			if(trigger != null){
				jobfacade.unscheduleJob(trigger)
			}
		}else {
			long delay = Long.parseLong(schedule)
			trigger = jobfacade.createTrigger(jobname, delay)
		}
		return trigger
	}

	/**
	 * Process remove data from scheduler when delete job file.
	 * Return true to stop notify file .job
	 * @param jobFile Job file
	 * @return true
	 */
	public boolean removeJob(File jobFile) {
		def jobNameRemove
		mapJobJobFile.each{ key,value->
			if(value.equals(jobFile)) {
				def jobDetail = jobfacade.getJob(key)
				//Add trigger to lstTriggerWaitJob
				if(jobfacade.getTrigger(key) != null ) {
					def mapTriggerWaitJob = [:]
					mapTriggerWaitJob["trigger"] = jobfacade.getTrigger(key)
					mapTriggerWaitJob["jobName"] = key
					lstTriggerWaitJob.add(mapTriggerWaitJob)
				}
				// Add instance file (if available to mapInstWaitJob)
				MonitorJobConfigLoader configLoader = new MonitorJobConfigLoader(this.bundleContext)
				def properties = configLoader.getProperties()
				def inst_dir = new File(properties.get(ResourceConstants.JOBINST_DIRECTORY))
				def instFile = inst_dir.listFiles()
				instFile.each{
					if(it.getName().endsWith(".instances") && it.getName().equals( key + ".instances")) {
						mapInstancesWaitJob[key] = it
						mapInstFileListInsts[it.getName()].each{ inst_name->
						//Unschedule instance of job and add trigger to lstTriggerWaitJob
						def trigger = jobfacade.getTrigger(inst_name)
						if(trigger != null ) {
							def mapTriggerWaitJob = [:]
							mapTriggerWaitJob["trigger"] = trigger
							mapTriggerWaitJob["jobName"] = inst_name
							lstTriggerWaitJob.add(mapTriggerWaitJob)
							jobfacade.unscheduleJob(trigger)
						}
					}
				}

				}
				Iterator it = lstJobWaitJobClass.iterator()
				while(it.hasNext()){
					def jobWaitClass = it.next()
					if(jobWaitClass["jobName"].equals(key)){
						it.remove()
					}
				}
				if(jobDetail != null) {
					jobfacade.removeJob(jobDetail)
					jobNameRemove = key
				}
			}

		}
		if(jobNameRemove != null) {
			mapJobJobFile.remove(jobNameRemove)
		}

		Iterator iter = lstJobWaitJobClass.iterator()
		while(iter.hasNext()) {
			def object = iter.next()
			if(object["jobName"].equals(jobNameRemove)){
				 iter.remove()
			 }
		}

		//Remove from mapJobDefaultSchedule
		def keys = mapJobDefaultSchedule.keySet();
		def removeKey = null
		keys.each { key->
			if(key.equals(jobNameRemove)){
		  		removeKey = key
			}
		}	
		mapJobDefaultSchedule.remove(removeKey)

		//Remove from mapJobListInstances
		keys = mapJobListInstances.keySet();
		keys.each { key->
			if(key.equals(jobNameRemove)){
		  		removeKey = key
			}
		}	
		mapJobListInstances.remove(removeKey)

		return true
	}

	/**
	 * Process remove data from scheduler when delete instances file.
	 * Return true to stop notify file .instances
	 * @param instfile Instances file
	 * @return true
	 */
	public boolean removeInstances(File instfile) {
		def instfilename = instfile.getName()
		//Loop each instance declare in instfile
		mapInstFileListInsts[instfilename].each{instName ->
			def jobDetail = jobfacade.getJob(instName)
			if(jobDetail != null){
				//Remove job from scheduler
				jobfacade.removeJob(jobDetail)
			}
		}


		//Remove from mapInstancesWaitJob
		def keys = mapInstancesWaitJob.keySet();
		def removeKey = null
		keys.each { key->
			if(mapInstancesWaitJob[key].equals(instfile)){
		  		removeKey = key
			}
		}	
		mapInstancesWaitJob.remove(removeKey)


		//Remove from mapJobListInstances
		def jobName = instfilename.substring(0,instfilename.lastIndexOf(".instances"))
		keys = mapJobListInstances.keySet();
		keys.each { key->
			if(key.equals(jobName)){
		  		removeKey = key
			}
		}	
		mapJobListInstances.remove(removeKey)

		//Remove from mapInstFileListInsts
		mapInstFileListInsts.remove(instfilename)
		return true
	}

	/**
	 * Process remove data from scheduler when delete trigger file.
	 * Return true to stop notify file .trg
	 * @param trgFile Trigger file
	 * @return true
	 */
	public boolean removeTrigger(File trgFile) {
		Iterator iter = mapTrgFileListTrgs.keySet().iterator();
		//Loop through trigger in trgFile
		while(iter.hasNext()) {
			String trgFileName = iter.next();
			if(trgFile.getName().equals(trgFileName)) {
				mapTrgFileListTrgs[trgFileName].each{ jobname ->

					    //Remove trigger from lists wait
					   Iterator iter2 = lstTriggerWaitAll.iterator()
						while(iter2.hasNext()){
							def trgWaitAll = iter2.next()
							if(trgWaitAll["jobName"].equals(jobname)) {
								iter2.remove()
							}
						}
						iter2 = lstTriggerWaitJob.iterator()
						while(iter2.hasNext()){
							def trgWaitJob = iter2.next()
							if(trgWaitJob["jobName"].equals(jobname)) {
								iter2.remove()
							}
						}
					//Unscheduled job with trigger
					def trigger = jobfacade.getTrigger(jobname)
					if(trigger != null ) {
						jobfacade.unscheduleJob(trigger)
					}
				}
			}
		}
		//Remove from mapTrgFileListTrgs
		mapTrgFileListTrgs.remove(trgFile.getName())
		return true
	}

	/**
	 * Process remove data from scheduler when delete jobclass file.
	 * Return true to stop notify file .cls
	 * @param jobcls job class file
	 * @return true
	 */
	public boolean removeJobCls(File jobcls) {

		Iterator iter = mapJCFileListJC.keySet().iterator();
		//Loop each class in class file
		while(iter.hasNext()) {
			String clsFileName = iter.next();
			if(jobcls.getName().equals(clsFileName)) {
				mapJCFileListJC[clsFileName].each{ clsName ->
					//Unschedule job in class before remove class
					mapJobInCls[clsName].each{ jobName ->
						//Unscheduled job with trigger
						def trigger = jobfacade.getTrigger(jobName)
						if(trigger != null ) {
							//Add trigger of job belong to job class to lists wait
							def triggerWaitAll = [:]
							triggerWaitAll["trigger"] = trigger
							triggerWaitAll["jobName"] = jobName
							triggerWaitAll["jobClass"] = clsName
							lstTriggerWaitAll.add(triggerWaitAll)
							def mapTriggerWaitJob = [:]
							mapTriggerWaitJob["trigger"] = trigger
							mapTriggerWaitJob["jobName"] = jobName
							lstTriggerWaitJob.add(mapTriggerWaitJob)
							//Unschedule job by trigger
							jobfacade.unscheduleJob(trigger)
						}
						//Add jobs of belong to 'clsName' to list lstJobWaitJobClass
						def jobWaitClass = [:]
						jobWaitClass["jobClass"] = clsName
						jobWaitClass["jobName"] = jobName
						lstJobWaitJobClass.add(jobWaitClass)

						//Remove jobclass from scheduler
						if(jobfacade.getJobClass(clsName) != null ) {
							jobfacade.deleteJobClass(clsName)
						}
					}
					//Remove from mapJobInCls 
					mapJobInCls.remove(clsFileName)
				}
			}

		}
		//Remove from mapJCFileListJC 
		mapJCFileListJC.remove(jobcls.getName())

		return true
	}
}