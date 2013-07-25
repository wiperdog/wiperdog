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

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.wiperdog.jobmanager.JobFacade;
import org.wiperdog.jobmanager.JobManagerException;
import org.wiperdog.jobmanager.dsl.Parser;


public class Main {
	private Scheduler scheduler;
	private SchedulerFactory sf;

	private JobFacade jf;
	
	private void initSchedulerCore() {
		try {
			sf = new StdSchedulerFactory();
			scheduler = sf.getScheduler();
			jf = new JobFacadeImpl(scheduler);
		} catch (SchedulerException e) {
		} catch (JobManagerException e) {
		}
	}
	
	private void finishSchedulerCore() {
		try {
			jf = null;
			scheduler.shutdown();
			scheduler = null;
			sf = null;
		} catch (SchedulerException e) {
		}
	}

	private void start(String path, long interval) {
		Parser parser = new Parser(jf, false);
	}
	
	/**
	 * Usage: java -jar xxx.jar jdl_path.xml [wait_to_exit_millisec]
	 * @param args
	 */
	public static void main(String [] args) {
		if (args.length > 0) {
			String jdlpath = args[0];
			long waittime = 10000; // default 10 sec
			if (args.length > 1) {
				try {
					waittime = Long.parseLong(args[1]);
				} catch (NumberFormatException e) {
					
				}
			}
			(new Main()).start(jdlpath, waittime);
			
		}
	}

}
