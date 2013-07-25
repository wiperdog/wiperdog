
/**
 * registerService
 *   OSGiサービス登録
 * @return ServiceRegistration オブジェクト
 * @param svcname 登録サービス名
 * @param cls 実装するJavaインタフェース、(非文字列）
 * @param property サービスプロパティ
 * @param handle (有れば)前回のServiceRegistrationオブジェクト
 *
 * インタフェースを実装したJavaScriptオブジェクトを用意し(obj)、以下の様に呼び出す。
 *   (felix コマンドの例)
 * registerService("org.apache.felix.shell.Command", org.apache.felix.shell.Command, obj, null);
 * 
 */
function registerService(svcname, cls, def, property, handle) {
	var svcobj = new cls(def);

	if (handle != undefined && handle != null) {
		handle.unregister();
	}

	return ctx.registerService(svcname, svcobj, property);
}


/**
 * registerFelixCommand
 *   felix 2.x のshellサービス用のコマンドを登録する。
 * @return ServiceRegistrationオブジェクト
 * @param def Commandの実装部分(JavaScriptオブジェクト)
 * @param handle 前回のServiceRegistrationオブジェクト（オプション)
 *
 *   使用する時は、
 *   var mycmd = {
 *       getName: function() {return "cmdname";} ,
 *       execute: function(line, out, err) { ... コマンドの内容 ... },
 *       getShortDescription: function() {},
 *       getUsage: function() {}
 *   };
 *
 *   を自前で作成し、registerFelixCommand(mycmd);
 *   と呼び出す。
 *   getShortDescriptionと、getUsageは無くても良い。
 *
 *   追加の引数で、前回の当関数の戻り値を渡すと、一度サービスをunregisterしてregisterする。 *
 */
function registerFelixCommand(def, handle) {
	var cmdHandle = null;
	if (def.name != null) {

		var proxyObj = {
			delegate: def, 
			execute: function(line, out, err) {
				if (this.delegate != null) {
					return this.delegate.execute(line, out, err);
				} else {
					return "";
				}
			},
			getName: function() {
				if (this.delegate != null) {
					return this.delegate.name;
				}
			} ,
			getShortDescription: function() {
				if (this.delegate != null && this.delegate.shortDescription != null) {
					return this.delegate.shortDescription;
				} else {
					return this.delegate.name;
				}
			},
			getUsage: function() {
				if (this.delegate != null && this.delegate.usage != null) {
					return this.delegate.usage;
				} else {
					return this.delegate.name;
				}
			}
		};

		cmdHandle = registerService("org.apache.felix.shell.Command", org.apache.felix.shell.Command, proxyObj, null, handle);
	}
	return cmdHandle;
}

/**
 * registerAddingTracker
 *   サービスの追加（もしくは存在）をトラッキング
 * @return ServiceTracker オブジェクト
 * @param name トラッキングするサービスの名称
 * @param listener サービスの追加で呼ばれるlistener
 * @param tracker 前回のtracker(optional)
 * 
 * サービスが追加された、もしくはこの関数呼出し時に既に存在する場合、
 *   listener.addingService(object) 
 * が呼ばれる。
 *   もしくは、ServiceReference を使用したい場合は、
 *   listener.addingServiceReference(reference)
 *   が呼ばれる。
 */
function registerAddingTracker(name, listener, tracker) {

	var customizer = new org.osgi.util.tracker.ServiceTrackerCustomizer({
		delegate: listener, 
		addingService: function(reference) {
			var obj = ctx.getService(reference);

			if (this.delegate != null && this.delegate.addingService != null) {
				this.delegate.addingService(obj);
			} else if (this.delegate != null && this.delegate.addingServiceReference != null) {
				this.delegate.addingServiceReference(reference);
			}
			return obj;
		},
		modifiedService: function(reference, object) {
			if (this.delegate != null && this.delegate.modifiedService != null) {
				this.delegate.modifiedService(object);
			}
		},
		removedService: function(reference, object) {
			if (this.delegate != null && this.delegate.removedService != null) {
				this.delegate.removedService(object);
			}
		}
	});

	if (tracker) {
		tracker.close();
	}
	var newtracker;
	// name が '('で始まっていたら、フィルタとして
	var pat = /^\(/;
	if (name.match(pat)) {
		// nameはフィルタパターン
		var filter = ctx.createFilter(name);
		newtracker = new org.osgi.util.tracker.ServiceTracker(ctx, filter, customizer);
	} else {
		// name はクラス名
		newtracker = new org.osgi.util.tracker.ServiceTracker(ctx, name, customizer);
	}
	newtracker.open();

	return newtracker;
}

/**
 * registerAddingTrackerのBundleTracker版
 */
function registerAddingBundleTracker(listener, handle) {
	var customizer = new org.osgi.util.tracker.BundleTrackerCustomizer({
		delegate: listener,
		addingBundle: function(bundle, event) {
			if (this.delegate != null && this.delegate.addingBundle != null) {
				return this.delegate.addingBundle(bundle, event);
			}
			return null;
		},
		modifiedBundle: function(bundle, event, obj) {
			if (this.delegate != null && this.delegate.modifiedBundle != null) {
				return this.delegate.modifiedBundle(bundle, event, obj);
			}
			return null;
		},
		removedBundle: function(bundle, event, obj) {
			if (this.delegate != null && this.delegate.removedBundle != null) {
				return this.delegate.removedBundle(bundle, event, obj);
			}
			return null;
		}
	});

	if (handle) {
		handle.close();
	}

	var mask = 65535;
	if (listener.mask != null) {
		mask = listener.mask;
	}
	var newtracker = new org.osgi.util.tracker.BundleTracker(ctx, mask, customizer);
	newtracker.open();

	return newtracker;
}

/**
 * felix command オブジェクトを後で呼ぶために名前で取得する。
 */
function getFelixShellCommand(cmdName) {
	var refShell = ctx.getServiceReference("org.apache.felix.shell.ShellService");
	if (refShell != null) {
		var svcShell = ctx.getService(refShell);
		var refCommand = svcShell.getCommandReference(cmdName);
		if (refCommand != null) {
			var objCommand = ctx.getService(refCommand);
			var proxyobj = {
				name: cmdName,
				command: objCommand,
				execute: function (args, cout, cerr) {
					var line = this.command.getName();
					var tout = java.lang.System.out;
					var terr = java.lang.System.err;
					if (args != null) {
						line = line + " " + args;
					}
					if (cout != null) {
						tout = cout;
					}
					if (cerr != null) {
						terr = cerr;
					}
					return objCommand.execute(line, tout, terr);
				}
			};

			return proxyobj;
		}
	}
	return null;
}

/**
 * スクリプトの再読み込み(編集時など)に対応したい時、ユニークなグローバル変数に
 * 値をもっておくと便利。
 * Javaから呼ばれる関数から呼んでも正しく動作しないので注意
 */
function getScriptGlobal(template) {
	// scriptpathは呼出し元の値が入っている。
	var gvals = globalmap.get(scriptpath);
	if (gvals == null) {
		gvals = template;
		globalmap.put(scriptpath, gvals);
	}

	return gvals;
}


/**
 * ユニークな名前で値を保持
 */
function setScriptGlobal(data) {
	// scriptpathは呼出し元の値が入っている。
	globalmap.put(scriptpath, data);
}

/**
 *
 */
function getForeignGlobal(id) {
	var data = globalmap.get(id);
	if (data == null) {
		var path = scriptdir + java.io.File.separator + id;
		data = globalmap.get(path);
	}
	return data;
}
