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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * 
 * @author kurohara
 *
 */
public interface JobFacade {

	public enum ControlJobType {
		TERMINATEJOB
	}
	
	/**
	 * createJobClass
	 * Jobクラスを作成して返却する。
	 * @param name
	 * @return
	 * @throws JobManagerException 
	 */
	JobClass createJobClass(String name) throws JobManagerException;

	/**
	 * ジョブクラスを作成して返却する。
	 * 
	 * @param name
	 * @param concurrency
	 * @param maxWaitTime
	 * @param maxRuntime
	 * @return
	 * @throws JobManagerException 
	 */
	JobClass createJobClass(String name, int concurrency, long maxWaitTime, long maxRuntime) throws JobManagerException;
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	JobClass getJobClass(String name);

	/**
	 * job name から job classを取得する。
	 * @param jobName
	 * @return
	 */
	JobClass [] findJobClassForJob(String jobName);
	
	/**
	 * 内部で使用しているschedulerオブジェクトを返却する。
	 * @return
	 */
	Object getSchedulerObject();
	
	/**
	 * 
	 * @param name
	 * @throws JobManagerException 
	 */
	void deleteJobClass(String name) throws JobManagerException;
	
	/**
	 * createJob
	 * ジョブの作成
	 * @param name
	 * @param scriptPathAndArguments
	 * @return
	 * @throws JobManagerException 
	 */
	JobDetail createJob(String name, String [] scriptPathAndArguments, boolean usePredefined) throws JobManagerException;
	
	/**
	 * createJob
	 * ジョブの作成
	 * @param name
	 * @param scriptPathAndArguments
	 * @param useOut
	 * @param useErr
	 * @return
	 * @throws JobManagerException 
	 */
	JobDetail createJob(String name, String [] scriptPathAndArguments, boolean useOut, boolean useErr, boolean usePredefined) throws JobManagerException;

	/**
	 * createJob
	 * ジョブの作成
	 * @param name
	 * @param className
	 * @param methodSignature
	 * @param args
	 * @return
	 * @throws JobManagerException 
	 */
	JobDetail createJob(String name, String className, String methodSignature, Object [] args) throws JobManagerException;

	/**
	 * createJob
	 * ジョブの作成
	 * @param name
	 * @param filterspec
	 * @param methodSignature
	 * @param args
	 * @return
	 * @throws JobManagerException 
	 */
	JobDetail createJob(String name, String [] filterspec, String methodSignature, Object [] args) throws JobManagerException;

	/**
	 * 
	 * @param executable
	 * @return
	 * @throws JobManagerException 
	 */
	JobDetail createJob(JobExecutable executable) throws JobManagerException;
	
	/**
	 * 制御ジョブの作成
	 * @param name
	 * @param type
	 * @param args
	 * @return
	 * @throws JobManagerException 
	 */
	public JobDetail createControlJob(String name, ControlJobType type, String [] args) throws JobManagerException;

	/**
	 * getJob
	 * ジョブの取得
	 * @param name
	 * @return
	 * @throws JobManagerException 
	 */
	JobDetail getJob(String name) throws JobManagerException;

	/**
	 * assignJobClass
	 * ジョブをジョブクラスに参加させる
	 * @param jobName
	 * @param className
	 * @throws JobManagerException 
	 */
	void assignJobClass(String jobName, String className) throws JobManagerException;

	/**
	 * 
	 * @param jobName
	 * @param className
	 * @throws JobManagerException 
	 */
	void revokeJobClass(String jobName, String className) throws JobManagerException;

	/**
	 * 
	 * @param className
	 * @throws JobManagerException 
	 */
	void revokeJobClass(String className) throws JobManagerException;
	
	/**
	 * createTrigger
	 * トリガの作成
	 * @param name
	 * @return
	 */
	Trigger createTrigger(String name);
	
	/**
	 * createTrigger
	 * トリガの作成
	 * @param name
	 * @param delay
	 * @return
	 */
	Trigger createTrigger(String name, long delay);
	
	/**
	 * 単発トリガを作成
	 * @param name
	 * @param at
	 * @return
	 */
	Trigger createTrigger(String name, Date at);
	
