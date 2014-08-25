import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;
import groovy.json.JsonBuilder

class LivetableDataAdapter {
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
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		response.setContentType("application/json")
		def params = request.getQueryStringMap()
		def rootLivetableRowList = []
		def rootLivetableData = [:]
		def legacyData = []
		def sortedResultData = [:]
		def dataModel = []

		def job = params["job"]

		def fromDate = params["fromDate"].replace("%20"," ")
		def toDate = params["toDate"].replace("%20"," ")
		def limit= (params["limit"]!=null)?params["limit"].toInteger():10

		def page = params["page"]
		def rows = (params["rows"] != null && !params["rows"].equals(""))?params["rows"]:20
		def sidx = params["sidx"]
		def sord = params["sord"]
		def genPam = params["genp"]
		def istIid = params["istIid"]
		def subType = params["subType"]
		if(subType != null) {
			subType = subType.replaceAll("\"","")
		}
		def str= "No data were retrieved!"
		def result = null
		if(job!="" && job!=null && istIid != null && !istIid.equals("")){
			def dataBusiness
			try{
				dataBusiness = DataBusiness.getInstance()
				//dataBusiness.getConnection(services.WiperdogConfig.getDataFromConfig())
				if(fromDate != "" || toDate != ""){
					result = dataBusiness.getDataInPeriod(job,fromDate,toDate,limit,istIid)
				} else {
					result = dataBusiness.getDataAllFields(job,limit,istIid)
				}
			} catch (Exception ex){
				return '{{error}}' + ex + '{{/error}}'
			} finally {
				/*if(dataBusiness != null){
					dataBusiness.db.closeConnection()
				}*/
			}
			def idx =0
			for(rec in result){
				//-- get a list of data row
				legacyData = rec['data']
				def subtractData = []
				//-- Parsing data and assign data to subtract list
				if(subType == null || subType.equals("")){ //-- Store
					subtractData = legacyData
				}else{ //-- Subtype
					def subTypeDataList = legacyData[subType]
					subtractData = subTypeDataList
				}

				for(r2 in subtractData){
					def livetableRows = [:]
					livetableRows['id'] = rec['fetchAt'] + "_" + idx
					//-- replace CRLF in values to avoid JSON.parse error
					def values = new ArrayList<String>(r2.values())
					livetableRows['cell'] = []
					def newdate = new Date().parse("yyyyMMddHHmmssz", rec['fetchAt'])
					livetableRows['cell'].add(newdate.format('yyyy/MM/dd HH:mm:ss z'))
					for(s in values){
						if(s instanceof java.lang.String){
							def newVal = s.replaceAll("\\\\","/")
							s = newVal.replaceAll("\r\n","")
							newVal = s.replaceAll("\r","")
							s = newVal.replaceAll("\n","")
							livetableRows['cell'].add(s)
						}else
							livetableRows['cell'].add(s)
					}
					if(dataModel != null && dataModel.size() <=0){
						dataModel = new ArrayList<String>(r2.keySet());
					}
					rootLivetableRowList.add(livetableRows)
					idx++
				}
			}
			rootLivetableData["records"] = rootLivetableRowList.size()

			rootLivetableData["total"] =  Math.round(rootLivetableRowList.size()/rows.toInteger())  //total page
			rootLivetableData["page"] = (page != null)?page:1
			rootLivetableData["rows"] = rootLivetableRowList
			rootLivetableData["userdata"] = [:]
			rootLivetableData["dataModel"] = dataModel
			sortedResultData = rootLivetableRowList
			//-- convert sidx to
			if(sord != null){
				//-- sorting
				def arrayMap1 = rootLivetableRowList.toArray(rootLivetableRowList)
				Arrays.sort(arrayMap1, new Comparator() {
							public int compare(arg0, arg1) {
								def p = arg1
								def n = arg0
								//-- sort by id first, late we sort by sidx req. pam.
								int lv = (sord.equals("asc"))?p["id"].compareTo(n["id"]):n["id"].compareTo(p["id"])
								if (lv > 0) {
									return 1
								} else if (lv < 0) {
									return -1
								} else {
									return 0
								}
								return lv
							}
						})
				sortedResultData = arrayMap1
				rootLivetableData["rows"] = sortedResultData
			}
			//if( rootLivetableData != null && rootLivetableData.size() > 0 ){
			//-- Parsing generate parameter
			//def builder = new JsonBuilder(rootLivetableData)
			if(genPam != null && genPam.equals("1")){
				def pagerName = params["pager"]
				if(pagerName == null || pagerName.equals(""))
					str = "Must specify parameter pager incase you want to build data model (with genp parameter)"
				def dataUrl =  ""
				if(subType == null || subType == "")
					dataUrl = "http://localhost:8089/liveTableAdapterr?job="+job +"&limit="+limit+"&fromDate="+fromDate + "&toDate=" + toDate + "&istIid="+istIid
				else
					dataUrl = "http://localhost:8089/liveTableAdapter?job="+job +"&limit="+limit+"&fromDate="+fromDate + "&toDate=" + toDate + "&istIid="+istIid+"&subType=" + subType

				//-- Building data model
				def model = []
				def colSpec
				for(name in dataModel){
					colSpec = [:]
					colSpec['name'] = name
					colSpec['index'] = name
					colSpec['width'] = "100"
					model.add(colSpec)
				}
				def dataModelStr = (new JsonBuilder(dataModel)).toPrettyString()
				def modelStr = (new JsonBuilder(model)).toPrettyString()

				def sortStr = (sord == null || sord.equals(""))?"asc":sord
				def returnData = [:]
				//returnData["url"] = dataUrl
				returnData["datatype"] = "json"
				returnData["colNames"] = dataModelStr
				returnData["colModel"] = modelStr
				def rowList = [10,20,30]
				returnData["rowList"] = rowList
				returnData["pager"] = pagerName
				returnData["viewrecords"] = true
				returnData["sortname"] = "RECORD_SEQ"
				returnData["sortorder"] = sortStr
				returnData["caption"] = job
				/*str =
						"{\"url\":\""+dataUrl+"\",\"datatype\":\"json\","+
						"\"colNames\":"+ dataModelStr +
						",\"colModel\":"+ modelStr +" ,\"rowList\":[10,20,30],"+
						"\"pager\":\"#"+pagerName+"\", \"viewrecords\": true, \"sortname\": \"RECORD_SEQ\","+
						"\"sortorder\": \""+ sortStr +"\", \"caption\":\""+job+"\"}"*/

				return returnData

			}else{
				//str = builder.toPrettyString()
				return rootLivetableData
			}
		}

	}
}