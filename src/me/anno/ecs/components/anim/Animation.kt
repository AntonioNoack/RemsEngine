package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.cache.ICacheData
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.fract
import me.anno.utils.pooling.Pools
import me.anno.utils.types.AnyToFloat
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import kotlin.math.max

/**
 * skeletal animation base class
 * */
abstract class Animation : PrefabSaveable, Renderable, ICacheData {
    companion object {
        private val LOGGER = LogManager.getLogger(Animation::class)

        data class FrameIndex(val fraction: Float, val index0: Int, val index1: Int)

        fun calculateMonotonousTime(frameIndex: Float, frameCount: Int): FrameIndex {
            val frameCount = max(frameCount, 1)

            val timeF = fract(frameIndex / frameCount) * frameCount

            val index0 = timeF.toInt() % frameCount
            val index1 = (index0 + 1) % frameCount

            val fraction = fract(timeF)
            return FrameIndex(fraction, index0, index1)
        }
    }

    constructor() : super()

    constructor(name: String, duration: Float) : super() {
        this.name = name
        this.duration = duration
    }

    @SerializedProperty
    var duration = 1f

    @Type("Skeleton/Reference")
    @SerializedProperty
    var skeleton: FileReference = InvalidRef

    @DebugProperty
    abstract val numFrames: Int

    abstract fun getMatrices(frameIndex: Float, dst: List<Matrix4x3f>): List<Matrix4x3f>?
    abstract fun getMatrices(frameIndex: Int, dst: List<Matrix4x3f>): List<Matrix4x3f>?

    abstract fun getMatrix(frameIndex: Float, boneId: Int, dst: List<Matrix4x3f>): Matrix4x3f?
    abstract fun getMatrix(frameIndex: Int, boneId: Int, dst: List<Matrix4x3f>): Matrix4x3f?

    fun getMappedAnimation(dstSkeleton: FileReference): Animation? {
        if (dstSkeleton == skeleton) return this
        val dstSkel = SkeletonCache[dstSkeleton]
        if (dstSkel == null) {
            LOGGER.warn("Missing Skeleton $dstSkeleton for retargeting")
            return null
        }
        return AnimationCache.getMappedAnimation(this, dstSkel)
    }

    fun getMappedMatrices(
        frameIndex: Float,
        dst: List<Matrix4x3f>,
        dstSkeleton: FileReference
    ): List<Matrix4x3f>? {
        return getMappedAnimation(dstSkeleton)
            ?.getMatrices(frameIndex, dst)
    }

    fun getMappedMatrix(
        frameIndex: Float,
        boneId: Int,
        dst: List<Matrix4x3f>,
        dstSkeleton: FileReference
    ): Matrix4x3f? {
        return getMappedAnimation(dstSkeleton)
            ?.getMatrix(frameIndex, boneId, dst)
    }

    fun getMappedMatrices(
        frameIndex: Int,
        dst: List<Matrix4x3f>,
        dstSkeleton: FileReference
    ): List<Matrix4x3f>? {
        return getMappedAnimation(dstSkeleton)
            ?.getMatrices(frameIndex, dst)
    }

    fun getMappedMatricesSafely(
        frameIndex: Float,
        dst: List<Matrix4x3f>,
        dstSkeleton: FileReference
    ): List<Matrix4x3f> {
        val base = getMappedMatrices(frameIndex, dst, dstSkeleton)
        if (base != null) return base
        for (i in dst.indices) dst[i].identity()
        return dst
    }

    fun getMappedMatrixSafely(
        frameIndex: Float,
        boneId: Int,
        dst: List<Matrix4x3f>,
        dstSkeleton: FileReference,
    ): Matrix4x3f {
        return getMappedMatrix(frameIndex, boneId, dst, dstSkeleton) ?: dst[0].identity()
    }

    fun getMappedMatricesSafely(
        frameIndex: Int,
        dst: List<Matrix4x3f>,
        dstSkeleton: FileReference
    ): List<Matrix4x3f> {
        val base = getMappedMatrices(frameIndex, dst, dstSkeleton)
        if (base != null) return base
        for (i in dst.indices) dst[i].identity()
        return dst
    }

    class PreviewData(skeleton: Skeleton, animation: Animation) {

        val bones = skeleton.bones
        val mesh = Mesh()
        val renderer = AnimMeshComponent()
        val state = AnimationState(animation.ref, 1f, 0f, 1f, LoopingState.PLAY_LOOP)

        init {
            val size = (bones.size - 1) * Skeleton.boneMeshVertices.size
            mesh.positions = Pools.floatArrayPool[size, false, true]
            mesh.normals = Pools.floatArrayPool[size, true, true]
            mesh.boneIndices = Pools.byteArrayPool[size * 4 / 3, true, true]
            mesh.skeleton = skeleton.ref
            Skeleton.generateSkeleton(
                bones, bones.map { it.bindPosition },
                mesh.positions!!, mesh.boneIndices!!
            )
            renderer.meshFile = mesh.ref
            renderer.animations = listOf(state)
        }

        fun destroy() {
            mesh.positions = Pools.floatArrayPool.returnBuffer(mesh.positions)
            mesh.normals = Pools.floatArrayPool.returnBuffer(mesh.normals)
            mesh.boneIndices = Pools.byteArrayPool.returnBuffer(mesh.boneIndices)
            mesh.destroy()
        }

        override fun toString(): String {
            return state.progress.toString()
        }
    }

    @DebugProperty
    private var previewData: PreviewData? = null

    override fun fill(pipeline: Pipeline, transform: Transform) {
        val skeleton = SkeletonCache[skeleton] ?: return
        if (previewData == null) previewData = PreviewData(skeleton, this)
        return previewData!!.run {
            if (renderer.prevTime != Time.gameTimeN) {
                state.update(renderer, Time.deltaTime.toFloat(), false)
                renderer.updateAnimState()
            }
            renderer.fill(pipeline, transform)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("duration", duration)
        writer.writeFile("skeleton", skeleton)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "duration" -> duration = AnyToFloat.getFloat(value, 0f)
            "skeleton" -> skeleton = value as? FileReference ?: InvalidRef
            else -> super.setProperty(name, value)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Animation) return
        dst.skeleton = skeleton
        dst.duration = duration
    }

    override fun destroy() {
        previewData?.destroy()
        previewData = null
    }

    override val approxSize get() = 100
}