package me.anno.utils.types

import org.joml.*

object Quaternions {

    const val toDegrees = 180.0 / Math.PI
    const val toDegreesFloat = toDegrees.toFloat()

    const val fromDegrees = Math.PI / 180.0
    const val fromDegreesFloat = toDegrees.toFloat()

    fun Quaterniondc.toEulerAnglesDegrees(): Vector3d {
        return getEulerAnglesXYZ(Vector3d()).mul(toDegrees)
    }

    fun Quaternionfc.toEulerAnglesDegrees(): Vector3f {
        return getEulerAnglesXYZ(Vector3f()).mul(toDegreesFloat)
    }

    fun Vector3f.toQuaternionDegrees(): Quaternionf {
        val x = x() * fromDegreesFloat
        val y = y() * fromDegreesFloat
        val z = z() * fromDegreesFloat
        return Quaternionf().rotateYXZ(y, x, z)
    }

    fun Vector3d.toQuaternionDegrees(): Quaterniond {
        val x = x() * fromDegrees
        val y = y() * fromDegrees
        val z = z() * fromDegrees
        return Quaterniond().rotateYXZ(y, x, z)
    }

}