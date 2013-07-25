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

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.PropertySettingJobFactory;
import org.wiperdog.jobmanager.JobClass;
import org.wiperdog.jobmanager.JobFacade;
import org.wiperdog.jobmanager.JobManagerException;
import org.wiperdog.jobmanager.JobResult;
import org.wiperdog.jobmanager.dsl.Parser;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

import org.wiperdog.rshell.api.RShellProvider;

public class Activator implements BundleActivator {
	public static final String PID = "com.insight_tec.pi.jobmanager";
	public static final String LOGGERNAME = PID;
	public static final String KEY_MAXRECEIVESIZE = "shell.maxreceivesize";
	public static final String KEY_MAXHISTORYDEPTH = "maxhistorydepth";
	private BundleContext context;

	private Logger logger = Logger.getLogger(Activator.class);

	private class Config implements ManagedService {
		public void updated(@SuppressWarnings("rawtypes") Dictionary properties)
				throws ConfigurationException {
			if (properties != null) {
				String strMaxReceiveSize = (String) properties.get(KEY_MAXRECEIVESIZE);
				try {
					((JobFacadeImpl)jf).setMaxReceiveSize(Integer.parseInt(strMaxReceiveSize));
				} catch (NumberFormatException e) {
					logger.trace("invalied number fomat in property(" + KEY_MAXRECEIVESIZE + ")");
				}
				String strMaxHistory = (String) properties.get(KEY_MAXHISTORYDEPTH);
				try {
					((JobFacadeImpl)jf).setMaxHistoryDepth(Integer.parseInt(strMaxHistory));
				} catch (NumberFormatException e) {
					
				}
			}
		}
	}
	
	/**
	 * command implementation for felix Gogo shell.
	 * @author kurohara
	 *
	 */
	private class GogoCommand {
		private void showJobDataMap(String indent, JobDataMap datamap) {
			Set<Map.Entry<String, Object>> entries = datamap.entrySet();
			for (Map.Entry<String, Object> entry : entries) {
				if (entry.getValue() instanceof String []) {
					System.out.println(indent + entry.getKey() + ":");
					for (String e : (String[]) entry.getValue()) {
						System.out.println(indent + "  " + e);
					}
				} else {
					System.out.println(indent + entry.getKey() + ":" + entry.getValue().toString());
				}
			}
		}
		
		/**
		 * load command
		 * @param args
		 */
		public void load(String [] args) {
			Parser parser = new Parser(jf, false);
			if (args.length > 0) {
				String filepath;
				if (args[0].startsWith("/")) {
					filepath = args[0];
				} else {
					filepath = System.getProperty("felix.home");
					if (filepath == null || filepath.length() == 0) {
						filepath = System.getProperty("user.dir");
					}
					filepath += "/" + args[0];
				}
				File fin = new File(filepath);
				if (fin.isFile() && fin.canRead()) {
					try {
						parser.parse(fin);
						System.out.println("loaded '" + fin.getAbsolutePath() + "'");
					} catch (IOException e) {
						System.out.println("IO error '" + fin.getAbsolutePath() + "'");
						logger.warn("", e);
					} catch (SAXException e) {
						System.out.println("xml syntax error " + fin.getAbsolutePath() + "'");
						logger.warn("", e);
					}
				} else {
					System.out.println("file is not readable or does't exist '" + fin.getAbsolutePath() + "'");
				}
			} else {
				System.out.println("need file path argument");
			}
		}

