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
public class XorOperator extends AbstractOperator {
	boolean pv = false;
	public XorOperator(String name) {
		super(name);
		logger.trace("XorOperator.XorOperator(" + name + ")");
	}
	
	@Override
	protected boolean chkSwitchBoard() {
		boolean b = false;
		for (boolean sb : switchboard) {
			if (b && sb) {
				b = false;
				break;
			}
			if (sb) {
				b = true;
			}
		}
		pv = b;
		return b;
	}

	public boolean getPValue() {
		return pv;
	}

}
