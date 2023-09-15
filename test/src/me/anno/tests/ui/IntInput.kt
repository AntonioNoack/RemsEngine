package me.anno.tests.ui

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.IntInput

fun main() {
    GFXBase.disableRenderDoc()
    testUI("IntInput", IntInput("Quality", "", Type.VIDEO_QUALITY_CRF, style))
}