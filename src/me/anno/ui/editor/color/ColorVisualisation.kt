package me.anno.ui.editor.color

import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.GOLDEN_RATIOf
import me.anno.ui.editor.color.ColorChooser.Companion.circleBarRatio

enum class ColorVisualisation(
    val naming: NameDesc,
    val ratio: Float,
    val needsHueChooser: Boolean
) {
    WHEEL(NameDesc("Wheel", "", "ui.input.color.vis.wheel"), 1f, false),
    CIRCLE(NameDesc("Circle", "", "ui.input.color.vis.circle"), 1f / (1f + circleBarRatio), false),
    BOX(NameDesc("Box", "", "ui.input.color.vis.box"), 1f / GOLDEN_RATIOf, true),
}