	/**
	 * cronトリガの作成
	 * @param name
	 * @param crondef
	 * @return
	 * @throws JobManagerException 
	 */
	Trigger createTrigger(String name, String crondef) throws JobManagerException;
	
	/**
	 * トリガを取得
	 * @param name
	 * @return
	 * @throws JobManagerException 
	 */
	Trigger getTrigger(String name) throws JobManagerException;
	
	// 2012-08-06 Luvina Insert start
	/**
	 * createTrigger: create trigger with name, time delay and interval
	 * @param name
	 * @param delay
	 * @param interval
	 * @return
	 * @throws JobManagerException
	 */
	Trigger createTrigger(String name, long delay, long interval);

	// 2012-08-06 Luvina Insert end

	/**
	 * Jobにスケジュールを割当
	 * @param job
	 * @param trigger
	 * @throws JobManagerException 
	 */
	void scheduleJob(JobDetail job, Trigger trigger) throws JobManagerException;
	
	/**
	 * jobをキックするが、その後jobが保持されない。
	 * 
	 * @param job
	 * @param trigger
	 * @throws JobManagerException 
	 */
	void triggerJobNondurably(JobDetail job, Trigger trigger) throws JobManagerException;

	/**
	 * トリガ削除
	 * @param trigger
	 * @throws JobManagerException 
	 */
	void unscheduleJob(Trigger trigger) throws JobManagerException;
	
	/**
	 * ジョブを削除
	 * @param job
	 * @throws JobManagerException 
	 */
	void removeJob(JobDetail job) throws JobManagerException;
	
	/**
	 * JobNetを作成
	 * @param name
	 * @return
	 */
	JobNet createJobNet(String name);

	/**
	 * JobNet取得
	 * @param name
	 * @return
	 */
	JobNet getJobNet(String name);

	/**
	 * JobNet内に 実行端末を作成
	 * @param net
	 * @param name
	 * @param jobName
	 * @return
	 */
	Terminal createForceRunTerminal(JobNet net, String name, String jobName);
	
	/**
	 * JobNet内に 実行端末を作成 ディレイ付き
	 * @param net
	 * @param name
	 * @param jobName
	 * @param interval
	 * @return
	 */
	Terminal createForceRunTerminal(JobNet net, String name, String jobName, long interval);
	
	/**
	 * JobNet内に実行抑制端末を作成
	 * @param net
	 * @param name
	 * @param jobName
	 * @return
	 */
	Terminal createProhibitTerminal(JobNet net, String name, String jobName);
	
	/**
	 * JobNet内に実行抑制端末を作成 タイムアウト付き
	 * @param net
	 * @param name
	 * @param jobName
	 * @param interval
	 * @return
	 */
	Terminal createProhibitTerminal(JobNet net, String name, String jobName, long interval);

	/**
	 * オペレータ作成
	 * @param net
	 * @param name
	 * @return
	 */
	Operator createOrOperator(JobNet net, String name);
	Operator createAndOperator(JobNet net, String name);
	Operator createXorOperator(JobNet net, String name);
	Operator createNotOperator(JobNet net, String name);
	Operator createCounterOperator(JobNet net, String name, int count);
	
	/**
	 * JobNetのノードを接続
	 * @param net
	 * @param upper
	 * @param lower
	 * @throws ClassCastException
	 * @throws ConditionBoardException
	 */
	void connect(JobNet net, String upper, String lower) throws ClassCastException, ConditionBoardException;
	
	/**
	 * JobNetのノード間の接続を切る
	 * @param net
	 * @param upper
	 * @param lower
	 */
	void disconnect(JobNet net, String upper, String lower);

	/**
	 * シグナル用擬似ジョブを作成
	 * @param net
	 * @param name
	 * @return
	 */
	Receiver createInterruptFollower(JobNet net, String name);
	
	/**
	 * JobNetの擬似Jobにシグナル
	 * @param net
	 * @param portName
	 * @param v
	 */
	void signalNet(JobNet net, String portName, boolean v);

