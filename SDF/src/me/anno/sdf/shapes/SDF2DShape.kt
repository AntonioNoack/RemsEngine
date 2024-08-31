package me.anno.sdf.shapes

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.maths.Maths.clamp
import me.anno.utils.types.Booleans.hasFlag
import me.anno.maths.Maths.max
import me.anno.sdf.TwoDims
import me.anno.sdf.modifiers.SDFHalfSpace
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Planef
import org.joml.Vector3f
import org.joml.Vector4f

// to do more 2d shapes
// https://www.iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm
// https://www.shadertoy.com/playlist/MXdSRf&from=36&num=12

// to do more 3d shapes
// https://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm

@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class SDF2DShape : SDFShape() {

    var axes: TwoDims = TwoDims.XY
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var rotary: Boolean = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override fun applyTransform(pos: Vector4f, seeds: IntArrayList) {
        super.applyTransform(pos, seeds)
        when (axes) {
            TwoDims.XY -> pos.z = 0f
            TwoDims.YX -> pos.set(pos.y, pos.x, 0f)
            TwoDims.XZ -> pos.set(pos.x, pos.z, 0f)
            TwoDims.ZX -> pos.set(pos.z, pos.x, 0f)
            TwoDims.YZ -> pos.set(pos.y, pos.z, 0f)
            TwoDims.ZY -> pos.set(pos.z, pos.y, 0f)
        }
    }

    fun writeFuncInput(builder: StringBuilder, posIndex: Int) {
        val axes = axes
        if (rotary) {
            builder.append("vec2(length(pos").append(posIndex).append('.').append(axes.glslName)
            builder.append("),pos").append(posIndex).append('.').append(
                when (axes) {
                    TwoDims.YZ, TwoDims.ZY -> 'x'
                    TwoDims.XZ, TwoDims.ZX -> 'y'
                    else -> 'z'
                }
            )
            builder.append(')')
        } else {
            builder.append("pos").append(posIndex).append('.').append(axes.glslName)
        }
    }

    @DebugAction
    fun bound11(s: Float = 0.1f) {
        when (axes) {
            TwoDims.YZ, TwoDims.ZY -> bound1(-s, +s, 0)
            TwoDims.XZ, TwoDims.ZX -> bound1(-s, +s, 1)
            else -> bound1(-s, +s, 2)
        }
    }

    fun bound1(min: Vector3f, max: Vector3f) {
        val dir1 = JomlPools.vec3f.create()
        dir1.set(min).sub(max).normalize()
        bound2(min, dir1)
        dir1.negate()
        bound2(max, dir1)
        JomlPools.vec3f.sub(1)
    }

    fun bound2(pos: Vector3f, dir: Vector3f) {
        if (root.prefab != null) {
            val prefab = Prefab("SDFHalfSpace")
            prefab[Path.ROOT_PATH, "plane"] = Planef(pos, dir)
            val type = 'd' // SDFHalfSpace is a distance mapper = 'd'
            Hierarchy.add(prefab, Path.ROOT_PATH, this, type)
        } else {
            val child = SDFHalfSpace()
            child.plane.set(pos, dir)
            addChild(child)
        }
    }

    fun boundX(min: Float, max: Float): Unit = bound1(min, max, 0)
    fun boundY(min: Float, max: Float): Unit = bound1(min, max, 1)
    fun boundZ(min: Float, max: Float): Unit = bound1(min, max, 2)
    fun bound1(min: Float, max: Float, axis: Int) {
        val mv = JomlPools.vec3f.create().set(0f)
        val xv = JomlPools.vec3f.create().set(0f)
        mv[axis] = min
        xv[axis] = max
        bound1(mv, xv)
        JomlPools.vec3f.sub(2)
    }

    open fun calculateBaseBounds2d(dst: AABBf) {
        dst.setMin(-1f, -1f, 0f)
        dst.setMax(+1f, +1f, 0f)
    }

    override fun calculateBaseBounds(dst: AABBf) {
        calculateBaseBounds2d(dst)
        if (rotary) {
            // axes becomes x, other becomes y
            val minZ = dst.minY
            val maxZ = dst.maxY
            val minXY = 0f
            val maxXY = max(-dst.minX, dst.maxX) // rotary, so that should be right
            when (axes) {
                TwoDims.YZ, TwoDims.ZY -> {// other: x
                    dst.setMin(minZ, minXY, minXY)
                    dst.setMax(maxZ, maxXY, maxXY)
                }
                TwoDims.XZ, TwoDims.ZX -> {// other: y
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
                TwoDims.YZ, TwoDims.ZY -> {// other: x
                    dst.minX = Float.NEGATIVE_INFINITY
                    dst.maxX = Float.POSITIVE_INFINITY
                }
                TwoDims.XZ, TwoDims.ZX -> {// other: y
                    dst.minY = Float.NEGATIVE_INFINITY
                    dst.maxY = Float.POSITIVE_INFINITY
                }
                else -> {// other: z
                    dst.minZ = Float.NEGATIVE_INFINITY
                    dst.maxZ = Float.POSITIVE_INFINITY
                }
            }
        }
        // todo iterate over bounds, and apply them
        for (halfSpace in distanceMappers) {
            if (halfSpace is SDFHalfSpace) {
                val plane = halfSpace.plane
                // project all outside points onto the plane
                val tmp = AABBf()
                for (i in 0 until 8) {
                    val px = if (i.hasFlag(1)) dst.maxX else dst.minX
                    val py = if (i.hasFlag(2)) dst.maxY else dst.minY
                    val pz = if (i.hasFlag(4)) dst.maxZ else dst.minZ
                    val dt = plane.dot(px, py, pz)
                    if (dt > 0f) {// todo correct sign?
                        // inside (?)
                        tmp.union(px, py, pz)
                    } else {
                        tmp.union(
                            // todo correct sign?
                            // todo projection direction must be within bounds...
                            clamp(px + dt * plane.dirX, dst.minX, dst.maxX),
                            clamp(py + dt * plane.dirY, dst.minY, dst.maxY),
                            clamp(pz + dt * plane.dirZ, dst.minZ, dst.maxZ)
                        )
                    }
                }
                dst.set(tmp)
            }
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDF2DShape) return
        dst.axes = axes
        dst.rotary = rotary
    }
}