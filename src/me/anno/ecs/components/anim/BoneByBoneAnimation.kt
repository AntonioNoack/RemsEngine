package me.anno.ecs.components.anim

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Arrays.resize
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f

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
        writer.writeFloatArray("translations", translations ?: f0)
        writer.writeFloatArray("rotations", rotations ?: f0)
        writer.writeFloatArray("scales", scales ?: f0)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "boneCount" -> boneCount = value as? Int ?: return
            "frameCount" -> frameCount = value as? Int ?: return
            "globalTransform" -> globalTransform.set(value as? Matrix4x3f ?: return)
            "globalInvTransform" -> globalInvTransform.set(value as? Matrix4x3f ?: return)
            "translations" -> translations = value as? FloatArray
            "rotations" -> rotations = value as? FloatArray
            "scales" -> scales = value as? FloatArray
            else -> super.setProperty(name, value)
        }
    }

    override fun getMatrices(frameIndex: Float, dst: List<Matrix4x3f>): List<Matrix4x3f>? {
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

    override fun getMatrix(frameIndex: Float, boneId: Int, dst: List<Matrix4x3f>): Matrix4x3f? {
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

    override fun getMatrices(frameIndex: Int, dst: List<Matrix4x3f>): List<Matrix4x3f>? {
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
            toImported(bone, dst.getOrNull(bone.parentId), tmpPos, tmpRot, tmpSca, dst[boneId])
        }
        JomlPools.vec3f.sub(2)
        return dst
    }

    override fun getMatrix(frameIndex: Int, boneId: Int, dst: List<Matrix4x3f>): Matrix4x3f? {
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

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is BoneByBoneAnimation) return
        dst.boneCount = boneCount
        dst.frameCount = frameCount
        dst.translations = translations
        dst.rotations = rotations
        dst.scales = scales
        dst.globalTransform.set(globalTransform)
        dst.globalInvTransform.set(globalInvTransform)
    }

    constructor(anim: ImportedAnimation, frameList: List<Int>? = null) : this() {
        fromImported(anim, frameList)
    }

    fun fromImported(src: ImportedAnimation, frameList: List<Int>?): BoneByBoneAnimation {
        skeleton = src.skeleton
        val skeleton = SkeletonCache[skeleton]!!
        boneCount = skeleton.bones.size
        frameCount = frameList?.size ?: src.frames.size
        initFloatArrays()
        val frames = src.frames
        val bones = skeleton.bones
        checkBoneOrder(bones)
        for (i in 0 until numFrames) {
            val frameIndex = frameList?.get(i) ?: i
            setFrameByImported(bones, i, frames[frameIndex])
        }
        return this
    }

    private fun initFloatArrays() {
        val size = boneCount * frameCount
        translations = FloatArray(size * 3)
        rotations = FloatArray(size * 4)
        scales = FloatArray(size * 3)
    }

    fun checkBoneOrder(bones: List<Bone>) {
        for (j in bones.indices) {
            val bone = bones[j]
            val pj = bone.parentId
            if (pj >= j) LOGGER.warn("Bones out of order, $j -> $pj")
        }
    }

    fun setFrameByImported(bones: List<Bone>, dstFrameIndex: Int, frameByImported: List<Matrix4x3f>) {
        for (j in bones.indices) {
            setFrameForBoneByImported(bones[j], dstFrameIndex, frameByImported)
        }
    }

    fun setFrameForBoneByImported(bone: Bone, frameId: Int, frameByImported: List<Matrix4x3f>) {
        val pos = JomlPools.vec3f.create()
        val sca = JomlPools.vec3f.create()
        val rot = JomlPools.quat4f.create()
        val tmpM = JomlPools.mat4x3f.create()
        // calculate rotations
        val boneId = bone.id
        val pose = frameByImported[boneId] // bind pose [world space] -> animated pose [world space]
        val parentPose = frameByImported.getOrNull(bone.parentId)
        fromImported(
            bone.bindPose, bone.inverseBindPose,
            pose, parentPose, tmpM, pos, rot, sca
        )
        setTranslation(frameId, boneId, pos)
        setRotation(frameId, boneId, rot)
        setScale(frameId, boneId, sca)
        JomlPools.vec3f.sub(2)
        JomlPools.quat4f.sub(1)
        JomlPools.mat4x3f.sub(1)
    }

    @Suppress("unused")
    fun toImported(dst: ImportedAnimation = ImportedAnimation()): ImportedAnimation {
        dst.skeleton = skeleton
        val skel = SkeletonCache[skeleton]!!
        val tmpPos = JomlPools.vec3f.create()
        val tmpSca = JomlPools.vec3f.create()
        val tmpRot = JomlPools.quat4f.create()
        val bones = skel.bones
        dst.frames = createArrayList(frameCount) { i ->
            val matrices = createArrayList(bones.size) { Matrix4x3f() }
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
        JomlPools.quat4f.sub(1)
        return dst
    }

    companion object {

        private val f0 = FloatArray(0)
        private val LOGGER = LogManager.getLogger(BoneByBoneAnimation::class)

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
            transformWeirdly(inverseBindPose, pos, rot, sca)
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
    }
}