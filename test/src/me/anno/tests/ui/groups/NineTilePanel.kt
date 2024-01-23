package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.maths.Maths.sq
import me.anno.engine.EngineBase
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.black

fun main() {
    disableRenderDoc()
    testUI3("NineTilePanel") {
        val alignments = listOf(
            AxisAlignment.MIN,
            AxisAlignment.CENTER,
            AxisAlignment.MAX,
        )
        EngineBase.showFPS = false
        NineTilePanel(style).apply {
            addLeftClickListener {
                val window = window!!
                splitX = (window.mouseX - x) / width * 0.5f
                splitY = (window.mouseY - y) / height * 0.5f
            }
            backgroundColor = -1
            for (j in alignments.indices) {
                for (i in alignments.indices) {
                    add(object : Panel(style) {

                        init {
                            alignmentX = alignments[i]
                            alignmentY = alignments[j]
                            backgroundColor =
                                (sq((i + j * 3 + 1) / (sq(alignments.size) + 1f)) * 255).toInt() * 0x10101 or black
                            backgroundRadius = 20f
                            addLeftClickListener {
                                weight = (((weight * 3).toInt() + 1) % 4) / 3f
                                weight2 = weight
                            }
                        }

                        override fun calculateSize(w: Int, h: Int) {
                            minW = 100
                            minH = 100
                        }
                    })
                }
            }
        }
    }
}