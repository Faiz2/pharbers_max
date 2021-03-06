package com.pharbers.aqll.pattern

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import module.CallAkkaHttpModuleMessage.msg_CallHttp
import module.ResultQueryModuleMessage._
import module.SampleCheckModuleMessage.msg_CheckBaseQuery
import module.LoginModuleMessage.msg_LoginBaseQuery
import module.FilesUploadModuleMessage._
import module.{ModelOperationModule, SampleReportModule, UserManageModule, MarketManageModule}
import module.ModelOperationModuleMessage._
import module.SampleReportModuleMessage._
import module.UserManageModuleMessage._
import module.MarketManageModuleMessage._

object PipeFilterActor {
	def prop(originSender : ActorRef, msr : MessageRoutes) : Props = {
		Props(new PipeFilterActor(originSender, msr))
	}
}

class PipeFilterActor(originSender : ActorRef, msr : MessageRoutes) extends Actor with ActorLogging {
	
	def dispatchImpl(cmd : CommonMessage, module : ModuleTrait) = {
		tmp = Some(true)
		module.dispatchMsg(cmd)(rst) match {
			case (_, Some(err)) => {
				originSender ! error(err)
				cancelActor					
			}
			case (Some(r), _) => {
				rst = Some(r) 
			}
			case _ => println("never go here")
		}
		rstReturn
	}
	
	var tmp : Option[Boolean] = None
	var rst : Option[Map[String, JsValue]] = msr.rst
	var next : ActorRef = null
	def receive = {
        case cmd : msg_CallHttp => dispatchImpl(cmd, module.CallAkkaHttpModule)
				case cmd : msg_LoginBaseQuery => dispatchImpl(cmd, module.LoginModule)
				case cmd : msg_filesuploadBase => dispatchImpl(cmd, module.FilesUploadModule)
				case cmd : msg_CheckBaseQuery => dispatchImpl(cmd, module.SampleCheckModule)
				case cmd : msg_mondelOperationBase => dispatchImpl(cmd, ModelOperationModule)
				case cmd : msg_resultqueryBase => dispatchImpl(cmd, module.ResultQueryModule)
				case cmd : msg_ResultCommand => dispatchImpl(cmd, ResultModule)
				case cmd : msg_LogCommand => dispatchImpl(cmd, LogModule)
				case cmd : msg_ReportBaseQuery => dispatchImpl(cmd, SampleReportModule)
				case cmd : msg_MarketManageBase => dispatchImpl(cmd, MarketManageModule)
				case cmd : msg_UserManageBase => dispatchImpl(cmd, UserManageModule)
				case cmd : ParallelMessage => {
		    cancelActor
			next = context.actorOf(ScatterGatherActor.prop(originSender, msr), "scat")
			next ! cmd
		}
		case timeout() => {
			originSender ! new timeout
			cancelActor
		}
	 	case _ => ???
	}
	
	val timeOutSchdule = context.system.scheduler.scheduleOnce(600 second, self, new timeout)

	def rstReturn = tmp match {
		case Some(_) => { rst match {
			case Some(r) => 
				msr.lst match {
					case Nil => {
						originSender ! result(toJson(r))
					}
					case head :: tail => {
						head match {
							case p : ParallelMessage => {
								next = context.actorOf(ScatterGatherActor.prop(originSender, MessageRoutes(tail, rst)), "scat")
								next ! p
							}
							case c : CommonMessage => {
								next = context.actorOf(PipeFilterActor.prop(originSender, MessageRoutes(tail, rst)), "pipe")
								next ! c
							}
						}
					}
					case _ => println("msr error")
				}
				cancelActor
			case _ => Unit
		}}
		case _ => println("never go here"); Unit
	}
	
	def cancelActor = {
		timeOutSchdule.cancel
//		context.stop(self)
	}
}