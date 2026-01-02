package me.anno.mesh.blender.impl

import org.joml.Quaternionf

enum class RotationMode {
    /* quaternion rotations (default, and for older Blender versions) */
    ROT_MODE_QUAT,
    /* euler rotations - keep in sync with enum in BLI_math_rotation.h */
    /** Blender 'default' (classic) - must be as 1 to sync with BLI_math_rotation.h defines */
    // ROT_MODE_EUL = 1,
    ROT_MODE_XYZ,
    ROT_MODE_XZY,
    ROT_MODE_YXZ,
    ROT_MODE_YZX,
    ROT_MODE_ZXY,
    ROT_MODE_ZYX,

    /* NOTE: space is reserved here for 18 other possible
     * euler rotation orders not implemented
     */
    /* axis angle rotations */
    ROT_MODE_AXISANGLE;

    fun getRotation(instance: BObject): Quaternionf {
        return when (this) {
            ROT_MODE_QUAT -> {
                val (x, y, z, w) = instance.quat
                Quaternionf(x, y, z, w)
            }
            ROT_MODE_XYZ -> {
                val (x, y, z) = instance.rotEuler
                Quaternionf().rotationXYZ(x, y, z)
            }
            ROT_MODE_XZY -> {
                val (x, y, z) = instance.rotEuler
                Quaternionf().rotationX(x)
                    .rotateZ(z).rotateY(y)
            }
            ROT_MODE_YXZ -> {
                val (x, y, z) = instance.rotEuler
                Quaternionf().rotateYXZ(y, x, z)
            }
            ROT_MODE_YZX -> {
                val (x, y, z) = instance.rotEuler
                Quaternionf().rotationY(y)
                    .rotateZ(z).rotateX(x)
            }
            ROT_MODE_ZXY -> {
                val (x, y, z) = instance.rotEuler
                Quaternionf().rotationZ(z)
                    .rotateX(x).rotateY(y)
            }
            ROT_MODE_ZYX -> {
                val (x, y, z) = instance.rotEuler
                Quaternionf().rotationZYX(z, y, x)
            }
            ROT_MODE_AXISANGLE -> {
                val (ax, ay, az) = instance.rotAxis
                val angle = instance.rotAngle
                Quaternionf().rotationAxis(angle, ax, ay, az)
            }
        }
    }

    companion object {
        fun byId(id: Int): RotationMode? {
            if (id == -1) return ROT_MODE_AXISANGLE
            return entries.getOrNull(id)
        }
    }
}
