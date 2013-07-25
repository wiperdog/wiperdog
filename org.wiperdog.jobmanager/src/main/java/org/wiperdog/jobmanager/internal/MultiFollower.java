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
import org.wiperdog.jobmanager.Follower;
import org.wiperdog.jobmanager.Predecessor;

/**
 * Follower
 * 後続ノードインタフェース
 * @author kurohara
 *
 */
public interface MultiFollower extends Follower {
	/**
	 * 先行ノードの 指定位置の後続ノードを自ノードにする。
	 * @param index 位置
	 * @param f 先行ノード
	 * @throws ConditionBoardException
	 */
	void connectUpperFlow(int index, Predecessor f) throws ConditionBoardException;
	
	/**
	 * 先行ノードの 指定位置から　自ノードへの接続を削除
	 * @param index
	 * @param p
	 * @throws ConditionBoardException
	 */
	void disconnectUpperFlow(int index, Predecessor p) throws ConditionBoardException;
}
