package com.pharbers.aqll.alcalc.aljobs

import com.pharbers.aqll.alcalc.almain.alShareData
import com.pharbers.aqll.alcalc.alprecess.alprecessdefines.alPrecessDefines._
import com.pharbers.aqll.alcalc.alstages.alStage
import com.pharbers.aqll.util.GetProperties

/**
  * Created by Alfred on 11/03/2017.
  */
class alConcertGroupJob(u : String, val parent : String) extends alJob {
    override val uuid: String = u

    def init(args : Map[String, Any]) = {
//        val restore_path = """config/sync/""" + parent + "/" + uuid
        val restore_path = s"${GetProperties.memorySplitFile}${GetProperties.sync}$parent/$uuid"
        cur = Some(alStage(restore_path))
        process = restore_data() :: do_map (alShareData.txt2IntegratedData(_)) :: do_calc() :: Nil
    }
}