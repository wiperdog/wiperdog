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

import org.wiperdog.jobmanager.ConditionBoardException;
import org.wiperdog.jobmanager.Predecessor;

/**
 * 
 * @author kurohara
 *
 */
public class NotOperator extends AbstractOperator {

	public NotOperator(String name) {
		super(name);
		logger.trace("NotOperator.NotOperator()");
	}

	@Override
	public void connectUpperFlow(Predecessor f) throws ConditionBoardException {
		if (upperConnectionList.size() > 0) {
			// error
			throw new ConditionBoardException("only 1 input is permitted for this Operator.");
		}
		super.connectUpperFlow(f);
	}
	
	@Override
	public void connectUpperFlow(int index, Predecessor f)
			throws ConditionBoardException {
		if (index  != 0) {
			// error
			throw new ConditionBoardException("only 1 input is permitted for this Operator");
		}
		super.connectUpperFlow(index, f);
	}
	
	@Override
	protected boolean chkSwitchBoard() {
		return ! switchboard.get(0);
	}

	public boolean getPValue() {
		return ! switchboard.get(0);
	}

}
