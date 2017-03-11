package com.pharbers.aqll.alcalc.almain

import akka.actor.{Actor, ActorContext, ActorLogging, ActorSystem, Props, Scheduler}
import com.pharbers.aqll.alcalc.aljobs.alJob
import com.pharbers.aqll.alcalc.aljobs.alJob.max_jobs
import com.pharbers.aqll.alcalc.aljobs.aljobtrigger._
import com.pharbers.aqll.alcalc.aljobs.aljobtrigger.alJobTrigger.{finish_max_job, push_max_job, schedule_jobs}

import scala.concurrent.stm.atomic
import scala.concurrent.stm.Ref
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Alfred on 10/03/2017.
  */
object alMaxDriver {
    def props = Props[alMaxDriver]
}

class alMaxDriver extends Actor
                     with ActorLogging
                     with alMaxJobsSchedule {

    override def receive = {
        case push_max_job(file_path) => {
            println(s"sign a job with file name $file_path")
            atomic { implicit txn =>
                jobs() = jobs() :+ max_jobs(file_path)
            }
        }

        case schedule_jobs() => {
//            println("schedule a job")
            var j : Option[alJob] = None
            atomic { implicit txn =>
                jobs() match {
                    case head :: lst => {
                        j = Some(head)
                        jobs() = lst
                    }
                    case Nil => {
                        j = None
                    }
                }
            }
            val result = j.map (x => x.result).getOrElse(None)
            println(result)
        }

        case finish_max_job(uuid) => {
            println(s"finish a job with uuid $uuid")
        }

        case _ => ???
    }
}

trait alMaxJobsSchedule { this : Actor =>
    val jobs = Ref(List[alJob]())
    val timer = context.system.scheduler.schedule(0 seconds, 1 seconds, self, new schedule_jobs)
}
