package com.pharbers.aqll.alcalc.almaxdefines

/**
  * Created by Alfred on 11/03/2017.
  */
case class alMaxProperty(val parent : String,
                         val uuid : String,
                         val subs : List[alMaxProperty],
                         var signed : Boolean = false,
                         var grouped : Boolean = false,
                         var isSumed : Boolean = false,
                         var sum : List[(String, (Double, Double, Double))] = Nil,
                         var isCalc : Boolean = false,
                         var finalValue : Double = 0.0,
                         var finalUnit : Double = 0.0
                        )
