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
package org.wiperdog.directorywatcher.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.wiperdog.directorywatcher.Listener;

import org.apache.log4j.Logger;

public final class WatcherService implements Runnable {
	
	private static final Logger logger = Logger.getLogger(WatcherService.class);
	
	private final ListenerWrapper listener;
	private boolean bRun = false;
	private Thread me = null;
	
	private boolean isSortAscendant = true; // or Descendant
	private boolean isSortNameFirst = true; // or TimeFirst
	
	public Listener getListener() {
		return listener;
	}
	
	public WatcherService(ListenerWrapper listener) {
		this.listener = listener;
	}

	public void setSortAscendant(boolean bAscendant) {
		this.isSortAscendant = bAscendant;
	}
	
	public void setSortNameFirst(boolean bNameFirst) {
		this.isSortNameFirst = bNameFirst;
	}
	
	public synchronized void start() {
		bRun = true;
		if (me == null || ! me.isAlive()) {
			logger.debug("DirectoryWatcher for:" + listener.getDirectory() + " is starting");
			me = new Thread(this, "DirectoryWatcher:"+listener.getDirectory());
			me.start();
		}
	}
	
	public synchronized void stop() {
		bRun = false;
		if (me != null && me.isAlive()) {
			logger.debug("DirectoryWatcher for:" + listener.getDirectory() + " is stopping");
			me.interrupt();
		}
	}

	private Set<File> prevFiles = null;
	private Map<File, Long> prevDatesMap = null;
	private Set<File> backlog_added = new HashSet<File>();
	
	// build file list searching specified directory recursively.
	private void enumFiles(File dir, int depth, Set<File> fileset) {
		final Set<File> childs = new HashSet<File>();
		File [] files = dir.listFiles(new FileFilter () {
			public boolean accept(File pathname) {
				// collect child directories
				if (pathname.isDirectory()) {
					if (listener.filterFile(pathname)) {
						childs.add(pathname);
					}
					return false;
				} else {
					return listener.filterFile(pathname);
				}
			}
		});
		Collections.addAll(fileset, files);
		if (--depth > 0) {
			for (File f : childs) {
				if (f.isDirectory()) {
					enumFiles(f, depth, fileset);
				}
			}
		}
	}
	
	
	File [] sortFiles(Set<File> fileset) {
		File [] filearray = new File [fileset.size()];
		fileset.toArray(filearray);
		Arrays.sort(filearray, new Comparator<File>() {
			public int compare(File arg0, File arg1) {
				int rv = 0;
				File p;
				File n;
				if (isSortAscendant) {
					p = arg0;
					n = arg1;
				} else {
					p = arg1;
					n = arg0;
				}
				if (isSortNameFirst) {
					rv = p.getAbsolutePath().compareTo(n.getAbsolutePath());
					if (rv == 0) {
						long lv = p.lastModified() - n.lastModified();
						rv = (lv < 0 ? -1 : (lv == 0 ? 0 : 1));
					}
				} else {
					long lv = p.lastModified() - n.lastModified();
					rv = (lv < 0 ? -1 : (lv == 0 ? 0 : 1));
					if (rv == 0){
						rv = p.getAbsolutePath().compareTo(n.getAbsolutePath());
					}
				}
				
				return rv;
			}
			
		});
		return filearray;
	}
	
