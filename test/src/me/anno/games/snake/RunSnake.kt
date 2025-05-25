package me.anno.games.snake

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.debug.TestEngine.Companion.testUI3

/**
 * Run Snake Game
 * */
fun main() {
    disableRenderDoc()
    testUI3("Snake") {
        // container for even padding on all sides
        PanelContainer(SnakeGamePanel(), Padding.Zero, style)
    }
}