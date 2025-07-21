package me.anno.utils.types

import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

/**
 * approximate line intersection
 * http://paulbourke.net/geometry/pointlineplane/calclineline.cs
 * */
object LineIntersection {

    const val MIN_DENOMINATOR_64 = 1e-307
    const val MIN_DENOMINATOR_32 = 1e-37f

    @JvmStatic
    fun lineIntersection(
        pos0: Vector3d, dir0: Vector3d,
        pos1: Vector3d, dir1: Vector3d,
        dst0: Vector3d, dst1: Vector3d
    ): Boolean {

        val p13x = pos0.x - pos1.x
        val p13y = pos0.y - pos1.y
        val p13z = pos0.z - pos1.z

        val d1321 = dir0.dot(p13x, p13y, p13z)
        val d1343 = dir1.dot(p13x, p13y, p13z)
        val d4321 = dir0.dot(dir1)
        val d2121 = dir0.lengthSquared()
        val d4343 = dir1.lengthSquared()
        val denominator = d2121 * d4343 - d4321 * d4321
        if (abs(denominator) < MIN_DENOMINATOR_64) return false

        val numerator = d1343 * d4321 - d1321 * d4343
        val mua = numerator / denominator
        val mub = (d1343 + d4321 * mua) / d4343

        dir0.mulAdd(mua, pos0, dst0)
        dir1.mulAdd(mub, pos1, dst1)
        return true
    }

    @JvmStatic
    fun lineIntersection(
        pos0: Vector3f, dir0: Vector3f,
        pos1: Vector3f, dir1: Vector3f,
        dst0: Vector3f, dst1: Vector3f
    ): Boolean {

        val p13x = pos0.x - pos1.x
        val p13y = pos0.y - pos1.y
        val p13z = pos0.z - pos1.z

        val d1321 = dir0.dot(p13x, p13y, p13z)
        val d1343 = dir1.dot(p13x, p13y, p13z)
        val d4321 = dir0.dot(dir1)
        val d2121 = dir0.lengthSquared()
        val d4343 = dir1.lengthSquared()
        val denominator = d2121 * d4343 - d4321 * d4321
        if (abs(denominator) < MIN_DENOMINATOR_32) return false

        val numerator = d1343 * d4321 - d1321 * d4343
        val mua = numerator / denominator
        val mub = (d1343 + d4321 * mua) / d4343

        dir0.mulAdd(mua, pos0, dst0)
        dir1.mulAdd(mub, pos1, dst1)
        return true
    }

    @JvmStatic
    fun lineIntersection(
        pos0: Vector2d, dir0: Vector2d,
        pos1: Vector2d, dir1: Vector2d,
        dst0: Vector2d, dst1: Vector2d
    ): Boolean {

        val p13x = pos0.x - pos1.x
        val p13y = pos0.y - pos1.y

        val d1321 = dir0.dot(p13x, p13y)
        val d1343 = dir1.dot(p13x, p13y)
        val d4321 = dir0.dot(dir1)
        val d2121 = dir0.lengthSquared()
        val d4343 = dir1.lengthSquared()
        val denominator = d2121 * d4343 - d4321 * d4321
        if (abs(denominator) < MIN_DENOMINATOR_64) return false

        val numerator = d1343 * d4321 - d1321 * d4343
        val mua = numerator / denominator
        val mub = (d1343 + d4321 * mua) / d4343

        dir0.mulAdd(mua, pos0, dst0)
        dir1.mulAdd(mub, pos1, dst1)
        return true
    }

    @JvmStatic
    fun lineIntersection(
        pos0: Vector2f, dir0: Vector2f,
        pos1: Vector2f, dir1: Vector2f,
        dst0: Vector2f, dst1: Vector2f
    ): Boolean {

        val p13x = pos0.x - pos1.x
        val p13y = pos0.y - pos1.y

        val d1321 = dir0.dot(p13x, p13y)
        val d1343 = dir1.dot(p13x, p13y)
        val d4321 = dir0.dot(dir1)
        val d2121 = dir0.lengthSquared()
        val d4343 = dir1.lengthSquared()
        val denominator = d2121 * d4343 - d4321 * d4321
        if (abs(denominator) < MIN_DENOMINATOR_32) return false

        val numerator = d1343 * d4321 - d1321 * d4343
        val mua = numerator / denominator
        val mub = (d1343 + d4321 * mua) / d4343

        dir0.mulAdd(mua, pos0, dst0)
        dir1.mulAdd(mub, pos1, dst1)
        return true
    }
}