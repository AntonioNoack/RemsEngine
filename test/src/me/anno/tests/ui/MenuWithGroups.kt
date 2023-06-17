package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.ComplexMenuGroup
import me.anno.ui.base.menu.ComplexMenuOption
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio

/**
 * a test for menus with multiple layers (groups)
 * */
fun main() {
    TestStudio.testUI3 {
        val list = PanelListY(DefaultConfig.style)
        fun option(name: String): ComplexMenuOption {
            return ComplexMenuOption(name, "", true) { _, _ ->
                list += TextPanel(name, DefaultConfig.style)
                true
            }
        }
        list += TextButton("Open UI", false, DefaultConfig.style)
            .addLeftClickListener {
                // open complex menu with groups
                val g1 = ComplexMenuGroup(
                    "Living Things", "", true, listOf(
                        option("Random Plant"),
                        ComplexMenuGroup(
                            "Animals", "", true, listOf(
                                option("Scorpion"),
                                option("Python"),
                                option("Elephant"),
                                option("Tiger"),
                            )
                        )
                    )
                )
                val g2 = ComplexMenuGroup(
                    "Non-living Things", "", true, listOf(
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