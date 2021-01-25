package me.anno.ui.custom

import me.anno.ui.base.Panel

interface CustomList {

    fun addChild(panel: Panel)

    fun move(index: Int, delta: Float)

    fun remove(index: Int)

    val customChildren: List<Panel>

}