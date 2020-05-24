package me.anno.ui.base

abstract class Constraint(val order: Int){

    abstract fun apply(panel: Panel)

}