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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.quartz.DateBuilder;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.TriggerKey;
import org.quartz.JobKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.matchers.KeyMatcher;
import org.wiperdog.jobmanager.Constants;
import org.wiperdog.jobmanager.JobClass;
import org.wiperdog.jobmanager.JobFacade;
import org.wiperdog.jobmanager.JobReceiver;
import org.wiperdog.jobmanager.JobResult.JOBSTATUS;
import org.wiperdog.jobmanager.TriggerReceiver.TRIGGEREVENT;
import org.apache.log4j.Logger;


import static org.quartz.JobKey.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.wiperdog.jobmanager.Constants.KEY_JOBRESULT;

/**
 * ジョブクラス実装
 * @author kurohara
 *
 */
public class JobClassImpl implements JobClass {
	private Logger logger = Logger.getLogger(JobClassImpl.class);
	public static final String KEY_PENDETAT = "pendetAt";
	
	/**
	 * 実行中止トリガキー
	 * 並列数の制限により実行をペンディングされているジョブを示すためのキーオブジェクトクラス
	 * @author kurohara
	 *
	 */
	private static class VetoedTriggerKey {
		private final Date vetoedAt; // 実行がペンディングされた時刻
		private final TriggerKey triggerkey;
		private final JobDetail jobDetail; // required to hold to support non-durable job
		private Logger logger = Logger.getLogger(VetoedTriggerKey.class);
		
		public VetoedTriggerKey(TriggerKey key, JobDetail job) {
			logger.trace("VetoedTriggerKey.VetoedTriggerKey(" + key.toString() + ")");
			this.triggerkey = key;
			this.jobDetail =  job;
			this.vetoedAt = new Date();
		}

		public VetoedTriggerKey(VetoedTriggerKey src) {
			vetoedAt = src.vetoedAt;
			triggerkey = src.triggerkey;
			jobDetail = src.jobDetail;
		}
		
		public TriggerKey getKey() {
			logger.trace("VetoedTriggerKey.getKey()");
			return triggerkey;
		}

		public JobDetail getJobDetail() {
			return jobDetail;
		}
		
		public boolean canProceed(long waitTimeMilli) {
			if ((new Date()).getTime() - vetoedAt.getTime() < waitTimeMilli) {
				logger.trace("VetoedTriggerKey.canProceed(" + waitTimeMilli + ") - true");
				return true;
			}
			logger.trace("VetoedTriggerKey.canProceed(" + waitTimeMilli + ") - false");
			return false;
		}
		
		public Date getVetoedDate() {
			return vetoedAt;
		}
		
		public String toString() {
			return "{ vetoed: '" + vetoedAt + "', trigger: '" + triggerkey.toString() + "', job: '" + jobDetail.getKey().toString() + "' }";
		}
	}
	
	private String name;
	private BlockingQueue<VetoedTriggerKey> vetoedQueue = new LinkedBlockingQueue<VetoedTriggerKey>();
	private Set<JobKey> running = new HashSet<JobKey>();
	private int numConcurrency;
	private long waitTime;
	private long maxRunTime;
	private final static String NULLNAME = "NULLNAME";
	private final static String NULLGROUP = "NULLGROUP";
	private static final Matcher<JobKey> NULLJOBMATCHER = KeyMatcher.keyEquals(new JobKey(NULLNAME, NULLGROUP));
	private JobFacade jobFacade;
	private final Scheduler scheduler;
	private final static String REASONKEY_CONCURRENCY = JobClassImpl.class.getName();
	private final List<JobKey> assignedList = new ArrayList<JobKey>();
	public static final String SUFFIX_CANCELJOB = "_cancel";
	
	public String toString() {
		List<Matcher<JobKey>> matcherlist = null;
		try {
			matcherlist = scheduler.getListenerManager().getJobListenerMatchers(name);
		} catch (SchedulerException e) {
			logger.info("failed to get matcher");
			logger.trace("", e);
		}
		String str = getClass().getSimpleName() + "(" + name + "), members : {";
		if (matcherlist != null) {
			for (Matcher<JobKey> keyMatcher : matcherlist) {
				str += keyMatcher.toString() + ",";
			}
		}
		str += "}";
		return str;
	}

