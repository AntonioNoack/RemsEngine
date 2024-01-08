package me.anno.ecs.components.anim

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Floats.f2s
import me.anno.utils.types.Floats.f3s
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.test.assertTrue

/**
 * a basic animation for characters, where animated translation only exists at the root node
 * this can be easier retargeted and customized than ImportedAnimation
 *
 * todo this is broken for driving and digging (downloads/3d), but works fine for Azeria -> probably a scale issue...
 * */
class BoneByBoneAnimation() : Animation() {

    val globalTransform = Matrix4x3f()
    val globalInvTransform = Matrix4x3f()

    var boneCount = 0
    var frameCount = 0

    /**
     * Array(frameCount) { Array(boneCount) { Vector3f() } }
     * should be zero, when bones are connected
     * */
    var translations: FloatArray? = null

    /**
     * Array(frameCount) { Array(boneCount) { Quaternionf() } }
     * */
    var rotations: FloatArray? = null

    /**
     * is uniform scale good enough?
     * Array(frameCount) { Array(boneCount) { Vector3f() } }
     * */
    var scales: FloatArray? = null

    override val numFrames get() = frameCount

    fun prepareBuffers() {
        val space = Maths.multiplyExact(boneCount, frameCount)
        val s3 = Maths.multiplyExact(3, space)
        val s4 = Maths.multiplyExact(4, space)
        val translations = translations.resize(s3)
        val rotations = rotations.resize(s4)
        val scale = scales.resize(s3)
        // set transform to identity
        translations.fill(0f)
        rotations.fill(0f)
        scale.fill(1f)
        for (i in 0 until space) {
            rotations[i * 4 + 3] = 1f
        }
        this.translations = translations
        this.rotations = rotations
        this.scales = scale
    }

    private fun getTranslationIndex(frameIndex: Int, boneIndex: Int): Int {
        return 3 * (frameIndex * boneCount + boneIndex)
    }

    fun getTranslation(frame: Int, bone: Int, dst: Vector3f): Vector3f {
        return dst.set(translations!!, getTranslationIndex(frame, bone))
    }

    fun getTranslation(fraction: Float, frame0: Int, frame1: Int, bone: Int, dst: Vector3f): Vector3f {
        val index0 = getTranslationIndex(frame0, bone)
        val index1 = getTranslationIndex(frame1, bone)
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
        src.get(translations!!, getTranslationIndex(frame, bone))
    }

    private fun getRotationIndex(frameIndex: Int, boneIndex: Int): Int {
        return 4 * (frameIndex * boneCount + boneIndex)
    }

    fun getRotation(frame: Int, bone: Int, dst: Quaternionf): Quaternionf {
        return dst.set(rotations!!, getRotationIndex(frame, bone))
    }

    fun getRotation(
        fraction: Float, frame0: Int, frame1: Int, bone: Int,
        tmp: Quaternionf, dst: Quaternionf
    ): Quaternionf {
        val src = rotations!!
        dst.set(src, getRotationIndex(frame0, bone))
        tmp.set(src, getRotationIndex(frame1, bone))
        dst.slerp(tmp, fraction)
        return dst
    }

    /**
     * modifies the bone in the respective frame;
     * call AnimationCache.invalidate() to notify the engine about your change
     * (this is not done automatically to save a bit on performance, since you would probably call this function a lot)
     * */
    fun setRotation(frame: Int, bone: Int, src: Quaternionf) {
        src.get(rotations!!, getRotationIndex(frame, bone))
    }

    private fun getScaleIndex(frameIndex: Int, boneIndex: Int): Int {
        return 3 * (frameIndex * boneCount + boneIndex)
    }

    fun getScale(frame: Int, bone: Int, dst: Vector3f): Vector3f {
        val scales = scales ?: return dst.set(1f)
        return dst.set(scales, getScaleIndex(frame, bone))
    }

    fun getScale(fraction: Float, frame0: Int, frame1: Int, bone: Int, dst: Vector3f): Vector3f {
        val index0 = getScaleIndex(frame0, bone)
        val index1 = getScaleIndex(frame1, bone)
        val scales = scales ?: return dst.set(1f)
        return dst.set(
            mix(scales[index0], scales[index1], fraction),
            mix(scales[index0 + 1], scales[index1 + 1], fraction),
            mix(scales[index0 + 2], scales[index1 + 2], fraction)
        )
    }

