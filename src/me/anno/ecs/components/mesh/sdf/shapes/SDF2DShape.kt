package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.sdf.modifiers.SDFHalfSpace
import me.anno.maths.Maths.max
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Vector3f

// todo more 2d shapes
// https://www.iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm
// https://www.shadertoy.com/playlist/MXdSRf&from=36&num=12

// todo more 3d shapes
// https://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm

open class SDF2DShape : SDFShape() {

    var axes = "xy"
        set(value) {
            // check whether the new value is valid
            if (value.length == 2 && value.count { it in "xyz" } == 2 && value[0] != value[1]) {
                invalidateShader()
                field = value
            }
        }

    var rotary = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    fun writeFuncInput(builder: StringBuilder, posIndex: Int) {
        val axes = axes
        if (rotary) {
            builder.append("vec2(length(pos").append(posIndex).append(".").append(axes)
            builder.append("),pos").append(posIndex).append(".").append(
                when (axes) {
                    "yz", "zy" -> 'x'
                    "xz", "zx" -> 'y'
                    else -> 'z'
                }
            )
            builder.append(')')
        } else {
            builder.append("pos").append(posIndex).append(".").append(axes)
        }
    }

    fun bound(min: Vector3f, max: Vector3f) {
        val dir1 = JomlPools.vec3f.create().set(min).sub(max)
        add(SDFHalfSpace(min, dir1))
        dir1.mul(-1f)
        add(SDFHalfSpace(max, dir1))
        JomlPools.vec3f.sub(1)
    }

    fun boundX(min: Float, max: Float) = bound(min, max, 0)
    fun boundY(min: Float, max: Float) = bound(min, max, 1)
    fun boundZ(min: Float, max: Float) = bound(min, max, 2)
    fun bound(min: Float, max: Float, axis: Int) {
        val mv = JomlPools.vec3f.create().set(0f)
        val xv = JomlPools.vec3f.create().set(0f)
        mv.setComponent(axis, min)
        xv.setComponent(axis, max)
        bound(mv, xv)
        JomlPools.vec3f.sub(2)
    }

    override fun calculateBaseBounds(dst: AABBf) {
        if (rotary) {
            // axes becomes x, other becomes y
            val minZ = dst.minY
            val maxZ = dst.maxY
            val minXY = 0f
            val maxXY = max(-dst.minX, dst.maxX) // rotary, so that should be right
            when (axes) {
                "yz", "zy" -> {// other: x
                    dst.setMin(minZ, minXY, minXY)
                    dst.setMax(maxZ, maxXY, maxXY)
                }
                "xz", "zx" -> {// other: y
                    dst.setMin(minXY, minZ, minXY)
                    dst.setMax(maxXY, maxZ, maxXY)
                }
                else -> {// other: z
                    dst.setMin(minXY, minXY, minZ)
                    dst.setMax(maxXY, maxXY, maxZ)
                }
            }
        } else {
            // x stays x, y stays y, z is unbounded
            when (axes) {
                "yz", "zy" -> {// other: x
                    dst.minX = Float.NEGATIVE_INFINITY
                    dst.maxX = Float.POSITIVE_INFINITY
                }
                "xz", "zx" -> {// other: y
                    dst.minY = Float.NEGATIVE_INFINITY
                    dst.maxY = Float.POSITIVE_INFINITY
                }
                else -> {// other: z
                    dst.minZ = Float.NEGATIVE_INFINITY
                    dst.maxZ = Float.POSITIVE_INFINITY
                }
            }
        }
    }

}