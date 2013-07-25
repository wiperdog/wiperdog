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

import java.io.StringWriter;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.wiperdog.jobmanager.Constants;

import org.wiperdog.rshell.api.RShell;
import org.wiperdog.rshell.api.RShellProvider;
import org.wiperdog.rshell.api.StreamInputProcessor;

/**
 * CommanderJob
 *  commander.csvで制御されるshell系ジョブ
 *  
 * @author kurohara
 *
 */
public class CommanderJob extends AbstractGenericJob {
	private RShellProvider commander;
	private String [] programargs;
	private StreamInputProcessor stdOutProcessor;
	private boolean bUseTmpfile = false;
	private String tmpFileSpec = "tmp/commandJobtmp%i";
	private static int iseq = 0;
	
	public CommanderJob() {
	}

	public void setCommander(RShellProvider csvc) {
		commander = csvc;
	}
	
	public void setProgramargs(String [] programargs) {
		this.programargs = programargs;
	}
	
	public void setUseTmpfile(boolean bUse) {
		bUseTmpfile = bUse;
	}
	
	public void setTmpFileSpec(String tmpfileSpec) {
		this.tmpFileSpec = tmpfileSpec;
	}
	
	private String getTmpfilePath() {
		return String.format(tmpFileSpec, Integer.valueOf(iseq));
	}
	
	@Override
	protected Object doJob(JobExecutionContext context) throws Throwable {
		JobDataMap data = context.getJobDetail().getJobDataMap();
		if (commander == null) {
			lastMsg = "no commander service has injected yet";
			logger.warn("no commander service has injected yet");
			return null;
		}
		Map<String,RShell> defmap = commander.getRShellMap();
		RShell def = null;
		if (programargs != null && programargs.length > 0) {
			String jobName = programargs[0];
			def = defmap.get(jobName);
		} else {
			lastMsg = "no programargs given";
			return null;
		}
		if (def != null) {
			Process proc = def.run(programargs);
			final long timeout = def.getTimeout();
			final Thread stopMe = Thread.currentThread();
			stdOutProcessor = StreamInputProcessor.start(
									proc.getInputStream(), 
									bUseTmpfile ? getTmpfilePath() : null, 
									(int) def.getXferLimit());
			// timout waiting thread
			(new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(timeout);
						stopMe.interrupt();
					} catch (InterruptedException e) {
					}
				}
			})).start();
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
				proc.destroy();
			}
			int rc = proc.exitValue();
			stdOutProcessor.stop();
			StringWriter stdoutData = new StringWriter();
			stdOutProcessor.writeTo(stdoutData);
			stdOutProcessor.clear();
			data.put(Constants.KEY_STDOUT, stdoutData.toString());
//			data.put(Constants.KEY_STDERR, stderrData.toString());
			lastMsg = stdoutData.toString();
			return Boolean.valueOf(rc == 0);
		} else {
			// no definition with command name found.
			lastMsg = "no commander definition for '" + programargs[0] + "' found";
		}
		return null;
	}
}
