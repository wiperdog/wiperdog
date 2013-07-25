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
package org.wiperdog.jobmanager;


public interface RootJobReceiver {
	public static enum ASSIGNMENT {
		VOID,
		CONVERT_TRUE,
		CONVERT_FALSE,
		PSEUDOJOB
	}

	void setInterruptedAssignement(RootJobReceiver.ASSIGNMENT a);
	
	void setMisfiredAssignment(RootJobReceiver.ASSIGNMENT a);

	public void setOutPatternAssignment(RootJobReceiver.ASSIGNMENT a);
	
	public void setErrPatternAssignment(RootJobReceiver.ASSIGNMENT a);
	
	public void setOutPattern(String pattern);
	
	public void setErrPattern(String pattern);
}
