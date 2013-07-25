package org.wiperdog.directorywatcher.internal;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wiperdog.directorywatcher.CountingListener;
import org.wiperdog.directorywatcher.Listener;
import org.wiperdog.directorywatcher.internal.ListenerWrapperImpl;
import org.wiperdog.directorywatcher.internal.WatcherService;

import static org.junit.Assert.*;

public final class Test2 {
	private static final String TESTFILE_PREFIX = "directorywatcher_ut_file";
	
	private String modifiedmsg = "";
	private String addedmsg = "";
	private String deletedmsg = "";

	private String directory = "";
	
	private int count = 0;
	
	public class MyListener implements CountingListener {

		public String getDirectory() {
			return directory;
		}

		public long getInterval() {
			return 100;
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

		public boolean notifyCount(int count) {
			Test2.this.count = count;
			return true;
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
	
	private void clearDirectory() throws IOException {
		File [] list = new File(directory).listFiles();
		for (File f : list) {
			f.delete();
		}
	}
	
	@Test
	public void doTest1() throws IOException, InterruptedException {
		directory = "src/test/tmp";
		
		clearDirectory();
		
		Listener l = new MyListener();
		ListenerWrapperImpl wrap = new ListenerWrapperImpl(l);
		wrap.setAddOnly(true);
		WatcherService watcher = new WatcherService(wrap);
	
		watcher.start();
		// 
		createFile(0);
		Thread.sleep(500);
		assertEquals("added:" + TESTFILE_PREFIX + 0, addedmsg);
		assertEquals(count, 1);
		addedmsg = "";
		Thread.sleep(500);
		assertEquals("added:" + TESTFILE_PREFIX + 0, addedmsg);
		updateFile(0);
		Thread.sleep(500);
		assertEquals(count, 1);
		createFile(1);
		Thread.sleep(500);
		assertEquals(count, 2);
		updateFile(1);
		assertEquals(count, 2);
		createFile(2);
		Thread.sleep(500);
		assertEquals(count, 3);
		deleteFile(1);
		Thread.sleep(500);
		assertEquals(count, 2);
		
		watcher.stop();
	}
	
	
}
