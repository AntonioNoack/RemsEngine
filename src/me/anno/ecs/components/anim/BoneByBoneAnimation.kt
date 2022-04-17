package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * a basic animation for characters, where animated translation only exists at the root node
 * this can be easier retargeted and customized than ImportedAnimation
 * */
class BoneByBoneAnimation : Animation() {

    val globalTransform = Matrix4x3f()
    val globalInvTransform = Matrix4x3f()

    var boneCount = 0
    var frameCount = 0

    var rootMotion: FloatArray? = null // Array(frameCount) { Vector3f() }
    var rotations: FloatArray? = null // Array(frameCount * boneCount) { Quaternionf() }

    fun getRootMotion(frame: Int, dst: Vector3f = Vector3f()) {
        val index = 3 * frame
        val rm = rootMotion!!
        dst.set(rm[index], rm[index + 1], rm[index + 2])
    }

    fun getRootMotion(fraction: Float, frame0: Int, frame1: Int, dst: Vector3f = Vector3f()) {
        val index0 = 3 * frame0
        val index1 = 3 * frame1
        val rm = rootMotion!!
        dst.set(
            mix(rm[index0], rm[index1], fraction),
            mix(rm[index0 + 1], rm[index1 + 1], fraction),
            mix(rm[index0 + 2], rm[index1 + 2], fraction)
        )
    }

    fun setRootMotion(frame: Int, v: Vector3f) {
        val index = 3 * frame
        val rm = rootMotion!!
        rm[index + 0] = v.x
        rm[index + 1] = v.y
        rm[index + 2] = v.z
    }

    fun getRotation(frame: Int, bone: Int, dst: Quaternionf = Quaternionf()) {
        val index = 4 * (frame * boneCount + bone)
        val rotations = rotations!!
        dst.set(rotations[index], rotations[index + 1], rotations[index + 2], rotations[index + 3])
    }

    fun getRotation(fraction: Float, frame0: Int, frame1: Int, bone: Int, dst: Quaternionf = Quaternionf()) {
        val idx0 = 4 * (frame0 * boneCount + bone)
        val idx1 = 4 * (frame1 * boneCount + bone)
        val values = rotations!!
        val tmp = JomlPools.quat4f.borrow()
        dst.set(values[idx0], values[idx0 + 1], values[idx0 + 2], values[idx0 + 3])
        dst.slerp(tmp.set(values[idx1], values[idx1 + 1], values[idx1 + 2], values[idx1 + 3]), fraction)
    }

    fun setRotation(frame: Int, bone: Int, v: Quaternionf) {
        val index = 4 * (frame * boneCount + bone)
        val rotations = rotations!!
        rotations[index + 0] = v.x
        rotations[index + 1] = v.y
        rotations[index + 2] = v.z
        rotations[index + 3] = v.w
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("boneCount", boneCount)
        writer.writeInt("frameCount", frameCount)
        writer.writeMatrix4x3f("globalTransform", globalTransform)
        writer.writeMatrix4x3f("globalInvTransform", globalInvTransform)
        writer.writeFloatArray("rootMotion", rootMotion)
        writer.writeFloatArray("rotations", rotations)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "boneCount" -> boneCount = value
            "frameCount" -> frameCount = value
            else -> super.readInt(name, value)
        }
    }

    override fun readMatrix4x3f(name: String, value: Matrix4x3f) {
        when (name) {
            "globalTransform" -> globalTransform.set(value)
            "globalInvTransform" -> globalInvTransform.set(value)
            else -> super.readMatrix4x3f(name, value)
        }
    }

    override fun readFloatArray(name: String, values: FloatArray) {
        when (name) {
            "rootMotion" -> rootMotion = values
            "rotations" -> rotations = values
            else -> super.readFloatArray(name, values)
        }
    }

    // todo we could cache the matrices at the frames in question :)
    // todo or we could be more generic, and allow other skeletons, and their rough mapping...

    override fun getMatrices(entity: Entity?, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val (fraction, frameIndex0, frameIndex1) = calculateMonotonousTime(time, frameCount)
        val bones = skeleton.bones
        val rotation = Quaternionf()
        val translation = Vector3f()
        val localTransforms = Array(bones.size) { Matrix4x3f() }
        for (boneId in bones.indices) {
            val bone = bones[boneId]
            getRotation(fraction, frameIndex0, frameIndex1, boneId, rotation)
            if (boneId == 0) {
                // the root may be animated
                getRootMotion(fraction, frameIndex0, frameIndex1, translation)
            } else {
                // get the offset from the skeleton: from parent to this
                bone.relativeTransform.getTranslation(translation)
            }
            val localTransform = localTransforms[boneId]
                .identity()
                .translate(translation)
                .rotate(rotation)
            val parentLocalTransform =
                if (bone.parentId < 0) null
                else localTransforms[bone.parentId]
            parentLocalTransform?.mul(localTransform, localTransform)
            dst[boneId].identity()
                //.set(globalInvTransform)
                .mul(localTransform)
                .mul(bone.inverseBindPose)
            //.mul(globalTransform)
        }
        return dst
    }

    override fun clone(): BoneByBoneAnimation {
        val clone = BoneByBoneAnimation()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as BoneByBoneAnimation
        clone.boneCount = boneCount
        clone.frameCount = frameCount
        clone.rootMotion = rootMotion
        clone.rotations = rotations
        clone.globalTransform.set(globalTransform)
        clone.globalInvTransform.set(globalInvTransform)
    }

    override val className: String = "BoneByBoneAnimation"

}