	/**
	 * コンストラクタ
	 * @param sched Quartzスケジューラインスタンス
	 * @param name  ジョブクラス名
	 * @throws SchedulerException
	 */
	public JobClassImpl(JobFacade jf, Scheduler sched, String name) throws SchedulerException {
		logger.trace("JobClass.JobClass(" + sched.toString() + "," + name + ")");
		this.name = name;
		this.jobFacade = jf;
		this.scheduler = sched;
		// default concurrency is 1
		this.numConcurrency = 1;
		// default runtime = near forever
		this.maxRunTime = Long.MAX_VALUE;
		// default wait time = near forever
		this.waitTime = Long.MAX_VALUE;
		ListenerManager lm = sched.getListenerManager();
		lm.addJobListener(new ConcurrencyJobListener(), NULLJOBMATCHER);
	}
	
	public JobClassImpl(JobClassImpl src) throws SchedulerException {
		name = src.name;
		scheduler = src.scheduler;
		src.vetoedQueue.drainTo(vetoedQueue);
		running.addAll(src.running);
		numConcurrency = src.numConcurrency;
		waitTime = src.waitTime;
		maxRunTime = src.maxRunTime;
		assignedList.addAll(src.assignedList);
		ListenerManager lm = scheduler.getListenerManager();
		lm.removeJobListener(name);
		lm.addJobListener(new ConcurrencyJobListener(), NULLJOBMATCHER);
	}

	public void close() {
		ListenerManager lm;
		try {
			lm = scheduler.getListenerManager();
			lm.removeJobListener(name);
		} catch (SchedulerException e) {
			logger.info("failed to close JobClass: " + name);
			logger.trace("", e);
		}
	}
	
	/**
	 * MaxWaitTime(ジョブ実行までの最大待ち時間)を設定
	 * @param timeInMillis 最大待ち時間(ミリ秒)
	 */
	public void setMaxWaitTime(long timeInMillis) {
		logger.trace("JobClass.setWaitTime(" + timeInMillis + ")");
		this.waitTime = timeInMillis < 0 ? Long.MAX_VALUE : timeInMillis;
	}

	/**
	 * MaxRunTime(最大実行継続可能時間)を設定
	 * @param timeInMillis 最大実行継続可能時間(ミリ秒)
	 */
	public void setMaxRunTime(long timeInMillis) {
		logger.trace("JobClass.setMaxRunTime(" + timeInMillis + ")");
		this.maxRunTime = timeInMillis < 0 ? Long.MAX_VALUE : timeInMillis;
	}
	
	/**
	 * 最大同時実行数をセット
	 * @param num 実行数
	 */
	public void setConcurrency(int num) {
		logger.trace("JobClass.setNumConcurrency(" + num + ")");
		this.numConcurrency = num;
	}

	/**
	 * ジョブクラスにジョブを追加
	 * @param key ジョブキー
	 */
	public void addJob(JobKey key) {
		logger.trace("JobClass.addJob(" + key.toString() + ")");
		try {
			assignedList.add(key);
			scheduler.getListenerManager().addJobListenerMatcher(name, KeyMatcher.keyEquals((key)));
		} catch (SchedulerException e) {
			logger.info("failed to add job: " + key.toString());
			logger.trace("", e);
		}
	}

	/**
	 * ジョブクラスからジョブを削除
	 * @param key
	 */
	public void deleteJob(JobKey key) {
		logger.trace("JobClass.deleteJob(" + key.toString() + ")");
		try {
			scheduler.getListenerManager().removeJobListenerMatcher(name, KeyMatcher.keyEquals(key));
			assignedList.remove(key);
		} catch (SchedulerException e) {
			logger.info("failed to delete job: " + key.toString());
			logger.trace("", e);
		}
	}
	
	/**
	 * 所属リストを取得
	 * @return
	 */
	public List<JobKey> getAssignedList() {
		logger.trace("JobClassImpl.getJobList()");
		return assignedList;
	}
	
	/**
	 * ジョブクラス名を取得
	 * @return ジョブクラス名
	 */
	public String getName() {
		logger.trace("JobClass.getName()");
		return name;
	}
	
