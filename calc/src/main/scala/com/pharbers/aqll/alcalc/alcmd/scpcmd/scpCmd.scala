package com.pharbers.aqll.alcalc.alcmd.scpcmd

import com.pharbers.aqll.alcalc.alcmd.shellCmdExce

/**
  * Created by Alfred on 10/03/2017.
  */

case class scpCmd(val file : String, val des_path : String, val host : String, val user_name : String, val pwd : String) extends shellCmdExce {
    val cmd = s"scp ${file} ${user_name}:${pwd}@${host}:~/${des_path}"
}