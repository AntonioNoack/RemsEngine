package me.anno.ui.editor.color

import me.anno.ui.editor.color.ColorChooser.Companion.CircleBarRatio
import me.anno.utils.GoldenRatio

enum class ColorVisualisation(val displayName: String, val ratio: Float, val needsHueChooser: Boolean, val needsLightnessChooser: Boolean){

    BOX("Box", 1f/GoldenRatio, true, false),

    WHEEL("Wheel", 1f, false, false),

    CIRCLE("Circle", 1f/(1f+CircleBarRatio), false, true);

}