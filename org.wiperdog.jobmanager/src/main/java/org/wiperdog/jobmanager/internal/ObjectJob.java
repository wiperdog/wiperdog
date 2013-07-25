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

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.UnableToInterruptJobException;
import org.wiperdog.jobmanager.Constants;
import org.wiperdog.jobmanager.JobExecutable;


@DisallowConcurrentExecution
public class ObjectJob extends AbstractGenericJob {
	JobDataMap data = null;
	Thread me = null;
	private JobExecutable getExecutable(JobDataMap datamap) {
		return (JobExecutable) data.get(Constants.KEY_OBJECT);
	}
	
	@Override
	protected Object doJob(JobExecutionContext context) throws Throwable {
		// save current running job data for interrupting.
		// use getMergedJobDataMap() instead of getJobDetail().getJobDataMap() to support trigger supplied job parameter(but not used yet).
		JobDataMap data = context.getMergedJobDataMap();
		this.data = data;
		me = Thread.currentThread();
		
		JobExecutable exe = null;
		Boolean value = Boolean.FALSE;
		try {
			exe = getExecutable(data);
		} catch (ClassCastException e) {
		}
		if (exe != null) {
			Object result = null;
			try {
				result = exe.execute(data);
			} catch (InterruptedException e) {
				throw e;
			}
			if (result == null) {
				//  execution ended with FALSE value
			} else {
				value = Boolean.parseBoolean(result.toString());
			}
		}
		this.data = null;
		return value;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		if (data != null) {
			JobExecutable exe = getExecutable(data);
			if (exe != null) {
				exe.stop(me);
			}
		}
	}
}
