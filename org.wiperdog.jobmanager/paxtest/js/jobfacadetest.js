
load(scriptdir + "/common/osgiutil.js");
load(scriptdir + "/common/valueutil.js");

(function() {
	var j1 = "myjob1";
	var j2 = "myjob2";
	
	function cleanup(jobFacade) {
		var job  = jobFacade.getJob(j1);
		if (job != null) {
			print("job '" + j1 + "' already exist, remove it first.");
			jobFacade.removeJob(job);
		}
	}

	function testfunc(jobFacade) {
		print("got JobFacade service, do testing work now");
		cleanup(jobFacade);
		// ジョブを作成
		var job = jobFacade.createJob(j1, [ "/bin/sh", "-c", "sleep 10;/bin/ls -l /tmp" ], true, true, false);
		// トリガを作成、20秒後に発火
		var trigger = jobFacade.createTrigger(j1, 20000);
		// JobClassを作成
		var cls = jobFacade.createJobClass(j1+"class", 1, 30000, 30000);
		// JobClassにアサイン
		jobFacade.assignJobClass(j1, j1+"class");
		// スケジュールを投入
		jobFacade.scheduleJob(job, trigger);
		// job名からトリガを取得するテスト。
		var tlist = jobFacade.getRelatedTrigger(j1);
		print("tlist.size() : " + tlist.size());
		// 一つのジョブに複数トリガの場合アリ
		for (var i = 0;i < tlist.size();++i) {
			print("trigger for job '" + j1 + "' : " + tlist.get(i).getKey().getName());
		}
		// 実行前ジョブを見る
		for (var i = 0;i < 5;++i) {
			var latency = jobFacade.getJobNextFireLatency(j1);
			print("latency of " + j1 + " is " + latency);
			java.lang.Thread.sleep(1000);
		}
		// 実行中のこのジョブは何個あるか？
		var nr = jobFacade.getJobRunningCount(j1);
		print("running count of " + j1 + " is " + nr);
		// JobClassから見て何個実行中か？
		nr = cls.getCurrentRunningCount();
		print("running count of " + j1 + "class is " + nr);
		// 実行開始されるまで待つ
		java.lang.Thread.sleep(16000); // sleep 16 sec
		// 実行中のこのジョブは何個あるか？
		nr = jobFacade.getJobRunningCount(j1);
		print("running count of " + j1 + " is " + nr);
		// JobClassから見て何個実行中か？
		nr = cls.getCurrentRunningCount();
		print("running count of " + j1 + "class is " + nr);
		// 実行終了するまで待つ
		java.lang.Thread.sleep(10000); // sleep 10 sec
		// 実行中のこのジョブは何個あるか？
		nr = jobFacade.getJobRunningCount(j1);
		print("running count of " + j1 + " is " + nr);
		// JobClassから見て何個実行中か？
		nr = cls.getCurrentRunningCount();
		print("running count of " + j1 + "class is " + nr);
		var latency = jobFacade.getJobNextFireLatency(j1);
		print("latency of " + j1 + " is " + latency);
	
		print("end test for JobFacade");
	}

	var clsobj = {
		addingService: function(obj) {
			if (obj != null) {
				print("creating test thread");
				(new java.lang.Thread(
					new java.lang.Runnable() {
						run: function() {
							testfunc(obj);
						}
					})
				).start();
			}
		},
	};

	var handles = getScriptGlobal({tracker:null});
	handles.tracker = registerAddingTracker("com.insight_tec.pi.jobmanager.JobFacade", clsobj, handles.tracker);

})();

