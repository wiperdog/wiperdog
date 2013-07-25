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
package org.wiperdog.jobmanager;

import java.util.Date;
import java.util.Map;

public interface JobResult {
	public enum JOBSTATUS {
		NONE,
		EXECUTED,
		INTERRUPTED,
		MISFIRED
	}
	
	/**
	 * get Job name
	 * @return
	 */
	String getName();

	/**
	 * get time of the job was started.
	 * @return
	 */
	Date getStartedAt();
	
	/**
	 * get time of the job was Time Over Interrupted.
	 * @return
	 */
	Date getInterruptedAt();
	
	/**
	 * get time of the job was ended.
	 * @return
	 */
	Date getEndedAt();

	/**
	 * get time of when the job was pended.
	 * @return
	 */
	Date getPendedAt();

	/**
	 * get time of when the job wait time was expired
	 * 
	 * @return
	 */
	Date getWaitexpiredAt();
	
	/**
	 * get execution result.
	 * @return
	 */
	Object getResult();

	/**
	 * get last job execution status.
	 * @return
	 */
	JOBSTATUS getLastStatus();
	
	/**
	 * get other paramaters
	 * @param key
	 * @return
	 */
	Object getParam(String key);
	
	/**
	 * get message for last event(error/accident).
	 * 
	 * @return
	 */
	String getMessage();
	
	/**
	 * Get additional data.
	 * @return
	 */
	Map<String, Object> getData();

	/**
	 * Put additional data.
	 * @param key
	 * @param value
	 */
	void putData(String key, Object value);
}
