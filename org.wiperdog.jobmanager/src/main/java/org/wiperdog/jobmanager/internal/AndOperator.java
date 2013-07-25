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

/**
 * 
 * @author kurohara
 *
 */
public class AndOperator extends AbstractOperator {
	boolean pvalue = false;
	public AndOperator(String name) {
		super(name);
		logger.trace("AndOperator.AndOperator(" + name + ")");
	}
	
	@Override
	protected boolean chkSwitchBoard() {
		logger.trace("AndOperator.chkSwitchBoard()");
		pvalue = true;
		for (Boolean b : switchboard) {
			logger.trace("	" + b);
			if (! b) {
				pvalue = false;
			}
		}
		return pvalue;
	}

	public boolean getPValue() {
		return pvalue;
	}

}
