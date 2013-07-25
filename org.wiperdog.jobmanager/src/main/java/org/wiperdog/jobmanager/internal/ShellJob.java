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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.wiperdog.jobmanager.Constants;


@DisallowConcurrentExecution
public class ShellJob extends AbstractGenericJob {
	public static final int DEF_MAX_DATA_SIZE = 4096;
	private String [] programargs;
	private Boolean bUseOut = false;
	private Boolean bUseErr = false;
	private int maxDataSize = DEF_MAX_DATA_SIZE; // max hold size
	
	public ShellJob() {
		logger.trace("ShellJob.ShellJob()");
	}
	
	private void close(Closeable s) {
		logger.trace("ShellJob.close(" + s.toString() + ")");
		try {
			s.close();
		} catch (IOException e) {
			logger.info("failed to close closeable object");
			logger.trace(e);
		}
	}

	private String getWorkingDirectory() {
		String wd = System.getProperty("felix.home");
		if (wd == null || wd.length() == 0) {
			wd = System.getProperty("user.dir");
		}
		return wd;
	}
	
	/**
	 * 
	 * @author kurohara
	 *
	 */
	private class OutputProcessor implements Runnable {
		private boolean bRun = true;
		private final InputStream is;
		private Thread me;
		private String result = "";
		private final boolean bHoldData;

		public OutputProcessor(InputStream in, boolean bHoldData) {
			logger.trace("OutputProcessor.OutputProcessor(" + in.toString() + ")");
			this.is = in;
			this.bHoldData = bHoldData;
		}
		
		public void start() {
			logger.trace("OutputProcessor.start()");
			me = new Thread(this);
			me.start();
		}
		
		public void stop() {
			logger.trace("OutputProcessor.stop()");
			bRun = false;
			close(is);
		}

		public String getResult() {
			logger.trace("OutputProcessor.getResult()");
			return result;
		}
		
		public void run() {
			logger.trace("OutputProcessor.run()");
			InputStreamReader r = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(r);
			int datasize = 0;
			
			while (bRun) {
				String strIn = null;
				try {
					strIn = br.readLine();
				} catch (IOException e) {
				}
				if (strIn == null) {
					bRun = false;
				} else {
					if (bHoldData) {
						String stringToAppend = strIn;
						if (datasize + strIn.length() + 1 > maxDataSize) {
							if (strIn.length() > maxDataSize - datasize - 1) {
								stringToAppend = strIn.substring(0, maxDataSize - datasize - 1);
							}
							bRun = false;
						}
						result += stringToAppend +"\n";
						datasize += stringToAppend.length() + 1;
					}
				}
			}
			close(r);
			close(br);
		}
		
	}
	
	public void setProgramargs(String [] args) {
		programargs = args;
	}
	
	/**
	 * This method is called by Queartz framework, so don't worry about no one in this java project calling this method.
	 * @param bUse
	 */
	public void setUseOut(Boolean bUse) {
		bUseOut = bUse;
	}
	
	/**
	 * This method is called by Queartz framework, so don't worry about no one in this java project calling this method.
	 * @param bUse
	 */
	public void setUseErr(Boolean bUse) {
		bUseErr = bUse;
	}

	public void setMaxDataSize(Integer size) {
		maxDataSize = size;
	}
	
	protected Object doJob(JobExecutionContext context) throws Throwable {
		logger.trace("ShellJob.doJob()");
		JobDataMap data = context.getJobDetail().getJobDataMap();
//		String [] args = (String[]) data.get(Constants.KEY_PROGRAMARGS);
		ProcessBuilder builder = new ProcessBuilder();
		if (! programargs[0].startsWith(".") && ! programargs[0].startsWith("/")) {
			programargs[0] = getWorkingDirectory() + "/" + programargs[0];
		}
		builder.command(programargs);
		
		java.lang.Process p = builder.start();
		OutputProcessor rin = new OutputProcessor(p.getInputStream(), bUseOut);
		rin.start();
		OutputProcessor rerr = new OutputProcessor(p.getErrorStream(), bUseErr);
		rerr.start();

		int rv = 1;
		int r = 1;
		try {
			rv = p.waitFor();
		} catch (InterruptedException e) {
			logger.trace("interrupted, destroying process then rethrow");
			p.destroy();
			throw e;
		} finally {
		
			rin.stop();
			rerr.stop();
			close(p.getOutputStream());

			try {
				r = p.exitValue();
			} catch (IllegalThreadStateException e) {
				logger.debug("process is not exited yet");
			}
			logger.trace("exit value of process is :" + r);
			data.put(Constants.KEY_STDOUT, rin.getResult());
			data.put(Constants.KEY_STDERR, rerr.getResult());
			data.put(Constants.KEY_EXITCODE, Integer.valueOf(r));
		}
		return r == 0 ? Boolean.TRUE : Boolean.FALSE;
	}

}
