package me.anno.gpu.buffer

import me.anno.gpu.Buffer
import org.lwjgl.opengl.GL11

class Attribute(val name: String, val type: AttributeType, val components: Int){
    constructor(name: String, components: Int): this(name, AttributeType.FLOAT, components)
    val byteSize = components * type.byteSize
    var offset = 0L
}

