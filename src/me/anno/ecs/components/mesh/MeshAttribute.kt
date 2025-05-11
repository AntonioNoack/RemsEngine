package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.Docs
import me.anno.gpu.buffer.Attribute
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable

@Docs("Represents a per-vertex attribute in Mesh. Data should be a native array for easy serialization.")
class MeshAttribute(var attribute: Attribute, var data: Any?) : Saveable() {

    constructor() : this(Attribute("", 0), null)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "attribute", attribute)
        writer.writeSomething(this, "data", data, true)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "attribute" -> attribute = value as? Attribute ?: return
            "data" -> data = value
            else -> super.setProperty(name, value)
        }
    }

    fun copyOf(): MeshAttribute {
        return MeshAttribute(attribute, copyOf(data))
    }

    override val approxSize: Int get() = 200

    companion object {
        fun copyOf(data: Any?): Any? {
            return when (data) {
                is BooleanArray -> data.copyOf()
                is ByteArray -> data.copyOf()
                is ShortArray -> data.copyOf()
                is CharArray -> data.copyOf()
                is IntArray -> data.copyOf()
                is LongArray -> data.copyOf()
                is FloatArray -> data.copyOf()
                is DoubleArray -> data.copyOf()
                // todo support buffers?
                else -> data
            }
        }
    }
}