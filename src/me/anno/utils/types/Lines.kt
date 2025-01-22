package me.anno.utils.types

import org.joml.Vector3d
import kotlin.math.abs

@Suppress("unused")
object Lines {

    /**
     * approximate line intersection
     * http://paulbourke.net/geometry/pointlineplane/calclineline.cs
     * */
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
        if (abs(denominator) < 1e-7) return false

        val numerator = d1343 * d4321 - d1321 * d4343
        val mua = numerator / denominator
        val mub = (d1343 + d4321 * mua) / d4343

        dir0.mulAdd(mua, pos0, dst0)
        dir1.mulAdd(mub, pos1, dst1)
        return true
    }
}