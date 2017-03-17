package com.pharbers.aqll.stub

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.pharbers.aqll.alcalc.alfilehandler.altext.FileOpt
import com.pharbers.aqll.alcalc.aljobs.aljobtrigger.alJobTrigger.{calc_register, group_register, push_max_job, worker_register}
import com.pharbers.aqll.alcalc.almain.{alCalcActor, alDriverSingleton, alGroupActor, alMaxDriver}
import com.typesafe.config.ConfigFactory

/**
  * Created by qianpeng on 2017/3/11.
  */
object stub_test_4 extends App{
	val config = ConfigFactory.load("split-worker_1")
	val system = ActorSystem("calc", config)

	if (system.settings.config.getStringList("akka.cluster.roles").contains("splitworker")) {
		Cluster(system).registerOnMemberUp {
			val a = system.actorSelection("akka.tcp://calc@127.0.0.1:2551/user/splitreception")
			val c = system.actorOf(alCalcActor.props)
			println(s"actorSelection = $a")
			if(!FileOpt("/Users/qianpeng/Desktop/scp").isDir) FileOpt("/Users/qianpeng/Desktop/scp").createDir
			a ! calc_register(c)
			a ! push_max_job("""config/new_test/2016-01.xlsx""")
		}
	}
}
