class DataToDrawChart {
	// Get data to BAR chart
	def static getDataToDrawBar(collection){
                def result
                def dataSize = collection.size
                if (dataSize > 2) {
                   result = [collection[dataSize - 2], collection[dataSize - 1]]
                } else {
		   result = collection
                }
		def dataChart = result.KEYEXPR._chart[0]
		def type
		def resultData
                def finalResultData = []
		def  unit = result[0].KEYEXPR._unit
                if (unit == null) {
                    unit = [:]
                }
		for(datChart in dataChart){
			type = datChart.type
			if(type == "bar") {
                                resultData = []
				// get data to draw bar chart
                                def chart_columns = datChart.chart_columns
				def hint_columns = datChart.hint_columns
				if (hint_columns == null ){
					hint_columns = []
					for(resDat in result[0].data[0]){
						hint_columns.add(resDat.key)
					}
				}
				def finalData = [:]
				for(chartColumn in chart_columns){
					def KEYEXPR
					def dataToDraw = [:]
					def dataToToolTip = [:]

					// xAxis
					def mapCategories = [:]
					mapCategories['categories'] = result['fetchAt']
					finalData['xAxis'] = mapCategories

                                        // chart_name
					finalData['chart_name'] = datChart.name

					// Chart Column and Hint Column
					finalData['chart_columns'] = chart_columns
					finalData['hint_columns'] = hint_columns

					if(result.KEYEXPR != null) {
						KEYEXPR = result.KEYEXPR._root[0]
					}
					if(KEYEXPR != null) {
						def lstKey = []
						for(record in result){
							for(dat in record.data){
								def tmp = []
								for(keyEXPR in KEYEXPR){
									tmp.add(dat[keyEXPR])
								}
								if(!lstKey.contains(tmp)){
									lstKey.add(tmp)
								}
							}
						}
						for(data in result['data']){
							for(keySet in lstKey){
								def dataChartColumn
								def key = ""
								for(keyS in keySet){ key += keyS + "." }
								key = key.substring(0, key.length()-1)

								if(dataToDraw[key] == null){
									dataToDraw[key] = []
								}
								for(dat in data){
									def isData = true
									for(int i = 0; i< KEYEXPR.size(); i++){
										isData = isData && (dat[KEYEXPR[i]] == keySet[i])
									}
									if(isData){
										dataChartColumn = dat[chartColumn]
									}
								}
								dataToDraw[key].add(dataChartColumn)
							}
						}

						def series = []
						for(dat2Draw in dataToDraw){
							def tmp = [:]
							tmp['name'] = dat2Draw.key
							tmp['data'] = dat2Draw.value
							series.add(tmp)
						}
						finalData['series'] = series

						// get detail data to draw tooltip
						def hintData = [:]
						for(elementResult in result){
							for(elementData in elementResult['data']){
								def mapHintData = [:]
								mapHintData['fetchAt'] = elementResult.fetchAt
								for(elementHint in hint_columns){
									mapHintData[elementHint] = elementData[elementHint]
									for(key in unit.keySet()){
                                                                                def value = unit[key]
										if(elementHint == key) {
											mapHintData[elementHint] = elementData[elementHint] + " ( " + value + " )"
										}
									}
								}
								def key = ""
								for(eKeyexpr in KEYEXPR){
									key += elementData[eKeyexpr] + "."
								}
								key = key.substring(0, key.length()-1)
								if(hintData[key] == null) {
									hintData[key] = []
								}
								hintData[key].add(mapHintData)
								mapHintData = [:]
							}
						}
						def detail_data = []
						def mapFinalData = [:]
						for(key in hintData.keySet()){
                                                        def value = hintData[key]
							mapFinalData['name'] = key
							mapFinalData['data'] = value
							detail_data.add(mapFinalData)
							mapFinalData = [:]
						}
						finalData['detail_data'] = detail_data
					} else {
						// series
						def series = []
						def tmp = [:]
						tmp['name'] = chartColumn
						tmp['data'] = []
						for(res in result){
							tmp['data'].add(res.data[0][chartColumn])
						}
						series.add(tmp)
						finalData['series'] = series

						// detail_data
						def hintData = []
						for(elementResult in result){
							def mapHintData = [:]
							mapHintData['fetchAt'] = elementResult.fetchAt
							for(elementHint in hint_columns){
							        mapHintData[elementHint] = elementResult.data[0][elementHint]
								for(key in unit.keySet()){
                                                                        def value = unit[key]
									if(elementHint == key) {
										mapHintData[elementHint] = elementResult.data[0][elementHint] + " ( " + value + " )"
									}
								}
							}
							hintData.add(mapHintData)
							mapHintData = [:]
						}
						def mapFinalData = [:]
						def listFinalData = []
						mapFinalData['data'] = hintData
						listFinalData.add(mapFinalData)
						finalData['detail_data'] = listFinalData
					}
					resultData.add(finalData)
					finalData = [:]
				}
                                finalResultData.add(resultData)
			}
		}
		return finalResultData
	}

