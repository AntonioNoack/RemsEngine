package me.anno.mesh.assimp

import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.utils.Maths.min
import me.anno.utils.Maths.mix
import me.anno.utils.search.BinarySearch.binarySearch
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.assimp.*
import kotlin.math.max


object AnimatedMeshesLoader2 : StaticMeshesLoader() {

    private val LOGGER = LogManager.getLogger(AnimatedMeshesLoader2::class)

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

    private fun interpolatePosition(animationTime: Double, aiNodeAnim: AINodeAnim): Vector3f {
        val keys = aiNodeAnim.mPositionKeys()!!
        if (aiNodeAnim.mNumPositionKeys() < 2) {
            return vec(keys[0].mValue())
        }
        val index = findPosition(animationTime, aiNodeAnim)
        val t1 = keys[index + 1].mTime()
        val t0 = keys[index].mTime()
        val fraction = ((animationTime - t0) / (t1 - t0)).toFloat()
        // println("$animationTime -> $index + $fraction")
        val start = keys[index].mValue()
        val end = keys[index + 1].mValue()
        return Vector3f(
            mix(start.x(), end.x(), fraction),
            mix(start.y(), end.y(), fraction),
            mix(start.z(), end.z(), fraction)
        )
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

    fun readNodeHierarchy(
        aiScene: AIScene,
        boneMap: Map<String, Bone>,
        animNodeCache: Map<String, AINodeAnim>,
        animationTime: Double,
        aiNode: AINode,
        nodeTransformParent: Matrix4f?,
        globalInverseTransform: Matrix4f
    ) {

        val nodeName = aiNode.mName().dataString()
        val localTransform = convert(aiNode.mTransformation())
        val aiNodeAnim: AINodeAnim? = animNodeCache[nodeName]

        if (aiNodeAnim != null) {

            // local matrix
            val scale = interpolateScale(animationTime, aiNodeAnim)
            val rotation = interpolateRotation(animationTime, aiNodeAnim)
            val translation = interpolatePosition(animationTime, aiNodeAnim)

            // println("$scale $rotation $translation")

            localTransform.identity()
                .translate(translation)
                .rotate(rotation)
                .scale(scale)

        }

        val nodeTransform =
            if (nodeTransformParent == null) localTransform
            else Matrix4f(nodeTransformParent).mul(localTransform)

        val bone = boneMap[nodeName]
        if (bone != null) {
            // inspired by DAE from Karl (ThinMatrix)
            // bone.skinningMatrix.set(nodeTransform).mul(convert(aiNode.mTransformation()).invert())
            // at least the DAE sample is working
            bone.skinningMatrix.set(globalInverseTransform).mul(nodeTransform).mul(bone.offsetMatrix)
            // bone.skinningMatrix.set(modelTransform).mul(Matrix4f(bone.offsetMatrix).invert())
        }

        val children = aiNode.mChildren()
        if (children != null) {
            for (i in 0 until aiNode.mNumChildren()) {
                readNodeHierarchy(
                    aiScene,
                    boneMap,
                    animNodeCache,
                    animationTime,
                    AINode.create(children[i]),
                    nodeTransform,
                    globalInverseTransform
                )
            }
        }

    }

    fun getDuration(
        animNodeCache: Map<String, AINodeAnim>
    ): Double {
        return animNodeCache.maxOfOrNull { (_, value) ->
            val t0 = value.mPositionKeys()!![value.mNumPositionKeys() - 1].mTime()
            val t1 = value.mRotationKeys()!![value.mNumRotationKeys() - 1].mTime()
            val t2 = value.mScalingKeys()!![value.mNumScalingKeys() - 1].mTime()
            max(t0, max(t1, t2))
        } ?: 1.0
    }

    fun boneTransform2(
        aiScene: AIScene,
        rootNode: AINode,
        relativeIndex: Double,
        transforms: ArrayList<Matrix4x3f>,
        globalInverseTransform: Matrix4f,
        boneList: List<Bone>,
        boneMap: Map<String, Bone>,
        animNodeCache: Map<String, AINodeAnim>
    ) {
        readNodeHierarchy(aiScene, boneMap, animNodeCache, relativeIndex, rootNode, null, globalInverseTransform)
        while (transforms.size < boneList.size) transforms.add(Matrix4x3f())
        for (i in boneList.indices) {
            transforms[i].set(boneList[i].skinningMatrix)
        }
    }


}