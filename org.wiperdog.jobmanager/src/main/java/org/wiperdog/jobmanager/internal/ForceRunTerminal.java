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

import static org.quartz.TriggerBuilder.newTrigger;

import org.apache.log4j.Logger;
import org.quartz.DateBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.TriggerBuilder;

import static org.quartz.TriggerKey.*;

public class ForceRunTerminal extends AbstractTerminal {
	private final Scheduler scheduler;
	private Logger logger = Logger.getLogger(Activator.LOGGERNAME);
	private long delay  = 0;
	
	public ForceRunTerminal(String name, Scheduler scheduler, JobKey jobkey) {
		super(name, jobkey);
		logger.trace("ForceRunTerminal.ForceRunTerminal(" + scheduler.toString() + "," + jobkey.toString() + ")");
		this.scheduler = scheduler;
	}
	
	public void setDelay(long delay) {
		this.delay = delay;
	}

	@Override
	protected void update(boolean v) {
		logger.trace("ForceRunTerminal[" + jobkey.getName() + "].update(" + v + ")");
		if (v) {
			runImmediate();
		}
	}
	
	private void runImmediate() {
		logger.trace("ForceRunTerminal.runImmediate()");
		Trigger t = null;
		try {
			t = scheduler.getTrigger(triggerKey(id, id + "immediatetrigger"));
			if (t != null) {
				TriggerBuilder tb = t.getTriggerBuilder();
				t = tb
						.forJob(jobkey)
						.startAt(DateBuilder.futureDate((int) delay, IntervalUnit.MILLISECOND))
						.build();
				scheduler.rescheduleJob(triggerKey(id, id + "immediatetrigger"), t);
			} else {
				t = newTrigger()
						.withIdentity(id, id + "immediatetrigger")
						.forJob(jobkey)
						.startAt(DateBuilder.futureDate((int) delay, IntervalUnit.MILLISECOND))
						.build();
				try {
					scheduler.scheduleJob(t);
				} catch (SchedulerException e) {
					logger.debug("scheduleJob() failed", e);
				}
			}
		} catch (SchedulerException e1) {
		}
		
	}

	public boolean getPValue() {
		// TODO Auto-generated method stub
		return false;
	}

}
