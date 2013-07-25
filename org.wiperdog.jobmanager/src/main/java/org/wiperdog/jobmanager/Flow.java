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
 * 結果のフロー(接続)インタフェース
 * 実世界での「水道パイプ」のようなイメージ
 * @author kurohara
 *
 */
public interface Flow {
	/**
	 * 接続のIDを取得
	 * @return
	 */
	String getId();
	
	/**
	 * 結果を後ろに流す
	 * @param v
	 */
	void call(boolean v);
}
