package com.pharbers.aqll

import com.pharbers.aqll.excel.dispose._
import com.pharbers.aqll.excel.common.ReadFileData
import scala.math._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.Imports.DBObject
import com.pharbers.aqll.util.MD5
import com.pharbers.aqll.util.dao._data_connection
import com.pharbers.aqll.excel.dispose.joinData._
import com.pharbers.aqll.excel.model._

object test extends App{

//    val excel_file_name = """file/excel/Hospital.xlsx"""
//    val excel_file_name1 = """file/excel/Products.xlsx"""
    
//    val xml_file_name = """file/xml/HospitalFields.xml"""
//    val xml_file_name_ch= """file/xml/HospitalTitles.xml"""
//    val xml_file_name = """file/xml/ProductsFields.xml"""
//    val xml_file_name_ch= """file/xml/ProductsTitles.xml"""
    
    
    
//    val hospdata : List[Hospital] = ReadFileData.hospdatadataobj(excel_file_name)
//    val productdata : List[Products] = ReadFileData.productdataobj(excel_file_name)
    
    //.groupBy(x => x(0)).map( y => Map(y._1 -> y._2.map(z => z(1)).distinct)).toList
    //val lst = productdata.groupBy( x => x.getTrade_Name).map( y => Map("Prod_Id" -> MD5.md5(y._1), "Trade_Name" -> y._1, "Package_Quantity" -> y._2.map(z => z.getPackage_Quantity).distinct))
//    lst foreach { x=> println(x)}
    
    
//    def getbulk(coll_name : String) : BulkWriteOperation = {
//        _data_connection.getCollection(coll_name).initializeUnorderedBulkOperation
//    }
    
//    joinData.insertHospitalInfo(excel_file_name)
//    joinData.insertProductInfo(excel_file_name1)
    
}
