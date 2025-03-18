package me.anno.tests.ui

import me.anno.animation.Interpolation
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.engine.WindowRenderFlags.showRedraws
import me.anno.ui.anim.AnimContainer
import me.anno.ui.anim.EventType
import me.anno.ui.anim.MoveAnimation
import me.anno.ui.anim.ScaleAnimation
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.SizeLimitingContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.TestEngine.Companion.testUI

/**
 * test ui with button that changes size when being hovered
 * */
fun main() {
    disableRenderDoc()
    testUI("AnimTest") {
        showRedraws = false
        val list = PanelListY(style)
        for (i in Interpolation.entries) {
            val animContainer = AnimContainer(TextButton(i.nameDesc, false, style), Padding(10, 0), style)
            // in the real world better not mix them
            animContainer.add(MoveAnimation(EventType.HOVER, i, 0f, 0.5f, 1f, 0.5f))
            animContainer.add(ScaleAnimation(EventType.TOUCH, i))
            list.add(animContainer)
        }
        ScrollPanelY(SizeLimitingContainer(list, 250, -1, style), style)
    }
}