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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.wiperdog.jobmanager.Flow;
import org.wiperdog.jobmanager.JobFacade;
import org.wiperdog.jobmanager.Operator;
import org.wiperdog.jobmanager.Receiver;
import org.wiperdog.jobmanager.Terminal;
import org.wiperdog.jobmanager.dsl.Parser;


public class AppMain {
	private String dotPath = "jobgraph.dot";
	private PrintWriter pw = null;
	public static final String PID =  "com.insight_tec.pi.jobmanager"; 
	public static final String LOGGERNAME = PID;
	public static final String LOGGERNAME_TRACE = PID + "_trace";
	
	static {
		setupLogger();
    	disableQuartzUpdateCheck();
	}
	
	public static void setupLogger() {
    	Logger logger = Logger.getRootLogger();
    	Layout layout = new SimpleLayout();
    	Appender appender = new ConsoleAppender(layout, "System.err");
    	logger.addAppender(appender);
    	//
    	logger = Logger.getLogger(LOGGERNAME);
    	logger.setLevel(Level.DEBUG);
    	//
    	logger = Logger.getLogger(LOGGERNAME_TRACE);
    	logger.setLevel(Level.TRACE);
    	
	}
	
	public static void disableQuartzUpdateCheck() {
    	System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");
	}
	
	
	public static final void main(String [] args) {
		if (args.length == 0) {
			System.out.println("AppMain filename [timeout_millis]");
			return;
		}
		(new AppMain())._main(args);
	}
	
	private void dotprintln(String str) {
		if (pw != null) {
			pw.println(str);
			pw.flush();
		}
	}

	private void showGraph(JobFacade f, File dotfile) {
		if (dotfile != null) {
			try {
				pw = new PrintWriter(new FileOutputStream(dotfile));
			} catch (FileNotFoundException e) {
			}
		} else {
			pw = null;
		}
		
		dotprintln("digraph {");

		Set<String> netset = f.keySetNet();
		List<String> nets = new ArrayList<String>();
		nets.addAll(netset);
		nets.add("");
		for (String netname : nets) {
			List<Object> elist = null;
			if (netname.length() == 0) {
				elist = f.getNodeList();
			} else {
				elist = f.getNodeList(netname);
			}
			if (elist != null) {
				for (Object o : elist) {
					String grafAttr = "";
					if (o instanceof Operator) {
						Operator op = (Operator) o;
						boolean [] tvs = op.getTValues();
						Flow [] ofs = op.getOutFlows();
						for (Flow flow : ofs) {
							dotprintln(netname + "_" + op.getName() + " -> " + netname + "_" + flow.getId() + ";");
						}
						grafAttr = " [label=\"";
						grafAttr += op.getName() + "\\n(" + op.getClass().getSimpleName() + ")\\n[ ";
						for (boolean b : tvs) {
							grafAttr += b + " ";
						}
						grafAttr += "]\\n" + op.getPValue() + "\"];";
						
						dotprintln(netname + "_" + op.getName() + grafAttr);
					}
					if (o instanceof Terminal) {
						Terminal ot = (Terminal) o;
						grafAttr = " [label=\"";
						grafAttr += ot.getName() + "\\n(" + ot.getClass().getSimpleName() + ")\\n" + ot.getPValue() + "\"];";
						dotprintln(netname + "_" + ot.getName() + grafAttr);
					}
					if (o instanceof Receiver) {
						Receiver or = (Receiver)o;
						Flow [] ofs = or.getOutFlows();
						for (Flow flow : ofs) {
							dotprintln(netname + "_" + or.getJobKey().getName() + " -> " + netname + "_" + flow.getId() + ";");
						}
						grafAttr = " [label=\"";
						grafAttr += or.getJobKey().toString() + "\\n(" + or.getClass().getSimpleName() + ")\"];";
						dotprintln(netname + "_" + or.getJobKey().getName() + grafAttr);
					}
				}
			}
		}
		dotprintln("}");
	}
	
	public void _main(String [] args) {
       SchedulerFactory sf = new StdSchedulerFactory();
       Scheduler scheduler;
		try {
			scheduler = sf.getScheduler();
			JobFacade f = new JobFacadeImpl(scheduler);
			Parser p = new Parser(f, false);
			File df = new File(args[0]);
			if (df.isFile()) {
				p.parse(df);
			} else {
				System.err.println("file not found");
			}

			showGraph(f, new File("jobgraph.dot"));
			
			scheduler.start();

			long waittime = 30000;
			if (args.length > 1) {
				try {
					waittime = Long.parseLong(args[1]);
				} catch (NumberFormatException e) {
					
				}
			}
			Thread.sleep(waittime);
			
			showGraph(f, new File("jobgraph2.dot"));
			
			System.out.println("end of test, shutting down scheduler");
			scheduler.shutdown();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
