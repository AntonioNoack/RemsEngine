package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.NamedSaveable
import me.anno.io.Saveable
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.utils.DataSetPanel

fun main() {
    disableRenderDoc()
    testUI3("DataSetPanel") {
        val v = ArrayList<Saveable>()
        for (i in 0 until 5) {
            val n = NamedSaveable()
            n.name = "Entry $i"
            v.add(n)
        }
        DataSetPanel(v, 0, DefaultConfig.style).apply {
            // checking resizing
            // sizeX += 3
            // sizeX--
            // checking alignment
            alignmentX = AxisAlignment.FILL
        }
    }
}