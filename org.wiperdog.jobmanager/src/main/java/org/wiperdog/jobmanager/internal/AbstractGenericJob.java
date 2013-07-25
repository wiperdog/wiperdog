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
import java.util.Set;

import org.apache.log4j.Logger;
import org.quartz.DateBuilder;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.quartz.DateBuilder.IntervalUnit;
import org.wiperdog.jobmanager.Constants;
import org.wiperdog.jobmanager.JobResult;
import org.wiperdog.jobmanager.Node;


import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.wiperdog.jobmanager.Constants.*;


/**
 * 
 * @author kurohara
 *
 */
public abstract class AbstractGenericJob implements InterruptableJob {
	public static final String KEY_MAXRUNTIME = "MAXRUNTIME";
	private static final String EMPTYGROUP = "__EMPTYGROUP";
	public static final int DEF_MAX_HISTORYDEPTH = 5;
	protected Thread me;
	private boolean bRun = true;	
	protected String lastMsg = "";
	protected Logger logger = Logger.getLogger(Activator.LOGGERNAME);
	
	protected abstract Object doJob(JobExecutionContext context) throws Throwable;
	
	protected final void setFailed(JobExecutionContext context) {
		logger.trace("AbstractGenericJob.setFailed(" + context.toString() + ")");
		JobDataMap data = context.getMergedJobDataMap();
		setFailed(data);
	}
	
	protected final void setFailed(JobDataMap data) {
		logger.trace("AbstractGenericJob.setFailed(" + data.toString() + ")");
		data.put(Node.KEY_JOBEXECUTIONFAILED, Boolean.valueOf(true));
	}
	
	/**
	 * Job for suicide.
	 * kill itself if overrunning.
	 * for individual running time setting.
	 * same as "RuntimeLimitterJob" in JobClassImpl.
	 * 
	 * @author kurohara
	 *
	 */
	public static final class SuicideJob implements InterruptableJob {
		private Logger logger = Logger.getLogger(Activator.LOGGERNAME);
		public static final String KEY_JOBKEY = "jobkey";
		
		public SuicideJob() {
			logger.trace("RuntimeLimitterJob.RuntimeLimitterJob()");
		}
		
		public void execute(JobExecutionContext context)
				throws JobExecutionException {
			logger.trace("RuntimeLimitterJob.execute()");
			logger.debug("interrupting job");
			JobKey jobkey = (JobKey) context.getMergedJobDataMap().get(KEY_JOBKEY);
			try {
				JobDetail targetJob = context.getScheduler().getJobDetail(jobkey);
				if (targetJob == null) {
					// no such job now
					logger.info("The job going to interrupt is already disappeared");
					return ;
				}
				// do interrupt
				context.getScheduler().interrupt(jobkey);
			} catch (UnableToInterruptJobException e) {
				logger.debug("	error on interrupt:", e);
			} catch (SchedulerException e) {
				logger.debug("	error on interrupt:", e);
			}
		}

		public void interrupt() throws UnableToInterruptJobException {
			logger.trace("RuntimeLimitterJob.interrupt()");
		}
	}

	/**
	 * setup suicide job
	 * @param context
	 */
	private JobDetail setSuicide(JobExecutionContext context) {
		// check if timed suicide is required.
		JobDataMap data = context.getJobDetail().getJobDataMap();
		Long maxRunTime = (Long) data.get(KEY_MAXRUNTIME);
		logger.trace("setting suicide job for :" + context.getJobDetail().getKey().getName() + ", time: " + maxRunTime);
		if (maxRunTime != null && maxRunTime.longValue() > 0 && ! maxRunTime.equals(Long.MAX_VALUE)) {
			JobKey key = context.getJobDetail().getKey();
			JobDataMap datamap = new JobDataMap();
			datamap.put(SuicideJob.KEY_JOBKEY, key);
			JobDetail sj = newJob()
					.usingJobData(datamap)
					.ofType(SuicideJob.class)
					.build();
			
			Trigger t = newTrigger()
					.forJob(sj)
					.startAt(DateBuilder.futureDate(maxRunTime.intValue(), IntervalUnit.MILLISECOND))
					.build();
			
			try {
				context.getScheduler().scheduleJob(sj, t);
				return sj;
			} catch (SchedulerException e) {
				logger.warn("failed to setup suicide job");
				logger.debug(e);
			}
		} else {
			logger.trace("maxruntime is not specified or NAN");
		}
		return null;
	}
	
	private void cancelSuicide(JobExecutionContext context, JobDetail cancelJob) {
		try {
			context.getScheduler().deleteJob(cancelJob.getKey());
		} catch (SchedulerException e) {
			logger.trace("failed to delete suicide job", e);
		}
	}
	
	public final void execute(JobExecutionContext context)
			throws JobExecutionException {
		logger.trace("AbstractGenericJob.execute(" + context.toString() + ")");
		me = Thread.currentThread();
		JobDataMap data = context.getMergedJobDataMap();
		
		@SuppressWarnings("unchecked")
		Set<String> reason = (Set<String>) data.get(Constants.KEY_PROHIBIT);

		logger.trace("	jobData(" + Constants.KEY_PROHIBIT + ") = " + (reason == null ? "null" : reason.toString()));
		if (reason != null && ! reason.isEmpty()) {
			// job execution is prohibited with some reason, return immediately
			context.setResult(null);
			logger.trace("	job(" + context.getJobDetail().getKey() + ") was prohibited so returning without doing anythig");
			return;
		}
		
		Trigger trigger = context.getTrigger();
		
		logger.trace("	job(" + trigger.getJobKey().toString() + ") is now starting.");
		try {
			JobDetail suicideJob = setSuicide(context);
			JobDataMap datamap = context.getJobDetail().getJobDataMap();
			JobResultImpl jresult = (JobResultImpl) datamap.get(KEY_JOBRESULT);
			if (jresult == null) {
				jresult = new JobResultImpl(trigger.getJobKey().getName());
			} else {
				jresult = new JobResultImpl(jresult); 
			}
			datamap.put(KEY_JOBRESULT, jresult);
			jresult.setStartedAt(new Date());
			Object ro = null;
			try {
				ro = doJob(context);
				context.setResult(ro);
				jresult.setResult(ro);
				jresult.setMessage(lastMsg);
				jresult.setEndedAt(new Date());
			} catch (InterruptedException e) {
				jresult.setInterruptedAt(new Date());
			} catch (Throwable t) {
				logger.debug("doJob() failed", t);
			} finally {
				if (suicideJob != null) {
					logger.trace("cancelling suicide job" +suicideJob.getKey().getName());
					cancelSuicide(context, suicideJob);
				}
				logger.trace("	job(" + trigger.getJobKey().toString() + ") finished, jobData:" + datamap.toString());
			}
		} catch (Throwable t) {
			logger.info("	job(" + trigger.getJobKey().getName() + ") execution failed,  Throwable:" + t.getMessage());
			throw new JobExecutionException(t);
		}
	}
	
	public void interrupt() throws UnableToInterruptJobException {
		logger.trace("AbstractGenericJob.interrupt()");
		bRun = false;
		if (me != null) {
			me.interrupt();
		}
	}
}