	/**
	 * JobNet名を列挙
	 * @return
	 */
	Set<String> keySetNet();
	
	/**
	 * 全ノードのリストを取得
	 * @return
	 */
	List<Object> getNodeList();
	
	/**
	 * JobNetのノードをすべて取得
	 * @param netName
	 * @return
	 */
	List<Object> getNodeList(String netName);
	
	/**
	 * JobNetのノードを取得
	 * @param netName
	 * @param objname
	 * @return
	 */
	Object getNode(String netName, String objname);

	/**
	 * Job名を列挙
	 * @return
	 */
	Set<String> keySetJob();
	
	/**
	 * JobClass名を列挙
	 * @return
	 */
	Set<String> keySetClass();

	/**
	 * トリガのキーを列挙
	 * @return
	 */
	Set<TriggerKey> getTriggerKeys();
	
	/**
	 * トリガを取得
	 * @param key
	 * @return
	 */
	Trigger getTrigger(TriggerKey key);

	/**
	 * get the JobReceiver connected to the job with given name.
	 * @param name
	 * @return
	 */
	JobReceiver getJobReceiver(String name);
	
	/**
	 * スケジューラを一時停止
	 * @throws JobManagerException 
	 */
	void pause() throws JobManagerException;
	
	/**
	 * スケジューラを再開
	 * @throws JobManagerException 
	 */
	void resume() throws JobManagerException;
	
	/**
	 * Jobの実行結果を取得
	 * @param name
	 * @return
	 */
	List<JobResult> getJobResult(String name);

	/**
	 * JobKeyを生成
	 * @param name
	 * @return
	 */
	JobKey jobKeyForName(String name);
	
	/**
	 * TriggerKeyを生成
	 * @param name
	 * @return
	 */
	TriggerKey triggerKeyForName(String name);

	/**
	 * Jobの最長実行時間を設定
	 * @param name
	 * @param timelength
	 * @throws JobManagerException 
	 */
	void setJobLastingTime(String name, long timelength) throws JobManagerException;

	/**
	 * ジョブの出力するデータの最大保持サイズを設定
	 * ShellJobだけに適用可能。 ShellJob以外のジョブに設定するとException。
	 * @param name
	 * @param size
	 * @throws JobManagerException
	 */
	void setJobDataReceiveSize(String name, int size) throws JobManagerException;

	/**
	 * ジョブの出力するデータの最大保持サイズを取得
	 * 
	 * @param name
	 * @return
	 * @throws JobManagerException
	 */
	int getJobDataReceiveSize(String name) throws JobManagerException;
	
	/**
	 * 実行結果の履歴保持数を設定
	 * @param name
	 * @param length
	 * @throws JobManagerException
	 */
	void setJobHistoryLength(String name, int length) throws JobManagerException;

	/**
	 * 実行結果の履歴保持数を設定
	 * @param name
	 * @return
	 * @throws JobManagerException
	 */
	int getJobHistoryLength(String name) throws JobManagerException;
	
	/**
	 * 実行中のJobを停止
	 * 
	 * @param name
	 * @return
	 * @throws JobManagerException 
	 */
	boolean interruptJob(String name) throws JobManagerException;
	
	/**
	 * 現在実行中のジョブをリストアップ
	 * 一つのジョブが複数同時に実行中の場合は、一つだけ返される。
	 * @return
	 */
	Set<String> getRunningJobSet();

	/**
	 * 次の実行までの時間を返す、実行の予定が無い場合、負の値が返される。
	 * ミリ秒
	 * @param jobname
	 * @return
	 * @throws JobManagerException 
	 */
	long getJobNextFireLatency(String jobname) throws JobManagerException;

	/**
	 * ジョブ名から、関連付けられているトリガを取得
	 * 
	 * @param jobname
	 * @return
	 * @throws JobManagerException 
	 */
	List<Trigger> getRelatedTrigger(String jobname) throws JobManagerException;
	
	/**
	 * ジョブは何個（何並列）実行中か？
	 * 
	 * @param jobname
	 * @return
	 */
	int getJobRunningCount(String jobname);
	
}
