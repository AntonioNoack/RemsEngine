package me.anno.ui.base.constraints

import me.anno.io.base.BaseWriter
import me.anno.ui.Panel
import kotlin.math.min
import kotlin.math.roundToInt

class AspectRatioConstraint
private constructor(var ratio: Float?, val getRatio: (() -> Float)?) : Constraint(25) {

    constructor() : this(1f)
    constructor(ratio: Float) : this(ratio, null)
    constructor(getRatio: (() -> Float)) : this(null, getRatio)

    override fun apply(panel: Panel) {
        val targetAspectRatio = ratio ?: getRatio!!.invoke()
        if (panel.w * targetAspectRatio > panel.h) {
            // too wide -> less width
            panel.w = min(panel.w, (panel.h / targetAspectRatio).roundToInt())
        } else {
            // too high -> less height
            panel.h = min(panel.h, (panel.w * targetAspectRatio).roundToInt())
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "ratio" -> ratio = value
            else -> super.readFloat(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val ratio = ratio
        if (ratio != null) writer.writeFloat("ratio", ratio)
    }

    override fun clone() = AspectRatioConstraint(ratio, getRatio)

    override val className = "AspectRatioConstraint"

}