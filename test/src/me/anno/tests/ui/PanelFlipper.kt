package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.texture.Filtering
import me.anno.io.files.Reference.getReference
import me.anno.language.translation.NameDesc
import me.anno.ui.base.IconPanel
import me.anno.ui.base.groups.PanelFlipper
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.EnumInput
import me.anno.ui.input.TextInput
import me.anno.utils.types.Strings.upperSnakeCaseToTitle
import me.anno.utils.types.Floats.f3

fun main() {
    val flipper = PanelFlipper(style)
    // add flippable items
    flipper.swipeSpeed = 1f
    flipper.add(TextInput("Add your name", "", "Anya", style))
    flipper.add(IconPanel(getReference("res://icon.png"), style).apply {
        filtering = Filtering.NEAREST
    })
    for (name in listOf("document", "executable", "folder", "image", "music")) {
        flipper.add(IconPanel(getReference("res://textures/fileExplorer/$name.png"), style).apply {
            filtering = Filtering.NEAREST
        })
    }
    for (child in flipper.children) {
        child.makeBackgroundTransparent()
    }
    // make big
    flipper.fill(1f)
    val ui = PanelListY(style)
    // settings
    ui.add(flipper)
    ui.add(UpdatingTextPanel(50L, style) { "Index: ${flipper.position.f3()}" })
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