package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import org.joml.Matrix4x3f

// todo blend animations...
// todo allow procedural animations; for that we'd need more knowledge about the model...
abstract class Animation : NamedSaveable() {

    // todo an animation should have a skeleton, so we can compare, whether we need to map the animations

    var duration = 1.0

    var skeleton: Skeleton? = null

    abstract fun getMatrices(entity: Entity, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>?

    fun getMappedMatrices(
        entity: Entity,
        time: Float,
        dst: Array<Matrix4x3f>,
        retargeting: Retargeting
    ): Array<Matrix4x3f>? {
        val base = getMatrices(entity, time, dst) ?: return null
        if (retargeting.isIdentityMapping) return base
        if (retargeting.srcSkeleton != skeleton) throw RuntimeException("Incompatible skeletons!")
        retargeting.validate()
        for (it in retargeting.dstSkeleton!!.bones!!.indices) {
            val bm = base.getOrNull(retargeting.mapping[it])
            if (bm != null) dst[it].set(bm)
            else dst[it].identity()
        }
        return dst
    }

    fun getMappedMatricesSafely(
        entity: Entity,
        time: Float,
        dst: Array<Matrix4x3f>,
        retargeting: Retargeting
    ): Array<Matrix4x3f> {
        val base = getMappedMatrices(entity, time, dst, retargeting)
        if (base == null) {
            for (i in retargeting.dstSkeleton!!.bones!!.indices) {
                dst[i].identity()
            }
        }
        return dst
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("duration", duration)
        writer.writeObject(this, "skeleton", skeleton)
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "duration" -> duration = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "skeleton" -> skeleton = value as? Skeleton
            else -> super.readObject(name, value)
        }
    }

    override val approxSize: Int = 100

}