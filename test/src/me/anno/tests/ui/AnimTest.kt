package me.anno.tests.ui

import me.anno.animation.Interpolation
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ui.anim.AnimContainer
import me.anno.ui.anim.EventType
import me.anno.ui.anim.MoveAnimation
import me.anno.ui.anim.ScaleAnimation
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.SizeLimitingContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.TestStudio.Companion.testUI

/**
 * test ui with button that changes size when being hovered
 * */
fun main() {
    testUI {
        DefaultConfig["debug.renderdoc.enabled"] = false
        DefaultConfig["debug.ui.showRedraws"] = false
        val list = PanelListY(style)
        for (i in Interpolation.values()) {
            // todo isHovered is incorrect, they all move together
            val animContainer = AnimContainer(TextButton(i.displayName, false, style), Padding(10, 0), style)
            // in the real world better not mix them
            animContainer.add(MoveAnimation(EventType.HOVER, i, 0f, 0.5f, 1f, 0.5f))
            animContainer.add(ScaleAnimation(EventType.TOUCH, i))
            list.add(animContainer)
        }
        ScrollPanelY(SizeLimitingContainer(list, 250, -1, style), style)
    }
}