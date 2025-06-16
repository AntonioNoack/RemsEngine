package me.anno.gpu.buffer

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable

data class Attribute(
    /**
     * name for use in GLSL
     * */
    var name: String,
    /**
     * what number type it uses
     * */
    var type: AttributeType,
    /**
     * how many components there are, 1 = scalar, more = vector
     * */
    var numComponents: Int
) : Saveable() {

    constructor() : this("", 0)
    constructor(name: String, components: Int) : this(name, AttributeType.FLOAT, components)

    val byteSize get() = numComponents * type.byteSize
    val alignment get() = (if (numComponents == 3) 4 else numComponents) * type.byteSize

    override fun toString(): String {
        return "Attribute($name,$type,$numComponents)"
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("name", name)
        writer.writeEnum("type", type)
        writer.writeInt("components", numComponents)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "name" -> this.name = value as? String ?: return
            "type" -> type = AttributeType.entries.firstOrNull { it.id == value } ?: return
            "components" -> numComponents = value as? Int ?: return
            else -> super.setProperty(name, value)
        }
    }

    fun withName(name: String): Attribute {
        if (name == this.name) return this
        return Attribute(name, type, numComponents)
    }

    override val approxSize: Int get() = 100
}

