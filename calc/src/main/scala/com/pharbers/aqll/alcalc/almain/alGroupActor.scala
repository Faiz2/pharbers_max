package com.pharbers.aqll.alcalc.almain

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, Props}
import akka.routing.BroadcastPool
import com.pharbers.aqll.alcalc.alcmd.pkgcmd.pkgCmd
import com.pharbers.aqll.alcalc.aldata.alStorage
import com.pharbers.aqll.alcalc.aljobs.alJob.grouping_jobs
import com.pharbers.aqll.alcalc.aljobs.aljobstates.alMaxGroupJobStates.{group_coreing, group_doing}
import com.pharbers.aqll.alcalc.aljobs.aljobstates.{alMasterJobIdle, alPointState}
import com.pharbers.aqll.alcalc.aljobs.aljobtrigger.alJobTrigger._
import com.pharbers.aqll.alcalc.almaxdefines.alMaxProperty
import com.pharbers.aqll.alcalc.alstages.alStage
import com.pharbers.aqll.alcalc.alprecess.alprecessdefines.alPrecessDefines._
import com.pharbers.aqll.alcalc.aljobs.alJob._
import com.pharbers.aqll.alcalc.aljobs.alPkgJob
import com.pharbers.aqll.calc.excel.IntegratedData.IntegratedData
import com.pharbers.aqll.calc.util.GetProperties
import com.pharbers.aqll.stub.stub_test_1.process

import scala.concurrent.stm.atomic
import scala.concurrent.stm.Ref
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by BM on 11/03/2017.
  */

case class FileParentUuid(var uuid: String)

object alGroupActor {
    def props : Props = Props[alGroupActor]
}

class alGroupActor extends Actor
                     with ActorLogging
                     with FSM[alPointState, FileParentUuid]
                     with alCreateConcretGroupRouter
					 with alPkgJob{

    startWith(alMasterJobIdle, new FileParentUuid(""))

    when(alMasterJobIdle) {
        case Event(can_sign_job(), _) => {
            sender() ! sign_job_can_accept()
            stay()
        }

        case Event(group_job(p), data) => {
            atomic { implicit tnx =>
                concert_ref() = Some(p)
            }
	        data.uuid = p.uuid
            val cj = grouping_jobs(Map(grouping_jobs.max_uuid -> p.uuid, grouping_jobs.group_uuid -> p.subs.head.uuid))
            context.system.scheduler.scheduleOnce(0 seconds, self, grouping_job(cj))
            goto(group_coreing) using data
        }
    }

    when(group_coreing) {
        case Event(grouping_job(cj), data) => {
            val result = cj.result
            val (p, sb) = result.get.asInstanceOf[(String, List[String])]
            val q = sb.map (x => alMaxProperty(p, x, Nil))
            atomic { implicit tnx =>
                result_ref() = Some(alMaxProperty(concert_ref.single.get.get.uuid, p, q))
                adjust_index() = -1
            }

	        // TODO : 稍后封装

	        cur = Some(new pkgCmd(s"config/sync/$p" :: Nil, s"config/compress/${data.uuid}/sync$p") :: Nil)
	        process = do_pkg() :: Nil
	        super.excute()

	        concert_router ! concert_adjust()
            goto(group_doing) using data
        }
    }

    when(group_doing) {
        case Event(concert_group_result(sub_uuid), data) => {

            val r = result_ref.single.get.map (x => x).getOrElse(throw new Exception("must have runtime property"))
            
            r.subs.find (x => x.uuid == sub_uuid).map (x => x.grouped = true).getOrElse(Unit)

            // TODO : 稍后封装
            cur = Some(new pkgCmd(s"config/group/$sub_uuid" :: Nil, s"config/compress/${r.parent}/group$sub_uuid") :: Nil)
	        process = do_pkg() :: Nil
	        super.excute()

            if (r.subs.filterNot (x => x.grouped).isEmpty) {

                val common = common_jobs()
                common.cur = Some(alStage(r.subs map (x => s"config/group/${x.uuid}")))
                common.process = restore_grouped_data() ::
                                    do_calc() :: do_union() :: do_calc() ::
                                    do_map (alShareData.txt2IntegratedData(_)) :: do_calc() :: Nil
//                                    presist_data(Some(r.uuid), Some("group")) :: Nil
                common.result

                val concert = common.cur.get.storages.head.asInstanceOf[alStorage]
                val m = alStorage.groupBy (x =>
                    (x.asInstanceOf[IntegratedData].getYearAndmonth, x.asInstanceOf[IntegratedData].getMinimumUnitCh)
                )(concert)

                val g = alStorage(m.values.map (x => x.asInstanceOf[alStorage].data.head.toString).toList)
                g.doCalc
                val sg = alStage(g :: Nil)
                val pp = presist_data(Some(r.uuid), Some("group"))
                pp.precess(sg)

                println(s"post group result ${r.parent} && ${r.uuid}")

                    //"akka.tcp://calc@127.0.0.1:2551/user/splitreception"
                val st = context.actorSelection(GetProperties.singletonPaht)
                    //context.actorSelection("akka://calc/user/splitreception")
                st ! group_result(r.parent, r.uuid)
                goto(alMasterJobIdle) using data
            } else stay()
        }
    }

    whenUnhandled {
        case Event(can_sign_job(), _) => {
            sender() ! service_is_busy()
            stay()
        }

        case Event(group_job(p), _) => {
            sender() ! service_is_busy()
            stay()
        }

        case Event(concert_adjust_result(_), _) => {
            atomic { implicit tnx =>
                adjust_index() = adjust_index() + 1
                sender() ! concert_adjust_result(adjust_index())
            }

            if (adjust_index.single.get == 3) {
                concert_router ! concert_group(result_ref.single.get.get)
            }
            stay()
        }
    }

    val concert_ref : Ref[Option[alMaxProperty]] = Ref(None)            // 向上传递的，返回master的，相当于parent
    val result_ref : Ref[Option[alMaxProperty]] = Ref(None)             // 当前节点上计算的东西，相当于result
    val adjust_index = Ref(-1)
    val concert_router = CreateConcretGroupRouter
}

trait alCreateConcretGroupRouter { this : Actor =>
    def CreateConcretGroupRouter =
        context.actorOf(BroadcastPool(4).props(alConcertGroupActor.props), name = "concert-group-router")
}