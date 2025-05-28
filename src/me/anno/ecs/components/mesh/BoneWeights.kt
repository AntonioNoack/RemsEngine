package me.anno.ecs.components.mesh

import me.anno.ecs.components.mesh.Mesh.Companion.MAX_WEIGHTS
import me.anno.utils.Color.argb
import org.joml.Vector4f
import kotlin.math.min

class BoneWeights(val weights: Vector4f, val boneIds: Int) {

    class VertexWeight(var weight: Float, val boneIndex: Byte)

    fun mix(other: BoneWeights, f: Float): BoneWeights {
        return mixBoneWeights(this, other, f)
    }

    companion object {

        fun joinBoneWeights(
            vertexWeightList: MutableList<VertexWeight>?,
            boneWeights: FloatArray, boneIndices: ByteArray, vertexIndex: Int
        ) {
            if (vertexWeightList != null) {
                vertexWeightList.sortByDescending { it.weight }
                val size = min(vertexWeightList.size, MAX_WEIGHTS)
                val startIndex = vertexIndex * MAX_WEIGHTS
                boneWeights[startIndex] = 1f
                for (j in 0 until size) {
                    val vw = vertexWeightList[j]
                    boneWeights[startIndex + j] = vw.weight
                    boneIndices[startIndex + j] = vw.boneIndex
                }
            } else boneWeights[vertexIndex * MAX_WEIGHTS] = 1f
        }

        fun getBoneIndices(bytes: ByteArray, byteOffset: Int): Int {
            if (byteOffset < 0 || byteOffset + 4 >= bytes.size) return 0
            return argb(
                bytes[byteOffset], bytes[byteOffset + 1],
                bytes[byteOffset + 2], bytes[byteOffset + 3]
            )
        }

        fun getBoneIndex(boneIndices: Int, slotId: Int): Byte {
            return boneIndices.shr(24 - slotId * 8).toByte()
        }

        fun addWeight(weights: ArrayList<VertexWeight>, weight: Float, boneIndex: Byte) {
            if (weight == 0f) return
            for (i in weights.indices) {
                val wei = weights[i]
                if (wei.boneIndex == boneIndex) {
                    wei.weight += weight
                    return
                }
            }
            weights.add(VertexWeight(weight, boneIndex))
        }

        fun addWeights(weights: ArrayList<VertexWeight>, a: BoneWeights, factor: Float) {
            addWeight(weights, a.weights.x * factor, getBoneIndex(a.boneIds, 0))
            addWeight(weights, a.weights.y * factor, getBoneIndex(a.boneIds, 1))
            addWeight(weights, a.weights.z * factor, getBoneIndex(a.boneIds, 2))
            addWeight(weights, a.weights.w * factor, getBoneIndex(a.boneIds, 3))
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
                    dstI += vw.boneIndex.toInt().and(255) shl (24 - j * 8)
                }
            }
            return BoneWeights(dstW, dstI)
        }
    }
}