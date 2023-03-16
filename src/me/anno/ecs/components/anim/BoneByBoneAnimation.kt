package me.anno.ecs.components.anim

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.mesh.assimp.Bone
import me.anno.studio.StudioBase
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.f3s
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.concurrent.thread
import kotlin.math.sin

/**
 * a basic animation for characters, where animated translation only exists at the root node
 * this can be easier retargeted and customized than ImportedAnimation
 * */
class BoneByBoneAnimation() : Animation() {

    val globalTransform = Matrix4x3f()
    val globalInvTransform = Matrix4x3f()

    var boneCount = 0
    var frameCount = 0

    var translations: FloatArray? = null // Array(frameCount) { Vector3f() }
    var rotations: FloatArray? = null // Array(frameCount * boneCount) { Quaternionf() }

    override val numFrames get() = frameCount

    fun getTranslation(frame: Int, bone: Int, dst: Vector3f): Vector3f {
        return dst.set(translations!!, 3 * (frame * boneCount + bone))
    }

    fun getTranslation(fraction: Float, frame0: Int, frame1: Int, bone: Int, dst: Vector3f = Vector3f()): Vector3f {
        val index0 = 3 * (frame0 * boneCount + bone)
        val index1 = 3 * (frame1 * boneCount + bone)
        val src = translations!!
        return dst.set(
            mix(src[index0], src[index1], fraction),
            mix(src[index0 + 1], src[index1 + 1], fraction),
            mix(src[index0 + 2], src[index1 + 2], fraction)
        )
    }

    /**
     * modifies the bone in the respective frame;
     * call AnimationCache.invalidate() to notify the engine about your change
     * (this is not done automatically to save a bit on performance, since you would probably call this function a lot)
     * */
    fun setTranslation(frame: Int, bone: Int, src: Vector3f) {
        src.get(translations!!, 3 * (frame * boneCount + bone))
    }

    fun getRotation(frame: Int, bone: Int, dst: Quaternionf = Quaternionf()): Quaternionf {
        return dst.set(rotations!!, 4 * (frame * boneCount + bone))
    }

    fun getRotation(
        fraction: Float, frame0: Int, frame1: Int, bone: Int,
        dst: Quaternionf = Quaternionf()
    ): Quaternionf {
        val idx0 = 4 * (frame0 * boneCount + bone)
        val idx1 = 4 * (frame1 * boneCount + bone)
        val src = rotations!!
        val tmp = JomlPools.quat4f.borrow()
        dst.set(src[idx0], src[idx0 + 1], src[idx0 + 2], src[idx0 + 3])
        dst.slerp(tmp.set(src[idx1], src[idx1 + 1], src[idx1 + 2], src[idx1 + 3]), fraction)
        return dst
    }

