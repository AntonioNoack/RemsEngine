package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.ecs.annotations.Range
import me.anno.maths.Maths.hasFlag
import me.anno.studio.Inspectable
import org.joml.Vector3d
import kotlin.math.floor
import kotlin.math.round

class SnappingSettings : Inspectable {

    @Range(0.0, Double.POSITIVE_INFINITY)
    var snapX by ConfigRef("ui.snapSettings.snapX", 0.0)

    @Range(0.0, Double.POSITIVE_INFINITY)
    var snapY by ConfigRef("ui.snapSettings.snapY", 0.0)

    @Range(0.0, Double.POSITIVE_INFINITY)
    var snapZ by ConfigRef("ui.snapSettings.snapZ", 0.0)

    @Range(0.0, Double.POSITIVE_INFINITY)
    var snapR by ConfigRef("ui.snapSettings.snapR", 0.0)

    var snapCenter by ConfigRef("ui.snapSettings.snapCenter", false)

    fun snapPosition(dst: Vector3d) {
        dst.x = snap(dst.x, snapX)
        dst.y = snap(dst.y, snapY)
        dst.z = snap(dst.z, snapZ)
    }

    fun snapRotation(dst: Vector3d) {
        val snapR = snapR
        dst.x = snap(dst.x, snapR)
        dst.y = snap(dst.y, snapR)
        dst.z = snap(dst.z, snapR)
    }

    fun snap(v: Double, s: Double): Double {
        return if (s > 0.0) {
            if (snapCenter) {
                floor(v / s) * s
            } else {
                round(v / s) * s
            }
        } else v
    }

    fun snapPosition(dst: Vector3d, rem: Vector3d, mask: Int = -1) {
        snapVector(dst, rem, snapX, snapY, snapZ, mask)
    }

    fun snapRotation(dst: Vector3d, rem: Vector3d, mask: Int = -1) {
        val snap = snapR
        snapVector(dst, rem, snap, snap, snap, mask)
    }

    fun snapVector(dst: Vector3d, rem: Vector3d, snapX: Double, snapY: Double, snapZ: Double, mask: Int) {
        if (mask.hasFlag(1)) {
            val vx = dst.x + rem.x
            dst.x = snap(vx, snapX)
            rem.x = vx - dst.x
        }

        if (mask.hasFlag(2)) {
            val vy = dst.y + rem.y
            dst.y = snap(vy, snapY)
            rem.y = vy - dst.y
        }

        if (mask.hasFlag(4)) {
            val vz = dst.z + rem.z
            dst.z = snap(vz, snapZ)
            rem.z = vz - dst.z
        }
    }
}