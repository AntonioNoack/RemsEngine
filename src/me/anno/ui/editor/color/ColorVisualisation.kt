package me.anno.ui.editor.color

import me.anno.language.translation.DictObj
import me.anno.ui.editor.color.ColorChooser.Companion.CircleBarRatio
import me.anno.utils.Maths.GoldenRatio

enum class ColorVisualisation(
    val displayName: DictObj,
    val ratio: Float,
    val needsHueChooser: Boolean,
    val needsLightnessChooser: Boolean
) {

    WHEEL(DictObj("Wheel", "ui.input.color.vis.wheel"), 1f, false, false),

    CIRCLE(DictObj("Circle", "ui.input.color.vis.circle"), 1f / (1f + CircleBarRatio), false, true),

    BOX(DictObj("Box", "ui.input.color.vis.box"), 1f / GoldenRatio, true, false),

}