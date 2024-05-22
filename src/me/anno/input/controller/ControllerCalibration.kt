package me.anno.input.controller

import me.anno.input.Controller.Companion.MAX_NUM_AXES
import me.anno.io.saveable.Saveable
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

    var deadZone = FloatArray(MAX_NUM_AXES) { 0.1f }
    var scale = FloatArray(MAX_NUM_AXES) { 1f }
    var center = FloatArray(MAX_NUM_AXES) { 0f }

    fun getValue(state: Float, axis: Int): Float {
        return if (axis in 0 until min(deadZone.size, min(scale.size, center.size))) {
            val v = state - center[axis]
            sign(v) * scale[axis] * max(0f, abs(v) - deadZone[axis])
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
        writer.writeFloatArray("dead", deadZone)
        writer.writeFloatArray("scale", scale)
        writer.writeFloatArray("center", center)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "isCalibrated" -> isCalibrated = value == true
            "dead" -> deadZone = value as? FloatArray ?: return
            "scale" -> scale = value as? FloatArray ?: return
            "center" -> center = value as? FloatArray ?: return
            else -> super.setProperty(name, value)
        }
    }

    companion object {
        // this probably is never called, right?
        init {
            registerCustomClass(ControllerCalibration())
        }
    }
}