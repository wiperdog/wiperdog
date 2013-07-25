package com.insight_tec.pi.jobmanager.internal;

public class BeanJob {
	public void dowork(String arg1) {
		try {
			System.out.println("start " + arg1);
			Thread.sleep(1000);
			System.out.println("end " + arg1);
		} catch (InterruptedException e) {
			System.out.println("interrupted");
		}
	}
}
