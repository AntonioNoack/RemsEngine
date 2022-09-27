package me.anno.mesh.assimp

import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.utils.search.BinarySearch.binarySearch
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.assimp.*
import kotlin.math.max


object AnimationLoader {

    // private val LOGGER = LogManager.getLogger(AnimationLoader::class)

    private inline fun findIndex(count: Int, compare: (Int) -> Int): Int {
        if (count == 2) return 0
        var index = binarySearch(count - 1, compare)
        if (index < 0) index = -1 - index
        return min(index, count - 2)
    }

    private fun findPosition(animationTime: Double, aiNodeAnim: AINodeAnim): Int {
        val keys = aiNodeAnim.mPositionKeys()!!
        val count = aiNodeAnim.mNumPositionKeys()
        return findIndex(count) { keys[it + 1].mTime().compareTo(animationTime) }
    }

    private fun findRotation(animationTime: Double, aiNodeAnim: AINodeAnim): Int {
        val keys = aiNodeAnim.mRotationKeys()!!
        val count = aiNodeAnim.mNumRotationKeys()
        return findIndex(count) { keys[it + 1].mTime().compareTo(animationTime) }
    }

    private fun findScale(animationTime: Double, aiNodeAnim: AINodeAnim): Int {
        val keys = aiNodeAnim.mScalingKeys()!!
        val count = aiNodeAnim.mNumScalingKeys()
        return findIndex(count) { keys[it + 1].mTime().compareTo(animationTime) }
    }

    private fun vec(v: AIVector3D): Vector3f = Vector3f(v.x(), v.y(), v.z())
    private fun quat(q: AIQuaternion): Quaternionf = Quaternionf(q.x(), q.y(), q.z(), q.w())

    private fun interpolateTranslation(animationTime: Double, aiNodeAnim: AINodeAnim): Vector3f {
        val keys = aiNodeAnim.mPositionKeys()!!
        if (aiNodeAnim.mNumPositionKeys() < 2) {
            return vec(keys[0].mValue())
        }
        val index = findPosition(animationTime, aiNodeAnim)
        val t1 = keys[index + 1].mTime()
        val t0 = keys[index].mTime()
        val fraction = ((animationTime - t0) / (t1 - t0)).toFloat()
        // LOGGER.info("$animationTime -> $index + $fraction")
        val start = keys[index].mValue()
        val end = keys[index + 1].mValue()
        return Vector3f(
            mix(start.x(), end.x(), fraction),
            mix(start.y(), end.y(), fraction),
            mix(start.z(), end.z(), fraction)
        )
    }

    @Suppress("unused")
    fun averageTranslation(aiNodeAnim: AINodeAnim): Vector3f {
        val keys = aiNodeAnim.mPositionKeys()!!
        val numKeys = aiNodeAnim.mNumPositionKeys()
        val sum = Vector3f()
        for (i in 0 until numKeys) {
            val value = keys[i].mValue()
            sum.add(value.x(), value.y(), value.z())
        }
        if (numKeys > 1) sum.mul(1f / numKeys)
        return sum
    }

    fun getTranslation(aiNode: AINode): Vector3f {
        val transform = aiNode.mTransformation()
        return Vector3f(transform.a4(), transform.b4(), transform.c4())
    }

    private fun interpolateRotation(animationTime: Double, aiNodeAnim: AINodeAnim): Quaternionf {
        val keys = aiNodeAnim.mRotationKeys()!!
        if (aiNodeAnim.mNumRotationKeys() < 2) {
            return quat(keys[0].mValue())
        }
        val index = findRotation(animationTime, aiNodeAnim)
        val t1 = keys[index + 1].mTime()
        val t0 = keys[index].mTime()
        val factor = ((animationTime - t0) / (t1 - t0)).toFloat()
        val start = quat(keys[index].mValue())
        val end = quat(keys[index + 1].mValue())
        return start.slerp(end, factor).normalize()
    }

    private fun interpolateScale(animationTime: Double, aiNodeAnim: AINodeAnim): Vector3f {
        val keys = aiNodeAnim.mScalingKeys()!!
        if (aiNodeAnim.mNumScalingKeys() < 2) {
            return vec(keys[0].mValue())
        }
        val index = findScale(animationTime, aiNodeAnim)
        val t1 = keys[index + 1].mTime()
        val t0 = keys[index].mTime()
        val factor = ((animationTime - t0) / (t1 - t0)).toFloat()
        val start = keys[index].mValue()
        val end = keys[index + 1].mValue()
        return Vector3f(
            mix(start.x(), end.x(), factor),
            mix(start.y(), end.y(), factor),
            mix(start.z(), end.z(), factor)
        )
    }

    fun getNodeTransform(
        animationTime: Double,
        nodeAnim: NodeAnim,
        dst: Matrix4x3f,
        animatedTranslation: Boolean
    ) {

        val aiNodeAnim = nodeAnim.aiNodeAnim

        // local matrix
        val translation = if (animatedTranslation) {
            interpolateTranslation(animationTime, aiNodeAnim)
        } else nodeAnim.average

        val rotation = interpolateRotation(animationTime, aiNodeAnim)
        val scale = interpolateScale(animationTime, aiNodeAnim)

        dst.identity()
            .translate(translation)
            .rotate(rotation)
            .scale(scale) // probably could be disabled

    }

