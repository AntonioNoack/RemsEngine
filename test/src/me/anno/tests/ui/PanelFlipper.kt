package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.translation.NameDesc
import me.anno.ui.base.IconPanel
import me.anno.ui.base.groups.PanelFlipper
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.input.EnumInput
import me.anno.ui.input.TextInput
import me.anno.utils.strings.StringHelper.upperSnakeCaseToTitle

fun main() {
    val flipper = PanelFlipper(style)
    // add flippable items
    flipper.add(TextInput("Add your name", "", "Anya", style))
    flipper.add(IconPanel(getReference("res://icon.png"), style))
    for (name in listOf("document", "executable", "folder", "image", "music")) {
        flipper.add(IconPanel(getReference("res://textures/fileExplorer/$name.png"), style))
    }
    // make big
    flipper.fill(1f)
    val ui = PanelListY(style)
    // settings
    ui.add(flipper)
    ui.add(
        EnumInput(
            NameDesc("Animation"),
            NameDesc(flipper.transitionType.name.upperSnakeCaseToTitle()),
            PanelFlipper.TransitionType.entries.map {
                NameDesc(it.name.upperSnakeCaseToTitle())
            }, style
        ).setChangeListener { _, index, _ ->
            flipper.transitionType = PanelFlipper.TransitionType.entries[index]
        }
    )
    testUI3("Flipper", ui)
}