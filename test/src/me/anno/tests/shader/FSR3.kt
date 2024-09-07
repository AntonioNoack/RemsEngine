package me.anno.tests.shader

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.graph.visual.render.effects.framegen.FrameGenInitNode.Companion.interFrames
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.IntInput
import me.anno.ui.input.NumberType
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    val path = downloads.getChild("3d/DamagedHelmet.glb")
    testUI3("FrameGen") {
        val list = PanelListY(style)
        val renderPanel = testScene(path) {
            it.renderView.renderMode = RenderMode.FSR3_MIXING
        }
        renderPanel.weight = 1f
        list.add(renderPanel)
        list.add(IntInput(NameDesc("Interpolated Frames"), "", interFrames, NumberType.LONG_PLUS, style)
            .setChangeListener { interFrames = it.toInt() })
        list
    }
}