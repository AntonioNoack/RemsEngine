package me.anno.animation.skeletal

import me.anno.animation.skeletal.bones.Bone
import me.anno.animation.skeletal.constraint.Constraint
import me.anno.animation.skeletal.morphing.MorphingMesh
import me.anno.gpu.GFX
import org.joml.Matrix4x3f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL21
import java.nio.FloatBuffer

// finally working with http://rodolphe-vaillant.fr/?e=77
// not 100% correctly though...

open class SkeletalAnimation(val skeleton: Skeleton, val mesh: MorphingMesh) {

    private val boneCount = skeleton.boneCount
    private val hierarchy = skeleton.hierarchy

    val bonePositions = FloatArray((skeleton.boneCount + 1) * 3)
    val bones: Array<Bone>
    val boneByName = HashMap<String, Bone>()

    val constraints = ArrayList<Constraint>()

    init {

        val bonePositions = hierarchy.calculateBonePositions(mesh.points, bonePositions)
        val parentIndices = hierarchy.parentIndices
        val names = hierarchy.names
        val positions = Array(boneCount + 1) {
            Vector3f(bonePositions[it * 3], bonePositions[it * 3 + 1], bonePositions[it * 3 + 2])
        }

        positions[boneCount].set(0f)

        bones = Array(skeleton.boneCount) { index ->
            Bone(index, names[index],
                    positions[if (index == 0) boneCount else parentIndices[index]], positions[index],
                    Vector3f(), Vector3f(-3f), Vector3f(3f))
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

    private var isValid = false
    private fun updateLocalBindPoses() {

        val hierarchy = skeleton.hierarchy
        val boneCount = hierarchy.names.size
        val parentIndices = hierarchy.parentIndices

        val bonePositions = hierarchy.calculateBonePositions(mesh.points, bonePositions)
        val positions = Array(boneCount + 1) { Vector3f(bonePositions[it * 3], bonePositions[it * 3 + 1], bonePositions[it * 3 + 2]) }
        for ((index, bone) in bones.withIndex()) {
            bone.head.set(positions[if (index == 0) boneCount else parentIndices[index]])
            bone.tail.set(positions[index])
            bone.updateDelta()
        }

        for (bone in bones) {
            bone.updateLocalBindPose()
        }

        isValid = true

    }

    fun parent(i: Int) = if (i < 0) 0 else hierarchy.parentIndices[i]

    fun getChain(from: Bone?, to: Bone?): List<Bone> {
        from ?: return emptyList()
        val bones = ArrayList<Bone>()
        var current: Bone? = to
        while(current != from && current != null){
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

    fun updateConstraints(){
        constraints.forEach { it.apply() }
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

    private fun uploadSkeleton(uniform: Int) {

        if(uniform <= 0) return

        for(boneIndex in bones.indices){
            if(boneIndex >= maxBoneCount) break
            val bone = bones[boneIndex]
            val matrix = bone.skinningMatrix
            skeletalBuffer.position(12 * boneIndex)
            matrix.get(skeletalBuffer)
        }

        skeletalBuffer.position(0)
        GFX.check()
        GL21.glUniformMatrix4x3fv(uniform, false, skeletalBuffer)
        GFX.check()

    }

    companion object {
        const val maxBoneCount = 256
        val skeletalBuffer: FloatBuffer =
            BufferUtils.createFloatBuffer(12 * maxBoneCount)
    }

}