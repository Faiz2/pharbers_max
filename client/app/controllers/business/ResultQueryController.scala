package controllers.business

import play.api._
import play.api.mvc._
import play.api.libs.json.JsValue
import com.pharbers.aqll.pattern
import play.api.libs.json.Json.toJson
import pattern.MessageRoutes
import pattern.ResultMessage.msg_CommonResultMessage
import controllers.common.requestArgsQuery.requestArgs
import module.business.ResultQueryModuleMessage._
import pattern.LogMessage.msg_log
/**
	* Created by Wli on 2017/1/5.
	*/
object ResultQueryController extends Controller{
    
    def resultQueryAjaxCall = Action (request => requestArgs(request) { jv => 
			import pattern.ResultMessage.common_result
			import pattern.LogMessage.common_log
			MessageRoutes(msg_log(toJson(Map("method" -> toJson("resultQueryAjaxCall"))), jv) :: msg_finalresult(jv) :: msg_hospitalresult(jv) :: msg_miniproductresult(jv) :: msg_CommonResultMessage() :: Nil, None)
		})
	
}