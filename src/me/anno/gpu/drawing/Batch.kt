package me.anno.gpu.drawing

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import org.lwjgl.opengl.GL20C

abstract class Batch(val base: StaticBuffer, attributes: List<Attribute>, val batchSize: Int = 65536) {

    var active = false
        private set

    private var shader: Shader? = null

    private var batchCount = 0
    private val buffer by lazy {
        StaticBuffer(
            attributes, batchSize, GL20C.GL_DYNAMIC_DRAW
        ).apply {
            createNioBuffer()
            nioBuffer!!.position(batchSize * stride)
            ensureBuffer() // maximum size :)
        }
    }

    val data get() = buffer.nioBuffer!!

    fun next() {
        if (++batchCount >= batchSize) {
            draw()
        }
    }

    fun start() {
        if (active) throw IllegalStateException("Already batching")
        active = true
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

    fun finish() {
        if (batchCount > 0) {
            draw()
        }
        active = false
        shader = null
    }

    abstract fun bindShader(): Shader

}