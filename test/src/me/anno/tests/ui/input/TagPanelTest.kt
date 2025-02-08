package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.input.TagsPanel
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    disableRenderDoc()
    OfficialExtensions.initForTests()
    testUI3("TagPanelTest", TagsPanel(style))
}