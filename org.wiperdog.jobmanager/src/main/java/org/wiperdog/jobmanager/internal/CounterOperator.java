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
public class CounterOperator extends AbstractOperator {
	private final int count;
	private int cc = 0;
	boolean pv = false;
	
	public CounterOperator(String name, int count) {
		super(name);
		this.count = count;
	}

	public boolean getPValue() {
		return pv;
	}

	
	@Override
	protected boolean chkSwitchBoard() {
		if (switchboard.get(0)) {
			pv = true;
			if (++cc > count) {
				pv = false;
			}
		} else {
			cc = 0;
			pv = false;
		}
		return pv;
	}

	@Override
	public void connectUpperFlow(int index, Predecessor f)
			throws ConditionBoardException {
		if (index > 0) {
			throw new  ConditionBoardException("only one upper node is acceptable for this node type");
		}
		super.connectUpperFlow(index, f);
	}

	@Override
	public void connectUpperFlow(Predecessor p)
			throws ConditionBoardException {
		if (upperConnectionList.size() > 0) {
			throw new  ConditionBoardException("only one upper node is acceptable for this node type");
		}
		super.connectUpperFlow(p);
	}
	
}