    /**
     * modifies the bone in the respective frame;
     * call AnimationCache.invalidate() to notify the engine about your change
     * (this is not done automatically to save a bit on performance, since you would probably call this function a lot)
     * */
    fun setRotation(frame: Int, bone: Int, src: Quaternionf) {
        src.get(rotations!!, 4 * (frame * boneCount + bone))
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("boneCount", boneCount)
        writer.writeInt("frameCount", frameCount)
        writer.writeMatrix4x3f("globalTransform", globalTransform)
        writer.writeMatrix4x3f("globalInvTransform", globalInvTransform)
        writer.writeFloatArray("rootMotion", translations)
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
            "rootMotion" -> translations = values
            "rotations" -> rotations = values
            else -> super.readFloatArray(name, values)
        }
    }

    override fun getMatrices(entity: Entity?, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val (fraction, frameIndex0, frameIndex1) = calculateMonotonousTime(time, frameCount)
        val bones = skeleton.bones
        val tmpPos = JomlPools.vec3f.borrow()
        val tmpRot = JomlPools.quat4f.borrow()
        for (boneId in 0 until min(dst.size, bones.size)) {
            val bone = bones[boneId]
            getTranslation(fraction, frameIndex0, frameIndex1, boneId, tmpPos)
            getRotation(fraction, frameIndex0, frameIndex1, boneId, tmpRot)
            toImported(bone, dst.getOrNull(bone.parentId), tmpPos, tmpRot, dst[boneId])
        }
        return dst
    }

    override fun getMatrices(index: Int, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val bones = skeleton.bones
        val tmpPos = JomlPools.vec3f.borrow()
        val tmpRot = JomlPools.quat4f.borrow()
        for (boneId in 0 until min(dst.size, bones.size)) {
            val bone = bones[boneId]
            getTranslation(index, boneId, tmpPos)
            getRotation(index, boneId, tmpRot)
            toImported(bone, dst.getOrNull(bone.parentId), tmpPos, tmpRot, dst[boneId])
        }
        return dst
    }

    // use this? idk...
    /*fun applyGlobal(dst: Array<Matrix4x3f>) {
        val global = globalTransform
        if (!global.isIdentity()) for (boneId in 0 until min(dst.size, boneCount)) {
            val dstI = dst[boneId]
            global.mul(dstI, dstI)
        }
    }*/

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
        clone.translations = translations
        clone.rotations = rotations
        clone.globalTransform.set(globalTransform)
        clone.globalInvTransform.set(globalInvTransform)
    }

    constructor(anim: ImportedAnimation) : this() {
        fromImported(anim)
    }

    fun fromImported(anim: ImportedAnimation): BoneByBoneAnimation {
        skeleton = anim.skeleton
        val skel = SkeletonCache[skeleton]!!
        boneCount = skel.bones.size
        frameCount = anim.frames.size
        val boneCount = skel.bones.size
        translations = FloatArray(boneCount * frameCount * 3)
        rotations = FloatArray(boneCount * frameCount * 4)
        val tmpPos = Vector3f()
        val tmpRot = Quaternionf()
        val tmpMat = Matrix4x3f()
        val frames = anim.frames
        val bones = skel.bones
        for (i in frames.indices) {
            val frame = frames[i]
            // calculate rotations
            for (j in bones.indices) {
                val bone = bones[j]
                val pj = bone.parentId
                val pose = frame[j] // bind pose [world space] -> animated pose [world space]
                val data = fromImported(bone.bindPose, pose, frame.getOrNull(pj), tmpPos, tmpMat)
                setTranslation(i, j, data.getTranslation(tmpPos))
                setRotation(i, j, data.getUnnormalizedRotation(tmpRot)) // probably would be the same as tmp2
            }
        }
        return this
    }

    @Suppress("unused")
    fun toImported(anim: ImportedAnimation = ImportedAnimation()): ImportedAnimation {
        anim.skeleton = skeleton
        val skel = SkeletonCache[skeleton]!!
        val tmpPos = JomlPools.vec3f.borrow()
        val tmpRot = JomlPools.quat4f.borrow()
        val bones = skel.bones
        anim.frames = Array(frameCount) { i ->
            val matrices = Array(bones.size) { Matrix4x3f() }
            for (j in bones.indices) {
                val bone = bones[j]
                val pj = bone.parentId
                getTranslation(i, j, tmpPos)
                getRotation(i, j, tmpRot)
                toImported(bone, matrices.getOrNull(pj), tmpPos, tmpRot, matrices[j])
            }
            matrices
        }
        return anim
    }

    override val className get() = "BoneByBoneAnimation"

    companion object {

        fun fromImported(
            bindPose: Matrix4x3f,
            skinning: Matrix4x3f,
            parentSkinning: Matrix4x3f?,
            tmp: Vector3f,
            dst: Matrix4x3f
        ): Matrix4x3f {
            skinning.mul(bindPose, dst) // position in model
            if (parentSkinning != null) {
                predict(parentSkinning, bindPose, tmp)
                dst.translateLocal(-tmp.x, -tmp.y, -tmp.z)
            }
            return dst
        }

        /**
         * warn: invalidates t!
         * */
        fun toImported(
            bone: Bone,
            parentSkinning: Matrix4x3f?,
            t: Vector3f, r: Quaternionf,
            dst: Matrix4x3f
        ) {
            dst.translationRotate(t, r)
            if (parentSkinning != null) {
                predict(parentSkinning, bone.bindPose, t)
                dst.translateLocal(t)
            }
            dst.mul(bone.inverseBindPose)
        }

        fun predict(parentSkinning: Matrix4x3f, bindPose: Matrix4x3f, dst: Vector3f) {
            parentSkinning.transformPosition(bindPose.getTranslation(dst))
        }


        fun format(m: Matrix4x3f): String {
            return "[[${m.m00.f3s()} ${m.m01.f3s()} ${m.m02.f3s()}] " +
                    "[${m.m10.f3s()} ${m.m11.f3s()} ${m.m12.f3s()}] " +
                    "[${m.m20.f3s()} ${m.m21.f3s()} ${m.m22.f3s()}] " +
                    "[${m.m30.f3s()}, ${m.m31.f3s()}, ${m.m32.f3s()}]]"
        }

        /**
         * ImportedAnimation -> BoneByBone -> ImportedAnimation
         * */
        @JvmStatic
        fun main(args: Array<String>) {

            // front legs are broken slightly???

            ECSRegistry.initMeshes()

            // val meshFile = downloads.getChild("3d/azeria/scene.gltf")
            val meshFile = downloads.getChild("3d/FemaleStandingPose/7.4.fbx")
            val animation = PrefabCache.getPrefabInstance(
                meshFile.getChild("animations").listChildren()!!.first()
                    .getChild("BoneByBone.json")
            ).run {
                if (this is ImportedAnimation) BoneByBoneAnimation(this)
                else this as BoneByBoneAnimation
            }

            // create test scene
            val mesh = AnimRenderer()
            mesh.skeleton = animation.skeleton
            mesh.animations = listOf(AnimationState(animation.ref, 1f, 0f, 1f, LoopingState.PLAY_LOOP))
            mesh.mesh = meshFile

            for (bone in SkeletonCache[mesh.skeleton]!!.bones) {
                LOGGER.debug(
                    "Bone ${bone.id}: ${bone.name}${" ".repeat(max(0, 80 - bone.name.length))}" +
                            "f0: ${animation.getTranslation(0, bone.id, Vector3f())}, " +
                            "${animation.getRotation(0, bone.id, Quaternionf())}"
                )
            }

            // create script, which modifies the animation at runtime
            thread {
                // rotations should be relative to their parent, probably
                //  but also in global space... is this contradicting?
                // yes, it is 😅, but we could define sth like a common up;
                val r = Quaternionf()
                var time = 0f
                while (!Engine.shutdown) {
                    val dr = sin(time * 20f) / 50f
                    val boneIndex = 67 // fox: 7
                    for (fi in 0 until animation.frameCount) {
                        animation.getRotation(fi, boneIndex, r)
                        r.rotateZ(dr)
                        animation.setRotation(fi, boneIndex, r)
                    }
                    AnimationCache.invalidate(animation)
                    time += 0.01f
                    Thread.sleep(10)
                }
            }

            testSceneWithUI(mesh) {
                DefaultConfig["debug.renderdoc.enabled"] = true
                StudioBase.instance?.enableVSync = true
            }

            Engine.requestShutdown()

        }
    }
}