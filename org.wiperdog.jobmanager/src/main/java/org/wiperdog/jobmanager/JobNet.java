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

import java.util.Collection;
import java.util.List;

/**
 * 
 * @author kurohara
 *
 */
public interface JobNet {

	/**
	 * ジョブネット内に強制実行端末要素の作成
	 * @param name 要素名
	 * @param jobName 対応ジョブ名
	 * @return 端末要素
	 */
	Terminal createForceRunTerminal(String name, String jobName);

	/**
	 * ジョブネット内に実行抑制端末用素の作成
	 * @param name 要素名
	 * @param jobName 対応ジョブ名
	 * @return 端末要素
	 */
	Terminal createProhibitTerminal(String name, String jobName);

	/**
	 * ジョブネット内にORオペレータ要素を作成
	 * @param name 要素名
	 * @return オペレータ要素
	 */
	Operator createOrOperator(String name);

	/**
	 * ジョブネット内にANDオペレータ要素を作成
	 * @param name 要素名
	 * @return オペレータ要素
	 */
	Operator createAndOperator(String name);

	/**
	 * ジョブネット内にXORオペレータ要素を作成
	 * @param name 要素名
	 * @return オペレータ要素
	 */
	Operator createXorOperator(String name);

	/**
	 * ジョブネット内にNOTオペレータ要素を作成
	 * @param name 要素名
	 * @return オペレータ要素
	 */
	Operator createNotOperator(String name);

	/**
	 * 
	 * @param name
	 * @return
	 */
	Operator createCounterOperator(String name, int count);

	/**
	 * ジョブネット内に割込み用端子を作成
	 * @param name 端子要素名
	 * @return 端子要素
	 */
	Receiver createInterruptFollower(String name);

	/**
	 * 割込み実行
	 * @param portName 端子名
	 * @param v 割込み値
	 */
	void interruptNet(String portName, boolean v);

	/**
	 * ジョブネット内全要素取得
	 * @return 要素のリスト
	 */
	List<? extends Object> getNodeList();

	/**
	 * 
	 * @param objname
	 * @return
	 */
	Object getNode(String objname);
	
	/**
	 * 要素を接続
	 * @param upper 上流要素名
	 * @param lower 下流要素名
	 * @throws ConditionBoardException 
	 * @throws ClassCastException 
	 */
	void connect(String upper, String lower) throws ClassCastException, ConditionBoardException;

	/**
	 * 要素の接続を外す
	 * @param upper 上流要素名
	 * @param lower 下流要素名
	 */
	void disconnect(String upper, String lower);

	/**
	 * 名前を取得
	 * @return
	 */
	String getName();

	
}
