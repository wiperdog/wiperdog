package com.insight_tec.pi.jobmanager.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.PropertySettingJobFactory;
import org.wiperdog.jobmanager.Constants;
import org.wiperdog.jobmanager.JobClass;
import org.wiperdog.jobmanager.JobFacade;
import org.wiperdog.jobmanager.JobResult;
import org.wiperdog.jobmanager.internal.JobFacadeImpl;


import static org.junit.Assert.*;

public class JobFacadeTest {
	private SchedulerFactory sf;
	private Scheduler scheduler;

	private static int pcount(String spec) throws Exception {
		int count = 0;
		String [] strPs = {"/bin/ps", "-ef"};
		
		ProcessBuilder builder = new ProcessBuilder(strPs);
		
		Process p = builder.start();

		InputStream ins = p.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
		
		String strLine = "";
		Pattern pat = Pattern.compile(spec);
		while ((strLine = reader.readLine()) != null) {
			Matcher m = pat.matcher(strLine);
			if (m.find()) {
				System.err.println("found: " + strLine);
				++count;
			}
		}
		
		return count;
	}
	
	@Before
	public void setup() throws Exception {
		sf = new StdSchedulerFactory();
		scheduler = sf.getScheduler();
		PropertySettingJobFactory jfactory = new PropertySettingJobFactory();
		jfactory.setWarnIfPropertyNotFound(false);
		scheduler.setJobFactory(jfactory);
	}
	
	@After
	public void shutdown() throws Exception {
		scheduler.shutdown();
		scheduler = null;
		sf = null;
	}
	
	@Test
	public void testFacade1() throws Exception {
		String jcname1 = "testjobclass";

		scheduler.start();
		JobFacade jf = new JobFacadeImpl(scheduler);
		
		assertNotNull(jf);
		
		JobClass jc1 = jf.createJobClass(jcname1);
		JobClass jc2 = jf.createJobClass(jcname1);
		
		assertEquals(jc1, jc2);

		jf.deleteJobClass(jcname1);
		
		jc2 = jf.createJobClass(jcname1);
		
		assertFalse(jc1 == jc2);
		
		jf.deleteJobClass(jcname1);

		scheduler.shutdown();
	}
	
	@Test
	public void testFacade2() throws Exception {
		String jcname1 = "testjobclass";
		String jobname1 = "testjob1";
		String jobname2 = "testjob2";
		String javaJobClass = "com.insight_tec.pi.jobmanager.internal.BeanJob";

		scheduler.start();
		JobFacade jf = new JobFacadeImpl(scheduler);
		
		assertNotNull(jf);
		
		JobClass jc = jf.createJobClass(jcname1);
		jc.setConcurrency(1);
		jc.setMaxRunTime(1000);
		jc.setMaxWaitTime(1000);

		JobDetail job = jf.createJob(jobname1, javaJobClass, "dowork", new String [] { "done!" });

		jf.assignJobClass(jobname1, jcname1);
		
		Trigger t = jf.createTrigger(jobname1, 100);
		
		jf.scheduleJob(job, t);

		// check if job-class with same name carries all properties set previously.
		jc = jf.createJobClass(jcname1);
		List<JobKey> jklist = jc.getAssignedList();
		
		assertEquals(1, jklist.size());

		JobDetail job2 = jf.createJob(jobname2, javaJobClass, "dowork", new String [] { "done!" });
		
		jf.assignJobClass(jobname2, jcname1);
		
		jklist = jc.getAssignedList();
		
		t = jf.createTrigger(jobname2, 100);
		
		jf.scheduleJob(job2, t);
		
		assertEquals(2, jklist.size());
		
		Thread.sleep(2000);

		scheduler.shutdown();
	}
	
