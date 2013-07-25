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
import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.JobKey;
import org.wiperdog.jobmanager.Flow;
import org.wiperdog.jobmanager.JobPatchBoard;
import org.wiperdog.jobmanager.Node;
import org.wiperdog.jobmanager.Receiver;


import static org.quartz.JobKey.*;

/**
 * 
 * @author kurohara
 *
 */
public class PseudoReceiver implements Receiver, Node {
	private final String id;
	private boolean pval = false;
	private final List<Flow> outList = new ArrayList<Flow>();
	private static final String PSEUDOGROUPNAME = "PSEUDO";
	private final Logger logger = Logger.getLogger(Activator.LOGGERNAME);

	public PseudoReceiver() {
		logger.trace("PseudoReceiver.PseudoReceiver()");
		id = "FOLLOWER_" + JobPatchBoard.getSequenceNumber();
	}
	
	public PseudoReceiver(String name) {
		logger.trace("PseudoReceiver.PseudoReceiver(" + name + ")");
		id = name;
	}
	
	public JobKey getJobKey() {
		logger.trace("PseudoReceiver.getJobKey()");
		return jobKey(PSEUDOGROUPNAME, id); // no job is associated so return id as jobkey.
	}

	public void addOutFlow(Flow f) {
		logger.trace("PseudoReceiver.addOutFlow(" + f.toString() + ")");
		outList.add(f);
	}

	public void deleteOutFlow(Flow f) {
		logger.trace("PseudoReceiver.deleteOutFlow(" + f.toString() + ")");
		outList.remove(f);
	}

	public void interruptNet(boolean v) {
		logger.trace("PseudoReceiver.interruptNet(" + v + ")");
		pval = v;
		for (Flow f : outList) {
			f.call(v);
		}
	}
	
	public Flow[] getOutFlows() {
		Flow [] ofs = new Flow[outList.size()];
		outList.toArray(ofs);
		return ofs;
	}

	public String getName() {
		return id;
	}

	public boolean getPValue() {
		return pval;
	}
}
