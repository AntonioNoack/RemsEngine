package me.anno.gpu.buffer

class Attribute(val name: String, val type: AttributeType, val components: Int){
    constructor(name: String, components: Int): this(name, AttributeType.FLOAT, components)
    val byteSize = components * type.byteSize
    var offset = 0L
}

