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
package org.wiperdog.jobmanager.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.wiperdog.jobmanager.JobResult;


public class JobResultImpl implements JobResult {
	private String name;
	private Date startedAt;
	private Date endedAt;
	private Date interruptedAt;
	private Date pendedAt;
	private Date waitexpiredAt;
	private Object result;
	private JOBSTATUS status = JOBSTATUS.NONE;
	private String message;
	private Map<String, Object> params = new HashMap<String,Object>();
	private Map<String, Object> data;
	
	public JobResultImpl(String name) {
		this.name = name;
	}

	public JobResultImpl(JobResultImpl src) {
		name = src.getName();
		startedAt = src.getStartedAt();
		endedAt = src.getEndedAt();
		interruptedAt = src.getInterruptedAt();
		pendedAt = src.getPendedAt();
		result = src.getResult();
		params.putAll(src.params);
	}
	
	public String getName() {
		return name;
	}

	public Date getStartedAt() {
		return startedAt;
	}

	public Date getInterruptedAt() {
		return interruptedAt;
	}

	public Date getEndedAt() {
		return endedAt;
	}

	public Date getPendedAt() {
		return pendedAt;
	}
	
	public Object getResult() {
		return result;
	}

	public JOBSTATUS getLastStatus() {
		return status;
	}
	
	public Object getParam(String key) {
		return params.get(key);
	}

	public void setStartedAt(Date at) {
		startedAt = at;
	}
	
	public void setEndedAt(Date at) {
		status = JOBSTATUS.EXECUTED;
		endedAt = at;
	}
	
	public void setInterruptedAt(Date at) {
		status = JOBSTATUS.INTERRUPTED;
		interruptedAt = at;
	}

	public void setPendedAt(Date at) {
		pendedAt = at;
	}
	
	public void setResult(Object result) {
		this.result = result;
	}

	public void setWaitexpiredAt(Date at) {
		waitexpiredAt = at;
	}
	
	public void setLastStatus(JOBSTATUS status) {
		this.status = status;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Map<String, Object> getData() {
		return data;
	}
	
	public void putData(String key, Object value) {
		if (data == null){
			data = new HashMap<String, Object>();
		}
		data.put(key, value);
	}

	public Date getWaitexpiredAt() {
		return waitexpiredAt;
	}
}
