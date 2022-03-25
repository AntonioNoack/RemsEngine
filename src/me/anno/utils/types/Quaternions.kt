package me.anno.utils.types

import org.joml.*

object Quaternions {

    const val toDegrees = 180.0 / Math.PI
    const val toDegreesFloat = toDegrees.toFloat()

    const val fromDegrees = Math.PI / 180.0
    const val fromDegreesFloat = fromDegrees.toFloat()

    fun Quaterniondc.toEulerAnglesDegrees(dst: Vector3d = Vector3d()): Vector3d {
        return getEulerAnglesXYZ(dst).mul(toDegrees)
    }

    fun Quaternionfc.toEulerAnglesDegrees(dst: Vector3f = Vector3f()): Vector3f {
        return getEulerAnglesXYZ(dst).mul(toDegreesFloat)
    }

    fun Vector3f.toQuaternionDegrees(dst: Quaternionf = Quaternionf()): Quaternionf {
        val x = x() * fromDegreesFloat
        val y = y() * fromDegreesFloat
        val z = z() * fromDegreesFloat
        return dst.identity().rotateYXZ(y, x, z)
    }

    fun Vector3d.toQuaternionDegrees(dst: Quaterniond = Quaterniond()): Quaterniond {
        val x = x() * fromDegrees
        val y = y() * fromDegrees
        val z = z() * fromDegrees
        return dst.identity().rotateYXZ(y, x, z)
    }

}