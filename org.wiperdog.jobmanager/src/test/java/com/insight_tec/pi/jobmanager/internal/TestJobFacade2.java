package com.insight_tec.pi.jobmanager.internal;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.PropertySettingJobFactory;
import org.wiperdog.jobmanager.JobClass;
import org.wiperdog.jobmanager.JobFacade;
import org.wiperdog.jobmanager.JobManagerException;
import org.wiperdog.jobmanager.JobResult;
import org.wiperdog.jobmanager.internal.JobFacadeImpl;


public class TestJobFacade2 {

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
	public void jobFacadeTest1() throws Exception {
		String jcname1 = "testjobclass";
		String jobname1 = "job1";
		String jobname2 = "job2";
		
		Thread.sleep(1000);
		System.out.println("TestJobFacade2.jobFacadeTest1 staring");
		scheduler.start();
		JobFacade jf = new JobFacadeImpl(scheduler);

		JobClass jc = jf.createJobClass(jcname1, 1, 10000, 50000);
		JobDetail job1 = jf.createJob(jobname1, new String [] {"/bin/sleep", "15"}, false);
		JobDetail job2 = jf.createJob(jobname2, new String [] {"/bin/sleep", "15"}, false);

		jf.assignJobClass(jobname1, jcname1);
		jf.assignJobClass(jobname2, jcname1);
		
		Trigger t = null;
		List<JobResult> jr = null;
		
//		t = jf.createTrigger(jobname1, 0);
		t = jf.createTrigger(jobname1, "0 * * * * ? *");
		jf.scheduleJob(job1, t);
//		t = jf.createTrigger(jobname2, 1000);
		t = jf.createTrigger(jobname2, "0 * * * * ? *");
		jf.scheduleJob(job2, t);

//		Thread.sleep(11000);
//		int c = pcount("/bin/sleep 15");
//		jr = jf.getJobResult(jobname1);
//		assertNotNull(jr);
//		assertEquals(1, jr.size());
		// 
		// reduce running time to avoid stopping build system.
		// TODO: increase sleep time on desktop test time.
		// Thread.sleep(30000);
		System.out.println("shutting down" + new Date());
		scheduler.shutdown();
	}
}
