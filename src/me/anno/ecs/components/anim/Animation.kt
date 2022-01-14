package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import org.joml.Matrix4x3f

// todo blend animations...
// todo allow procedural animations; for that we'd need more knowledge about the model...
abstract class Animation : NamedSaveable {

    constructor() : super()

    constructor(name: String, duration: Double) : super() {
        this.name = name
        this.duration = duration
    }

    final override var name: String = ""

    var duration = 1.0

    var skeleton: FileReference = InvalidRef

    fun calculateMonotonousTime(time: Float, frameCount: Int): Triple<Float, Int, Int> {
        var timeF = (time % duration) / duration * frameCount
        if (timeF < 0f) timeF += frameCount

        val index0 = timeF.toInt() % frameCount
        val index1 = (index0 + 1) % frameCount

        val fraction = Maths.fract(timeF).toFloat()

        return Triple(fraction, index0, index1)
    }

    abstract fun getMatrices(entity: Entity?, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>?

    fun getMappedMatrices(
        entity: Entity?,
        time: Float,
        dst: Array<Matrix4x3f>,
        retargeting: Retargeting
    ): Array<Matrix4x3f>? {
        val base = getMatrices(entity, time, dst) ?: return null
        if (retargeting.isIdentityMapping) return base
        if (retargeting.srcSkeleton != skeleton) throw RuntimeException("Incompatible skeletons!")
        retargeting.validate()
        for (it in SkeletonCache[retargeting.dstSkeleton]!!.bones.indices) {
            val bm = base.getOrNull(retargeting.mapping[it])
            if (bm != null) dst[it].set(bm)
            else dst[it].identity()
        }
        return dst
    }

    fun getMappedMatricesSafely(
        entity: Entity?,
        time: Float,
        dst: Array<Matrix4x3f>,
        retargeting: Retargeting
    ): Array<Matrix4x3f> {
        val base = getMappedMatrices(entity, time, dst, retargeting)
        if (base == null) {
            for (i in SkeletonCache[retargeting.dstSkeleton]!!.bones.indices) {
                dst[i].identity()
            }
        }
        return dst
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("duration", duration)
        writer.writeFile("skeleton", skeleton)
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "duration" -> duration = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "skeleton" -> skeleton = value
            else -> super.readFile(name, value)
        }
    }

    override val approxSize: Int = 100

}