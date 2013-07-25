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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.wiperdog.jobmanager.ConditionBoardException;
import org.wiperdog.jobmanager.Flow;
import org.wiperdog.jobmanager.JobPatchBoard;
import org.wiperdog.jobmanager.Operator;
import org.wiperdog.jobmanager.Predecessor;


/**
 * 
 * @author kurohara
 *
 */
public abstract class AbstractOperator implements Operator {
	protected final String id;
	protected final List<Flow> outFlowList = new ArrayList<Flow>();
	protected final List<Flow> upperConnectionList = new ArrayList<Flow>(); // 上流と接続している自ノードのFlow(接続)リスト
	protected final List<Boolean> switchboard = new ArrayList<Boolean>();
	protected final Logger logger = Logger.getLogger(Activator.LOGGERNAME);

	protected AbstractOperator() {
		logger.trace("AbstractOperator.AbstractOperator()");
		id = "Operator_" + JobPatchBoard.getSequenceNumber();
	}
	
	protected AbstractOperator(String name) {
		logger.trace("AbstractOperator.AbstractOperator(" + name + ")");
		id = name;
	}

	public boolean[] getTValues() {
		boolean [] tv = new boolean [switchboard.size()];
		for (int i = 0;i < switchboard.size();++i) {
			tv[i] = switchboard.get(i);
		}
		return tv;
	}
	
	public String getName() {
		return id;
	}
	
	public Flow[] getOutFlows() {
		Flow [] ofs = new Flow[outFlowList.size()];
		outFlowList.toArray(ofs);
		return ofs;
	}
	
	public String toString() {
		String str = this.getClass().getSimpleName() + "(" + id + ") outflows :{";
		
		for (Flow f: outFlowList) {
			str += f.getId() + ",";
		}
		
		str += "}";
			
		return str;
	}
	
	/**
	 * 上流接続実装
	 * @author kurohara
	 *
	 */
	private final class InFlow implements Flow {
		private final Predecessor upper;
		private final int position;
		
		public InFlow(Predecessor upper, int position) {
			logger.trace("AbstractOperator.InFlow.InFlow(" + upper.toString() + "," + position);
			this.upper = upper;
			this.position = position;
			if (switchboard.size() <= position) {
				for (int i = switchboard.size();i <= position;++i) {
					switchboard.add(Boolean.valueOf(false));
				}
			}
		}
		
		public void disconnect() throws ConditionBoardException {
			logger.trace("AbstractOperator.InFlow.disconnect()");
			upper.deleteOutFlow(this);
		}
		
		public String getId() {
			logger.trace("AbstractOperator.InFlow.getId()");
			return AbstractOperator.this.id;
		}

		public void call(boolean v) {
			logger.trace("AbstractOperator.InFlow.call(" + v + ") - " + AbstractOperator.this.id + " - " + position);
			switchboard.set(position, Boolean.valueOf(v));
			flowOut(chkSwitchBoard());
		}

		public int getPosition() {
			logger.trace("InAbstractOperator.Flow.getPosition()");
			return position;
		}
		
		public Predecessor getPredecessor() {
			return upper;
		}
		
		public boolean isConnectedTo(Predecessor p) {
			logger.trace("AbstractOperator.InFlow.isConnectedTo(" + p.toString() + ")");
			// simply check referece
			if (upper == p) {
				return true;
			} else {
				return false;
			}
		}
	}

	protected abstract boolean chkSwitchBoard();
	
	/**
	 * 全出力先に通知
	 * @param v
	 */
	protected void flowOut(boolean v) {
		logger.trace("AbstractOperator.flowOut(" + v + ")");
		for (Flow f : outFlowList) {
			f.call(v);
		}
	}

	/**
	 * 出力先の追加
	 */
	public void addOutFlow(Flow flow) throws ConditionBoardException {
		logger.trace("AbstractOperator.addOutFlow(" + flow.toString() + ")");
		outFlowList.add(flow);
	}

	/**
	 * 出力先の削除
	 */
	public void deleteOutFlow(Flow flow) throws ConditionBoardException {
		logger.trace("AbstractOperator.deleteOutFlow(" + flow.toString()+ ")");
		outFlowList.remove(flow);
	}

	/**
	 * Flow(接続パイプ)を作成して所定の位置に保持するのみ
	 * @param o
	 * @return
	 * @throws ConditionBoardException
	 */
	private Flow _connect(Predecessor o) throws ConditionBoardException {
		logger.trace("AbstractOperator._connect(" + o.toString() + ")");
		_disconnect(o);
		Flow f = new InFlow(o, upperConnectionList.size());
		upperConnectionList.add(f);
		return f;
	}
	
	private Flow _disconnect(Predecessor o) throws ConditionBoardException {
		logger.trace("AbstractOperator._disconnect(" + o.toString() +")");
		Flow found = null;
		int i = 0;
		for (;i < upperConnectionList.size();++i) {
			Flow f = upperConnectionList.get(i);
			if (((InFlow)f).isConnectedTo(o)) {
				((InFlow)f).disconnect();
				found = f;
				break;
			}
		}
		if (found != null) {
			upperConnectionList.remove(i);
		}
		return found;
	}

	/**
	 * Flow(接続パイプ)を作成して所定の位置に保持するのみ
	 * @param index
	 * @param o
	 * @return
	 * @throws ConditionBoardException
	 */
	private Flow _connect(int index, Predecessor o) throws ConditionBoardException  {
		logger.trace("AbstractOperator._connect(" + index + "," + o.toString() + ")");
		// 指定位置のFlowを削除して挿入するので位置は保存されるはず。
		//
		_disconnect(index, o);
		Flow f = new InFlow(o, index);
		upperConnectionList.add(index, f);
		return f;
	}
	
	private Flow _disconnect(int index, Predecessor o) throws ConditionBoardException {
		logger.trace("AbstractOperator._disconnect(" + index + "," + o.toString() + ")");
		Flow of = upperConnectionList.remove(index);
		if (of != null && ((InFlow)of).isConnectedTo(o)) {
			((InFlow)of).disconnect();
		}
		return of;
	}
	
	public void connectUpperFlow(Predecessor f) throws ConditionBoardException {
		logger.trace("AbstractOperator.connectUpperFlow(" + f.toString() + ")");
		f.addOutFlow(_connect(f));
	}
	
	public void connectUpperFlow(int index, Predecessor f)
			throws ConditionBoardException {
		logger.trace("AbstractOperator.connectUpperFlow(" + index + "," + f.toString() + ")");
		f.addOutFlow(_connect(index, f));
	}
	
	public void disconnectUpperFlow(int index, Predecessor p)
			throws ConditionBoardException {
		logger.trace("AbstractOperator.disconnectUpperFlow(" + index + "," + p.toString() + ")");
		InFlow f = (InFlow) _disconnect(index, p);
		
		p.deleteOutFlow(f);
		
	}
	
	public void disconnectUpperFlow(Predecessor p)
			throws ConditionBoardException {
		logger.trace("AbstractOperator.disconnectUpperFlow(" + p.toString() + ")");
		InFlow f = (InFlow) _disconnect(p);
		p.deleteOutFlow(f);
	}
	
	public String getId() {
		logger.trace("AbstractOperator.getId()");
		return id;
	}
	
}