		/**
		 * list command
		 * @param args
		 */
		public void list(String [] args) {
			Set<String> keys = jf.keySetJob();
			System.out.println("Job list - ");
			for (String name : keys) {
				JobDetail job = null;
				try {
					job = jf.getJob(name);
				} catch (JobManagerException e) {
				}
				JobDataMap datamap = job.getJobDataMap();
				System.out.println("    " + name);
				System.out.println("        " + job.getJobClass().getClass().getSimpleName());
				showJobDataMap("        ", datamap);
				List<JobResult> results = jf.getJobResult(name);
				if (results != null) {
					System.out.println("        executeion results: ");
					for (JobResult r : results) {
						if (r != null) {
							System.out.println("");
							System.out.println("            result: " + (r.getResult() != null ? r.getResult().toString() : "null"));
							System.out.println("            started: " + r.getStartedAt());
							System.out.println("            Interrupted: " + r.getInterruptedAt());
							System.out.println("            ended: " + r.getEndedAt());
							System.out.println("            pended: " + r.getPendedAt());
							System.out.println("            message: " + r.getMessage());
						}
					}
				}
			}
			System.out.println("Trigger list - ");
			Set<TriggerKey> tkeys = jf.getTriggerKeys();
			for (TriggerKey key : tkeys) {
				Trigger t = jf.getTrigger(key);
				System.out.println("    " + t.getKey().toString());
				System.out.println("        startTime: " + t.getStartTime());
				System.out.println("        previousFireTime: " + t.getPreviousFireTime());
				System.out.println("        nextFireTime: " + t.getNextFireTime());
			}
			System.out.println("Class list - ");
			keys = jf.keySetClass();
			for (String name : keys) {
				 JobClass c = jf.getJobClass(name);
				 System.out.println("    " + name);
				 System.out.println("        concurrency: " + c.getConcurrency());
				 System.out.println("        maxWaitTime: " + c.getMaxWaitTime());
				 System.out.println("        maxRunTime: " + c.getMaxRunTime());
				 Object [] vq = ((JobClassImpl)c).getVetoedQueue();
				 System.out.println("        vetoed queue:");
				 for (Object o : vq) {
					 System.out.println("          " + o.toString());
				 }
			}
		}
	}

	/**
	 * CommanderJobにはCommanderServiceが必要
	 * @author kurohara
	 *
	 */
	private class CommanderServiceTrackerCustomizer implements ServiceTrackerCustomizer {

		public Object addingService(ServiceReference reference) {
			Object svc = context.getService(reference);
			((JobFacadeImpl)jf).setCommander((RShellProvider) svc);
			return svc;
		}

		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void start(BundleContext context) throws Exception {
		this.context = context;
		initSchedulerCore();
		
		context.registerService(JobFacade.class.getName(), jf, null);
		
		// register felix Gogo shell command
		@SuppressWarnings("rawtypes")
		Dictionary props = new Hashtable();
		props.put("osgi.command.scope", "scheduler");
		props.put("osgi.command.function", new String [] {"load", "list"});
		context.registerService(GogoCommand.class.getName(), new GogoCommand(), props);
		
		// JobFacadeImplにCommanderServiceをセットするため。
		ServiceTracker tracker = new ServiceTracker(context, RShellProvider.class.getName(), new CommanderServiceTrackerCustomizer());
		tracker.open();
		// Config を登録
		props = new Hashtable();
		props.put(Constants.SERVICE_PID, PID);
		context.registerService(ManagedService.class.getName(), new Config(), props);
	}
	
	public void stop(BundleContext context) throws Exception {
		finishSchedulerCore();
	}

	private Scheduler scheduler;
	private SchedulerFactory sf;

	private JobFacade jf;
	
	private void initSchedulerCore() {
		sf = new StdSchedulerFactory();
		try {
			scheduler = sf.getScheduler();
			PropertySettingJobFactory jfactory = new PropertySettingJobFactory();
			jfactory.setWarnIfPropertyNotFound(false);
			scheduler.setJobFactory(jfactory);
			jf = new JobFacadeImpl(scheduler);
			scheduler.start();
		} catch (SchedulerException e) {
			logger.trace("", e);
		} catch (JobManagerException e) {
			logger.trace("", e);
		}
	}
	
	private void finishSchedulerCore() {
		try {
			jf = null;
			scheduler.shutdown();
			scheduler = null;
			sf = null;
		} catch (SchedulerException e) {
			logger.trace("", e);
		}
	}

}