	// Get data to LINE chart
	def static getDataToDrawLine(collection){
             def finalResult = []
             def returnResult = []
             def key_chart = collection[0].KEYEXPR._chart
             def key_root = collection[0].KEYEXPR._root
             def key_unit = collection[0].KEYEXPR._unit
             if (key_unit == null) {
                key_unit = [:]
             }
             if (key_root == null) {
                key_root = [:]
             }
             def categories = []
             def listNameKey = []

             for(record in collection){
             	categories.add(record.fetchAt)
             }

             for(record in collection){
             	def data = record['data']
             	for(dat in data){
             		def datName
		        for(keyRoot in key_root){
             			if(datName == null){
		             		datName = dat[keyRoot]
             			}else{
				        datName += "." + dat[keyRoot]
		             	}
             		}
		        if(!listNameKey.contains(datName)){
             		    listNameKey.add(datName)
		        }
             	   }
             }   

             for(chart in key_chart){
             	if(chart.type == "line"){
             		def result = [:]
		        def collumns = chart.chart_columns
                        def hintColumns = chart.chart_columns.clone()
		        if(chart.hint_columns != null){
			    for(hintCol in chart.hint_columns){
				if(!hintColumns.contains(hintCol)){
					hintColumns.add(hintCol)
				}
			    }
		        }
		        result.xAxis = [:]
             		result.xAxis.categories = categories
                        def chartName
                        def title = chart.name
		        for(col in collumns){
             		    if(chartName != null){
		      		chartName += "_" + col
             		    }else {
		       		chartName = col
             	            }
		        }
             		result.chart_name = chartName 
		        result.series = []
             		result.chart_columns = collumns
                        result.hint_columns = hintColumns
             		result.detail_data = []
                        result.title = title
             		for(nameKey in listNameKey){
		             for(col in collumns){
             			def seriesRecord = [:]
				def detailDataRecord = [:]
		             	if(nameKey != null){
					seriesRecord['name'] = nameKey + "($col)"
             				detailDataRecord['name'] = nameKey + "($col)"
				}else{
		             	        seriesRecord['name'] = col
             				detailDataRecord['name'] = col
				}
		             	seriesRecord['data'] = []
             			detailDataRecord['data'] = []
				for(record in collection){
		             		boolean hasData = false
             				def data = record['data']
				        def eachData
		             		for(dat in data){
             				     def datName
				             for(keyRoot in key_root){
				             	if(datName == null){
		             			        datName = dat[keyRoot]
             					}else{
							datName += "." + dat[keyRoot]
						}
				             }
		             		     if(datName == nameKey){
             					hasData = true
						eachData = dat
				             }
		             		 }
             				 if(hasData){
					      seriesRecord['data'].add(eachData[col])
                                              def fullDetailData = [:]
		             		      fullDetailData.fetchAt = record.fetchAt
             				      if(hintColumns != null){
						 for(hintCol in hintColumns){
				             	     fullDetailData[hintCol] = eachData[hintCol]  + (key_unit[hintCol] != null ? ("( " + key_unit[hintCol] + " )") : "") 
		             			 }
             				      }else{
						 fullDetailData = eachData
                                                 fullDetailData.fetchAt = record.fetchAt
				              }
		             		      detailDataRecord['data'].add(fullDetailData)
             				  }else{
					      seriesRecord['data'].add(null)
				              detailDataRecord['data'].add(null)
		             		  }
             				}
			             	result.series.add(seriesRecord)
                                        result.detail_data.add(detailDataRecord)
             			}
		             }
             		finalResult.add(result)
             	   }
             }
             if(finalResult != []) {
               returnResult.add(finalResult)
             }
             return returnResult
	}