    fun readAnimationFrame(
        aiScene: AIScene,
        boneMap: Map<String, Bone>,
        isRootAnim: Boolean,
        animNodeCache: Map<String, NodeAnim>,
        timeIndex: Double,
        aiNode: AINode,
        skinningMatrices: Array<Matrix4x3f>,
        parentTransform: Matrix4x3f?,
        globalTransform: Matrix4x3f?,
        globalInverseTransform: Matrix4x3f?
    ) {

        val name = aiNode.mName().dataString()
        val localTransform = convert(aiNode.mTransformation())
        val nodeAnim = animNodeCache[name]

        val nextIsRootAnim = if (nodeAnim != null) {
            getNodeTransform(timeIndex, nodeAnim, localTransform, isRootAnim)
            false
        } else isRootAnim

        parentTransform?.mul(localTransform, localTransform)

        val bone = boneMap[name]
        if (bone != null) {

            val boneOffsetMatrix = bone.inverseBindPose
            val skinningMatrix = skinningMatrices[bone.id]
            if (globalTransform == null) {
                skinningMatrix
                    .set(localTransform)
                    .mul(boneOffsetMatrix)
            } else {
                // the same, just converting the vertices temporarily into local global space and then back
                skinningMatrix
                    .set(globalInverseTransform!!)
                    .mul(localTransform)
                    .mul(boneOffsetMatrix)
                    .mul(globalTransform)
            }
        }

        val children = aiNode.mChildren()
        if (children != null) {
            for (i in 0 until aiNode.mNumChildren()) {
                readAnimationFrame(
                    aiScene,
                    boneMap, nextIsRootAnim,
                    animNodeCache, timeIndex,
                    AINode.create(children[i]),
                    skinningMatrices,
                    localTransform,
                    globalTransform,
                    globalInverseTransform
                )
            }
        }

    }


    fun readAnimationFrame(
        aiScene: AIScene,
        boneMap: Map<String, Bone>,
        animNodeCache: Map<String, NodeAnim>,
        animationTime: Double,
        aiNode: AINode,
        frameIndex: Int,
        rootMotion: FloatArray,
        rotations: FloatArray
    ) {

        val name = aiNode.mName().dataString()
        val nodeAnim = animNodeCache[name]

        if (nodeAnim != null) {

            val bone = boneMap[name]!!

            // localTransform = T * R * (S=1)
            // rotation is something
            // scale is null
            // println("avg vs read: ${nodeAnim.average} vs ${localTransform.getTranslation(Vector3f())}")

            val aiNodeAnim = nodeAnim.aiNodeAnim

            if (bone.parentId == -1) {
                // set root motion
                val index = frameIndex * 3
                val translation = interpolateTranslation(animationTime, aiNodeAnim)
                rootMotion[index + 0] = translation.x
                rootMotion[index + 1] = translation.y
                rootMotion[index + 2] = translation.z
            }

            val rotation = interpolateRotation(animationTime, aiNodeAnim)
            val index = (frameIndex * boneMap.size + bone.id) * 4
            rotations[index + 0] = rotation.x
            rotations[index + 1] = rotation.y
            rotations[index + 2] = rotation.z
            rotations[index + 3] = rotation.w

        }

        val children = aiNode.mChildren()
        if (children != null) {
            for (i in 0 until aiNode.mNumChildren()) {
                readAnimationFrame(
                    aiScene, boneMap,
                    animNodeCache, animationTime,
                    AINode.create(children[i]),
                    frameIndex, rootMotion, rotations
                )
            }
        }

    }

    fun getDuration(
        animNodeCache: Map<String, NodeAnim>
    ): Double {
        return animNodeCache.maxOfOrNull { (_, v0) ->
            val value = v0.aiNodeAnim
            val t0 = value.mPositionKeys()!![value.mNumPositionKeys() - 1].mTime()
            val t1 = value.mRotationKeys()!![value.mNumRotationKeys() - 1].mTime()
            val t2 = value.mScalingKeys()!![value.mNumScalingKeys() - 1].mTime()
            max(t0, max(t1, t2))
        } ?: 1.0
    }

    fun loadAnimationFrame(
        aiScene: AIScene,
        rootNode: AINode,
        timeIndex: Double,
        skinningMatrices: Array<Matrix4x3f>,
        globalTransform: Matrix4x3f?,
        globalInverseTransform: Matrix4x3f?,
        boneMap: Map<String, Bone>,
        animNodeCache: Map<String, NodeAnim>
    ) {
        readAnimationFrame(
            aiScene, boneMap, true, animNodeCache,
            timeIndex, rootNode, skinningMatrices,
            null, globalTransform, globalInverseTransform
        )
    }

    fun loadAnimationFrame(
        aiScene: AIScene,
        rootNode: AINode,
        timeIndex: Double,
        frameIndex: Int,
        rootMotion: FloatArray,
        rotations: FloatArray,
        boneMap: Map<String, Bone>,
        animNodeCache: Map<String, NodeAnim>
    ) {
        readAnimationFrame(
            aiScene, boneMap, animNodeCache,
            timeIndex, rootNode, frameIndex, rootMotion, rotations
        )
    }

}