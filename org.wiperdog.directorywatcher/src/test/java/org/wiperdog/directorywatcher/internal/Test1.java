package org.wiperdog.directorywatcher.internal;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wiperdog.directorywatcher.Listener;
import org.wiperdog.directorywatcher.internal.ListenerWrapperImpl;
import org.wiperdog.directorywatcher.internal.WatcherService;

import static org.junit.Assert.*;

public final class Test1 {
	private static final String TESTFILE_PREFIX = "directorywatcher_ut_file";
	
	private String modifiedmsg = "";
	private String addedmsg = "";
	private String deletedmsg = "";

	private String directory = "";
	
	public class MyListener implements Listener {

		public String getDirectory() {
			return directory;
		}

		public long getInterval() {
			return 1000;
		}

		public boolean filterFile(File file) {
			if (file.isFile()) {
				String strName = file.getName();
				if (strName.startsWith(TESTFILE_PREFIX)) {
					return true;
				}
			}
			return false;
		}

		public boolean notifyModified(File target) throws IOException {
			modifiedmsg = "modified:" + target.getName();
			return true;
		}

		public boolean notifyAdded(File target) throws IOException {
			addedmsg = "added:" + target.getName();
			return true;
		}

		public boolean notifyDeleted(File target) throws IOException {
			deletedmsg = "deleted:" + target.getName();
			return false;
		}
	}

	private void createFile(int id) throws IOException {
		File nf = new File(directory + "/" + TESTFILE_PREFIX + id);
		nf.createNewFile();
	}

	private void updateFile(int id) throws IOException {
		File nf = new File(directory + "/" + TESTFILE_PREFIX + id);
		OutputStream os = new FileOutputStream(nf);
		os.write(0);
	}
	
	private void deleteFile(int id) throws IOException {
		File nf = new File(directory + "/" + TESTFILE_PREFIX + id);
		nf.delete();
	}
	
	@Test
	public void doTest1() throws IOException, InterruptedException {
		directory = "/tmp";
		Listener l = new MyListener();
		ListenerWrapperImpl wrap = new ListenerWrapperImpl(l);
		wrap.setAddOnly(true);
		WatcherService watcher = new WatcherService(wrap);
	
		watcher.start();
		// 
		createFile(0);
		Thread.sleep(1200);
		assertEquals("added:" + TESTFILE_PREFIX + 0, addedmsg);
		addedmsg = "";
		Thread.sleep(1200);
		assertEquals("added:" + TESTFILE_PREFIX + 0, addedmsg);
		
		Thread.sleep(1200);
		
		watcher.stop();
	}
	
	@Test
	public void doTest2() throws IOException, InterruptedException {
		directory = "/tmp";
		Listener l = new MyListener();
		ListenerWrapperImpl wrap = new ListenerWrapperImpl(l);
		wrap.setAddOnly(false);
		WatcherService watcher = new WatcherService(wrap);
	
		watcher.start();
		// 
		createFile(0);
		Thread.sleep(1200);
		assertEquals("added:" + TESTFILE_PREFIX + 0, addedmsg);
		addedmsg = "";
		Thread.sleep(1200);
		assertEquals("", addedmsg);
		
		Thread.sleep(1200);
		
		watcher.stop();
	}
	
}
