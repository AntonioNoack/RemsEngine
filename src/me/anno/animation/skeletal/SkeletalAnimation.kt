package me.anno.animation.skeletal

import me.anno.animation.skeletal.bones.Bone
import me.anno.animation.skeletal.constraint.Constraint
import me.anno.animation.skeletal.morphing.MorphingMesh
import me.anno.gpu.GFX
import me.anno.utils.types.Vectors.f2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL21C
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer
import kotlin.math.max

// finally working with the help of http://rodolphe-vaillant.fr/?e=77
// not 100% correctly though...

open class SkeletalAnimation(val skeleton: Skeleton, usesPositionForBindPoses: Boolean) {

    private var isValid = false

    private val boneCount = skeleton.boneCount
    private val hierarchy = skeleton.hierarchy

    val bonePositions = FloatArray((skeleton.boneCount + 1) * 3)

    lateinit var bones: Array<Bone>

    val boneByName = HashMap<String, Bone>()

    val constraints = ArrayList<Constraint>()

    var morphingMesh: MorphingMesh? = null

    init {
        createBones(usesPositionForBindPoses)
    }

    constructor(skeleton: Skeleton, mesh: MorphingMesh) : this(skeleton, true) {
        morphingMesh = mesh
    }

    fun createBones(usesPositionForBindPoses: Boolean) {

        val parentIndices = hierarchy.parentIndices
        val names = hierarchy.names

        bones = Array(skeleton.boneCount) { index ->
            Bone(
                index, names[index],
                Vector3f(), Vector3f(),
                Vector3f(), Vector3f(-3f), Vector3f(3f),
                usesPositionForBindPoses
            )
        }

        for ((index, bone) in bones.withIndex()) {
            boneByName[bone.name] = bone
            if (index > 0) {
                val parent = bones[parentIndices[index]]
                bone.parent = parent
                parent.children += bone
            }
        }

    }

    fun getBone(name: String) = boneByName[name]

    fun invalidate() {
        isValid = false
    }

    private fun updateLocalBindPoses() {
        skeleton.hierarchy.updateLocalBindPoses(this)
        isValid = true
    }

    fun parent(i: Int) = if (i < 0) 0 else hierarchy.parentIndices[i]

    fun getChain(from: Bone?, to: Bone?): List<Bone> {
        from ?: return emptyList()
        val bones = ArrayList<Bone>()
        var current: Bone? = to
        while (current != from && current != null) {
            bones += current
            current = current.parent
        }
        bones += from
        bones.reverse()
        return bones
    }

    private fun calculateUserTransforms(startIndex: Int, endIndex: Int) {
        for (tailIndex in startIndex until endIndex) {
            bones[tailIndex].updateUserTransform()
        }
    }

    open fun updateAllAndUpload(uniform: Int, localTransform: Matrix4x3f) {

        if (!isValid) updateLocalBindPoses()

        updateConstraints()

        update(0, boneCount)

        uploadSkeleton(uniform)

    }

    fun updateConstraints() {
        for (it in constraints) {
            it.apply()
        }
    }

    fun update(startIndex: Int, endIndex: Int) {

        calculateUserTransforms(startIndex, endIndex)

        val tmp = Matrix4x3f()
        for (i in startIndex until endIndex) {
            bones[i].updateTransform(tmp)
        }

    }

    fun add(constraint: Constraint): Constraint {
        constraints += constraint
        return constraint
    }

    var first = true
    private fun uploadSkeleton(uniform: Int) {

        if (uniform <= 0) return

        for (boneIndex in bones.indices) {
            if (boneIndex >= maxBoneCount) break
            val bone = bones[boneIndex]
            val matrix = bone.skinningMatrix
            skeletalBuffer.position(12 * boneIndex)
            matrix.get(skeletalBuffer)
            if (first) LOGGER.info("$boneIndex/${bones.size}/${skeleton.hierarchy.getName(boneIndex)}:\n${matrix.f2()}")
        }
        first = false

        for (boneIndex in bones.size until maxBoneCount) {
            skeletalBuffer.position(12 * boneIndex)
            uniform4x3.get(skeletalBuffer)
        }

        skeletalBuffer.position(0)
        GFX.check()
        // only update the bones we need
        GL21C.nglUniformMatrix4x3fv(uniform, max(1, bones.size), false, MemoryUtil.memAddress(skeletalBuffer))
        // GL21.glUniformMatrix4x3fv(uniform, false, skeletalBuffer)
        GFX.check()

    }

    companion object {
        private val LOGGER = LogManager.getLogger(SkeletalAnimation::class)
        const val maxBoneCount = 256
        val uniform4x3 = Matrix4x3f()
        val skeletalBuffer: FloatBuffer =
            BufferUtils.createFloatBuffer(12 * maxBoneCount)
    }

}