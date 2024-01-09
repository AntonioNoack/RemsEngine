package me.anno.gpu.drawing

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader

abstract class Batch(name: String, val base: StaticBuffer, attributes: List<Attribute>, val batchSize: Int = 65536) {

    var active = false
        private set

    private var shader: Shader? = null

    private var batchCount = 0
    private val buffer by lazy {
        StaticBuffer(name, attributes, batchSize, BufferUsage.DYNAMIC).apply {
            createNioBuffer()
            nioBuffer!!.position(batchSize * stride)
            ensureBuffer() // maximum size :)
        }
    }

    val attributes get() = buffer.attributes

    val data get() = buffer.nioBuffer!!

    fun next() {
        if (++batchCount >= batchSize) {
            draw()
        }
    }

    fun start(): Int {
        if (active) return 1
        active = true
        return 0
    }

    private fun draw() {
        val shader = shader ?: bindShader()
        draw(shader)
    }

    private fun draw(shader: Shader) {
        buffer.ensureBufferWithoutResize()
        base.drawInstanced(shader, buffer)
        buffer.clear()
        batchCount = 0
    }

    fun finish(v: Int) {
        if (v == 0) {
            if (batchCount > 0) {
                draw()
            }
            active = false
            shader = null
        }
    }

    abstract fun bindShader(): Shader

}