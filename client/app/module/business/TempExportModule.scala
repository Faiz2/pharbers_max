package module.business

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import com.pharbers.aqll.pattern.ModuleTrait
import com.pharbers.aqll.pattern.MessageDefines
import com.pharbers.aqll.pattern.CommonMessage
import java.io.File
import com.pharbers.aqll.util.dao.from
import com.mongodb.casbah.Imports.{$and, _}
import com.pharbers.aqll.util.dao._data_connection_cores
import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}
import com.mongodb.{DBObject}
import com.pharbers.aqll.util.GetProperties
import scala.collection.immutable.List
import com.pharbers.aqll.util.file.csv.scala._
import scala.collection.mutable.ListBuffer

/**
  * Created by Wli on 2017/2/13 0013.
  */

object TempExportModuleMessage {
    sealed class msg_tempexportBase extends CommonMessage
    case class msg_finalresult1(data : JsValue) extends msg_tempexportBase
}

object TempExportModule  extends ModuleTrait{
    import TempExportModuleMessage._
    import controllers.common.default_error_handler.f
    def dispatchMsg(msg : MessageDefines)(pr : Option[Map[String, JsValue]]) : (Option[Map[String, JsValue]], Option[JsValue]) = msg match {
        case msg_finalresult1(data) => msg_finalresult_func(data)
        case _ => ???
    }

    def msg_finalresult_func(data : JsValue)(implicit error_handler : Int => JsValue) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        def dateListConditions(getter : JsValue => Any)(key : String, value : JsValue) : Option[DBObject] = getter(value) match {
            case None => None
            case Some(x) => {
                val fm = new SimpleDateFormat("MM/yyyy")
                val start = fm.parse(x.asInstanceOf[List[String]].head).getTime
                val end = fm.parse(x.asInstanceOf[List[String]].last).getTime
                Some("Date" $gte start $lte end)
            }
        }

        def conditionsAcc(o : List[DBObject], keys : List[String], func : (String, JsValue) => Option[DBObject]) : List[DBObject] = keys match {
            case Nil => o
            case head :: lst => func(head, (data \ head).as[JsValue]) match {
                case None => conditionsAcc(o, lst, func)
                case Some(y) => conditionsAcc(y :: o, lst, func)
            }
        }

        def conditions : List[DBObject] = {
            var con = conditionsAcc(Nil, "Date" :: Nil, dateListConditions(x => x.asOpt[List[String]]))
            con
        }
        try {
            (Some(Map("finalResult" -> toJson(write_CsvFile(data,conditions)))), None)
        } catch {
            case ex : Exception => (None, Some(error_handler(ex.getMessage().toInt)))
        }
    }

    def write_CsvFile(data : JsValue,conditions : List[DBObject]) : String = {
        val connectionName = (data \ "company").asOpt[String].get
        val fileName = UUID.randomUUID + ".csv"
        val file : File = new File(GetProperties.Client_Export_FilePath+fileName)
        if(!file.exists()){file.createNewFile()}
        val writer = CSVWriter.open(file,"GBK")
        writer.writeRow(List("Panel_ID","Date","City","Product","Sales","Units"))
        var first = 0
        var step = 10000
        val sum = (from db() in connectionName where $and(conditions)).count(_data_connection_cores)
        //var temp: List[Map[String,JsValue]] = List.empty
        while (first < sum) {
            val result = (from db() in connectionName where $and(conditions)).selectSkipTop(first)(step)("Date")(finalResultTempJsValue(_))(_data_connection_cores).toList
            writeConFunc(result,writer)
            //temp = groupBy4(r)(temp)
            //println(temp.size)
            if(sum - first < step){
                step = sum - first
            }
            first += step
            println(first)
        }
        //writeConFunc(temp : List[Map[String,JsValue]], writer : CSVWriter)
        writer.close()
        fileName
    }

    def writeConFunc(result : List[Map[String,JsValue]],writer : CSVWriter) {
        result.foreach { x =>
            val lb : ListBuffer[AnyRef] = ListBuffer[AnyRef]()
            lb.append(x.get("Panel_ID").get)
            lb.append(x.get("Date").get)
            lb.append(x.get("City").get)
            lb.append(x.get("Product").get)
            lb.append(x.get("Sales").get)
            lb.append(x.get("Units").get)
            writer.writeRow(lb.toList)
        }
    }

    def finalResultTempJsValue(x : MongoDBObject) : Map[String,JsValue] = {
        val timeDate = Calendar.getInstance
        timeDate.setTimeInMillis(x.getAs[Number]("Date").get.longValue)
        var year = timeDate.get(Calendar.YEAR).toString
        var month = (timeDate.get(Calendar.MONTH)+1).toString
        Map(
            "Panel_ID" -> toJson(x.getAs[String]("Panel_ID").get),
            "Date" -> toJson(year + (if(month.length<2){"0"+month}else{month})),
            "City" -> toJson(x.getAs[String]("City").get),
            "Product" -> toJson(x.getAs[String]("Product").get),
            "Sales" -> toJson(x.getAs[Number]("f_sales").get.doubleValue),
            "Units" -> toJson(x.getAs[Number]("f_units").get.doubleValue)
        )
    }

    /*def groupBy4(results : List[Map[String,JsValue]])(lst: List[Map[String,JsValue]]): List[Map[String, JsValue]] ={
        ((results ::: lst).groupBy{ x =>
            (x.get("Panel_ID").get,x.get("Date").get,x.get("City").get,x.get("Product"))
        }.map{ y =>
            val Salessum = y._2.map(z => z.get("Sales").get.as[Double]).sum
            val Unitssum = y._2.map(x => x.get("Units").get.as[Double]).sum
            Map(
                "Panel_ID" -> toJson(y._1._1),
                "Date" ->toJson(y._1._2),
                "City" -> toJson(y._1._3),
                "Product" -> toJson(y._1._4),
                "Sales" -> toJson(Salessum),
                "Units" -> toJson(Unitssum)
            )
        }) toList
    }*/
}