	private boolean processDirectory(File dir, int depth) {
		if  (depth == 0) {
			return true;
		}
		try {
			if (dir != null && dir.isDirectory()) {
				long measureTime = 0;
				Set<File> fPresent = new HashSet<File>();
				enumFiles(dir, depth, fPresent);
				listener.notifyCount(fPresent.size());
				Set<File> added = arraySub(fPresent, prevFiles);
				added.addAll(backlog_added);
				measureTime = (new Date()).getTime();
				if (added != null && added.size() > 0) {
					logger.debug("start ADD for " + added.size() + " files");
					// ファイル追加処理だけはソートされていた方が良い。
					// TODO: 暫定対処、まとめて全部ソート、もっと高速なやり方を考えるべき
					File [] sorted = sortFiles(added);
					for (File f : sorted) {
						try {
							logger.debug("notifyAdded:" + f.getName());
							//
							// if notifyAdded() returned false, the file processed should be re-processed next time.
							//  so we handle this file as "not added now".
							if (listener.notifyAdded(f)) {
								// remove from backlog anyway
								backlog_added.remove(f);
								
								continue;
							}
						} catch (IOException e) {
							logger.debug("exception on calling notifyAdded()", e);
						} catch (Exception e) {
							logger.debug("exception on calling notifyAdded()", e);
						}
						// add to backlog
						backlog_added.add(f);
					}
					logger.debug("end ADD for " + added.size() + " files in " + ((new Date()).getTime() - measureTime) + " millisecs");
				}
				// use to calculate deleted set with using  prefFiles and fPresent(all of files exist)
				//   because we should call notifyDeleted() even if notifyAdded() has been continuously failed.
				Set<File> deleted = arraySub(prevFiles, fPresent);
				measureTime = (new Date()).getTime();
				if (deleted != null && deleted.size() > 0) {
					logger.debug("start DELETE for " + deleted.size() + " files");
					for (File f : deleted) {
						try {
							logger.debug("notifyDeleted:" + f.getName());
							// If we can, we should handle failed file as "not deleted now", but it seemed troublesome.
							if (! listener.notifyDeleted(f)) {
								// do nothing now.
							}
						} catch (IOException e) {
							logger.debug("exception on calling notifyDeleted()", e);
						} catch (Exception e) {
							logger.debug("exception on calling notifyDeleted()", e);
						}
						// remove from backlog anyway
						backlog_added.remove(f);
					}
					logger.debug("end DELETE for " + deleted.size() + " files in " + ((new Date()).getTime() - measureTime) + " millisecs");
				}
	
				if (prevDatesMap == null) 
					prevDatesMap = new HashMap<File, Long>();
	
				measureTime = (new Date()).getTime();
				// use next set because we need to neglect the file that was failed on notifyAdded().
				for (File f : fPresent) {
					long fileLastModified = f.lastModified();
					boolean bUpdateTimestamp = true;
					if (prevDatesMap.get(f) == null) {
						// add/delete
					} else {
						// 
						if (prevDatesMap.get(f) < fileLastModified) {
							logger.debug("notifyModified:" + f.getName());
							try {
								logger.debug("start MODIFIED");
								// mark whether done successfully
								bUpdateTimestamp = listener.notifyModified(f);
								logger.debug("end MODFIED in " + ((new Date()).getTime() - measureTime) + " millisecs");
							} catch (IOException e) {
								logger.debug("exception on calling notifyModified()", e);
							} catch (Exception e) {
								logger.debug("exception on calling notifyModified()", e);
							}
						}
					}
					
					if (bUpdateTimestamp) {
						prevDatesMap.put(f, fileLastModified);
					}
					
					// remove time-stamp data if file  is deleted.
					if (deleted.contains(f)) {
						prevDatesMap.remove(f);
					}
				}
				prevFiles = fPresent;
			} else {
				logger.debug(dir.getAbsolutePath() + " is not a directory");
			}
		} catch (Exception e) {
			logger.debug("", e);
		}
		return true;
	}
	
	public void run() {
		String oldDir = "";
		
		File dir = null;
		while (true) {
			synchronized (this) {
				if (! bRun) {
					break;
				}
			}

			try {
				String directory = listener.getDirectory();
				if (! oldDir.equals(directory) && directory != null) {
					dir = new File(directory);
					Thread.currentThread().setName("DirectoryWatcher:" + directory);
					oldDir = directory;
				}
				int depth = 1;
				boolean isAddOnly = false;
				if (listener instanceof ListenerWrapper) {
					depth = ((ListenerWrapper) listener).getDepth();
					isAddOnly = ((ListenerWrapper) listener).isAddOnly();
				}
				processDirectory(dir, depth);
				if (isAddOnly) {
					prevFiles.clear();
					prevDatesMap.clear();
				}
			} catch (Exception e) {
				logger.debug("", e);
			}
			try {
				long interval = listener.getInterval();
				// stop this thread if interval was set to Long.MAX_VALUE
				if (interval == Long.MAX_VALUE) {
					break;
				}
				// select default interval (5 sec) if interval time is invalid
				if (interval <= 0) 
					interval = 5000;
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				
			}
		}
	}

	private Set<File> arraySub(Set<File> lhs, Set<File> rhs) {
		Set<File> result = new HashSet<File>();
		if (lhs != null) {
			result.addAll(lhs);
		}
		
		if (rhs != null) {
			result.removeAll(rhs);
		}
		
		return result;
	}
	
}
