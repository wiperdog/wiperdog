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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.KeyMatcher;
import org.wiperdog.jobmanager.ConditionBoardException;
import org.wiperdog.jobmanager.Flow;
import org.wiperdog.jobmanager.JobPatchBoard;
import org.wiperdog.jobmanager.JobReceiver;
import org.wiperdog.jobmanager.JobResult;
import org.wiperdog.jobmanager.JobResultSource;
import org.wiperdog.jobmanager.Node;
import org.wiperdog.jobmanager.Receiver;
import org.wiperdog.jobmanager.RootJobReceiver;
import org.wiperdog.jobmanager.TriggerReceiver;
import org.wiperdog.jobmanager.JobResult.JOBSTATUS;


import static org.wiperdog.jobmanager.Constants.*;

/**
 * Receiver for a Job.
 * @author kurohara
 *
 */
public final class JobReceiverImpl implements JobReceiver, RootJobReceiver {
	private final JobKey jobkey;
	private final String id = "FOLLOWER_" + JobPatchBoard.getSequenceNumber();
	private final List<Flow> outList = new ArrayList<Flow>();
	private final List<JobResult> resultList = new ArrayList<JobResult>();
	private int historySize = 10;
	private boolean pval = false;
	private Logger logger = Logger.getLogger(Activator.LOGGERNAME);
	private PseudoJobReceiver intReceiver = new PseudoJobReceiver(JOBSUFFIX_INTERRUPTED);
	private PseudoJobReceiver misReceiver = new PseudoJobReceiver(JOBSUFFIX_MISFIRED);
	private PseudoJobReceiver outPatternReceiver = new PseudoJobReceiver(JOBSUFFIX_OUTPATTERN);
	private PseudoJobReceiver errPatternReceiver = new PseudoJobReceiver(JOBSUFFIX_ERRPATTERN);
	private RootJobReceiver.ASSIGNMENT interruptedAssignment = RootJobReceiver.ASSIGNMENT.CONVERT_FALSE;
	private RootJobReceiver.ASSIGNMENT misfiredAssignment = RootJobReceiver.ASSIGNMENT.VOID;
	private RootJobReceiver.ASSIGNMENT outPatternAssignment  = RootJobReceiver.ASSIGNMENT.VOID;
	private RootJobReceiver.ASSIGNMENT errPatternAssignment  = RootJobReceiver.ASSIGNMENT.VOID;

	private Pattern outPattern;
	private Pattern errPattern;
	
	/**
	 * JobReceiver for pseudo job receiver, sharing JobListener with JobReceiverImpl.
	 * @author kurohara
	 *
	 */
	private class PseudoJobReceiver implements JobReceiver {
		private final List<Flow> outList = new ArrayList<Flow>();
		private boolean pvalue = false;
		private final String nameSuffix;
		
		public PseudoJobReceiver(String suffix) {
			nameSuffix = suffix;
		}
		
		public JobKey getJobKey() {
			return jobkey;
		}

		public void addOutFlow(Flow f) throws ConditionBoardException {
			outList.add(f);
		}

		public void deleteOutFlow(Flow f) throws ConditionBoardException {
			outList.remove(f);
		}

		public Flow[] getOutFlows() {
			Flow [] oa = new Flow[outList.size()];
			return outList.toArray(oa);
		}

		public int resultCount() {
			return JobReceiverImpl.this.resultCount();
		}

		public JobResult getJobResult(int index) {
			return JobReceiverImpl.this.getJobResult(index);
		}

		public List<JobResult> getJobResultList() {
			return JobReceiverImpl.this.getJobResultList();
		}

		public String getName() {
			return JobReceiverImpl.this.getName() + nameSuffix;
		}

		public boolean getPValue() {
			return pvalue;
		}


		public void putEvent(String name, TRIGGEREVENT event, Date date) {
			// TODO Auto-generated method stub
			
		}
		
		public void call(boolean v) {
			logger.trace(this.getName() + ".call(" + v + ")");
			logger.trace(" outList has " + outList.size() + " followers");
			pvalue = v;
			for (Flow f : outList) {
				f.call(v);
			}
		}
	}
	
	public String toString() {
		String str = getClass().getSimpleName() + "(" + jobkey.getName() + "), outlist: {";
		for (Flow f : outList) {
			str += f.getId() + ",";
		}
		str += "}";
		
		return str;
	}
	
	public JobReceiverImpl(Scheduler sched, JobKey jobkey) throws SchedulerException {
		this.jobkey = jobkey;
		sched.getListenerManager().addJobListener(new NodeJobListener(), KeyMatcher.keyEquals(jobkey));
	}

