package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.min
import org.joml.Matrix4x3f

// done blend animations...
// todo allow procedural animations; for that we'd need more knowledge about the model
abstract class Animation : PrefabSaveable {

    constructor() : super()

    constructor(name: String, duration: Float) : super() {
        this.name = name
        this.duration = duration
    }

    final override var name: String = ""

    var duration = 1f

    var skeleton: FileReference = InvalidRef

    abstract val numFrames: Int

    fun calculateMonotonousTime(time: Float, frameCount: Int): Triple<Float, Int, Int> {
        val duration = duration
        val timeF = fract(time / duration) * frameCount

        val index0 = timeF.toInt() % frameCount
        val index1 = (index0 + 1) % frameCount

        val fraction = fract(timeF).toFloat()

        return Triple(fraction, index0, index1)
    }

    abstract fun getMatrices(entity: Entity?, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>?
    abstract fun getMatrices(index: Int, dst: Array<Matrix4x3f>): Array<Matrix4x3f>?

    fun getMappedMatrices(
        entity: Entity?,
        time: Float,
        dst: Array<Matrix4x3f>,
        retargeting: Retargeting?
    ): Array<Matrix4x3f>? {
        val base = getMatrices(entity, time, dst) ?: return null
        if (retargeting == null || retargeting.isIdentityMapping) return base
        if (retargeting.srcSkeleton != skeleton) throw RuntimeException("Incompatible skeletons!")
        return getMappedMatrices(base, dst, SkeletonCache[retargeting.dstSkeleton]!!, retargeting)
    }

    fun getMappedMatrices(
        frameIndex: Int,
        dst: Array<Matrix4x3f>,
        dstSkeleton: Skeleton,
        retargeting: Retargeting?
    ): Array<Matrix4x3f>? {
        val base = getMatrices(frameIndex, dst) ?: return null
        if (retargeting == null || retargeting.isIdentityMapping) return base
        if (retargeting.srcSkeleton != skeleton) throw RuntimeException("Incompatible skeletons!")
        return getMappedMatrices(base, dst, dstSkeleton, retargeting)
    }

    private fun getMappedMatrices(
        srcMatrices: Array<Matrix4x3f>,
        dstMatrices: Array<Matrix4x3f>,
        dstSkeleton: Skeleton,
        retargeting: Retargeting
    ): Array<Matrix4x3f> {
        retargeting.validate()
        val dstToSrc = retargeting.dstToSrc
        val dstToSrcM = retargeting.dstToSrcM
        val srcToDstM = retargeting.srcToDstM
        val dstSize = min(dstMatrices.size, dstSkeleton.bones.size)
        val dstMapSize = dstToSrc.size
        for (i in 0 until dstMapSize) {
            val src = srcMatrices.getOrNull(dstToSrc[i])
            val dst = dstMatrices[i]
            if (src != null) {
                dst.set(srcToDstM[i])
                dst.mul(src)
                dst.mul(dstToSrcM[i])
            } else dst.identity()
        }
        for (i in dstMapSize until dstSize) {
            dstMatrices[i].identity()
        }
        return dstMatrices
    }

    fun getMappedMatricesSafely(
        entity: Entity?,
        time: Float,
        dst: Array<Matrix4x3f>,
        dstSkeleton: Skeleton,
        retargeting: Retargeting?
    ): Array<Matrix4x3f> {
        val base = getMappedMatrices(entity, time, dst, retargeting)
        return if (base == null) {
            for (i in dstSkeleton.bones.indices) {
                dst[i].identity()
            }
            dst
        } else base
    }

    fun getMappedMatricesSafely(
        frameIndex: Int,
        dst: Array<Matrix4x3f>,
        dstSkeleton: Skeleton,
        retargeting: Retargeting?
    ): Array<Matrix4x3f> {
        val base = getMappedMatrices(frameIndex, dst, dstSkeleton, retargeting)
        return if (base == null) {
            for (i in dstSkeleton.bones.indices) {
                dst[i].identity()
            }
            dst
        } else base
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("duration", duration)
        writer.writeFile("skeleton", skeleton)
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "duration" -> duration = value.toFloat()
            else -> super.readDouble(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "duration" -> duration = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "skeleton" -> skeleton = value
            else -> super.readFile(name, value)
        }
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Animation
        clone.skeleton = skeleton
        clone.duration = duration
    }

    override val approxSize: Int = 100

}