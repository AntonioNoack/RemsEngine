package me.anno.ecs.components.mesh

import me.anno.utils.Maths.length
import me.anno.utils.Maths.max
import org.joml.Vector3f
import kotlin.math.abs

object NormalCalculator {

    fun needsNormalsComputation(normals: FloatArray): Boolean {
        for (j in 0 until normals.size / 3) {
            val i = j * 3
            if (abs(normals[i]) + abs(normals[i + 1]) + abs(normals[i + 2]) < 0.001) {
                return true
            }
        }
        return false
    }

    fun normalIsInvalid(normals: FloatArray, offset: Int): Boolean {
        // the smallest possible values:
        // 1 for 1,0,0
        // 1.73 for sqrt(1/3),sqrt(1/3),sqrt(1/3)
        // we could do a better test here, but I want a little programmer freedom for error...
        return abs(normals[offset]) + abs(normals[offset + 1]) + abs(normals[offset + 2]) < 0.3f
    }

    // calculate = pure arithmetics
    // compute = calculations with rules
    private fun calculateFlatNormal(
        positions: FloatArray,
        i0: Int, i1: Int, i2: Int,
        a: Vector3f, b: Vector3f, c: Vector3f
    ): Vector3f {
        a.set(positions[i0], positions[i0 + 1], positions[i0 + 2])
        b.set(positions[i1], positions[i1 + 1], positions[i1 + 2])
        c.set(positions[i2], positions[i2 + 1], positions[i2 + 2])
        b.sub(a)
        c.sub(a)
        return b.cross(c).normalize()
    }

    private fun computeNormalsIndexed(positions: FloatArray, normals: FloatArray, indices: IntArray) {
        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        val weights = IntArray(positions.size / 3)
        for (j in weights.indices) {
            val i = j * 3
            if (abs(normals[i]) + abs(normals[i + 1]) + abs(normals[i + 2]) > 0.001) {
                // fine -> we don't need weights
                weights[j] = -1
            } else {
                // clear it, because we will sum our normal there
                normals[i + 0] = 0f
                normals[i + 1] = 0f
                normals[i + 2] = 0f
            }
        }
        for (i in indices.indices step 3) {
            val i0 = indices[i + 0]
            val i1 = indices[i + 1]
            val i2 = indices[i + 2]
            if (weights[i0] >= 0 || weights[i1] >= 0 || weights[i2] >= 0) {
                // we need this point
                val normal = calculateFlatNormal(positions, i0 * 3, i1 * 3, i2 * 3, a, b, c)
                if (weights[i0] >= 0) addWeightAndNormal(weights, i0, normals, normal)
                if (weights[i1] >= 0) addWeightAndNormal(weights, i1, normals, normal)
                if (weights[i2] >= 0) addWeightAndNormal(weights, i2, normals, normal)
            }
        }
        // apply all the normals, smooth shading
        for (j in weights.indices) {
            val weight = weights[j]
            // < 0: the normal is already done
            // = 0: no faces were found
            // = 1: we don't need to further normalize it, as its weight is already 1
            // > 1: we need to normalize it
            if (weight > 1) {
                val i = j * 3
                // dividing by the weight count is no enough, since the normal needs to be normalized,
                // and avg(normals) will not have length 1, if there are different input normals
                val weightInv = 1f / max(0.1f, length(normals[i + 0], normals[i + 1], normals[i + 2]))
                normals[i + 0] *= weightInv
                normals[i + 1] *= weightInv
                normals[i + 2] *= weightInv
            }
        }
    }

    private fun computeNormalsNonIndexed(positions: FloatArray, normals: FloatArray) {
        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        // just go through the vertices
        // mode to calculate smooth shading by clustering points?
        for (i in positions.indices step 9) {
            // check whether the normal update is needed
            val needsUpdate = normalIsInvalid(normals, i) ||
                    normalIsInvalid(normals, i + 3) ||
                    normalIsInvalid(normals, i + 6)
            if (needsUpdate) {
                // flat shading
                val normal = calculateFlatNormal(positions, i, i + 3, i + 6, a, b, c)
                for (offset in i until i + 9 step 3) {
                    normals[offset + 0] = normal.x
                    normals[offset + 1] = normal.y
                    normals[offset + 2] = normal.z
                }
            }
        }
    }

    fun checkNormals(positions: FloatArray, normals: FloatArray, indices: IntArray?) {
        // first an allocation free check
        if (!needsNormalsComputation(normals)) return
        if (indices == null) {
            computeNormalsNonIndexed(positions, normals)
        } else {
            computeNormalsIndexed(positions, normals, indices)
        }
    }

    private fun addWeightAndNormal(weights: IntArray, i0: Int, normals: FloatArray, normal: Vector3f) {
        weights[i0]++
        val j = i0 * 3
        normals[j + 0] += normal.x
        normals[j + 1] += normal.y
        normals[j + 2] += normal.z
    }


}