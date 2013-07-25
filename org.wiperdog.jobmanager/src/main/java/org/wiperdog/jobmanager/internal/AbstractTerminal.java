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

import org.apache.log4j.Logger;
import org.quartz.JobKey;
import org.wiperdog.jobmanager.ConditionBoardException;
import org.wiperdog.jobmanager.Flow;
import org.wiperdog.jobmanager.Predecessor;
import org.wiperdog.jobmanager.Terminal;


/**
 * 
 * @author kurohara
 *
 */
public abstract class AbstractTerminal implements Terminal {
	protected final JobKey jobkey;
	protected final String id;
	private final Flow inflow;
	private String alias;
	private final Logger logger = Logger.getLogger(Activator.LOGGERNAME);

	public String toString() {
		return this.getClass().getSimpleName() + "(" + id + "): inflow : " + (inflow == null ? "null" : inflow.getId());
	}
	
	abstract protected void update(boolean v);

	protected AbstractTerminal(String name, JobKey jobkey) {
		String idname = name;
		inflow = new InFlow();
		if (name == null || name.length() == 0) {
			idname = "_Terminal_for_" + jobkey.getName();
		}
		logger.trace("AbstractTerminal.AbstractTerminal(" + idname + "," + jobkey.toString() + ")");
		this.id = idname;
		this.jobkey = jobkey;
	}
	
	public void setAlias(String alias) {
		logger.trace("AbstractTerminal.setAlias(" + alias + ")");
		this.alias = alias;
	}
	
	public String getAlias() {
		logger.trace("AbstractTerminal.getAlias()");
		return alias;
	}
	
	public String getId() {
		logger.trace("AbstractTerminal.getId()");
		return id;
	}
	
	public String getName() {
		return id;
	}
	
	public JobKey getJobKey() {
		return jobkey;
	}
	
	public void connectUpperFlow(Predecessor f) throws ConditionBoardException {
		logger.trace("AbstractTerminal.connectUpperFlow(" + f.toString() + ")");
		f.addOutFlow(inflow);
	}

	public void disconnectUpperFlow(Predecessor p)
			throws ConditionBoardException {
		logger.trace("AbstractTerminal.disconnectUpperFlow(" + p.toString() + ")");
		p.deleteOutFlow(inflow);
	}
	
	/**
	 * 
	 * @author kurohara
	 *
	 */
	private final class InFlow implements Flow {

		public InFlow() {
			logger.trace("AbstractTerminal.InFlow.InFlow()");
		}
		public void call(boolean v) {
			logger.trace("AbstractTerminal.InFlow.call(" + v + ")");
			update(v);
		}
		
		public String getId() {
			logger.trace("AbstractTerminal.InFlow.getId()");
			return AbstractTerminal.this.id;
		}
	}

}