    fun setScale(frame: Int, bone: Int, src: Vector3f) {
        src.get(scales!!, getScaleIndex(frame, bone))
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("boneCount", boneCount)
        writer.writeInt("frameCount", frameCount)
        writer.writeMatrix4x3f("globalTransform", globalTransform)
        writer.writeMatrix4x3f("globalInvTransform", globalInvTransform)
        writer.writeFloatArray("rootMotion", translations ?: f0)
        writer.writeFloatArray("rotations", rotations ?: f0)
        writer.writeFloatArray("scales", scales ?: f0)
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
            "scales" -> scales = values
            else -> super.readFloatArray(name, values)
        }
    }

    override fun getMatrices(frameIndex: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val (fraction, frameIndex0, frameIndex1) = calculateMonotonousTime(frameIndex, frameCount)
        val bones = skeleton.bones
        val tmpPos = JomlPools.vec3f.create()
        val tmpSca = JomlPools.vec3f.create()
        val tmpRot = JomlPools.quat4f.create()
        val tmpRot1 = JomlPools.quat4f.create()
        for (boneId in 0 until min(dst.size, bones.size)) {
            val bone = bones[boneId]
            getTranslation(fraction, frameIndex0, frameIndex1, boneId, tmpPos)
            getRotation(fraction, frameIndex0, frameIndex1, boneId, tmpRot1, tmpRot)
            getScale(fraction, frameIndex0, frameIndex1, boneId, tmpSca)
            toImported(bone, dst.getOrNull(bone.parentId), tmpPos, tmpRot, tmpSca, dst[boneId])
        }
        JomlPools.quat4f.sub(2)
        JomlPools.vec3f.sub(2)
        return dst
    }

    override fun getMatrix(frameIndex: Float, boneId: Int, dst: Array<Matrix4x3f>): Matrix4x3f? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val (fraction, frameIndex0, frameIndex1) = calculateMonotonousTime(frameIndex, frameCount)
        val bones = skeleton.bones
        val tmpPos = JomlPools.vec3f.create()
        val tmpSca = JomlPools.vec3f.create()
        val tmpRot = JomlPools.quat4f.create()
        val tmpRot1 = JomlPools.quat4f.create()
        fun calculateRecursively(boneId: Int) {
            val bone = bones[boneId]
            val parentId = bone.parentId
            if (parentId in dst.indices) calculateRecursively(boneId)
            getTranslation(fraction, frameIndex0, frameIndex1, boneId, tmpPos)
            getRotation(fraction, frameIndex0, frameIndex1, boneId, tmpRot1, tmpRot)
            getScale(fraction, frameIndex0, frameIndex1, boneId, tmpSca)
            toImported(bone, dst.getOrNull(parentId), tmpPos, tmpRot, tmpSca, dst[boneId])
        }
        calculateRecursively(boneId)
        JomlPools.quat4f.sub(2)
        JomlPools.vec3f.sub(2)
        return dst[boneId]
    }

    override fun getMatrices(frameIndex: Int, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        // println("getting matrices for $frameIndex")
        val skeleton = SkeletonCache[skeleton] ?: return null
        val bones = skeleton.bones
        val tmpPos = JomlPools.vec3f.create()
        val tmpSca = JomlPools.vec3f.create()
        val tmpRot = JomlPools.quat4f.borrow()
        for (boneId in 0 until min(dst.size, bones.size)) {
            val bone = bones[boneId]
            getTranslation(frameIndex, boneId, tmpPos)
            getRotation(frameIndex, boneId, tmpRot)
            getScale(frameIndex, boneId, tmpSca)
            // print("  $boneId -> ${bone.parentId}: $tmpPos, $tmpRot, $tmpSca")
            toImported(bone, dst.getOrNull(bone.parentId), tmpPos, tmpRot, tmpSca, dst[boneId])
            // println(" -> ${dst[boneId]}")
        }
        JomlPools.vec3f.sub(2)
        return dst
    }

    override fun getMatrix(frameIndex: Int, boneId: Int, dst: Array<Matrix4x3f>): Matrix4x3f? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val bones = skeleton.bones
        val tmpPos = JomlPools.vec3f.create()
        val tmpSca = JomlPools.vec3f.create()
        val tmpRot = JomlPools.quat4f.borrow()
        fun calculateRecursively(boneId: Int) {
            val bone = bones[boneId]
            val parentId = bone.parentId
            if (parentId in dst.indices) calculateRecursively(boneId)
            getTranslation(frameIndex, boneId, tmpPos)
            getRotation(frameIndex, boneId, tmpRot)
            getScale(frameIndex, boneId, tmpSca)
            toImported(bone, dst.getOrNull(parentId), tmpPos, tmpRot, tmpSca, dst[boneId])
        }
        calculateRecursively(boneId)
        JomlPools.vec3f.sub(2)
        return dst[boneId]
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
        dst.scales = scales
        dst.globalTransform.set(globalTransform)
        dst.globalInvTransform.set(globalInvTransform)
    }

    constructor(anim: ImportedAnimation) : this() {
        fromImported(anim)

        if (false) {
            println("Converting ${anim.name}!!")
            val new = toImported(ImportedAnimation())
            val frame = 0
            val skeleton = SkeletonCache[anim.skeleton]!!
            val src = anim.getMatrices(frame, Array(boneCount) { Matrix4x3f() })
            val dst = new.getMatrices(frame, Array(boneCount) { Matrix4x3f() })

            fun Matrix4x3f.f2() = "" +
                    "[(${m00.f2s()} ${m10.f2s()} ${m20.f2s()} ${m30.f2s()})" +
                    " (${m01.f2s()} ${m11.f2s()} ${m21.f2s()} ${m31.f2s()})" +
                    " (${m02.f2s()} ${m12.f2s()} ${m22.f2s()} ${m32.f2s()})]"

            for (boneI in 0 until boneCount) {
                val bone = skeleton.bones[boneI]
                println(
                    "  ${bone.name} [$boneI -> ${bone.parentId}]:\n" +
                            "      ${src[boneI].f2()} ->\n" +
                            "      ${dst[boneI].f2()} via\n" +
                            "      ${getTranslation(frame, boneI, Vector3f())}, ${
                                getRotation(
                                    frame,
                                    boneI,
                                    Quaternionf()
                                )
                            }, ${getScale(frame, boneI, Vector3f())}"
                )
                assertTrue(src[boneI].equals(dst[boneI], 0.01f))
            }
        }
    }

    fun fromImported(src: ImportedAnimation): BoneByBoneAnimation {
        skeleton = src.skeleton
        val skel = SkeletonCache[skeleton]!!
        boneCount = skel.bones.size
        frameCount = src.frames.size
        val boneCount = skel.bones.size
        val s1 = boneCount * frameCount
        translations = FloatArray(s1 * 3)
        rotations = FloatArray(s1 * 4)
        scales = FloatArray(s1 * 3)
        val pos = Vector3f()
        val sca = Vector3f()
        val rot = Quaternionf()
        val tmpM = Matrix4x3f()
        val frames = src.frames
        val bones = skel.bones
        for (j in bones.indices) {
            val bone = bones[j]
            val pj = bone.parentId
            if (pj >= j) throw IllegalStateException("Bones out of order, $j -> $pj")
        }
        for (i in frames.indices) {
            val frame = frames[i]
            // calculate rotations
            for (j in bones.indices) {
                val bone = bones[j]
                val pj = bone.parentId
                val pose = frame[j] // bind pose [world space] -> animated pose [world space]
                fromImported(
                    bone.bindPose, bone.inverseBindPose,
                    pose, frame.getOrNull(pj), tmpM, pos, rot, sca
                )
                /*if (i == 0 && j < 10) {
                    println("fromImported[$i/$j]: ${bone.bindPose} x $pose x ${frame.getOrNull(pj)} -> $pos, $rot, $sca")
                }*/
                setTranslation(i, j, pos)
                setRotation(i, j, rot)
                setScale(i, j, sca)
            }
        }
        return this
    }

    @Suppress("unused")
    fun toImported(dst: ImportedAnimation = ImportedAnimation()): ImportedAnimation {
        dst.skeleton = skeleton
        val skel = SkeletonCache[skeleton]!!
        val tmpPos = JomlPools.vec3f.create()
        val tmpSca = JomlPools.vec3f.create()
        val tmpRot = JomlPools.quat4f.borrow()
        val bones = skel.bones
        dst.frames = Array(frameCount) { i ->
            val matrices = Array(bones.size) { Matrix4x3f() }
            for (j in bones.indices) {
                val bone = bones[j]
                val pj = bone.parentId
                getTranslation(i, j, tmpPos)
                getRotation(i, j, tmpRot)
                getScale(i, j, tmpSca)
                toImported(bone, matrices.getOrNull(pj), tmpPos, tmpRot, tmpSca, matrices[j])
            }
            matrices
        }
        JomlPools.vec3f.sub(2)
        return dst
    }

    override val className: String get() = "BoneByBoneAnimation"

    companion object {

        private val LOGGER = LogManager.getLogger(BoneByBoneAnimation::class)
        private val f0 = FloatArray(0)

        fun fromImported(
            bindPose: Matrix4x3f,
            inverseBindPose: Matrix4x3f,
            skinning: Matrix4x3f,
            parentSkinning: Matrix4x3f?,
            tmp: Matrix4x3f,
            dstPos: Vector3f, dstRot: Quaternionf, dstSca: Vector3f,
        ) {
            skinning.mul(bindPose, tmp) // position in model
            if (parentSkinning != null) {
                // (parent * bindPose)^-1 * dst
                predict(parentSkinning, bindPose, JomlPools.mat4x3f.borrow())
                    .invert().mul(tmp, tmp)
            }

            // make that the rotation is always in world space
            //  by rotating the base of the rotation into world space
            // todo there probably is a way to transform this without awkward maths
            val pos = tmp.getTranslation(dstPos)
            val rot = tmp.getUnnormalizedRotation(dstRot)
            val sca = tmp.getScale(dstSca)
            transformWeirdly(bindPose, pos, rot, sca)
        }

        fun transformWeirdly(bindPose: Matrix4x3f, pos: Vector3f, rot: Quaternionf, sca: Vector3f) {
            bindPose.transformDirection(pos)
            bindPose.transformRotation(rot)
            // bindPose.transformDirection(sca)
        }

        fun fromImported(
            bindPose: Matrix4x3f,
            inverseBindPose: Matrix4x3f,
            skinning: Matrix4x3f,
            parentSkinning: Matrix4x3f?,
            tmpV: Vector3f, tmpQ: Quaternionf, tmpS: Vector3f,
            dst: Matrix4x3f
        ): Matrix4x3f {
            fromImported(bindPose, inverseBindPose, skinning, parentSkinning, tmpV, tmpQ, tmpS, dst)
            dst.translationRotateScale(tmpV, tmpQ, tmpS)
            return dst
        }

        /**
         * warn: invalidates pos, rot and sca!
         * */
        fun toImported(
            bindPose: Matrix4x3f,
            inverseBindPose: Matrix4x3f,
            parentSkinning: Matrix4x3f?,
            pos: Vector3f, rot: Quaternionf, sca: Vector3f,
            dst: Matrix4x3f
        ): Matrix4x3f {
            // println("      input: $pos,$rot,$sca")
            transformWeirdly(inverseBindPose, pos, rot, sca)
            // println("      output: $pos,$rot,$sca")
            dst.translationRotateScale(pos, rot, sca)
            bindPose.mul(dst, dst)
            @Suppress("IfThenToSafeAccess")
            if (parentSkinning != null) {
                // dst = (parent * bindPose) * dst
                parentSkinning.mul(dst, dst)
                // = predict(parentSkinning, bone.bindPose, JomlPools.mat4x3f.borrow()).mul(dst, dst)
            }
            return dst.mul(inverseBindPose)
        }

        /**
         * warn: invalidates pos, rot and sca!
         * */
        fun toImported(
            bone: Bone,
            parentSkinning: Matrix4x3f?,
            pos: Vector3f, rot: Quaternionf, sca: Vector3f,
            dst: Matrix4x3f
        ): Matrix4x3f {
            return toImported(
                bone.bindPose, bone.inverseBindPose,
                parentSkinning, pos, rot, sca, dst
            )
        }

        /**
         * rotate the main axis of a quaternion;
         * I don't know whether anyone uses this, whether this is a legit operation;
         * I think I need it here, and it looks correct to me
         * */
        fun Matrix4x3f.transformRotation(q: Quaternionf) {
            val v = JomlPools.vec3f.borrow()
            transformDirection(v.set(q.x, q.y, q.z))
            q.set(v.x, v.y, v.z, q.w)
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
    }
}