package me.anno.gpu.buffer

import me.anno.utils.types.Strings.addPrefix

class Attribute(val name: String, val type: AttributeType, val components: Int, val isNativeInt: Boolean = false) {

    constructor(name: String, components: Int) : this(name, AttributeType.FLOAT, components, false)

    val glslType get() = types[if (isNativeInt) 4 + components else components]
    val byteSize = components * type.byteSize
    var offset = 0L
    var stride = 0

    fun withName(name: String) = Attribute(name, type, components, isNativeInt)

    override fun equals(other: Any?): Boolean {
        return other is Attribute && other.name == name &&
                other.type == type && other.components == components &&
                other.isNativeInt == isNativeInt &&
                other.offset == offset
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + components
        result = 31 * result + isNativeInt.hashCode()
        result = 31 * result + byteSize
        result = 31 * result + offset.hashCode()
        return result
    }

    companion object {

        val types = Array(8) {
            val isInt = it >= 4
            val sub = it and 3
            addPrefix(if (isInt) "i" else null, if (sub == 1) "float" else "vec$sub")
        }

    }

}

