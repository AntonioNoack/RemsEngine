package me.anno.input

import me.anno.input.Controller.Companion.MAX_NUM_AXES
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class ControllerCalibration() : Saveable() {

    constructor(isGamepad: Boolean) : this() {
        if (isGamepad) initGamepad()
    }

    // theoretically, the center could be depending on what the value was last
    // practically, we will just have a dead spot

    var isCalibrated = false

    var dead = FloatArray(MAX_NUM_AXES) { 0.1f }
    var scale = FloatArray(MAX_NUM_AXES) { 1f }
    var center = FloatArray(MAX_NUM_AXES) { 0f }

    fun getValue(state: Float, axis: Int): Float {
        return if (axis in 0 until min(dead.size, min(scale.size, center.size))) {
            val v = state - center[axis]
            sign(v) * scale[axis] * max(0f, abs(v) - dead[axis])
        } else state
    }

    private fun initGamepad() {
        // shoulder buttons, from -1 to +1 normally
        center[4] = -1f
        center[5] = -1f
        scale[4] = 0.5f
        scale[5] = 0.5f
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeBoolean("isCalibrated", isCalibrated)
        writer.writeFloatArray("dead", dead)
        writer.writeFloatArray("scale", scale)
        writer.writeFloatArray("center", center)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "isCalibrated" -> isCalibrated = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readFloatArray(name: String, values: FloatArray) {
        when (name) {
            "dead" -> dead = values
            "scale" -> scale = values
            "center" -> center = values
            else -> super.readFloatArray(name, values)
        }
    }

    override val className: String = "ControllerCalibration"

    companion object {
        init {
            ISaveable.registerCustomClass(ControllerCalibration())
        }
    }

}