	public JobReceiverImpl(Scheduler sched, JobKey jobkey, int maxHistory) throws SchedulerException {
		this.jobkey = jobkey;
		sched.getListenerManager().addJobListener(new NodeJobListener(), KeyMatcher.keyEquals(jobkey));
		this.historySize = maxHistory;
	}

	public void setHistorySize(int size) {
		this.historySize = size;
	}
	
	public int getHistorySize() {
		return historySize;
	}
	
	public JobKey getJobKey() {

		return jobkey;
	}
	
	public void addOutFlow(Flow f) {
		outList.add(f);
	}
	
	public void deleteOutFlow(Flow f) {
		outList.remove(f);
	}
	
	private void output(boolean v) {
		for (Flow f : outList) {
			f.call(v);
		}
	}
	
	private void pushJobResult(JobResult jr) {
		resultList.add(jr);
		if (resultList.size() > historySize) {
			resultList.remove(0);
		}
	}
	
	/**
	 * 
	 * @author kurohara
	 *
	 */
	private class NodeJobListener implements JobListener {

		public String getName() {
			return id;
		}

		public void jobToBeExecuted(JobExecutionContext context) {
		}

		public void jobExecutionVetoed(JobExecutionContext context) {
		}

		public void jobWasExecuted(JobExecutionContext context,
				JobExecutionException jobException) {
			// check if job was really executed
			//  neglect the job that has not executed
			JobDataMap datamap = context.getJobDetail().getJobDataMap();
			@SuppressWarnings("unchecked")
			Set<String> p = (Set<String>) datamap.get(KEY_PROHIBIT);
			// output result if this job was not prohibited
			if (p == null || p.size() == 0) {
				// check result 
				boolean v = true;
				
				if (jobException != null) {
					v = false;
				} else {
					try {
						Boolean r = (Boolean) context.getResult();
						if (r == null || !r) {
							v = false;
						}
					} catch (ClassCastException e) {
						v = false;
					}
					if (v) {
						try {
							Boolean b = (Boolean) context.getMergedJobDataMap().get(Receiver.KEY_JOBEXECUTIONFAILED);
							if (b != null && b) {
								v = false;
							}
						} catch (ClassCastException e) {
							v = false;
						}
					}
				}

				
				JobResultImpl jr = (JobResultImpl) datamap.get(KEY_JOBRESULT);
				if (jr == null) {
					// something is wrong in LOGIC.
					logger.warn("Somethins is wrong in logic");
					return;
				}
				logger.trace("jobresult.started: " + jr.getStartedAt());
				logger.trace("jobresult.ended: " + jr.getEndedAt());
				logger.trace("jobresult.interrupted: " + jr.getInterruptedAt());
				logger.trace("jobresult.getLastStatus() = " +jr.getLastStatus().name());
	
				pushJobResult(jr);
				if (jr.getLastStatus() == JOBSTATUS.INTERRUPTED) {
					if (interruptedAssignment == RootJobReceiver.ASSIGNMENT.CONVERT_FALSE) {
						v = false;
					} else if (interruptedAssignment == RootJobReceiver.ASSIGNMENT.CONVERT_TRUE) {
						v = true;
					} else if (interruptedAssignment == RootJobReceiver.ASSIGNMENT.PSEUDOJOB) {
						intReceiver.call(true);
						// it was interrupted and no conversion made, so job has no result. 
						// return immediately.
						return;
					} else {
						return;
					}
				} else {
					if (interruptedAssignment == RootJobReceiver.ASSIGNMENT.PSEUDOJOB) {
						intReceiver.call(false);
					}
				}
				
				//
				// 以下の処理はShell系ジョブ特有のものだが、それ以外のジョブも恩恵に預かることができる（今はShell系だけ）。
				//
				String strStdout = (String) datamap.get(KEY_STDOUT);
				String strStderr = (String) datamap.get(KEY_STDERR);
				Integer exitCode = (Integer) datamap.get(KEY_EXITCODE);
				// it is ok to set values after pushJobResult()
				jr.putData(KEY_STDOUT, strStdout);
				jr.putData(KEY_STDERR, strStderr);
				jr.putData(KEY_EXITCODE, exitCode);
				//
				boolean bOutMatched = false;
				if (strStdout != null && outPattern != null) {
					logger.trace("check stdout match: " + outPattern.pattern() + ":" + strStdout);
					bOutMatched = outPattern.matcher(new StringBuffer(strStdout)).matches();
				}
				boolean bErrMatched = false;
				if (strStderr != null && errPattern != null) {
					logger.trace("check stderr match: " + errPattern.pattern() + ":" + strStderr);
					bErrMatched = errPattern.matcher(new StringBuffer(strStderr)).matches();
				}

				if (bOutMatched) {
					if (outPatternAssignment == ASSIGNMENT.CONVERT_FALSE) {
						// reverse matched result
						v = false;
					} else if (outPatternAssignment == ASSIGNMENT.CONVERT_TRUE) {
						v = true;
					} else if (outPatternAssignment == ASSIGNMENT.PSEUDOJOB) {
						outPatternReceiver.call(true);
					}
				} else {
					if (outPatternAssignment == ASSIGNMENT.PSEUDOJOB) {
						outPatternReceiver.call(false);
					}
				}
				
				if (bErrMatched) {
					if (errPatternAssignment == ASSIGNMENT.CONVERT_FALSE) {
						// reverse matched result;
						v = false;
					} else if (errPatternAssignment == ASSIGNMENT.CONVERT_TRUE) {
						v = true;
					} else if (errPatternAssignment == ASSIGNMENT.PSEUDOJOB) {
						errPatternReceiver.call(true);
					}
				} else {
					if (errPatternAssignment == ASSIGNMENT.PSEUDOJOB) {
						errPatternReceiver.call(false);
					}
				}
				
				pval = v;
				output(v);
			} else {
				// job execution was prohibited in some reason
				// may have some result anyway
				JobResultImpl jr = (JobResultImpl) datamap.get(KEY_JOBRESULT);
				if (jr == null) {
					// something is wrong in LOGIC.
					logger.warn("Somethins is wrong in logic");
					return;
				}
				logger.trace("jobresult.started: " + jr.getStartedAt());
				logger.trace("jobresult.ended: " + jr.getEndedAt());
				logger.trace("jobresult.interrupted: " + jr.getInterruptedAt());
				logger.trace("jobresult.getLastStatus() = " +jr.getLastStatus().name());
	
				pushJobResult(jr);

			}
		}
		
	}

