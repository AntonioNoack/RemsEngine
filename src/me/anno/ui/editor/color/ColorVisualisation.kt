package me.anno.ui.editor.color

import me.anno.language.translation.NameDesc
import me.anno.ui.editor.color.ColorChooser.Companion.CircleBarRatio
import me.anno.utils.Maths.GoldenRatio

enum class ColorVisualisation(
    val naming: NameDesc,
    val ratio: Float,
    val needsHueChooser: Boolean,
    val needsLightnessChooser: Boolean
) {

    WHEEL(NameDesc("Wheel", "", "ui.input.color.vis.wheel"), 1f, false, false),

    CIRCLE(NameDesc("Circle", "", "ui.input.color.vis.circle"), 1f / (1f + CircleBarRatio), false, true),

    BOX(NameDesc("Box", "", "ui.input.color.vis.box"), 1f / GoldenRatio, true, false),

}