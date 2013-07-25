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

/**
 * Follower
 * 
 * @author kurohara
 *
 */
public interface Follower {
	/**
	 * 先行ノードに自ノードを追加する
	 * @param f 先行ノード
	 * @throws ConditionBoardException
	 */
	void connectUpperFlow(Predecessor f) throws ConditionBoardException;
	
	/**
	 * 先行ノードから自ノードへの接続を削除
	 * @param p
	 * @throws ConditionBoardException
	 */
	void disconnectUpperFlow(Predecessor p) throws ConditionBoardException;
	
}