	/**
	 * 最大実行継続可能時間を過ぎたジョブを停止させる為のジョブ
	 * @author kurohara
	 *
	 */
	public static final class RuntimeLimitterJob implements InterruptableJob {
		private Logger logger = Logger.getLogger(Activator.LOGGERNAME);
		public static final String KEY_JOBKEY = "jobkey";
		
		public RuntimeLimitterJob() {
			logger.trace("RuntimeLimitterJob.RuntimeLimitterJob()");
		}
		
		public void execute(JobExecutionContext context)
				throws JobExecutionException {
			logger.trace("RuntimeLimitterJob.execute()");
			JobKey jobkey = (JobKey) context.getMergedJobDataMap().get(KEY_JOBKEY);
			logger.debug("interrupting job(" + jobkey.getName() + ")");
			try {
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
	 * ジョブクラスに属するすべてのジョブを監視するJobListener
	 * @author kurohara
	 *
	 */
	private class ConcurrencyJobListener implements JobListener {
		
		public ConcurrencyJobListener() {
			logger.trace("ConcurrencyJobListener.ConcurrencyJobListener()");
		}

		public String getName() {
			logger.trace("ConcurrencyJobListener.getName() - '" + name + "'");
			return name;
		}

		public synchronized void jobToBeExecuted(JobExecutionContext context) {
			logger.trace("ConcurrencyJobListener.jobToBeExecuted()");
			Trigger trigger = context.getTrigger();
			JobKey key = trigger.getJobKey();
			JobDetail detail = context.getJobDetail();
			JobDataMap datamap = detail.getJobDataMap();
			logger.trace("    job = " + key.getName());
			@SuppressWarnings("unchecked")
			Set<String> reasons = (Set<String>) datamap.get(Constants.KEY_PROHIBIT);
			if (reasons == null) {
				reasons = new HashSet<String>();
				datamap.put(Constants.KEY_PROHIBIT, reasons);
			}
			// trigger with bigger priority is "re-scheduled" for immediate execution
			if (running.size() >= numConcurrency || (! vetoedQueue.isEmpty() && trigger.getPriority() <= Trigger.DEFAULT_PRIORITY)) {
				VetoedTriggerKey vk = new VetoedTriggerKey(trigger.getKey(), detail);
				logger.trace("offering into vetoedQueue, size:" + vetoedQueue.size());
				vetoedQueue.offer(vk);
				reasons.add(REASONKEY_CONCURRENCY);
				// create result data here if not exist
				JobResultImpl jresult = (JobResultImpl) datamap.get(KEY_JOBRESULT);
				if (jresult == null) {
					jresult = new JobResultImpl(trigger.getJobKey().getName());
				} else {
					// make copy 
					jresult = new JobResultImpl(jresult);
				}
				datamap.put(KEY_JOBRESULT, jresult);
				jresult.setPendedAt(vk.getVetoedDate());
				jresult.putData(Constants.KEY_PENDINGJOBCLASS, name);
				logger.debug("ConcurrencyJobListener: numConcurrency exceeded for job: " + trigger.getJobKey().toString());
				return;
			}
			
			//
			// save key as running job
			running.add(key);
			// clear prohibit reason of concurrency check
			reasons.remove(REASONKEY_CONCURRENCY);
			if (maxRunTime != Long.MAX_VALUE && maxRunTime > 0) {
				logger.trace("insert 'RUNTIMEOVER kill' job");
				// schedule runtime over cancelling job here.
				JobDetail cancelJob = newJob(RuntimeLimitterJob.class)
					    .withIdentity(key.getName() + SUFFIX_CANCELJOB, key.getGroup() + SUFFIX_CANCELJOB)
					    .build();
				cancelJob.getJobDataMap().put(RuntimeLimitterJob.KEY_JOBKEY, key);
				Trigger cancelTrigger = newTrigger()
					    .withIdentity(key.getName() + SUFFIX_CANCELJOB, key.getGroup() + SUFFIX_CANCELJOB)
					    .startAt( DateBuilder.futureDate((int) maxRunTime, IntervalUnit.MILLISECOND) )
					    .forJob(cancelJob)
					    .build();
				try {
					context.getScheduler().scheduleJob(cancelJob, cancelTrigger);
				} catch (SchedulerException e) {
					logger.debug("failed to insert 'RUNTIMEOVER kill' job");
				}
			}
		}

		public void jobExecutionVetoed(JobExecutionContext context) {
			logger.trace("ConcurrencyJobListener.jobExecutionVetoed()");
		}

		public synchronized void jobWasExecuted(JobExecutionContext context,
				JobExecutionException jobException) {
			logger.trace("ConcurrencyJobListener.jobWasExecuted()");
			Scheduler sched = context.getScheduler();

			try {
				sched.pauseAll();
			} catch (SchedulerException e1) {
				logger.debug("scheduler.pauseAll() failed");
				logger.trace("", e1);
			}

			try {
				// remove RuntimeLimitter job
				sched.deleteJob(jobKey(context.getJobDetail().getKey().getName() + SUFFIX_CANCELJOB, context.getJobDetail().getKey().getGroup() + SUFFIX_CANCELJOB));
			} catch (SchedulerException e) {
				logger.debug("failed to delete RuntimeLimitter job for ("+context.getJobDetail().getKey().getName() +"), may already removed");
			}
			try {
				// 
				if (running.remove(context.getTrigger().getJobKey())) {
					//
					// remove oldest element from vetoed queue, then check if it can be re-scheduled.
					// if it can not be, throw it away.
					// 
					while (true) {
						VetoedTriggerKey vetoedkey = vetoedQueue.poll();
						if (vetoedkey != null) {
							try {
								JobDetail job = vetoedkey.getJobDetail();
								// re-schedule with "run immediately trigger" if it can proceed.
								if (vetoedkey.canProceed(waitTime)) {
									logger.trace("re-launching the waiting job: " + vetoedkey.getJobDetail().getKey().toString()+ ":" + vetoedkey.getKey().toString());
									// check if job was durable
									if (sched.checkExists(job.getKey())) {
										Trigger oldTrigger = sched.getTrigger(vetoedkey.getKey());
										Trigger rft = newTrigger()
												.startNow()
												// to distinguish re-fire trigger, set priority to DEFAULT + 1
												.withPriority(Trigger.DEFAULT_PRIORITY + 1)
												.forJob(job)
												.build();
										if (oldTrigger != null && ! oldTrigger.mayFireAgain()) {
											sched.rescheduleJob(vetoedkey.getKey(), rft);
											logger.trace("rescheduleJob(" + vetoedkey.getKey().toString() + ":" + rft.getKey().toString());
										}  else {
											sched.scheduleJob(rft);
											logger.trace("scheduleJob(" + job.getKey().toString() + ":" + rft.getKey().toString());
										}
									} else {
										Trigger rft = newTrigger()
												.startNow()
												// to distinguish re-fire trigger, set priority to DEFAULT + 1
												.withPriority(Trigger.DEFAULT_PRIORITY + 1)
												.forJob(job.getKey())
												.build();
										sched.scheduleJob(job, rft);
										logger.trace("scheduleJob(" + job.getKey().toString() + ":" + rft.getKey().toString());
									}
									if (logger.isTraceEnabled()) {
										Trigger rft = sched.getTrigger(vetoedkey.getKey());
										logger.trace(
												"trigger is updated for re-launch: " + vetoedkey.getKey().toString() + 
												" { nextfiretime : " + rft.getNextFireTime() + 
												", previousfiretime : " + rft.getPreviousFireTime() + "}");
									}
									break;
								} else {
									expiredWaitTime(vetoedkey);
								}
							} catch (SchedulerException e) {
								logger.info("failed to re-schedule vetoed job: " + vetoedkey.toString());
								logger.trace("", e);
							} catch (Throwable t) {
								logger.info("Something has been thrown at post processing of job execution.", t);
							}
						} else {
							break;
						}
					}
				}
			} catch (Throwable t) {
				logger.info("Something has been thrown at post processing of job execution.", t);
			} finally {
				try {
					sched.resumeAll();
				} catch (SchedulerException e) {
					logger.info("failed to resume scheduler");
					logger.trace("", e);
				}
				logger.trace("schduler resumed");
			}
		}
	}

	private void expiredWaitTime(VetoedTriggerKey vetoedkey) {
		// wait time is over, giving up, throw it away.
		logger.info("waitTime exceeded for job:" + vetoedkey.getKey().toString() + ", giving up");
		JobReceiverImpl receiver = (JobReceiverImpl) jobFacade.getJobReceiver(vetoedkey.getKey().getName());
		if (receiver != null) {
			receiver.putEvent(vetoedkey.getKey().getName(), TRIGGEREVENT.MISFIRED, new Date());
			int ilast = receiver.resultCount() - 1;
			if (ilast >= 0) {
				JobResultImpl result = (JobResultImpl) receiver.getJobResult(ilast);
				result.setWaitexpiredAt(new Date());
			}
		}
	}
	
	private void vetoedCancelled(VetoedTriggerKey vetoedkey) {
		// wait time is over, giving up, throw it away.
		logger.info("cancelled wainting job:" + vetoedkey.getKey().toString() + "");
		JobReceiverImpl receiver = (JobReceiverImpl) jobFacade.getJobReceiver(vetoedkey.getKey().getName());
		receiver.putEvent(vetoedkey.getKey().getName(), TRIGGEREVENT.MISFIRED, new Date());
		int ilast = receiver.resultCount() - 1;
		if (ilast >= 0) {
			JobResultImpl result = (JobResultImpl) receiver.getJobResult(ilast);
			result.setWaitexpiredAt(new Date());
		}
	}
	
	public int getConcurrency() {
		return numConcurrency;
	}

	public long getMaxWaitTime() {
		return waitTime;
	}

	public long getMaxRunTime() {
		return maxRunTime;
	}
	
	public Object [] getVetoedQueue() {
		return vetoedQueue.toArray();
	}
	
	public synchronized void removeAllExpiredJob() {
		BlockingQueue<VetoedTriggerKey> tmpqueue = new LinkedBlockingQueue<VetoedTriggerKey>();
		while (! vetoedQueue.isEmpty()) {
			VetoedTriggerKey vetoedkey;
			try {
				vetoedkey = vetoedQueue.take();
				if ((new Date()).getTime() - vetoedkey.getVetoedDate().getTime() > this.waitTime) {
					// timeout expired
					expiredWaitTime(vetoedkey);
				} else {
					tmpqueue.add(vetoedkey);
				}
			} catch (InterruptedException e) {
			}
		}
		// copy back to primary VetoedList
		vetoedQueue.clear();
		vetoedQueue.addAll(tmpqueue);
	}

	public synchronized void cancelSpecifiedVetoedJob(JobKey jk) {
		BlockingQueue<VetoedTriggerKey> tmpqueue = new LinkedBlockingQueue<VetoedTriggerKey>();
		while (! vetoedQueue.isEmpty()) {
			VetoedTriggerKey vetoedkey;
			try {
				vetoedkey = vetoedQueue.take();
				
				if (vetoedkey.getJobDetail().getKey().equals(jk)) {
					// cancelled
					vetoedCancelled(vetoedkey);
//					expiredWaitTime(vetoedkey);
				} else {
					tmpqueue.add(vetoedkey);
				}
			} catch (InterruptedException e) {
			}
		}
		// copy back to primary VetoedList
		vetoedQueue.clear();
		vetoedQueue.addAll(tmpqueue);
	}
	
	public int getCurrentRunningCount() {
		return running.size();
	}

	public List<JobKey> getVetoedList() {
		List<JobKey> rlist = new ArrayList<JobKey>();
		int qsize = vetoedQueue.size();
		VetoedTriggerKey [] varr = new VetoedTriggerKey [qsize];
		vetoedQueue.toArray(varr);
		for (int i = 0;i < varr.length;++i) {
			JobDetail job = null;
			try {
				job = varr[i].getJobDetail();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (job != null) {
				rlist.add(job.getKey());
			}
		}
		return rlist;
	}
	
	public boolean isJobVetoed(JobKey jobkey) {
		List<JobKey> klist = getVetoedList();
		try {
			for (JobKey k : klist) {
				if (k.equals(jobkey)) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
