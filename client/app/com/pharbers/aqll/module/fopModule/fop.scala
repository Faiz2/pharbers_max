package com.pharbers.aqll.module.fopModule

import java.io.File
import play.api.libs.Files
import java.io.FileInputStream
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsValue
import java.util.UUID
import com.pharbers.aqll.util.{GetProperties, MD5, StringOption}

object fop {

	val storepath = GetProperties.loadConf("File.conf").getString("Files.FileBase_FilePath")

	def uploadFile(data : MultipartFormData[TemporaryFile])(implicit error_handler : Int => JsValue) : JsValue = {
	    try {
  	      	var lst : List[JsValue] = Nil
      	    data.files.foreach { x =>
						val uuid = UUID.randomUUID
						val file = new File(storepath + "Transfer/")
						if(!file.exists()) {
							file.mkdir()
						}
						new TemporaryFile(x.ref.file).moveTo(new File(storepath + "Transfer/" + uuid), true)
      	  	  	lst = lst :+ toJson(uuid.toString)
      	  	}
      	    Json.toJson(Map("status" -> toJson("ok"), "result" -> toJson(lst)))
  	    } catch {
  	    	case ex : Exception => error_handler(-1)
  	    }
	}

	def downloadFile(name : String) : Array[Byte] = {
	  	val file = new File(storepath + "/Template/"+ name)
			val reVal : Array[Byte] = new Array[Byte](file.length.intValue)
			new FileInputStream(file).read(reVal)
			reVal
	}

	def exportFile(name : String) : Array[Byte] = {
		val file = new File(storepath + "/Export/" + name)
		val reVal : Array[Byte] = new Array[Byte](file.length.intValue)
		new FileInputStream(file).read(reVal)
		reVal
	}

	def uploadHospitalFile(data : MultipartFormData[TemporaryFile])(implicit error_handler : Int => JsValue) : JsValue = {
		try {
			Json.toJson(Map("status" -> toJson("ok"), "result" -> toJson(moveToFile(data))))
		} catch {
			case ex : Exception => error_handler(-1)
		}
	}

    def moveToFile(data : MultipartFormData[TemporaryFile]) : List[JsValue] = {
			var lst : List[JsValue] = Nil
			val company = data.dataParts.get("company").get.head
			val timestamp = data.dataParts.get("timestamp").get.head
			val market = data.dataParts.get("market").get.head
			val path = storepath + company + "/Hospital/"
			val filename = MD5.md5(company+timestamp+StringOption.takeStringSpace(market))
			val file = new File(path)
			if(!file.exists()) { file.mkdir() }else{
				val file1 : File = new File(path + filename)
				if(file1.exists()) file1.delete()
			}
			data.files.foreach { x =>
				Files.TemporaryFile(x.ref.file).moveTo(new File(path + filename) , true)
				lst = lst :+ toJson(filename.toString)
			}
					lst
			}
}