package me.anno.ui.base.components

open class LTRB(var left: Int, var top: Int, var right: Int, var bottom: Int){

    val width: Int get() = left + right
    val height: Int get() = top + bottom

}