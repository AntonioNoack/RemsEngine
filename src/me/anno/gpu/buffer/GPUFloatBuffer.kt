package me.anno.gpu.buffer

import me.anno.gpu.Buffer
import java.nio.FloatBuffer

abstract class GPUFloatBuffer(attributes: List<Attribute>): Buffer(attributes){
    lateinit var floatBuffer: FloatBuffer
}