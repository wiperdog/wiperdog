/**
 * Get data to create tree menu
 */

class GenerateTreeMenu {
	/**
	 * getData2CreateMenu: get data to create tree menu
	 * @Param JOB_DIR: job directory
	 * @Return data2CreateMenu
	 */
	public static getData2CreateMenu(String JOB_DIR) {
		def data2CreateMenu = [:]
		def shell = new GroovyShell()

		// Get list job from job directory
		def list_job = getListJobFromJobDir(JOB_DIR)
		data2CreateMenu = getData2CreateMenu(list_job)
		return data2CreateMenu
	}

	/**
	 * getListJobFromJobDir: get list job from job directory
	 * @Param JOB_DIR: job directory
	 * @Return list_job
	 */
	public static getListJobFromJobDir(String JOB_DIR) {
		// Get list job
		def list_job = []
		if(JOB_DIR != null){
			def job_dir = new File(JOB_DIR)
			if(job_dir.isDirectory()){
				job_dir.listFiles().each {file ->
					def fileName = file.getName()
					if(fileName.endsWith('.job')){
						list_job.add(fileName)
					}
				}
			}
		}
		return list_job
	}

	/**
	 * getListJobFromMongo: get list job from mongo db
	 * @Return list_job
	 */
	public static getListJobFromMongo() {
		def tmp = []
		def list_job = []
	    def mongoDBConn = MongoDBConnection.getWiperdogConnection()
	    def collections = mongoDBConn.db.getCollectionNames()
	    for(def collection in collections) {
			if(collection.lastIndexOf(".") > 0 && collection.split("\\.").size() > 2) {
				def jobname = collection.substring(0, collection.lastIndexOf("."))
				if(!list_job.contains(jobname)){
					list_job.add(jobname)
				}
			} else {
				if(!list_job.contains(collection)) {
					list_job.add(collection)
				}
			}
		}
		//mongoDBConn.close()
		return list_job
	}
	
	def static getData2CreateMenu(list_job){
		def data2CreateMenu = [:]
		def shell = new GroovyShell()
		
		def dbmsInfoFile = new File(MonitorJobConfigLoader.getProperties().get(ResourceConstants.USE_FOR_XWIKI))
		def dbmsInfo = shell.evaluate(dbmsInfoFile.getText())
		def root = dbmsInfo['TreeMenuInfo']

		//Initial
		def output = [:]
		def tmpKey = ""
		root.each{k,v->
			if (!v.isEmpty()) {
				output[k]=[:]
				v.each{c, valuec->
					tmpKey = k + "." + c
					output[tmpKey] = []
				}
			} else {
				output[k] = []
			}
		}

		//Bind job name to create tree menu
		def isOthersJob
		def isOthersJobInGroup
		list_job.each {job->
			isOthersJob = true
			def tmpArray = job.split("\\.")
			if (tmpArray.size() >= 2) {
				root.each{k,v->
					if (tmpArray[0] == k) {
						if (!v.isEmpty()) {
							if (tmpArray.size() >= 3) {
								isOthersJobInGroup = true
								v.each{c, valuec->
									if (tmpArray[1] == c) {
										tmpKey = k + "." + c
										output[tmpKey].add(job)
										isOthersJobInGroup = false
										//Set to not add in others group
										isOthersJob = false
									}
								}
								if (isOthersJobInGroup) {
									tmpKey = k + ".Others"
									output[tmpKey].add(job)
									//Set to not add in others group
									isOthersJob = false
								}
							}
						} else {
							if ((tmpArray.size() >= 2) && (output[k] instanceof List)) {
								output[k].add(job)						
								//Set to not add in others group
								isOthersJob = false
							}
						}	
					}
				}
			}
			if (isOthersJob) {
				output["Others"].add(job)
			}
		}
		data2CreateMenu['root'] = root
		data2CreateMenu['output'] = output
		return data2CreateMenu
	}

	/**
	 * Recursively function, used for gen tree menu data 
	 * Input treeItem: root tree map of menu (not leaf)
	 *       mapCollection: Map of collections, Item of map has key is a job group and value is list job which is applied for that group
	 *       parentList: used for recursively to canculate key if data if leaf
	 * Output: If data is leaf, create key of group data, get list of job which is applied for group from mapCollection and create menu Item
	 *         If data isn't leaf, create node and call recursively function with sub data
	 **/
	public static getMenuItemsStr(treeItem, mapCollection, parentList = []) {     
	     def ul_open = false
	     def result = ""
	     def parentStr = ""
	     def parentLstforChild = []

	     //If data isn't leaf, create node and call recursively function with sub data
	     if (treeItem instanceof Map) {
	         result += "<ul id='treemenu2' class='treeview'>"
	         treeItem.each{itemKey, itemVal -> 
	             parentList.each{parentListItem->
	                 parentLstforChild.add(parentListItem)
	             }
	             parentLstforChild.add(itemKey)
		     result += "<li>"+ itemKey
	             result += getMenuItemsStr(itemVal, mapCollection, parentLstforChild)
	             result +="</li>"
	             parentLstforChild = []
	         }
	         result += "</ul>"
	     }
	     
	     //If data is leaf, create key of group data, get list of job which is applied for group from mapCollection and create menu Item
	     if (treeItem instanceof List) {
	         result += "<ul>"
	         parentList.each{parentItem -> 
	              if (parentStr != ""){
	                  parentStr += "."
	              }
	              parentStr += parentItem
	         }
	         if (mapCollection[parentStr] != null) {
	             mapCollection[parentStr].each {item->
	                 result += "<li><a>" + item +"</a></li>"
	             }
	         }
	         result += "</ul>"
	     }
	     return result
	}
}
