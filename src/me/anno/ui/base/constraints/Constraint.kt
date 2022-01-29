package me.anno.ui.base.constraints

import me.anno.ui.Panel

abstract class Constraint(val order: Int){

    abstract fun apply(panel: Panel)

}