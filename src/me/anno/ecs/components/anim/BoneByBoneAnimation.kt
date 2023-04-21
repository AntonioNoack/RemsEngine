package me.anno.ecs.components.anim

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.config.DefaultConfig
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
import me.anno.utils.types.Arrays.resize
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

    fun prepareBuffers() {
        val space = Math.multiplyExact(boneCount, frameCount)
        val s3 = Math.multiplyExact(3, space)
        val s4 = Math.multiplyExact(4, space)
        val translations = translations.resize(s3)
        val rotations = rotations.resize(s4)
        // set transform to identity
        translations.fill(0f)
        rotations.fill(0f)
        for (i in 0 until space) {
            rotations[i * 4 + 3] = 1f
        }
        this.translations = translations
        this.rotations = rotations
    }

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

    fun getRotation(frame: Int, bone: Int, dst: Quaternionf): Quaternionf {
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

    override fun getMatrices(index: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val (fraction, frameIndex0, frameIndex1) = calculateMonotonousTime(index, frameCount)
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

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as BoneByBoneAnimation
        dst.boneCount = boneCount
        dst.frameCount = frameCount
        dst.translations = translations
        dst.rotations = rotations
        dst.globalTransform.set(globalTransform)
        dst.globalInvTransform.set(globalInvTransform)
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
        val tmpV = Vector3f()
        val tmpQ = Quaternionf()
        val tmpM = Matrix4x3f()
        val frames = anim.frames
        val bones = skel.bones
        for (i in frames.indices) {
            val frame = frames[i]
            // calculate rotations
            for (j in bones.indices) {
                val bone = bones[j]
                val pj = bone.parentId
                val pose = frame[j] // bind pose [world space] -> animated pose [world space]
                val data = fromImported(bone.bindPose, pose, frame.getOrNull(pj), tmpV, tmpQ, tmpM)
                setTranslation(i, j, data.getTranslation(tmpV))
                setRotation(i, j, data.getUnnormalizedRotation(tmpQ)) // probably would be the same as tmp2
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

    override val className: String get() = "BoneByBoneAnimation"

    companion object {

        fun fromImported(
            bindPose: Matrix4x3f,
            skinning: Matrix4x3f,
            parentSkinning: Matrix4x3f?,
            tmpV: Vector3f, tmpQ: Quaternionf,
            dst: Matrix4x3f
        ): Matrix4x3f {
            skinning.mul(bindPose, dst) // position in model
            if (parentSkinning != null) {
                // (parent * bindPose)^-1 * dst
                predict(parentSkinning, bindPose, JomlPools.mat4x3f.borrow())
                    .invert().mul(dst, dst)
            }

            // make that the rotation is always in world space
            //  by rotating the base of the rotation into world space
            // todo there probably is a way to transform this without awkward maths
            val pos = dst.getTranslation(tmpV)
            val rot = dst.getUnnormalizedRotation(tmpQ)
            rotQuat(rot, bindPose)
            bindPose.transformDirection(pos)
            dst.translationRotate(pos, rot)

            return dst
        }

        /**
         * rotate the main axis of a quaternion;
         * I don't know whether anyone uses this, whether this is a legit operation;
         * I think I need it here, and it looks correct to me
         * */
        fun rotQuat(q: Quaternionf, rot: Matrix4x3f) {
            val v = JomlPools.vec3f.borrow()
            rot.transformDirection(v.set(q.x, q.y, q.z))
            q.set(v.x, v.y, v.z, q.w)
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

            // todo there probably is a way to transform this without awkward maths
            bone.inverseBindPose.transformDirection(t)
            rotQuat(r, bone.inverseBindPose)

            dst.translationRotate(t, r)
            if (parentSkinning != null) {
                // dst = (parent * bindPose) * dst
                bone.bindPose.mul(dst, dst)
                parentSkinning.mul(dst, dst)
                // predict(parentSkinning, bone.bindPose, JomlPools.mat4x3f.borrow()).mul(dst, dst)
            }
            dst.mul(bone.inverseBindPose)
        }

        fun predict(parentSkinning: Matrix4x3f, bindPose: Matrix4x3f, dst: Matrix4x3f): Matrix4x3f {
            return parentSkinning.mul(bindPose, dst)
        }

        fun format(m: Matrix4x3f): String {
            return "[[${m.m00.f3s()} ${m.m01.f3s()} ${m.m02.f3s()}] " +
                    "[${m.m10.f3s()} ${m.m11.f3s()} ${m.m12.f3s()}] " +
                    "[${m.m20.f3s()} ${m.m21.f3s()} ${m.m22.f3s()}] " +
                    "[${m.m30.f3s()}, ${m.m31.f3s()}, ${m.m32.f3s()}]]"
        }

        // todo recode animation retargeting using this class :3
        /**
         * ImportedAnimation -> BoneByBone -> ImportedAnimation
         * */
        @JvmStatic
        fun main(args: Array<String>) {

            // front legs are broken slightly???

            // todo why is this springy/squishy?

            ECSRegistry.initMeshes()

            val meshFile = downloads.getChild("3d/azeria/scene.gltf")
            // val meshFile = downloads.getChild("3d/FemaleStandingPose/7.4.fbx")
            var animFile = meshFile.getChild("animations")
            if (animFile.listChildren()!!.first().isDirectory) animFile = animFile.listChildren()!!.first()
            animFile = animFile.getChild("BoneByBone.json")
            val animation = PrefabCache.getPrefabInstance(animFile).run {
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
                    val dr = sin(time * 20f) / 20f
                    val boneIndex = if (meshFile.absolutePath.contains("azeria")) 7 else 67
                    for (fi in 0 until animation.frameCount) {
                        animation.getRotation(fi, boneIndex, r)
                        r.rotateX(-dr)
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