	@Test
	public void testFacade3() throws Exception {
		String jcname1 = "testjobclass";
		String jobname1 = "testjob1";

		scheduler.start();
		
		JobFacade jf = new JobFacadeImpl(scheduler);
		
		assertNotNull(jf);
		
		JobClass jc = jf.createJobClass(jcname1);
		jc.setConcurrency(1);
		jc.setMaxRunTime(5000);
		jc.setMaxWaitTime(5000);

//		JobDetail job = jf.createJob(jobname1, new String [] {"/bin/sh", "-c", "echo start;sleep 10;echo end" }, false);
		JobDetail job = jf.createJob(jobname1, new String [] {"/bin/sleep", "10"}, false);

		jf.assignJobClass(jobname1, jcname1);
		
		Trigger t = jf.createTrigger(jobname1, 100);
		
		jf.scheduleJob(job, t);

		// try to interrupt
		Thread.sleep(1000);
		jf.interruptJob(jobname1);

		Thread.sleep(500);
		
		int rc = jf.getJobRunningCount(jobname1);
		int rc2 = pcount("sleep 10");
		System.out.println("rc2=" + rc2);
		assertEquals(0, rc);

		scheduler.shutdown();
	}
	
	@Test
	public void testFacade4() throws Exception {
		String jobname1 = "testjob1";

		scheduler.start();
		
		JobFacade jf = new JobFacadeImpl(scheduler);
		
		assertNotNull(jf);
		
		JobDetail job = jf.createJob(jobname1, new String [] {"/bin/echo", "-n", "abcdefg"}, true/* stdout */ , true/* stderr */, false /* predefined */);
		
		jf.setJobHistoryLength(jobname1, 2);

		Trigger t = jf.createTrigger(jobname1, 10);
		
		jf.scheduleJob(job, t);

		Thread.sleep(500);

		List<JobResult> jrlist = jf.getJobResult(jobname1);
		assertEquals(jrlist.size(), 1);
		String msg = (String) jrlist.get(0).getData().get(Constants.KEY_STDOUT);
		System.err.println("msg=" + msg);
		System.err.println("ended at: " + jrlist.get(0).getEndedAt());

		t = jf.createTrigger(jobname1, 10);
		jf.scheduleJob(job, t);
		Thread.sleep(500);
		jrlist = jf.getJobResult(jobname1);
		assertEquals(jrlist.size(), 2);
		
		t = jf.createTrigger(jobname1, 10);
		jf.scheduleJob(job, t);
		Thread.sleep(500);
		jrlist = jf.getJobResult(jobname1);
		assertEquals(jrlist.size(), 2);
		
		t = jf.createTrigger(jobname1, 10);
		jf.scheduleJob(job, t);
		Thread.sleep(500);
		jrlist = jf.getJobResult(jobname1);
		assertEquals(jrlist.size(), 2);
		
		scheduler.shutdown();
	}
	
	@Test
	public void testFacade5() throws Exception {
		String jobname1 = "testjob1";

		scheduler.start();
		
		JobFacade jf = new JobFacadeImpl(scheduler);
		
		assertNotNull(jf);
		
		JobDetail job = jf.createJob(jobname1, new String [] {"/bin/cat", "/etc/hosts"}, true/* stdout */ , true/* stderr */, false /* predefined */);
		
		jf.setJobDataReceiveSize(jobname1, 50);

		Trigger t = jf.createTrigger(jobname1, 50);
		
		jf.scheduleJob(job, t);

		Thread.sleep(500);

		while (true) {
			if (jf.getJobRunningCount(jobname1) > 0) {
				Thread.sleep(500);
			} else {
				break;
			}
		}
		
		List<JobResult> jrlist = jf.getJobResult(jobname1);
		assertEquals(1, jrlist.size());
		String msg = (String) jrlist.get(0).getData().get(Constants.KEY_STDOUT);
		System.err.println("msg=" + msg);
		System.err.println("ended at: " + jrlist.get(0).getEndedAt());

		assertEquals(50, msg.length());
		
		scheduler.shutdown();
	}
	
}
