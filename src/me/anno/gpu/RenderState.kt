package me.anno.gpu

import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.GeoShader
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.utils.structures.SecureStack
import org.lwjgl.opengl.GL20.GL_LOWER_LEFT
import org.lwjgl.opengl.GL45.*

object RenderState {

    // the renderer is set per framebuffer; makes the most sense
    // additionally, it would be possible to set the blend-mode and depth there in a game setting
    // (not that practical in RemsStudio)
    // val renderer = SecureStack(Renderer.colorRenderer)

    val blendMode = object : SecureStack<BlendMode?>(BlendMode.DEFAULT) {
        // could be optimized
        override fun onChangeValue(newValue: BlendMode?, oldValue: BlendMode?) {
            // LOGGER.info("Blending: $newValue <- $oldValue")
            if (newValue == null) {
                glDisable(GL_BLEND)
            } else {
                // todo if is parent, then use the parent value
                glEnable(GL_BLEND)
                var self: BlendMode? = newValue
                var index = index
                if (self == BlendMode.INHERIT) {
                    while (self == BlendMode.INHERIT) {
                        self = values[index--]
                    }
                    onChangeValue(self, oldValue)
                } else {
                    self?.forceApply()
                }
            }
        }
    }

    val depthMode = object : SecureStack<DepthMode>(DepthMode.ALWAYS) {
        override fun onChangeValue(newValue: DepthMode, oldValue: DepthMode) {
            if (newValue != DepthMode.ALWAYS) {
                glEnable(GL_DEPTH_TEST)
                glDepthFunc(newValue.func)
                val reversedDepth = newValue.reversedDepth
                glClearDepth(if (reversedDepth) 0.0 else 1.0)
                glClipControl(GL_LOWER_LEFT, if (reversedDepth) GL_ZERO_TO_ONE else GL_NEGATIVE_ONE_TO_ONE)
                // glDepthRange(-1.0, 1.0)
                // glDepthFunc(GL_LESS)
            } else {
                glDisable(GL_DEPTH_TEST)
            }
        }
    }

    val depthMask = object : SecureStack<Boolean>(true) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            glDepthMask(newValue)
        }
    }

    val geometryShader = SecureStack<GeoShader?>(null)

    val instanced = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            // nothing changes on the OpenGL side,
            // just the shaders need to be modified
        }
    }

    val cullMode = object : SecureStack<Int>(0) {
        override fun onChangeValue(newValue: Int, oldValue: Int) {
            if (newValue != 0) {
                glEnable(GL_CULL_FACE)
                glCullFace(newValue)
            } else {
                glDisable(GL_CULL_FACE)
            }
        }
    }

    val scissorTest = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            if (newValue) glEnable(GL_SCISSOR_TEST)
            else glDisable(GL_SCISSOR_TEST)
        }
    }

    inline fun renderPurely(render: () -> Unit) {
        blendMode.use(null) {
            depthMode.use(DepthMode.ALWAYS, render)
        }
    }

    inline fun renderDefault(render: () -> Unit) {
        blendMode.use(BlendMode.DEFAULT) {
            depthMode.use(DepthMode.ALWAYS, render)
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
        override fun onChangeValue(newValue: Framebuffer?, oldValue: Framebuffer?) {
            val index = size - 1
            Frame.bind(newValue, changeSizes[index], xs[index], ys[index], ws[index], hs[index])
        }
    }

    inline fun useFrame(
        buffer: Framebuffer?,
        renderer: Renderer,
        render: () -> Unit
    ) {
        val index = framebuffer.size
        xs[index] = 0//xs[index - 1]
        ys[index] = 0//ys[index - 1]
        ws[index] = -1
        hs[index] = -1
        changeSizes[index] = false
        renderers[index] = renderer
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

    inline fun useFrame(renderer: Renderer, render: () -> Unit) =
        useFrame(currentBuffer, renderer, render)

    inline fun useFrame(buffer: Framebuffer?, render: () -> Unit) =
        useFrame(buffer, currentRenderer, render)

    inline fun useFrame(w: Int, h: Int, changeSize: Boolean, buffer: Framebuffer?, render: () -> Unit) =
        useFrame(0, 0, w, h, changeSize, buffer, currentRenderer, render)

    inline fun useFrame(
        w: Int,
        h: Int,
        changeSize: Boolean,
        buffer: Framebuffer?,
        renderer: Renderer,
        render: () -> Unit
    ) = useFrame(0, 0, w, h, changeSize, buffer, renderer, render)

    inline fun useFrame(x: Int, y: Int, w: Int, h: Int, changeSize: Boolean, render: () -> Unit) =
        useFrame(x, y, w, h, changeSize, currentBuffer, currentRenderer, render)

    inline fun useFrame(x: Int, y: Int, w: Int, h: Int, changeSize: Boolean, buffer: Framebuffer?, render: () -> Unit) =
        useFrame(x, y, w, h, changeSize, buffer, currentRenderer, render)

}