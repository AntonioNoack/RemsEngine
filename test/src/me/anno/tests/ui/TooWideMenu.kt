package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.menu.Menu.openMenuByPanels
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.utils.ThumbnailPanel

// this was a bug and has been fixed
// todo menus have a weird padding on the right, again
fun main() {
    disableRenderDoc()
    testUI3("TooWideMenu", TextButton(NameDesc("Click Me"), style)
        .addLeftClickListener {
            openMenuByPanels(
                it.windowStack, NameDesc("TooWide"),
                listOf(object : ThumbnailPanel(Material().ref, style) {
                    override fun calculateSize(w: Int, h: Int) {
                        super.calculateSize(w, h)
                        minW = 100
                        minH = 100
                    }
                })
            )
        })
}