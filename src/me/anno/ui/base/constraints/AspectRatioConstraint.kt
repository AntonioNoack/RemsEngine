package me.anno.ui.base.constraints

import me.anno.ui.Panel
import kotlin.math.min
import kotlin.math.roundToInt

class AspectRatioConstraint
private constructor(val ratio: Float?, val getRatio: (() -> Float)?) : Constraint(25) {

    constructor(ratio: Float) : this(ratio, null)
    constructor(getRatio: (() -> Float)) : this(null, getRatio)

    override fun apply(panel: Panel) {
        val targetAspectRatio = ratio ?: getRatio!!.invoke()
        if (panel.w * targetAspectRatio > panel.h) {
            // zu breit -> weniger breit
            panel.w = min(panel.w, (panel.h / targetAspectRatio).roundToInt())
        } else {
            // zu hoch -> weniger hoch
            panel.h = min(panel.h, (panel.w * targetAspectRatio).roundToInt())
        }
    }

}