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
package org.wiperdog.jobmanager.dsl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.wiperdog.jobmanager.ConditionBoardException;
import org.wiperdog.jobmanager.JobClass;
import org.wiperdog.jobmanager.JobFacade;
import org.wiperdog.jobmanager.JobManagerException;
import org.wiperdog.jobmanager.JobNet;
import org.wiperdog.jobmanager.JobReceiver;
import org.wiperdog.jobmanager.RootJobReceiver;
import org.wiperdog.jobmanager.Terminal;
import org.wiperdog.jobmanager.RootJobReceiver.ASSIGNMENT;
import org.wiperdog.jobmanager.internal.JobReceiverImpl;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class Parser {
	private static Logger logger = Logger.getLogger(Parser.class);
	private static final char ec = '\\';
	public static String [] splitCommandline(String commandLine) {
		String [] result = null;
		StringBuffer sb = new StringBuffer();
		char [] carr = commandLine.toCharArray();
		List<String> array = new ArrayList<String>();
		boolean escape = false;
		boolean dquoting = false;
		boolean squoting = false;
		for (int i = 0;i < carr.length;++i) {
			if (escape) {
				if (carr[i] != '\'' && carr[i] != '"' && carr[i] != ' ' && carr[i] != '\t') {
					sb.append(ec);
				}
				if (carr[i] == ec) {
					escape = true;
				} else {
					sb.append(carr[i]);
					escape = false;
				}
			} else {
				if (carr[i] == ec) {
					escape = true;
				} else if (carr[i] == '"' && !squoting) {
					dquoting = !dquoting;
					if (!dquoting) {
						array.add(sb.toString());
						sb = new StringBuffer();
					}
				} else if (carr[i] == '\'' && !dquoting) {
					squoting = !squoting;
					if (!squoting) {
						array.add(sb.toString());
						sb = new StringBuffer();
					}
				} else if ((carr[i] == ' ' || carr[i] == '\t') && !squoting && !dquoting) {
					String strArg = sb.toString().trim();
					if (strArg.length() > 0) {
						array.add(sb.toString());
						sb = new StringBuffer();
					}
				} else {
					sb.append(carr[i]);
				}
			}
		}
		
		if (sb.length() > 0) {
			array.add(sb.toString());
		}
		
		result = new String[array.size()];
		array.toArray(result);
		return result;
	}

	/**
	 * to override error/warning  output
	 * @author kurohara
	 *
	 */
	private static final class ParserDigester extends Digester {
		private final Logger logger;
		public ParserDigester(Logger logger) {
			super();
			this.logger = logger;
		}
		
		@Override
		public void warning(SAXParseException exception) throws SAXException {
			logger.trace(exception);
		}
		
		@Override
		public void error(SAXParseException exception) throws SAXException {
			logger.info("scheduler dsl parse error");
			logger.debug(exception);
		}
		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			logger.info("scheduler dsl parse error");
			logger.debug(exception);
		}
	}
	
	private Digester digester = new ParserDigester(logger);
	private final JobFacade jfacade;
	private static final String NODE_MEMBERJOB = "MemberJob";
	private static final String NODE_TOP = "JobDefinition";
	private static final String NODE_JOB = "Job";
	private static final String NODE_ARG ="Arg";
	private static final String NODE_TRIGGER = "Trigger";
	private static final String NODE_ASSIGNTRIGGER = "AssignTrigger";
	private static final String NODE_PSEUDO = "Pseudo";
	private static final String NODE_JOBNET = "JobNet";
	private static final String NODE_TERMINAL = "Terminal";
	private static final String NODE_JOBCLASS = "JobClass";
	private static final String NODE_OPERATOR = "Operator";
	private static final String NODE_PREDECESSOR = "Predecessor";
	private static final String NODE_DELAY = "Delay";
	private static final String NODE_SETJOBCLASS = "SetJobClass";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_FORJOBNAME = "forJobName";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_ENABLED = "enabled";
	private static final String ATTR_CLASS = "class";
	
	public static abstract class AbstractSdlElement {
		protected List<AbstractSdlElement> elements = new ArrayList<AbstractSdlElement>();
		
		public void addChild(AbstractSdlElement e) {
			elements.add(e);
		}
		
		public void construct(AbstractSdlElement parent, JobFacade f) {
			for (AbstractSdlElement e : elements) {
				try {
					e.construct(this, f);
				} catch (Throwable t) {
					logger.info("invalid node fouind:" + e.toString());
				}
			}
		}
		
		public void connect(AbstractSdlElement parent, JobFacade f) {
			for (AbstractSdlElement e : elements) {
				try {
					e.connect(this, f);
				} catch (Throwable t) {
					logger.info("invalid node fouind:" + e.toString());
				}
			}
		}
		
		public void print(int depth) {
			String spcs = "";
			for (int i = 0;i < depth;++i) {
				spcs += " ";
			}
			System.out.println(spcs + this.getClass().getSimpleName());
			
			for (AbstractSdlElement e : elements) {
				e.print(depth + 1);
			}
		}
	}
	
	/**
	 * TopElement
	 *    path: /JobScheduler
	 * @author kurohara
	 *
	 */
	public static class TopElement extends AbstractSdlElement {
	}
	
	/**
	 * JobElement
	 *    path: /JobScheduler/Job
	 * @author kurohara
	 *
	 */
	public static class JobElement extends AbstractSdlElement {
		private String scriptPath;
		private String name;
		private String jobjectName;
		private String methodName;
		private long maxRunTime = 0;
		private List<String> childArg = new ArrayList<String>();
		RootJobReceiver.ASSIGNMENT interrupted = RootJobReceiver.ASSIGNMENT.VOID;
		RootJobReceiver.ASSIGNMENT misfired = RootJobReceiver.ASSIGNMENT.VOID;
		RootJobReceiver.ASSIGNMENT stdout = RootJobReceiver.ASSIGNMENT.VOID;
		RootJobReceiver.ASSIGNMENT stderr = RootJobReceiver.ASSIGNMENT.VOID;
		Boolean useOut = false;
		Boolean useErr = false;
		String outPattern;
		String errPattern;

		public void setName(String name) {
			this.name = name;
		}
		
		public void setScriptPath(String scriptPath) {
			this.scriptPath = scriptPath;
		}

		public void addArg(String arg) {
			childArg.add(arg);
		}
		
		public void setMaxRunTime(String arg) {
			try {
				if (arg != null && arg.length() > 0) {
					maxRunTime = Long.parseLong(arg);
				}
			} catch (NumberFormatException e) {
			}
		}

		public void setUseOut(String arg) {
			useOut = Boolean.parseBoolean(arg);
		}
		
		public void setUseErr(String arg){
			useErr = Boolean.parseBoolean(arg);
		}
		
		public void setInterrupted(String arg) {
			if ("true".equalsIgnoreCase(arg)) {
				interrupted = RootJobReceiver.ASSIGNMENT.CONVERT_TRUE;
			} else if ("false".equalsIgnoreCase(arg)) {
				interrupted = RootJobReceiver.ASSIGNMENT.CONVERT_FALSE;
			} else if ("interrupted".equalsIgnoreCase(arg)) {
				interrupted = RootJobReceiver.ASSIGNMENT.PSEUDOJOB;
			} else {
				interrupted = RootJobReceiver.ASSIGNMENT.VOID;
			}
		}
		
		public void setMisfired(String arg) {
			if ("true".equalsIgnoreCase(arg)) {
				misfired = RootJobReceiver.ASSIGNMENT.CONVERT_TRUE;
			} else if ("false".equalsIgnoreCase(arg)) {
				misfired = RootJobReceiver.ASSIGNMENT.CONVERT_FALSE;
			} else if ("misfired".equalsIgnoreCase(arg)) {
				misfired = RootJobReceiver.ASSIGNMENT.PSEUDOJOB;
			} else {
				misfired = RootJobReceiver.ASSIGNMENT.VOID;
			}
		}
		
		public void setStdout(String arg) {
			if (arg.startsWith("true:")) {
				outPattern = arg.substring("true:".length());
				useOut = true;
				stdout = ASSIGNMENT.CONVERT_TRUE;
			} else if (arg.startsWith("false:")) {
				outPattern = arg.substring("false:".length());
				useOut = true;
				stdout = ASSIGNMENT.CONVERT_FALSE;
			} else if (arg.startsWith("stdout:")) {
				outPattern = arg.substring("stdout:".length());
				useOut = true;
				stdout = ASSIGNMENT.PSEUDOJOB;
			}
		}
		
		public void setStderr(String arg) {
			if (arg.startsWith("true:")) {
				errPattern = arg.substring("true:".length());
				useErr = true;
				stderr = ASSIGNMENT.CONVERT_TRUE;
			} else if (arg.startsWith("false:")) {
				errPattern = arg.substring("false:".length());
				useErr = true;
				stderr = ASSIGNMENT.CONVERT_FALSE;
			} else if (arg.startsWith("stderr:")) {
				errPattern = arg.substring("stderr:".length());
				useErr = true;
				stderr = ASSIGNMENT.PSEUDOJOB;
			}
		}
		
		private boolean usePredefined = false;
		public void setUsePredefined(String arg) {
			if ("TRUE".equalsIgnoreCase(arg) || ! "0".equals(arg)) {
				usePredefined = true;
			}
		}

		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			if (scriptPath != null) {
				// for shell job
				String [] arr = splitCommandline(scriptPath);
				String [] farr = new String[childArg.size()];
				childArg.toArray(farr);
				String [] argarr = new String [arr.length + farr.length];
				System.arraycopy(arr, 0, argarr, 0, arr.length);
				System.arraycopy(farr, 0, argarr, arr.length, farr.length);
				try {
					f.createJob(name, argarr, useOut, useErr, usePredefined);
				} catch (JobManagerException e) {
					// TODO: error should be logged properly
					return;
				}
				if (maxRunTime != 0) {
					try {
						f.setJobLastingTime(name, maxRunTime);
					} catch (JobManagerException e) {
						// TODO: error should be logged properly
						return;
					}
				}
				JobReceiver receiver = f.getJobReceiver(name);
				if (receiver instanceof RootJobReceiver) {
					RootJobReceiver rr = (RootJobReceiver)receiver;
					rr.setInterruptedAssignement(interrupted);
					rr.setMisfiredAssignment(misfired);
					rr.setOutPattern(outPattern);
					rr.setOutPatternAssignment(stdout);
					rr.setErrPattern(errPattern);
					rr.setErrPatternAssignment(stderr);
				}
			}
		}
	}

	/**
	 * ArgElement
	 *    path: /JobScheduler/Job/Arg
	 * @author kurohara
	 *
	 */
	public static class ArgElement extends AbstractSdlElement {
		private String arg;
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			((JobElement)parent).addArg(arg);
		}
	}

	/**
	 * TriggerElement
	 *    path: /JobScheduler/Trigger
	 * @author kurohara
	 *
	 */
	public static class TriggerElement extends AbstractSdlElement {
		private String forJobName;
		private String name;
		private String at;
		private Trigger trigger;
		
		public void setForJobName(String forJobName) {
			this.forJobName = forJobName;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setAt(String at) {
			this.at = at;
		}
		
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			String cronPatrn = "\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S";
			
			if (at == null || at.equalsIgnoreCase("now")) {
				trigger = f.createTrigger(name);
			} else if (at.matches(cronPatrn)) {
				try {
					trigger = f.createTrigger(name, at);
				} catch (JobManagerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					long interval = Long.parseLong(at);
					trigger = f.createTrigger(name, interval);
				} catch (NumberFormatException e) {
					logger.info("invalid 'at' spec:" + at);
				}
			}
		}
		
		public void connect(AbstractSdlElement parent, JobFacade f) {
			super.connect(parent, f);
			if (forJobName != null && trigger != null) {
				JobDetail job = null;
				try {
					job = f.getJob(forJobName);
				} catch (JobManagerException e) {
				}
				if (job != null) {
					try {
						f.scheduleJob(job, trigger);
					} catch (JobManagerException e) {
					}
				} else {
					// no specified job found.
				}
			}
		}
	}
	
	/**
	 * AssignTriggerElement
	 *    path: /JobScheduler/AssignTrigger
	 * @author kurohara
	 *
	 */
	public static class AssignTriggerElement extends AbstractSdlElement {
		private String jobName;
		private String triggerName;
		
		public void setJobName(String jobName) {
			this.jobName = jobName;
		}
		
		public void setTriggerName(String triggerName) {
			this.triggerName = triggerName;
		}
		
		@Override
		public void connect(AbstractSdlElement parent, JobFacade f) {
			super.connect(parent, f);
			if (jobName != null && triggerName != null) {
				Trigger t = null;
				try {
					t = f.getTrigger(triggerName);
				} catch (JobManagerException e) {
				}
				JobDetail job = null;
				try {
					job = f.getJob(jobName);
				} catch (JobManagerException e) {
				}
				try {
					f.scheduleJob(job, t);
				} catch (JobManagerException e) {
				}
			}
		}
	}

	/**
	 * PseudoElement
	 *    path: /JobScheduler/JobNet/Pseudo
	 * @author kurohara
	 *
	 */
	public static class PseudoElement extends AbstractSdlElement {
		private String name;
		
		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			
			if (name != null) {
				JobNetElement e = (JobNetElement) parent;
				
				// 
				f.createInterruptFollower(e.getJobNet(), name);
			}
		}
	}

	/**
	 * JobNetElement
	 *    path: /JobScheduler/JobNet
	 * @author kurohara
	 *
	 */
	public static class JobNetElement extends AbstractSdlElement {
		private String name;
		private JobNet net;
		private String enabled;
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public JobNet getJobNet() {
			return net;
		}
		
		public void setEnabled(String enabled) {
			this.enabled = enabled;
		}

		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			net = f.createJobNet(name);
			super.construct(parent, f);
		}
	}

	/**
	 * TerminalElement
	 *    path: /JobScheduler/JobNet/Terminal
	 * @author kurohara
	 *
	 */
	public static class TerminalElement extends AbstractSdlElement {
		private static long id = 0;
		private String type;
		private String name;
		private String forJobName;
		private long delay;
		private List<String> predecessors = new ArrayList<String>();
		private JobNet container;
		
		public void setType(String type) {
			this.type = type;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setForJobName(String forJobName) {
			this.forJobName = forJobName;
		}

		public void setDelay(String delay) {
			try {
				this.delay = Long.parseLong(delay);
			} catch (NumberFormatException e) {
			}
		}
		
		public void addPredecessor(String predecessor) {
			predecessors.add(predecessor);
		}
		
		public JobNet getContainer() {
			return container;
		}
		
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			container = ((JobNetElement)parent).getJobNet();
			
			if (name == null) {
				name = "Terminal_" + type + "_" + ++id;
			}
			Terminal t = null;
			if (type.equalsIgnoreCase("force")) {
				t = f.createForceRunTerminal(container, name, forJobName, delay);
			} else if (type.equalsIgnoreCase("prohibit")) {
				t = f.createProhibitTerminal(container, name, forJobName, delay);
			} else {
				
			}
			
		}
		
		public void connect(AbstractSdlElement parent, JobFacade f) {
			super.connect(parent, f);
			if (container != null) {
				for (String p : predecessors) {
					try {
						f.connect(container, p, name);
					} catch (ClassCastException e) {
					} catch (ConditionBoardException e) {
					}
				}
			}
		}
	}

	/**
	 * PredecessorElement
	 *    path: /JobScheduler/JobNet/Terminal/Predecessor
	 * @author kurohara
	 *
	 */
	public static class PredecessorElement extends AbstractSdlElement {
		private String name;
		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			if (parent instanceof TerminalElement) {
				((TerminalElement)parent).addPredecessor(name);
			} else if (parent instanceof OperatorElement) {
				((OperatorElement)parent).addPredecessor(name);
			}
		}
	}
	
	/**
	 * OperatorElement
	 *    path: /JobScheduler/JobNet/Operator
	 * @author kurohara
	 *
	 */
	public static class OperatorElement extends AbstractSdlElement {
		private String name;
		private String type;
		private int count;
		private List<String> predecessors = new ArrayList<String>();
		private JobNet container;
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public void setCount(String count) {
			try {
				this.count = Integer.parseInt(count);
			} catch (NumberFormatException e) {
	
			}
		}
		
		public void addPredecessor(String predecessor) {
			predecessors.add(predecessor);
		}
		
		public JobNet getContainer() {
			return container;
		}
		
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			if (parent instanceof JobNetElement) {
				container = ((JobNetElement)parent).getJobNet();
			} else if (parent instanceof TerminalElement) {
				container = ((TerminalElement)parent).getContainer();
			} else if (parent instanceof OperatorElement) {
				container = ((OperatorElement)parent).getContainer();
			} else {
				// error
				logger.info("failed to construct Operator");
				return;
			}
			if (type.equalsIgnoreCase("or")) {
				f.createOrOperator(container, name);
			} else if (type.equalsIgnoreCase("and")) {
				f.createAndOperator(container, name);
			} else if (type.equalsIgnoreCase("not")) {
				f.createNotOperator(container, name);
			} else if (type.equalsIgnoreCase("xor")) {
				f.createXorOperator(container, name);
			} else if (type.equalsIgnoreCase("count")) {
				f.createCounterOperator(container, name, count);
			} else {
				
			}
		}
		
		public void connect(AbstractSdlElement parent, JobFacade f) {
			if (container != null) {
				// connect to predecessor
				for (String p : predecessors) {
					try {
						f.connect(container, p, name);
					} catch (ClassCastException e) {
						logger.info("failed to connect Operator because of bad cast operatrion");
					} catch (ConditionBoardException e) {
						logger.info("failed to connect Operator:" + e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * JobClassElement
	 *    path: /JobScheduler/JobClass
	 * @author kurohara
	 *
	 */
	public static class JobClassElement extends AbstractSdlElement {
		private String name;
		private String concurrency;
		private String maxruntime;
		private String maxwaittime;
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setConcurrency(String concurrency) {
			this.concurrency = concurrency;
		}
		
		public void setMaxruntime(String maxruntime) {
			this.maxruntime = maxruntime;
		}
		
		public void setMaxwaittime(String maxwaittime) {
			this.maxwaittime = maxwaittime;
		}
		
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			super.construct(parent, f);
			JobClass jc = null;
			try {
				jc = f.createJobClass(name);
			} catch (JobManagerException e) {
			}
			if (jc != null) {
				if (concurrency == null || concurrency.length() == 0) {
					jc.setConcurrency(1);
				} else {
					jc.setConcurrency(Integer.parseInt(concurrency));
				}
				jc.setMaxRunTime(Long.parseLong(maxruntime));
				jc.setMaxWaitTime(Long.parseLong(maxwaittime));
			}			
		}
	}
	
	public static class MemberJobElement extends AbstractSdlElement {
		private String name;
		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public void construct(AbstractSdlElement parent, JobFacade f) {
			// TODO Auto-generated method stub
			super.construct(parent, f);
		}
		
	}
	
	/**
	 * SetJobClassElement
	 *    path: /JobScheduler/SetJobClass
	 * @author kurohara
	 *
	 */
	public static class SetJobClassElement extends AbstractSdlElement {
		private String forJobName;
		private String classname;
		
		public void setForJobName(String forJobName) {
			this.forJobName = forJobName;
		}
		
		public void setClassName(String className) {
			this.classname = className;
		}
		
		@Override
		public void connect(AbstractSdlElement parent, JobFacade f) {
			super.connect(parent, f);
			try {
				f.assignJobClass(forJobName, classname);
			} catch (JobManagerException e) {
			}
		}
	}
	
	public Parser(JobFacade facade, boolean validate) {
		digester.setClassLoader(this.getClass().getClassLoader());
		jfacade = facade;
		//
		digester.setValidating(validate);
		
		String path;
		// setup parser
		path = NODE_TOP;
		digester.addObjectCreate(path, TopElement.class);
		digester.addSetProperties(path);
		//
		path = NODE_TOP + "/" + NODE_JOB;
		digester.addObjectCreate(path, JobElement.class);
		digester.addSetProperties(path);
		digester.addCallMethod(path + "/" + NODE_ARG, "addArg", 0);
		digester.addSetNext(path, "addChild");
//		//
//		path = NODE_TOP + "/" + NODE_JOB + "/" + NODE_ARG;
//		digester.addObjectCreate(path, ArgElement.class);
//		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_JOBNET + "/" + NODE_PSEUDO;
		digester.addObjectCreate(path, PseudoElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_TRIGGER;
		digester.addObjectCreate(path, TriggerElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_ASSIGNTRIGGER;
		digester.addObjectCreate(path, AssignTriggerElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_JOBNET;
		digester.addObjectCreate(path, JobNetElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_JOBNET + "/" + NODE_TERMINAL;
		digester.addObjectCreate(path, TerminalElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = "*/" + NODE_OPERATOR;
		digester.addObjectCreate(path, OperatorElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = "*/" + NODE_PREDECESSOR;
		digester.addObjectCreate(path, PredecessorElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_JOBCLASS;
		digester.addObjectCreate(path, JobClassElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_SETJOBCLASS;
		digester.addObjectCreate(path, SetJobClassElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		//
		path = NODE_TOP + "/" + NODE_JOBCLASS + "/" + NODE_MEMBERJOB ;
		digester.addObjectCreate(path,  MemberJobElement.class);
		digester.addSetProperties(path);
		digester.addSetNext(path, "addChild");
		
	}
	
	public void parse(InputStream is) throws IOException, SAXException {
		try {
			digester.resetRoot();
			digester.parse(is);
			AbstractSdlElement top = (AbstractSdlElement) digester.getRoot();
			if (top != null) {
				// for debug
				// top.print(0);
				// pass 1
				top.construct(null, jfacade);
				// pass 2
				top.connect(null, jfacade);
			}
		} catch (IOException e) {
			logger.info(e);
			throw e;
		} catch (SAXException e) {
			logger.info(e);
			throw e;
		}
	}
	
	public void parse(File inf) throws IOException, SAXException {
		InputStream is = null;
		try {
			is = new FileInputStream(inf);
			parse(is);
		} catch (FileNotFoundException e) {
			logger.info(e);
			throw e;
		} finally {
			if (is != null){
				is.close();
			}
		}
	}
}
