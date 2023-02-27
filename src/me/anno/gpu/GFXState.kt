package me.anno.gpu

import me.anno.cache.instances.VideoCache
import me.anno.fonts.FontManager.TextCache
import me.anno.gpu.GFX.supportsClipControl
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.image.ImageGPUCache
import me.anno.utils.structures.stacks.SecureStack
import org.lwjgl.opengl.GL20C.GL_LOWER_LEFT
import org.lwjgl.opengl.GL45C.*

/**
 * holds rendering-related state,
 * currently, with OpenGL, this must be changed from a single thread only!
 * all functions feature rendering-callbacks, so you can change settings without having to worry about the previously set state by your caller
 *
 * renamed from OpenGL to GFXState, because we might support Vulkan in the future
 * */
object GFXState {

    var session = 0
        private set

    /**
     * in OpenGL ES (e.g. on Android),
     * the context can be destroyed, when the app
     * is closed temporarily to save resources & energy
     * then all loaded memory has become invalid
     * */
    fun newSession() {
        session++
        GFX.gpuTasks.clear() // they all have become invalid
        OpenGLShader.invalidateBinding()
        Texture2D.invalidateBinding()
        OpenGLBuffer.invalidateBinding()
        // clear all caches, which contain gpu data
        FBStack.clear()
        TextCache.clear()
        VideoCache.clear()
        ImageGPUCache.clear()
    }

    // the renderer is set per framebuffer; makes the most sense
    // additionally, it would be possible to set the blend-mode and depth there in a game setting
    // (not that practical in RemsStudio)
    // val renderer = SecureStack(Renderer.colorRenderer)

    val blendMode = object : SecureStack<BlendMode?>(BlendMode.DEFAULT) {
        // could be optimized
        override fun onChangeValue(newValue: BlendMode?, oldValue: BlendMode?) {
            // LOGGER.info("Blending: $newValue <- $oldValue")
            GFX.check()
            if (newValue == null) {
                glDisable(GL_BLEND)
            } else {
                glEnable(GL_BLEND)
                // if is parent, then use the parent value
                if (newValue == BlendMode.INHERIT) {
                    var self: BlendMode? = newValue
                    var index = index
                    while (self == BlendMode.INHERIT) {
                        self = values[index--]
                    }
                    return onChangeValue(self, oldValue)
                }
                newValue.forceApply()
            }
        }
    }

    val depthMode = object : SecureStack<DepthMode>(DepthMode.ALWAYS) {
        override fun onChangeValue(newValue: DepthMode, oldValue: DepthMode) {
            GFX.check()
            if (newValue.func != 0) {
                glEnable(GL_DEPTH_TEST)
                glDepthFunc(newValue.func)
                val reversedDepth = newValue.reversedDepth
                if (supportsClipControl) {
                    glClipControl(GL_LOWER_LEFT, if (reversedDepth) GL_ZERO_TO_ONE else GL_NEGATIVE_ONE_TO_ONE)
                } else {
                    // does this work??
                    glDepthRange(0.0, 1.0)
                }
                // glDepthRange(-1.0, 1.0)
                // glDepthFunc(GL_LESS)
            } else {
                glDisable(GL_DEPTH_TEST)
            }
        }
    }

