package me.anno.ui.custom

import me.anno.ui.base.Panel
import me.anno.ui.custom.data.ICustomDataCreator

interface CustomList: ICustomDataCreator {

    fun addChild(panel: Panel)

    fun move(index: Int, delta: Float)

    fun remove(index: Int)

    val dataChildren: List<ICustomDataCreator>

}