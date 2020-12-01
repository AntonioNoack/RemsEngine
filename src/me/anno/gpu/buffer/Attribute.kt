package me.anno.gpu.buffer

class Attribute(val name: String, val type: AttributeType, val components: Int, val isNativeInt: Boolean = false){
    constructor(name: String, components: Int): this(name, AttributeType.FLOAT, components, false)
    val byteSize = components * type.byteSize
    var offset = 0L
}