	public Flow[] getOutFlows() {
		Flow [] ofs = new Flow[outList.size()];
		outList.toArray(ofs);
		return ofs;
	}

	public int resultCount() {
		return resultList.size();
	}

	public List<JobResult> getJobResultList() {
		return resultList;
	}

	public JobResult getJobResult(int index) {
		return resultList.get(index);
	}

	public String getName() {
		return id;
	}

	public boolean getPValue() {
		return pval;
	}
	
	public void putEvent(String name, TRIGGEREVENT event, Date date) {
		// convert to JobResult
		JobResultImpl jr = new JobResultImpl(jobkey.getName());
		switch (event) {
		case MISFIRED:
			jr.setLastStatus(JOBSTATUS.MISFIRED);
			break;
		}
		
		pushJobResult(jr);
		// TODO: do convert "misfired" to job result then call output().
	}
	
	public JobReceiver getInterruptedReceiver() {
		return intReceiver;
	}
	
	public JobReceiver getMisfiredReceiver() {
		return misReceiver;
	}
	
	public JobReceiver getOutPatternReceiver() {
		return this.outPatternReceiver;
	}
	
	public JobReceiver getErrPatternReceiver() {
		return this.errPatternReceiver;
	}
	
	public void setInterruptedAssignement(RootJobReceiver.ASSIGNMENT a) {
		logger.trace(this.getClass().getSimpleName() + ".setInterruptedAssignement(" + a.name() + ")");
		interruptedAssignment = a;
	}
	
	public void setMisfiredAssignment(RootJobReceiver.ASSIGNMENT a) {
		logger.trace(this.getClass().getSimpleName() + ".setMisfiredAssignment(" + a.name() + ")");
		misfiredAssignment = a;
	}
	
	public void setOutPatternAssignment(RootJobReceiver.ASSIGNMENT a) {
		outPatternAssignment = a;
	}
	
	public void setErrPatternAssignment(RootJobReceiver.ASSIGNMENT a) {
		errPatternAssignment = a; 
	}
	
	public void setOutPattern(String pattern) {
		if (pattern == null) {
			outPattern = null;
		} else {
			try {
				outPattern = Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				outPattern = null;
			}
		}
	}
	
	public void setErrPattern(String pattern) {
		if (pattern == null) {
			errPattern = null;
		} else {
			try {
				errPattern = Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				errPattern = null;
			}
		}
	}
	
	/**
	 * Schedulerからのjob実行なしでJobResultを追加する方法。
	 * どうしてもjob実行を経由できないときのみ使用する。
	 * @param jr
	 */
	public void addJobResult(JobResult jr) {
		this.pushJobResult(jr);
	}
}
