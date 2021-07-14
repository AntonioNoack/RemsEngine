package me.anno.ecs.components.anim

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter

class Retargeting : NamedSaveable() {

    var srcSkeleton: Skeleton? = null
    var dstSkeleton: Skeleton? = null

    val src get() = srcSkeleton?.bones?.map { it.name }
    var dst = emptyList<String>()

    var isValid = false

    val isIdentityMapping get() = srcSkeleton == dstSkeleton

    // mapping[dstBoneId] is srcBoneId or -1
    var mapping = IntArray(0)

    fun validate() {
        if (isValid) return
        // calculate all the indices
        val src = src!!
        val srcMap = src.withIndex().associate { it.value to it.index }
        if (mapping.size == src.size) {
            for (it in src.indices) {
                mapping[it] = srcMap[dst.getOrNull(it)] ?: -1
            }
        } else {
            mapping = IntArray(src.size) { srcMap[dst.getOrNull(it)] ?: -1 }
        }
        isValid = true
    }

    fun invalidate() {
        isValid = false
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "srcSkeleton", srcSkeleton)
        writer.writeObject(this, "dstSkeleton", dstSkeleton)
        writer.writeStringArray("dstNames", dst.toTypedArray())
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "srcSkeleton" -> {
                srcSkeleton = value as? Skeleton; invalidate()
            }
            "dstSkeleton" -> {
                dstSkeleton = value as? Skeleton; invalidate()
            }
            else -> super.readObject(name, value)
        }
    }

    override fun readStringArray(name: String, values: Array<String>) {
        when (name) {
            "dstNames" -> {
                dst = values.toList()
                invalidate()
            }
            else -> super.readStringArray(name, values)
        }
    }

    override val className: String = "Retargeting"
    override val approxSize: Int = 20

}