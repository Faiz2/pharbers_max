package com.pharbers.aqll.alcalc.alcmd.pkgcmd

import com.pharbers.aqll.alcalc.alcmd.shellCmdExce

/**
  * Created by Alfred on 10/03/2017.
  */
case class unPkgCmd(val compress_file : String, val des_dir : String) extends shellCmdExce {
    val cmd = s"tar -xzvf ${compress_file}.tar.gz -C ${des_dir}"
}