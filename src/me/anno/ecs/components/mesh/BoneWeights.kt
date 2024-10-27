package me.anno.ecs.components.mesh

import me.anno.ecs.components.mesh.Mesh.Companion.MAX_WEIGHTS
import me.anno.utils.Color.argb
import org.joml.Vector4f
import kotlin.math.min

class BoneWeights(val weights: Vector4f, val boneIds: Int) {

    class VertexWeight(var weight: Float, val boneId: Byte)

    fun mix(other: BoneWeights, f: Float): BoneWeights {
        return mixBoneWeights(this, other, f)
    }

    companion object {

        fun joinBoneWeights(
            vertexWeightList: MutableList<VertexWeight>?,
            boneWeights: FloatArray, boneIds: ByteArray, i: Int
        ) {
            if (vertexWeightList != null) {
                vertexWeightList.sortByDescending { it.weight }
                val size = min(vertexWeightList.size, MAX_WEIGHTS)
                val startIndex = i * MAX_WEIGHTS
                boneWeights[startIndex] = 1f
                for (j in 0 until size) {
                    val vw = vertexWeightList[j]
                    boneWeights[startIndex + j] = vw.weight
                    boneIds[startIndex + j] = vw.boneId
                }
            } else boneWeights[i * MAX_WEIGHTS] = 1f
        }

        fun getBoneIds(bytes: ByteArray, i: Int): Int {
            if (i < 0 || i + 4 >= bytes.size) return 0
            return argb(bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3])
        }

        fun getBoneId(boneIds: Int, i: Int): Byte {
            return boneIds.shr(24 - i * 8).toByte()
        }

        fun addWeight(weights: ArrayList<VertexWeight>, weight: Float, boneId: Byte) {
            if (weight == 0f) return
            for (i in weights.indices) {
                val wei = weights[i]
                if (wei.boneId == boneId) {
                    wei.weight += weight
                    return
                }
            }
            weights.add(VertexWeight(weight, boneId))
        }

        fun addWeights(weights: ArrayList<VertexWeight>, a: BoneWeights, factor: Float) {
            addWeight(weights, a.weights.x * factor, getBoneId(a.boneIds, 0))
            addWeight(weights, a.weights.y * factor, getBoneId(a.boneIds, 1))
            addWeight(weights, a.weights.z * factor, getBoneId(a.boneIds, 2))
            addWeight(weights, a.weights.w * factor, getBoneId(a.boneIds, 3))
        }

        fun mixBoneWeights(
            a: BoneWeights,
            b: BoneWeights,
            f: Float
        ): BoneWeights {
            val weights = ArrayList<VertexWeight>(8)
            addWeights(weights, a, 1f - f)
            addWeights(weights, b, f)
            return joinBoneWeights(weights)
        }

        fun joinBoneWeights(vertexWeightList: MutableList<VertexWeight>?): BoneWeights {
            var dstI = 0
            val dstW = Vector4f(1f, 0f, 0f, 0f)
            if (vertexWeightList != null) {
                vertexWeightList.sortByDescending { it.weight }
                val size = min(vertexWeightList.size, MAX_WEIGHTS)
                for (j in 0 until size) {
                    val vw = vertexWeightList[j]
                    dstW[j] = vw.weight
                    dstI += vw.boneId.toInt().and(255) shl (24 - j * 8)
                }
            }
            return BoneWeights(dstW, dstI)
        }
    }
}