    val depthMask = object : SecureStack<Boolean>(true) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            GFX.check()
            glDepthMask(newValue)
        }
    }

    val instanced = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            // nothing changes on the OpenGL side,
            // just the shaders need to be modified
        }
    }

    /**
     * a flag for shaders whether their animated version (slower) is used
     * */
    val animated = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            // nothing changes on the OpenGL side,
            // just the shaders need to be modified
        }
    }

    /**
     * a flag for shaders whether their limited-transform version (faster for 10k+ instances) is used
     * */
    val limitedTransform = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            // nothing changes on the OpenGL side,
            // just the shaders need to be modified
        }
    }

    val cullMode = object : SecureStack<CullMode>(CullMode.BOTH) {
        override fun onChangeValue(newValue: CullMode, oldValue: CullMode) {
            GFX.check()
            if (newValue != CullMode.BOTH) {
                glEnable(GL_CULL_FACE)
                glCullFace(newValue.opengl)
            } else {
                glDisable(GL_CULL_FACE)
            }
        }
    }

    @Suppress("unused")
    val stencilTest = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            if (newValue) glEnable(GL_STENCIL_TEST)
            else glDisable(GL_STENCIL_TEST)
        }
    }

    val scissorTest = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            if (newValue) glEnable(GL_SCISSOR_TEST)
            else glDisable(GL_SCISSOR_TEST)
        }
    }

    /**
     * render without blending and without depth test
     * */
    inline fun renderPurely(render: () -> Unit) {
        blendMode.use(null) {
            depthMode.use(DepthMode.ALWAYS, render)
        }
    }

    /**
     * render with back-to-front alpha blending and without depth test
     * */
    inline fun renderDefault(render: () -> Unit) {
        blendMode.use(BlendMode.DEFAULT) {
            depthMode.use(DepthMode.ALWAYS, render)
        }
    }

    // maximum expected depth for OpenGL operations
    // could be changed, if needed...
    private const val maxSize = 512
    val renderers = Array<Renderer>(maxSize) { colorRenderer }

    val currentRenderer get() = renderers[framebuffer.index]
    val currentBuffer get() = framebuffer.values[framebuffer.index]

    val xs = IntArray(maxSize)
    val ys = IntArray(maxSize)
    val ws = IntArray(maxSize)
    val hs = IntArray(maxSize)
    val changeSizes = BooleanArray(maxSize)

    val framebuffer = object : SecureStack<IFramebuffer>(NullFramebuffer) {
        override fun onChangeValue(newValue: IFramebuffer, oldValue: IFramebuffer) {
            Frame.bind(newValue, changeSizes[index], xs[index], ys[index], ws[index], hs[index])
        }
    }

    fun useFrame(
        buffer: IFramebuffer,
        renderer: Renderer,
        render: () -> Unit
    ) = useFrame(0, 0, -1, -1, buffer, renderer, render)

    fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        buffer: IFramebuffer, renderer: Renderer, render: () -> Unit
    ) = useFrame(x, y, w, h, false, buffer, renderer, render)

    private fun useFrame(
        x: Int, y: Int, w: Int, h: Int, changeSize: Boolean,
        buffer: IFramebuffer, renderer: Renderer, render: () -> Unit
    ) {
        val index = framebuffer.size
        if (index >= xs.size) {
            throw StackOverflowError("Reached recursion limit for useFrame()")
        }
        xs[index] = x
        ys[index] = y
        ws[index] = w
        hs[index] = h
        changeSizes[index] = changeSize
        buffer.use(index, renderer, render)
    }

    fun useFrame(renderer: Renderer, render: () -> Unit) =
        useFrame(currentBuffer, renderer, render)

    fun useFrame(buffer: IFramebuffer, render: () -> Unit) =
        useFrame(buffer, currentRenderer, render)

    fun useFrame(
        w: Int, h: Int, changeSize: Boolean,
        buffer: IFramebuffer, renderer: Renderer, render: () -> Unit
    ) = useFrame(0, 0, w, h, changeSize, buffer, renderer, render)

    fun useFrame(w: Int, h: Int, changeSize: Boolean, buffer: IFramebuffer, render: () -> Unit) =
        useFrame(w, h, changeSize, buffer, currentRenderer, render)

    fun useFrame(w: Int, h: Int, changeSize: Boolean, render: () -> Unit) =
        useFrame(w, h, changeSize, currentBuffer, currentRenderer, render)

    fun useFrame(x: Int, y: Int, w: Int, h: Int, render: () -> Unit) =
        useFrame(x, y, w, h, currentBuffer, currentRenderer, render)

    fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        buffer: IFramebuffer, render: () -> Unit
    ) = useFrame(x, y, w, h, buffer, currentRenderer, render)

    fun useFrame(
        w: Int, h: Int, changeSize: Boolean,
        renderer: Renderer, render: () -> Unit
    ) = useFrame(w, h, changeSize, currentBuffer, renderer, render)

    fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        renderer: Renderer, render: () -> Unit
    ) = useFrame(x, y, w, h, currentBuffer, renderer, render)

}