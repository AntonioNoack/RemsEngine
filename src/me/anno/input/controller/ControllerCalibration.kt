package me.anno.input.controller

import me.anno.engine.EngineBase
import me.anno.io.saveable.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.config.ConfigBasics
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
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

    var deadZone = FloatArray(MAX_NUM_AXES)
    var scale = FloatArray(MAX_NUM_AXES)
    var center = FloatArray(MAX_NUM_AXES)

    init {
        deadZone.fill(0.1f)
        scale.fill(1f)
        center.fill(0f)
    }

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

        private val MAX_NUM_AXES = 32 // idk

        // this probably is never called, right?
        init {
            registerCustomClass(ControllerCalibration())
        }

        fun formatGuid(guid: String): String {
            var str = guid.trim()
            if (str.startsWith('0')) {
                val index = str.indexOfFirst { it != '0' }
                if (index < 0) return "0"
                str = "${index}x${str.substring(index)}"
            }
            if (str.endsWith('0')) {
                var index = str.indexOfLast { it != '0' }
                if (index < 0) index = 0
                str = "${str.substring(0, index + 1)}x${str.length - index - 1}"
            }
            return str
        }

        private fun getCalibrationFile(guid: String) =
            ConfigBasics.configFolder.getChild("controller/${formatGuid(guid)}.json")

        fun loadCalibration(guid: String): ControllerCalibration? {
            val file = getCalibrationFile(guid)
            if (!file.exists || file.isDirectory) return null
            return JsonStringReader.readFirstOrNull(file, EngineBase.workspace, ControllerCalibration::class)
                .waitFor()
        }

        fun saveCalibration(guid: String, calibration: ControllerCalibration) {
            if (!calibration.isCalibrated) throw IllegalArgumentException(
                "You should not save a controller calibration, " +
                        "that has not actually been calibrated"
            )
            val file = getCalibrationFile(guid)
            file.getParent().tryMkdirs()
            JsonStringWriter.save(calibration, file, EngineBase.workspace)
        }

    }
}