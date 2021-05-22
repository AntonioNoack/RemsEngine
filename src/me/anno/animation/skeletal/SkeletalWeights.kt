package me.anno.animation.skeletal

import me.anno.utils.Maths.clamp
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

class SkeletalWeights(
    val bonesPerPoint: Int,
    val indices: IntArray,
    val weights: FloatArray
) {

    init {
        if (indices.size != weights.size) throw IllegalArgumentException()
    }

    /**
     * simplifies the bone hierarchy for faster calculations on the GPU
     * */
    fun removeBonesCenter(kept: SortedSet<Int>, hierarchy: SkeletalHierarchy): SkeletalWeights {

        // resolve parent indices...
        // assign weights somehow... average of parent + (child if there is only 1)

        if (0 !in kept) throw IllegalArgumentException("Root must be kept")

        val boneCount = hierarchy.names.size
        val removed = (0 until boneCount) - kept
        val parentIndices = hierarchy.parentIndices

        val parentsOfRemoved = removed.map {
            var parent = it
            do {
                parent = parentIndices[parent]
            } while (parent !in kept)
            parent
        }

        // children of children?
        val childrenOfRemoved = IntArray(boneCount)
        for (bone in kept) {
            if (bone > 0) {// root doesn't have a parent
                val parent = parentIndices[bone]
                if (parent !in kept) {
                    // a bone, which could need an additional anchor
                    when (childrenOfRemoved[parent]) {
                        +0 -> {
                            // found :)
                            // cannot be 0, because it has a parent
                            childrenOfRemoved[parent] = bone
                        }
                        -1 -> {
                        } // too many children :/
                        else -> {
                            // too many children
                            childrenOfRemoved[parent] = -1
                        }
                    }
                }
            }
        }

        val removedFastAccess = IntArray(boneCount) { -1 }
        removed.forEachIndexed { index, bone -> removedFastAccess[bone] = index }

        val indexMapping = IntArray(boneCount)
        kept.forEachIndexed { index, bone ->
            indexMapping[bone] = index
        }

        val newIndices = IntArray(indices.size)
        val newWeights = FloatArray(weights.size)

        val localWeights = FloatArray(bonesPerPoint * 2)
        val localIndices = IntArray(bonesPerPoint * 2)
        for (i in 0 until indices.size / bonesPerPoint) {

            // collect the new values
            val i0 = i * bonesPerPoint
            var fillIndex = 0

            // add a found value
            fun add(bone: Int, weight: Float) {
                for (j in 0 until fillIndex) {
                    if (localIndices[j] == bone) {
                        localWeights[j] += weight // found :)
                        return
                    }
                }
                // not found -> add new one
                localWeights[fillIndex] = weight
                localIndices[fillIndex] = bone
                fillIndex++
            }

            // find new values
            for (j in 0 until bonesPerPoint) {
                val index = i0 + j
                val wi = weights[index]
                if (wi <= 0f) break
                val originalBone = indices[index]
                val removalIndex = removedFastAccess[originalBone]
                if (removalIndex >= 0) {
                    // indeed was removed -> replace it
                    val child = childrenOfRemoved[originalBone]
                    val parent = parentsOfRemoved[removalIndex]
                    if (child > 0) {
                        // has a single child as well
                        add(parent, wi / 2f)
                        add(child, wi / 2f)
                    } else {
                        // only has a parent
                        add(parent, wi)
                    }
                } else {
                    // is fine
                    add(originalBone, wi)
                }
            }

            // save the values...
            // for that we need to sort the values and weights...
            for (a in 0 until fillIndex) {
                for (b in a + 1 until fillIndex) {
                    if (localWeights[a] < localWeights[b]) {
                        // switch
                        val tempW = localWeights[a]
                        localWeights[a] = localWeights[b]
                        localWeights[b] = tempW
                        val tempI = localIndices[a]
                        localIndices[a] = localIndices[b]
                        localIndices[b] = tempI
                    }
                }
            }

            // save the values and remap the indices
            for (j in 0 until min(fillIndex, bonesPerPoint)) {
                newIndices[i0 + j] = indexMapping[localIndices[j]]
                newWeights[i0 + j] = localWeights[j]
            }
        }

        return SkeletalWeights(bonesPerPoint, newIndices, newWeights)

    }

    fun write(dos: DataOutputStream) {
        dos.writeByte(bonesPerPoint)
        dos.writeInt(indices.size)
        for (i in indices) dos.writeByte(i) // bone limit = 256, which should be fine
        for (i in 0 until weights.size / bonesPerPoint) {
            val w0 = 1e-5f
            var weightSum = w0
            val index0 = i * bonesPerPoint
            for (j in 0 until bonesPerPoint) weightSum += weights[j + index0]
            dos.writeByte(mapWeight((weights[index0] + w0) / weightSum))
            for (j in 1 until bonesPerPoint) dos.writeByte(mapWeight(weights[j + index0] / weightSum))
        }
    }

    companion object {

        fun mapWeight(f: Float) = clamp((f * 255f).roundToInt(), 0, 255)
        fun unmapWeight(i: Int) = i / 255f

        fun read(dis: DataInputStream): SkeletalWeights {
            val bonesPerPoint = dis.read()
            val weightCount = dis.readInt()
            val indices = IntArray(weightCount) { dis.read() }
            val weights = FloatArray(weightCount) { unmapWeight(dis.read()) }
            return SkeletalWeights(bonesPerPoint, indices, weights)
        }

    }

}