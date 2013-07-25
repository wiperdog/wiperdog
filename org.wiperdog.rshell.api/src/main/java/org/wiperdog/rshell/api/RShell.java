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
package org.wiperdog.rshell.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Restricted Shell Definition interface.
 * 
 * @author kurohara
 *
 */
public interface RShell {
	/**
	 * get name of this Restricted Shell.
	 * @return
	 */
	public String getName();

	/**
	 * get program arguemts.
	 * @return
	 */
	public String getProgramArgs();

	/**
	 * get transfer limit size.
	 * @return
	 */
	public long getXferLimit();

	/**
	 * get timeout for running.
	 * @return
	 */
	public long getTimeout();
	
	/**
	 * start process with parameters.
	 * @param parameters
	 * @return
	 * @throws IOException
	 */
	public java.lang.Process run(String [] parameters) throws IOException;
	
	/**
	 * start process with parameters.
	 * @param param
	 * @return
	 * @throws IOException
	 */
	public java.lang.Process run(String param) throws IOException;
}
