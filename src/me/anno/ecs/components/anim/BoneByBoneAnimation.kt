package me.anno.ecs.components.anim

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.mix
import me.anno.mesh.assimp.Bone
import me.anno.studio.StudioBase
import me.anno.utils.OS.downloads
import me.anno.utils.hpc.ThreadLocal2
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

    /**
     * transforms animated positions from offset space to local space such that
     * we don't have to handle animating positions in most cases;
     *
     * isn't tested with globalTransform yet
     * */
    class Helper {

        val locals = ArrayList<Matrix4x3f>()
        val localIInv = ArrayList<Matrix4x3f>()
        val predictedTranslations = ArrayList<Vector3f>()

        fun ensure(size: Int) {
            locals.ensureCapacity(size)
            localIInv.ensureCapacity(size)
            predictedTranslations.ensureCapacity(size)
            for (i in locals.size until size) {
                locals.add(Matrix4x3f())
                localIInv.add(Matrix4x3f())
                predictedTranslations.add(Vector3f())
            }
        }

        fun fromImported(bone: Bone, skinning: Matrix4x3f, parentSkinning: Matrix4x3f?, dst: Matrix4x3f): Matrix4x3f {
            val pred = predict(parentSkinning, bone)
            val boneId = bone.id
            dst.set(skinning).mul(bone.bindPose) // position in model
            val localI = if (bone.parentId < 0) locals[boneId].set(dst) // position relative to parent
            else localIInv[bone.parentId].mul(dst, locals[boneId])
            localI.invert(localIInv[boneId])
            return dst.translateLocal(-pred.x, -pred.y, -pred.z)
        }

        fun toImported(
            bone: Bone,
            parentSkinning: Matrix4x3f?,
            t: Vector3f, r: Quaternionf,
            dst: Matrix4x3f
        ): Matrix4x3f {
            dst.translationRotate(t, r)
            val pred = predict(parentSkinning, bone)
            dst.translateLocal(pred)
            dst.mul(bone.inverseBindPose)
            return dst
        }

        fun predict(parentSkinning: Matrix4x3f?, bone: Bone): Vector3f {
            val pred = predictedTranslations[bone.id]
            if (parentSkinning != null)
                parentSkinning
                    .transformPosition(bone.bindPose.getTranslation(pred))
            else pred.set(0f)
            return pred
        }

    }

    val globalTransform = Matrix4x3f()
    val globalInvTransform = Matrix4x3f()

    var boneCount = 0
    var frameCount = 0

    var translations: FloatArray? = null // Array(frameCount) { Vector3f() }
    var rotations: FloatArray? = null // Array(frameCount * boneCount) { Quaternionf() }

    override val numFrames get() = frameCount

    fun getTranslation(frame: Int, bone: Int, dst: Vector3f = Vector3f()): Vector3f {
        val index = 3 * (frame * boneCount + bone)
        val rm = translations!!
        return dst.set(rm[index], rm[index + 1], rm[index + 2])
    }

    fun getTranslation(fraction: Float, frame0: Int, frame1: Int, bone: Int, dst: Vector3f = Vector3f()): Vector3f {
        val index0 = 3 * (frame0 * boneCount + bone)
        val index1 = 3 * (frame1 * boneCount + bone)
        val rm = translations!!
        return dst.set(
            mix(rm[index0], rm[index1], fraction),
            mix(rm[index0 + 1], rm[index1 + 1], fraction),
            mix(rm[index0 + 2], rm[index1 + 2], fraction)
        )
    }

    /**
     * modifies the bone in the respective frame;
     * call AnimationCache.invalidate() to notify the engine about your change
     * (this is not done automatically to save a bit on performance, since you would probably call this function a lot)
     * */
    fun setTranslation(frame: Int, bone: Int, v: Vector3f) {
        val index = 3 * (frame * boneCount + bone)
        val rm = translations!!
        rm[index + 0] = v.x
        rm[index + 1] = v.y
        rm[index + 2] = v.z
    }

    fun getRotation(frame: Int, bone: Int, dst: Quaternionf = Quaternionf()): Quaternionf {
        val index = 4 * (frame * boneCount + bone)
        val src = rotations!!
        return dst.set(src[index], src[index + 1], src[index + 2], src[index + 3])
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
    fun setRotation(frame: Int, bone: Int, v: Quaternionf) {
        val index = 4 * (frame * boneCount + bone)
        val dst = rotations!!
        dst[index] = v.x
        dst[index + 1] = v.y
        dst[index + 2] = v.z
        dst[index + 3] = v.w
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
        val calc = calc0.get()
        calc.ensure(bones.size)
        for (boneId in bones.indices) {
            val bone = bones[boneId]
            getTranslation(fraction, frameIndex0, frameIndex1, boneId, tmpPos)
            getRotation(fraction, frameIndex0, frameIndex1, boneId, tmpRot)
            calc.toImported(bone, dst.getOrNull(bone.parentId), tmpPos, tmpRot, dst[boneId])
        }
        return dst
    }

    override fun getMatrices(index: Int, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val bones = skeleton.bones
        val tmpPos = JomlPools.vec3f.borrow()
        val tmpRot = JomlPools.quat4f.borrow()
        val calc = calc0.get()
        calc.ensure(bones.size)
        for (boneId in bones.indices) {
            val bone = bones[boneId]
            getTranslation(index, boneId, tmpPos)
            getRotation(index, boneId, tmpRot)
            calc.toImported(bone, dst.getOrNull(bone.parentId), tmpPos, tmpRot, dst[boneId])
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
        println("$boneCount x $frameCount")
        val boneCount = skel.bones.size
        translations = FloatArray(boneCount * frameCount * 3)
        rotations = FloatArray(boneCount * frameCount * 4)
        val tmpPos = Vector3f()
        val tmpRot = Quaternionf()
        val tmpMat = Matrix4x3f()
        val calc = calc0.get()
        calc.ensure(boneCount)
        for ((i, frame) in anim.frames.withIndex()) {
            // calculate rotations
            for ((j, bone) in skel.bones.withIndex()) {
                val pj = bone.parentId
                val pose = frame[j] // bind pose [world space] -> animated pose [world space]
                pose.getTranslation(tmpPos) // animation offset by root motion + animation rotations
                pose.getUnnormalizedRotation(tmpRot)
                val opt = calc.fromImported(bone, pose, frame.getOrNull(pj), tmpMat)
                setTranslation(i, j, opt.getTranslation(tmpPos))
                setRotation(i, j, opt.getUnnormalizedRotation(tmpRot)) // probably would be the same as tmp2
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
        val calc = calc0.get()
        calc.ensure(boneCount)
        anim.frames = Array(frameCount) { fi ->
            val matrices = Array(skel.bones.size) { Matrix4x3f() }
            for ((bi, bone) in skel.bones.withIndex()) {
                val pj = bone.parentId
                getTranslation(fi, bi, tmpPos)
                getRotation(fi, bi, tmpRot)
                calc.toImported(bone, matrices.getOrNull(pj), tmpPos, tmpRot, matrices[bi])
            }
            matrices
        }
        return anim
    }

    override val className = "BoneByBoneAnimation"

    companion object {

        val calc0 = ThreadLocal2 { Helper() }

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

            val meshFile = downloads.getChild("3d/azeria/scene.gltf")
            val imported = PrefabCache.getPrefabInstance(
                meshFile.getChild("animations").listChildren()!!.first()
            ) as ImportedAnimation

            val boneByBone = BoneByBoneAnimation(imported)
            /*val exported = boneByBone.toImported()

            for (i in 0 until imported.numFrames) {
                println()
                println(imported.frames[i].joinToString { format(it) })
                println(exported.frames[i].joinToString { format(it) })
            }*/

            // create test scene
            val mesh = AnimRenderer()
            mesh.skeleton = boneByBone.skeleton
            mesh.animations = listOf(AnimationState(boneByBone.ref, 1f, 0f, 1f, LoopingState.PLAY_LOOP))
            mesh.mesh = meshFile

            for (bone in SkeletonCache[mesh.skeleton]!!.bones) {
                println("${bone.id}: ${bone.name}")
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
                    val boneIndex = 7
                    for (fi in 0 until boneByBone.frameCount) {
                        boneByBone.getRotation(fi, boneIndex, r)
                        r.rotateZ(dr)
                        boneByBone.setRotation(fi, boneIndex, r)
                    }
                    AnimationCache.invalidate(boneByBone, boneByBone.skeleton)
                    time += 0.01f
                    Thread.sleep(10)
                }
            }

            testSceneWithUI(mesh) {
                StudioBase.instance?.enableVSync = true
            }

            // Engine.requestShutdown()

        }
    }
}