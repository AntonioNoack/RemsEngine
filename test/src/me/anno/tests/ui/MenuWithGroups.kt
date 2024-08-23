package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.ComplexMenuGroup
import me.anno.ui.base.menu.ComplexMenuOption
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3

/**
 * a test for menus with multiple layers (groups)
 * */
fun main() {
    disableRenderDoc()
    testUI3("Menu With Groups") {
        val list = PanelListY(DefaultConfig.style)
        fun option(name: String): ComplexMenuOption {
            return ComplexMenuOption(NameDesc(name), true) {
                list += TextPanel(name, DefaultConfig.style)
            }
        }
        list += TextButton("Open UI", DefaultConfig.style)
            .addLeftClickListener {
                // open complex menu with groups
                val g1 = ComplexMenuGroup(
                    NameDesc("Living Things"), true, listOf(
                        option("Random Plant"),
                        ComplexMenuGroup(
                            NameDesc("Animals"), true, listOf(
                                option("Scorpion"),
                                option("Python"),
                                option("Elephant"),
                                option("Tiger"),
                            )
                        )
                    )
                )
                val g2 = ComplexMenuGroup(
                    NameDesc("Non-living Things"), true, listOf(
                        option("Random Car"),
                        option("Random House"),
                    )
                )
                val g3 = option("Random Thing")
                val ws = GFX.someWindow.windowStack
                Menu.openComplexMenu(
                    ws, 0, 0, NameDesc("Test Menu"),
                    listOf(g1, g2, g3)
                )
            }
        list
    }
}