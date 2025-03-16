package me.anno.gpu.drawing

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader

/**
 * draws many shapes at once;
 * optimization, because drawCalls can be very expensive
 * */
abstract class Batch(name: String, val base: StaticBuffer, val attributes: List<Attribute>, val batchSize: Int = 65536) {

    var active = false
        private set

    private var shader: Shader? = null

    private var batchCount = 0
    private val buffer by lazy {
        StaticBuffer(name, attributes, batchSize, BufferUsage.STREAM)
    }

    val data by lazy {
        buffer.apply {
            getOrCreateNioBuffer().position(batchSize * stride)
            ensureBuffer() // maximum size :)
        }.getOrCreateNioBuffer()
    }

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

    fun finish(batch: Int) {
        if (batch != 0) return
        if (batchCount > 0) {
            draw()
        }
        active = false
        shader = null
    }

    abstract fun bindShader(): Shader
}