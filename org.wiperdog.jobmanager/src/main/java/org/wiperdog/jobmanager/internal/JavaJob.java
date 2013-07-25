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
package org.wiperdog.jobmanager.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.quartz.JobExecutionContext;

public class JavaJob extends AbstractGenericJob {
	private String className;
	private String methodName;
	private Object [] args;
	private Class<?> clsobj;
	private Object tgtobj;
	private Method method;
	
	public void setClassName(String className) {
		this.className = className;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public void setArgs(Object [] args) {
		this.args = args;
	}
	
	private void  prepareJob() throws ClassNotFoundException, InstantiationException, IllegalAccessException  {
		try {
			clsobj = Class.forName(className);
			Method [] methods = clsobj.getMethods();
			for (Method m : methods) {
				if (m.getName().equals(methodName)) {
					Class<?> [] argtypes = m.getParameterTypes();
					int i = 0;
					boolean bMatched = true;
					for (Class<?> t : argtypes) {
						if (t.isInstance(args[i])) {
							
						} else {
							bMatched = false;
							break;
						}
					}
					if (bMatched) {
						method = m;
						break;
					}
				}
			}
			tgtobj = clsobj.newInstance();
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (InstantiationException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw e;
		}
	}
	
	@Override
	protected Object doJob(JobExecutionContext context) throws Throwable {
		Boolean result = Boolean.FALSE;
		prepareJob();
		try {
			method.invoke(tgtobj, args);
			result = Boolean.TRUE;
		} catch (IllegalAccessException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw e;
		}
		
		return result;
	}

}
