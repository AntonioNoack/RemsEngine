package me.anno.engine.ui.control

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Range
import me.anno.studio.Inspectable
import org.joml.Vector3d
import kotlin.math.floor
import kotlin.math.round

class SnappingSettings : Inspectable {

    @Range(0.0, Double.POSITIVE_INFINITY)
    var snapSize: Double
        get() = DefaultConfig["ui.snapSettings.snapSize", 1.0]
        set(value) {
            DefaultConfig["ui.snapSettings.snapSize"] = value
        }

    var snapX: Boolean
        get() = DefaultConfig["ui.snapSettings.snapX", false]
        set(value) {
            DefaultConfig["ui.snapSettings.snapX"] = value
        }

    var snapY: Boolean
        get() = DefaultConfig["ui.snapSettings.snapY", false]
        set(value) {
            DefaultConfig["ui.snapSettings.snapY"] = value
        }

    var snapZ: Boolean
        get() = DefaultConfig["ui.snapSettings.snapZ", false]
        set(value) {
            DefaultConfig["ui.snapSettings.snapZ"] = value
        }

    var snapCenter: Boolean
        get() = DefaultConfig["ui.snapSettings.snapCenter", false]
        set(value) {
            DefaultConfig["ui.snapSettings.snapCenter"] = value
        }

    fun applySnapping(dst: Vector3d) {
        fun snap(v: Double): Double {
            val s = snapSize
            return if (snapCenter) {
                floor(v / s) * s
            } else {
                round(v / s) * s
            }
        }
        if (snapSize > 0.0) {
            if (snapX) dst.x = snap(dst.x)
            if (snapY) dst.y = snap(dst.y)
            if (snapZ) dst.z = snap(dst.z)
        }
    }

    fun applySnapping(dst: Vector3d, rem: Vector3d) {
        val s = snapSize
        if (s > 0.0) {
            if (snapX) {
                val v = dst.x + rem.x
                dst.x = round(v / s) * s
                rem.x = v - dst.x
            } else rem.x = 0.0
            if (snapY) {
                val v = dst.y + rem.y
                dst.y = round(v / s) * s
                rem.y = v - dst.y
            } else rem.y = 0.0
            if (snapZ) {
                val v = dst.z + rem.z
                dst.z = round(v / s) * s
                rem.z = v - dst.z
            } else rem.z = 0.0
        }
    }
}