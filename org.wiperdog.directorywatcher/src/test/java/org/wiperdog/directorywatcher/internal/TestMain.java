package org.wiperdog.directorywatcher.internal;


import java.io.File;
import java.io.IOException;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wiperdog.directorywatcher.Listener;
import org.wiperdog.directorywatcher.internal.ListenerWrapperImpl;
import org.wiperdog.directorywatcher.internal.WatcherService;

import static org.junit.Assert.*;

public final class TestMain {
	private String directory;
	private class MyListener implements Listener {

		public boolean filterFile(File file) {
			if (file.getName().endsWith(".csv")) {
				return true;
			}
			return false;
		}

		public String getDirectory() {
			return directory;
		}

		public long getInterval() {
			return 1000;
		}

		public boolean notifyAdded(File target) throws IOException {
			System.out.println("Added:" + target.getAbsolutePath());
			return true;
		}

		public boolean notifyDeleted(File target) throws IOException {
			System.out.println("Deleted:" + target.getAbsolutePath());
			return true;
		}

		public boolean notifyModified(File target) throws IOException {
			System.out.println("Modified:" + target.getAbsolutePath());
			return true;
		}
		
	}
	
	
	@Test
	public void test() throws InterruptedException {
		directory = "/tmp";
		Listener l = new MyListener();
		WatcherService watcher = new WatcherService(new ListenerWrapperImpl(l));

		watcher.start();
		Thread.sleep(1200);
		watcher.stop();
	}
	
//	public static void main(String [] args) {
//		TestMain m = new TestMain();
//		m.test();
//	}
}
