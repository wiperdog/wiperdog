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
import java.io.IOException;

import org.wiperdog.directorywatcher.CountingListener;
import org.wiperdog.directorywatcher.Listener;


public class ListenerWrapperImpl implements ListenerWrapper {
	private final Listener delegate;
	private int depth  = 1;
	private boolean bHandleRetry = false;
	private boolean isAddOnly = false;
	
	public ListenerWrapperImpl(Listener delegate) {
		this.delegate = delegate;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public void setHandleRetry(boolean bHandleRetry) {
		this.bHandleRetry = bHandleRetry;
	}
	
	public String getDirectory() {
		return delegate.getDirectory();
	}

	public long getInterval() {
		return delegate.getInterval();
	}

	public boolean filterFile(File file) {
		return delegate.filterFile(file);
	}

	public boolean notifyModified(File target) throws IOException {
		boolean bSucceeded = delegate.notifyModified(target);
		if (bHandleRetry) {
			return bSucceeded;
		} else {
			return true;
		}
	}

	public boolean notifyAdded(File target) throws IOException {
		boolean bSucceeded = delegate.notifyAdded(target);
		if (bHandleRetry) {
			return bSucceeded;
		} else {
			return true;
		}
	}

	public boolean notifyDeleted(File target) throws IOException {
		boolean bSucceeded = delegate.notifyDeleted(target);
		if (bHandleRetry) {
			return bSucceeded;
		} else {
			return true;
		}
	}

	public int getDepth() {
		return depth;
	}

	public Listener getDelegate() {
		return delegate;
	}

	public void setAddOnly(boolean isAddOnly) {
		this.isAddOnly = isAddOnly;
	}
	
	public boolean isAddOnly() {
		return isAddOnly;
	}

	public boolean notifyCount(int count) {
		if (delegate instanceof CountingListener) {
			return ((CountingListener)delegate).notifyCount(count);
		}
		return true;
	}


}