	// Get data to PIE chart
	def static getDataToDrawPie(collection){
		def result = collection[collection.size() - 1]
		if(result.KEYEXPR == null){
			  return null
		}
		def dataChart = result.KEYEXPR._chart
		def type
		def resultData
                def finalResultData = []
                def numOfChart
		for(elementChart in dataChart){
			type = elementChart.type
			if(type == "pie") {
				numOfChart = 0
				resultData = []
				for(elementData in result.data){
					def dataPie = [:]
					dataPie['type'] = "pie"
					dataPie['chart_name'] = elementChart.name
					if (result.KEYEXPR._unit != null) {
							dataPie['unit'] = '( ' + result.KEYEXPR._unit[elementChart.chart_columns[0]] + ' )'
					} else {
							dataPie['unit'] = ""
					}

					dataPie['data'] = []
					for(elementChartCol in elementChart.chart_columns){
						def tmp = []
						tmp.add(elementChartCol)
						tmp.add(elementData[elementChartCol])
						dataPie['data'].add(tmp)
					}

					def KEYEXPR
					if(result.KEYEXPR != null) {
						KEYEXPR = result.KEYEXPR._root
					}
					dataPie['name'] = ""
					if(KEYEXPR != null) {
						for(keyEXPR in KEYEXPR){
							dataPie['name'] += elementData[keyEXPR] + "."
						}
						dataPie['name'] = dataPie['name'].substring(0, dataPie['name'].length()-1)
					} else {
						dataPie['name'] = elementChart.name.replace(" ", "") + numOfChart
                                                numOfChart++
					}
					resultData.add(dataPie)
				}
				finalResultData.add(resultData)
			}
		}
		return finalResultData
	}

	// Get data to AREA chart
	def static getDataToDrawArea(collection){
		def result = collection
		def dataChart = result.KEYEXPR._chart[0]
		def type
		def resultData
		def finalResultData = []
		def lstKey
		def  unit = result[0].KEYEXPR._unit
		if (unit == null) {
			unit = [:]
		}

		for(itemDataChart in dataChart){
			type = itemDataChart.type
			if(type == "area") {
				resultData = []
				def chart_columns = itemDataChart.chart_columns
				def keyExprRoot = null
				if(result.KEYEXPR != null) {
					keyExprRoot = result.KEYEXPR._root[0]
				}
				def finalData = [:]
				if(keyExprRoot == null) {// KEYEXPR hasn't _root
					// xAxis
					def mapCategories = [:]
					mapCategories['categories'] = result['fetchAt']
					finalData['xAxis'] = mapCategories
					// chart_name
					finalData['chart_name'] = itemDataChart.name

					def series = []
					def listFinalData = []
					def detail_data = []
					for(chartColumn in chart_columns){

						// series
						def tmp = [:]
						tmp['name'] = chartColumn
						tmp['data'] = []
						for(itemResult in result){
							if (itemResult.data[0] != null) {
								tmp['data'].add(itemResult.data[0][chartColumn])
							} else {
								tmp['data'].add(null)
							}
						}
						series.add(tmp)
						finalData['unit'] = unit[chartColumn]
					}
					finalData['series'] = series
					finalData['chartItemName'] = ["root"]
					resultData.add(finalData)
					finalData = [:]
				} else { // KEYEXPR has _root
					lstKey = []
					for(record in result){
						for(dat in record.data){
							def tmp = []
							for(keyExpRo in keyExprRoot){
								tmp.add(dat[keyExpRo])
							}
							if(!lstKey.contains(tmp)){
								lstKey.add(tmp)
							}
						}
					}
					def itemList = []
					for(keySet in lstKey){
						// xAxis
						def mapCategories = [:]
						mapCategories['categories'] = result['fetchAt']
						finalData['xAxis'] = mapCategories
						
						def valueOfKey = ""
						for(keys in keySet){
							valueOfKey += keys + "."
						}
						valueOfKey = valueOfKey.substring(0, valueOfKey.length()-1)
						finalData['chart_name'] = itemDataChart.name + " (" + valueOfKey + ")"
                                                itemList.add(valueOfKey.replaceAll("\\.","_"))
						finalData['series'] = []
						def mapSeries
						for(itemChart in chart_columns){
							mapSeries = [:]
							mapSeries['name'] = itemChart
							mapSeries['data'] = []
							def isHasData
							for(oneRunData in result['data']){
								isHasData = false
								for(itemData in oneRunData){
									def dataKey = ""
									for(itemKeyExpr in keyExprRoot){
										 dataKey += itemData[itemKeyExpr] + "."
									}
									dataKey = dataKey.substring(0, dataKey.length()-1)
									if (valueOfKey.equals(dataKey)) {
										isHasData = true
										mapSeries['data'].add(itemData[itemChart])
									}
								}
								if (!isHasData) {
									mapSeries['data'].add(null)
								}
							}
							finalData['series'].add(mapSeries)
                                                        finalData['unit'] = unit[itemChart]
						}
                                                finalData['chartItemName'] = itemList
						resultData.add(finalData)
						finalData = [:]
					}		
				} // End else
				finalResultData.add(resultData)
			}
		}
		return finalResultData
	}

