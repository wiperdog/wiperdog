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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.KeyMatcher;
import org.wiperdog.jobmanager.Constants;


/**
 * 
 * @author kurohara
 *
 */
public final class ProhibitTerminal extends AbstractTerminal {
	private final Scheduler scheduler;
	private boolean bOpen = false;
	private long openedAt = 0;
	private long gateTimer = 0;
	private final Logger logger = Logger.getLogger(Activator.LOGGERNAME);
	
	public static final String REASONKEY_JOBNODE = "" + ProhibitTerminal.class.getName();
	
	@SuppressWarnings("unchecked")
	protected ProhibitTerminal(String name, Scheduler scheduler, JobKey jobkey) throws SchedulerException {
		super(name, jobkey);
		logger.trace("ProhibitTerminal.ProhibitTerminal(" + name + "," + scheduler.toString() + "," + jobkey.toString()+")");
		this.scheduler = scheduler;
		scheduler.getListenerManager().addJobListener(new NodeJobListener(), KeyMatcher.keyEquals(jobkey));
	}

	public void setGateTimer(long interval) {
		logger.trace("ProhibitTerminal.setGateTimer(" + interval + ")");
		this.gateTimer = interval;
	}
	
	@Override
	protected void update(boolean v) {
		logger.trace("ProhibitTerminal.update(" + v + ")");
		bOpen = v;
		if (v) {
			openedAt = (new Date()).getTime();
		}
	}

	/**
	 * 
	 * @author kurohara
	 *
	 */
	private final class NodeJobListener implements JobListener {

		public NodeJobListener() {
			logger.trace("NodeJobListener.NodeJobListener()");
		}
		
		public String getName() {
			logger.trace("NodeJobListener.getName()");
			return id;
		}

		public void jobToBeExecuted(JobExecutionContext context) {
			logger.trace("NodeJobListener.jobToBeExecuted()");
			logger.trace("	bOpen:" + bOpen);

			@SuppressWarnings("unchecked")
			Set<String> p = (Set<String>) context.getMergedJobDataMap().get(Constants.KEY_PROHIBIT);
			
			if (p == null) {
				p = new HashSet<String>();
				context.getJobDetail().getJobDataMap().put(Constants.KEY_PROHIBIT, p);
			}
			if (! bOpen || (gateTimer != 0 && openedAt + gateTimer < (new Date()).getTime())) {
				// set prohibit reason value here
				p.add(REASONKEY_JOBNODE);
			} else {
				p.remove(REASONKEY_JOBNODE);
			}
			
		}

		public void jobExecutionVetoed(JobExecutionContext context) {
			logger.trace("NodeJobListener.jobExecutionVetoed()");
		}

		public void jobWasExecuted(JobExecutionContext context,
				JobExecutionException jobException) {
			logger.trace("NodeJobListener.jobWasExecuted()");
		}
		
	}

	public boolean getPValue() {
		return bOpen;
	}

}
