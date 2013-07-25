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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.wiperdog.directorywatcher.Listener;

import org.apache.log4j.Logger;

public final class Activator implements BundleActivator {
	
	/** DW000 */
	private static final Logger logger = Logger.getLogger(Activator.class);
	
	private List<WatcherService> watcherList = new ArrayList<WatcherService>();
	
	private BundleContext context;
	private class ListenerTrackerCustomizer implements ServiceTrackerCustomizer {

		public Object addingService(ServiceReference reference) {
			Object service = context.getService(reference);
			logger.debug("addingService(" + service.toString()+ ")");
			if (service instanceof Listener) {
				synchronized (this) {
					ListenerWrapperImpl wrapper = new ListenerWrapperImpl((Listener)service);
					Object oDepth = reference.getProperty(Listener.PROPERTY_DEPTH);
					try {
						if (oDepth != null) {
							wrapper.setDepth(Integer.parseInt(oDepth.toString()));
						}
						Object oRetry = reference.getProperty(Listener.PROPERTY_HANDLERETRY);
						if (oRetry != null) {
							wrapper.setHandleRetry(Boolean.parseBoolean(oRetry.toString()));
						}
					} catch (NumberFormatException e) {
						logger.debug("invalid setting data format");
					}
					String strIsAddonly = (String) reference.getProperty(Listener.PROPERTY_ISADDONLY);
					if (strIsAddonly != null) {
						wrapper.setAddOnly(Boolean.parseBoolean(strIsAddonly));
					}
					WatcherService watcher = new WatcherService(wrapper);
					watcherList.add(watcher);
					watcher.start();
					return service;
				}
			}
			return null;
		}

		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
			WatcherService entryToRemove = null;
			if (service instanceof Listener) {
				synchronized (this) {
					for (WatcherService s : watcherList) {
						Listener candidate = s.getListener();
						if (candidate instanceof ListenerWrapper) {
							candidate = ((ListenerWrapper) candidate).getDelegate();
						}
						if (candidate == service) {
							entryToRemove = s;
						}
					}
					if (entryToRemove != null) {
						entryToRemove.stop();
						watcherList.remove(entryToRemove);
					}
				}
			}
			
		}
		
	}
	
	private ServiceTracker tracker = null;
	public void start(BundleContext context) {
		this.context = context;
		tracker = new ServiceTracker(context, Listener.class.getName(), new ListenerTrackerCustomizer());
		tracker.open();
	}

	public void stop(BundleContext context) {
		tracker.close();
		for (WatcherService watcher : watcherList) {
			watcher.stop();
		}
	}

}
