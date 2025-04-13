package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.engine.WindowRenderFlags
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.ListAlignment
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.utils.Color.black

fun main() {
    disableRenderDoc()
    testUI3("List Stresstest 2d") {
        WindowRenderFlags.enableVSync = false
        val n = 100 // _000
        val all = PanelListY(style)
        val list = PanelList2D(style)
        list.background.color = black
        list.childWidth = 120
        list.childHeight = 24
        val controls = PanelListX(style)
        controls.add(
            EnumInput(
                NameDesc("AlignmentX"), NameDesc(list.listAlignmentX.name),
                ListAlignment.entries.map { NameDesc(it.name) }, style
            ).setChangeListener { _, index, _ ->
                list.listAlignmentX = ListAlignment.entries[index]
            }
        )
        controls.add(
            EnumInput(
                NameDesc("AlignmentY"), NameDesc(list.listAlignmentY.name),
                ListAlignment.entries.map { NameDesc(it.name) }, style
            ).setChangeListener { _, index, _ ->
                list.listAlignmentY = ListAlignment.entries[index]
            }
        )
        controls.add(
            BooleanInput(NameDesc("IsY"), list.isY, false, style)
                .setChangeListener { isY ->
                    list.isY = isY // todo toggling this sometimes behaves weirdly
                }
        )
        all.add(controls)
        for (i in 0 until n) {
            val p = TextPanel("Test-$i", style)
            p.textAlignmentX = AxisAlignment.CENTER
            p.textAlignmentY = AxisAlignment.CENTER
            p.alignmentX = AxisAlignment.CENTER
            p.alignmentY = AxisAlignment.CENTER
            list.add(p)
        }
        all.add(ScrollPanelY(list, style).fill(1f))
        all
    }
}