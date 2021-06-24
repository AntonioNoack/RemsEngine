package me.anno.gpu

import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.utils.structures.SecureStack
import org.lwjgl.opengl.GL11.*

object RenderSettings {

    // der Renderer wird nicht global sondern pro Framebuffer gesetzt; macht zumindest den meisten Sinn
    // val renderer = SecureStack(Renderer.colorRenderer)

    val blendMode = object : SecureStack<BlendMode?>(BlendMode.DEFAULT) {
        override fun onChangeValue(v: BlendMode?) {
            if (v == null) {
                glDisable(GL_BLEND)
            } else {
                // todo if is parent, then use the parent value
                glEnable(GL_BLEND)
                v.forceApply()
            }
        }
    }

    val depthMode = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(v: Boolean) {
            if (v) {
                glEnable(GL_DEPTH_TEST)
            } else {
                glDisable(GL_DEPTH_TEST)
            }
        }
    }

    inline fun renderPurely(render: () -> Unit){
        blendMode.use(null){
            depthMode.use(false, render)
        }
    }

    inline fun renderDefault(render: () -> Unit){
        blendMode.use(BlendMode.DEFAULT){
            depthMode.use(false, render)
        }
    }

    private const val maxSize = 512
    val renderers = Array(maxSize) { colorRenderer }

    val currentRenderer get() = renderers[framebuffer.size - 1]
    val currentBuffer get() = framebuffer.values[framebuffer.size - 1]

    val xs = IntArray(maxSize)
    val ys = IntArray(maxSize)
    val ws = IntArray(maxSize)
    val hs = IntArray(maxSize)
    val changeSizes = BooleanArray(maxSize)

    val framebuffer = object : SecureStack<Framebuffer?>(null) {

        override fun onChangeValue(v: Framebuffer?) {
            val index = size - 1
            Frame.bind(null, v, changeSizes[index], xs[index], ys[index], ws[index], hs[index])
        }

    }

    inline fun useFrame(
        buffer: Framebuffer?,
        renderer: Renderer,
        render: () -> Unit
    ) {
        val index = framebuffer.size
        xs[index] = xs[index - 1]
        ys[index] = ys[index - 1]
        // todo is this correct?
        ws[index] = buffer?.w ?: GFX.width
        hs[index] = buffer?.h ?: GFX.height
        changeSizes[index] = false
        renderers[index] = renderer
        framebuffer.use(currentBuffer, render)
    }

    inline fun useFrame(
        renderer: Renderer,
        render: () -> Unit
    ) { useFrame(currentBuffer, renderer, render) }

    inline fun useFrame(
        buffer: Framebuffer?,
        render: () -> Unit
    ) { useFrame(buffer, currentRenderer, render) }

    inline fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        changeSize: Boolean,
        render: () -> Unit
    ) {
        val index = framebuffer.size
        xs[index] = x
        ys[index] = y
        ws[index] = w
        hs[index] = h
        changeSizes[index] = changeSize
        renderers[index] = currentRenderer
        framebuffer.use(currentBuffer, render)
    }

    inline fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        changeSize: Boolean,
        buffer: Framebuffer?,
        render: () -> Unit
    ) {
        val index = framebuffer.size
        xs[index] = x
        ys[index] = y
        ws[index] = w
        hs[index] = h
        changeSizes[index] = changeSize
        renderers[index] = currentRenderer
        framebuffer.use(buffer, render)
    }

    inline fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        changeSize: Boolean,
        buffer: Framebuffer?,
        renderer: Renderer,
        render: () -> Unit
    ) {
        val index = framebuffer.size
        xs[index] = x
        ys[index] = y
        ws[index] = w
        hs[index] = h
        changeSizes[index] = changeSize
        renderers[index] = renderer
        framebuffer.use(buffer, render)
    }

}