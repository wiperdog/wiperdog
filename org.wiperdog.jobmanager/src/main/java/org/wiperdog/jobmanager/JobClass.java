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

import java.util.List;

import org.quartz.JobKey;

public interface JobClass {
	/**
	 * JobClassをクローズする
	 */
	void close();
	
	/**
	 * JobClass名を取得
	 * @return
	 */
	String getName();
	
	/**
	 * 設定された同時実行数を取得
	 * @return
	 */
	int getConcurrency();
	
	/**
	 * ジョブが同時実行数の制限によりスケジュール時刻に実行できないときの最大待ち時間
	 * を取得
	 * @return
	 */
	long getMaxWaitTime();
	
	/**
	 * ジョブが実行開始されたときの実行継続可能時間を取得
	 * 実行継続時間を過ぎたジョブはinterruptされる。
	 * @return
	 */
	long getMaxRunTime();

	/**
	 * 最大同時実行数を設定
	 * @param nc
	 */
	void setConcurrency(int nc);
	
	/**
	 * 最大待ち時間をセット
	 * @param waittime
	 */
	void setMaxWaitTime(long waittime);
	
	/**
	 * 最大実行継続時間をセット
	 * @param runtime
	 */
	void setMaxRunTime(long runtime);

	/**
	 * ジョブクラスにジョブを追加
	 * @param key ジョブキー
	 */
	void addJob(JobKey key);
	
	/**
	 * ジョブクラスからジョブを削除
	 * @param key
	 */
	void deleteJob(JobKey key);
	
	/**
	 * 所属リストを取得
	 * @return
	 */
	List<JobKey> getAssignedList();
	
	/**
	 * 現在実行中のジョブ数を取得
	 * @return
	 */
	int getCurrentRunningCount();
	
	/**
	 * 現在ジョブ実行が停止されたジョブのリスト
	 * @return
	 */
	List<JobKey> getVetoedList();
	
	/**
	 * 同時実行数制限により実行待ちしているjobをキャンセルする。
	 * @param jk
	 */
	void cancelSpecifiedVetoedJob(JobKey jk);
	
	/**
	 * ジョブが、同時実行数の制限で止められているかどうか。
	 * 
	 * @param jobkey
	 * @return
	 */
	boolean isJobVetoed(JobKey jobkey);
}
