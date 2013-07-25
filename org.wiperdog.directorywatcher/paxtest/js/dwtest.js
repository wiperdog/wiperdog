load(scriptdir + "/common/osgiutil.js");

(function() {
	importClass(java.lang.Thread);
	deleteSymbol("Date");
	importClass(java.util.Date);
	importClass(java.io.File);
	importClass(java.util.Hashtable);
	importClass(   com.insight_tec.pi.directorywatcher.Listener);
	var clsname = "com.insight_tec.pi.directorywatcher.Listener";

	var listener1 = {
		datadir: scriptdir + "/../data",
		getDirectory: function() {
			return this.datadir;
		},
		getInterval: function() {
			return 1000;
		},
		filterFile: function(file) {
			return true;
		},
 		notifyModied: function(file) {
			print("LISTENER-1: modification is notified for: " + file.getPath());
			return false;
		},
		notifyAdded: function(file) {
			print("LISTENER-1: addition is notified for: " + file.getPath());
			return false;
		},
		notifyDeleted: function(file) {
			print("LISTENER-1: deletion is notified for: " + file.getPath());
			return false;
		},
	};

	var listener2 = {
		datadir: scriptdir + "/../data",

		getDirectory: function() {
			return this.datadir;
		},
		getInterval: function() {
			return 1000;
		},
		filterFile: function(file) {
			return true;
		},
 		notifyModied: function(file) {
			print("LISTENER-2: modification is notified for: " + file.getPath());
			return false;
		},
		notifyAdded: function(file) {
			print("LISTENER-2: addition is notified for: " + file.getPath());
			return false;
		},
		notifyDeleted: function(file) {
			print("LISTENER-2: deletion is notified for: " + file.getPath());
			return false;
		},
	};

	var testcmd = {
		datadir: scriptdir + "/../data/", 
		name: "dwtest",
		createTestFile: function(name) {
			var of = new File(this.datadir + name);
			// of.mkdirs();
			of.createNewFile();
		},
		deleteTestFile: function(name) {
			var of = new File(this.datadir + name);
			of["delete"]();
		},
		modifyTestFile: function(name) {
			var of = new File(this.datadir + name);
			of.setLastModified((new Date()).getTime());
		},
		execute: function() {
			// 
			print("creating testfile");
			this.createTestFile("testfile1");
			Thread.sleep(2000); // wait for 2 sec
			print("creating testfile");
			this.createTestFile("subdir/testfile1");
			Thread.sleep(2000);
			print("creating testfile");
			this.createTestFile("subdir/subsubdir/testfile1");
			Thread.sleep(2000);
			print("modifing testfile");
			this.modifyTestFile("testfile1");
			this.modifyTestFile("subdir/testfile1");
			this.modifyTestFile("subdir/subsubdir/testfile1");
			Thread.sleep(2000);
			print("deleting testfile");
			this.deleteTestFile("subdir/subsubdir/testfile1");
			this.deleteTestFile("subdir/testfile1");
			this.deleteTestFile("testfile1");
		},
	};

	var handles = getScriptGlobal({ listener1: null, listener2: null, cmd: null});
	var prop1 = new java.util.Hashtable();
	var prop2 = new java.util.Hashtable();

//	prop1.put(Listener.PROPERTY_DEPTH, "3");
//	prop1.put(Listener.PROPERTY_HANDLERETRY, "false");

	prop2.put(Listener.PROPERTY_DEPTH, "2");
	prop2.put(Listener.PROPERTY_HANDLERETRY, "true");

	// register DirectoryWatcher Listener - 1
	handles.listener1 = registerService(clsname, Listener, listener1, prop1, handles.listener1);
	handles.listener2 = registerService(clsname, Listener, listener2, prop2, handles.listener2);
 
	// register test command
	handles.cmd = registerFelixCommand(testcmd, handles.cmd);

})();

