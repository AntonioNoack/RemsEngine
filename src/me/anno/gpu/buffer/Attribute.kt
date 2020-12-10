package me.anno.gpu.buffer

import me.anno.utils.StringHelper.addPrefix

class Attribute(val name: String, val type: AttributeType, val components: Int, val isNativeInt: Boolean = false) {

    constructor(name: String, components: Int) : this(name, AttributeType.FLOAT, components, false)

    val glslType get() = types[if (isNativeInt) 4 + components else components]
    val byteSize = components * type.byteSize
    var offset = 0L

    fun withName(name: String) = Attribute(name, type, components, isNativeInt)

    companion object {

        val types = Array(8) {
            val isInt = it >= 4
            val sub = it and 3
            addPrefix(if (isInt) "i" else null, if (sub == 1) "float" else "vec$sub")
        }

    }

}