	// Get data to draw subtype
	def static getDataToDrawSubtype(dataSubtype){
	  def listDataStore = []
	  def mapForKey = [:]
	  
	  def listKey = []
	  for(datSub in dataSubtype[0]['data']){
		  listKey.add(datSub.key)
	  }
	  for(keySubtype in listKey){
		  mapForKey[keySubtype] = []
		  for(eachRunJob in dataSubtype){
			  def dataStoreKey = [:]
			  for(eRunJob in eachRunJob){
				  if(eRunJob.key == "data") {
					  // process data job
					  dataStoreKey['data'] = eachRunJob['data'][keySubtype]
				  } else if(eRunJob.key == "KEYEXPR"){
						// process data keyexpr
						def mapKeyexpr = [:]
					  // root of keyexpr
					  if(eachRunJob['KEYEXPR'][keySubtype] != null) {
						  mapKeyexpr['_root'] = eachRunJob['KEYEXPR'][keySubtype]
					  }
					  // chart of keyexpr
					  if(eachRunJob.KEYEXPR._chart instanceof Map) {
						  // chart is map data
						  for(keys in eachRunJob.KEYEXPR._chart.keySet()){
							  if(keySubtype == keys) {
								  mapKeyexpr['_chart'] = eachRunJob['KEYEXPR']['_chart'][keys]
							  }
						  }
					  } else {
							mapKeyexpr['_chart'] = eachRunJob.KEYEXPR._chart
					  }
					  // unit of keyexpr
                                          //Check if unit by key of subtype
                                          if (eachRunJob.KEYEXPR._unit != null){
                                              for(keys in eachRunJob.KEYEXPR._unit.keySet()){
						  if(keySubtype == keys) {
							mapKeyexpr['_unit'] = eachRunJob['KEYEXPR']['_unit'][keys]
				                  }
					      } 
                                              if (mapKeyexpr['_unit'] == null) {
					          mapKeyexpr['_unit'] = eachRunJob.KEYEXPR._unit
                                              }
                                          }

                                          // description of keyexpr
					  mapKeyexpr['_description'] = eachRunJob.KEYEXPR._description

					  dataStoreKey['KEYEXPR'] = mapKeyexpr
					  mapKeyexpr = [:]
				  } else {
					  dataStoreKey[eRunJob.key] = eRunJob.value
				  }
			  }
			  mapForKey[keySubtype].add(dataStoreKey)
			  dataStoreKey = [:]
		  }
	  }
	  return mapForKey
  }
}
