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
package org.wiperdog.directorywatcher;

import java.io.File;
import java.io.IOException;
/**
 * directorywatcher リスナ インタフェース
 * このインタフェースを実装して OSGiにサービス登録すればOK
 * 自動的にWatcherServiceの管理下に入る。
 * 
 * 登録後にディレクトリパス等パラメータを変更したい時は、一度 ServiceRegistration.unregister()を行って、
 * パラメータの変更後、改めてサービス登録を行えばよい。
 * 
 * @author kurohara
 *
 */
public interface Listener {
	static final String PROPERTY_LISTENERNAME = "listenername";
	
	static final String PROPERTY_DEPTH = "depth";
	
	static final String PROPERTY_HANDLERETRY = "handleRetry";

	static final String PROPERTY_SORT_NAMEFIRST = "namefirst";
	
	static final String PROPERTY_SORT_ASCENDANT = "ascendant";
	
	static final String PROPERTY_ISADDONLY = "isaddonly";
	
	/**
	 * 監視したいディレクトリを返却
	 * 監視開始前に一度呼ばれる。
	 * 
	 * @return
	 */
	String getDirectory();
	
	/**
	 * 監視間隔を返却
	 * 監視開始前に一度呼ばれる
	 * 
	 * ミリ秒
	 * @return
	 */
	long getInterval();
	
	/**
	 * ファイルのフィルタ処理
	 * 監視対象だった時はtrueを返す。
	 * 対象ディレクトリの全ファイルについて呼ばれる。
	 * ファイル名のチェック(拡張子など)のみ行えばよい。
	 * 更新時刻などのチェックは、WatcherServiceにより行われる。
	 * 
	 * 毎インターバルに呼ばれる。
	 * 
	 * @param file
	 * @return
	 */
	boolean filterFile(File file);

	/**
	 * 監視対象のファイルで、変更されたファイルについてよばれる。
	 * @param target
	 * @return
	 * @throws IOException
	 */
	boolean notifyModified(File target) throws IOException;

	/**
	 * 監視対象のファイルで、追加されたファイルについて呼ばれる。
	 * @param target
	 * @return
	 * @throws IOException
	 */
	boolean notifyAdded(File target) throws IOException;
	
	/**
	 * 監視対象のファイルで、削除されたファイルに付いて呼ばれる。
	 * @param target
	 * @return
	 * @throws IOException
	 */
	boolean notifyDeleted(File target) throws IOException;
}
