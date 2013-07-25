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

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import static org.quartz.JobKey.*;
/**
 * 
 * @author kurohara
 *
 */
public class JobTerminateJob extends AbstractGenericJob {

	public static final String KEY_JOBNAMETOTERM = "jobname";
	
	@Override
	protected Object doJob(JobExecutionContext context) throws Throwable {
		JobDataMap data = context.getJobDetail().getJobDataMap();
		String jobname = data.getString(KEY_JOBNAMETOTERM);
		if (jobname != null && jobname.length() > 0) {
			try {
				boolean b = context.getScheduler().interrupt(jobKey(jobname));
				return Boolean.valueOf(b);
			} catch (Exception e) {
				
			}
		}
		return null;
	}

}
