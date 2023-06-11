package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.database.DataSetView
import me.anno.gpu.GFXBase.disableRenderDoc
import me.anno.io.NamedSaveable
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.TestStudio.Companion.testUI3

fun main() {
    disableRenderDoc()
    testUI3 {
        val v = ArrayList<NamedSaveable>()
        for (i in 0 until 5) {
            val n = NamedSaveable()
            n.name = "Entry $i"
            v.add(n)
        }
        DataSetView(v, 0, DefaultConfig.style).apply {
            // checking resizing
            // sizeX += 3
            // sizeX--
            // checking alignment
            alignmentX = AxisAlignment.FILL
        }
    }
}