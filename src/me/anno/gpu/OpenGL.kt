package me.anno.gpu

import me.anno.cache.instances.TextCache
import me.anno.cache.instances.VideoCache
import me.anno.gpu.GFX.supportsClipControl
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.MultiFramebuffer
import me.anno.gpu.pipeline.CullMode
import me.anno.gpu.shader.GeoShader
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.image.ImageGPUCache
import me.anno.utils.structures.stacks.SecureBoolStack
import me.anno.utils.structures.stacks.SecureStack
import org.lwjgl.opengl.GL20.GL_LOWER_LEFT
import org.lwjgl.opengl.GL45.*

object OpenGL {

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
            GFX.check()
            if (newValue != DepthMode.ALWAYS) {
                glEnable(GL_DEPTH_TEST)
                glDepthFunc(newValue.func)
                val reversedDepth = newValue.reversedDepth
                if (supportsClipControl) {
                    glClearDepth(if (reversedDepth) 0.0 else 1.0)
                    glClipControl(GL_LOWER_LEFT, if (reversedDepth) GL_ZERO_TO_ONE else GL_NEGATIVE_ONE_TO_ONE)
                } else {
                    // does this work??
                    glClearDepth(if (reversedDepth) 0.0 else 1.0)
                    glDepthRange(0.0, 1.0)
                }
                // glDepthRange(-1.0, 1.0)
                // glDepthFunc(GL_LESS)
            } else {
                glDisable(GL_DEPTH_TEST)
            }
        }
    }

    /**
     * clears the depth according to the current depthMode
     * */
    fun clearDepth() {
        glClear(GL_DEPTH_BUFFER_BIT)
    }

    val depthMask = object : SecureStack<Boolean>(true) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            GFX.check()
            glDepthMask(newValue)
        }
    }

    @Deprecated("This is deprecated, because it's sometimes slow, and not supported on all devices")
    val geometryShader = SecureStack<GeoShader?>(null)

    val instanced = object : SecureBoolStack(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            // nothing changes on the OpenGL side,
            // just the shaders need to be modified
        }
    }

    /**
     * a flag for shaders weather their animated version (slower) is used
     * */
    val animated = object : SecureBoolStack(false) {
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

    val stencilTest = object : SecureBoolStack(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            if (newValue) glEnable(GL_STENCIL_TEST)
            else glDisable(GL_STENCIL_TEST)
        }
    }

    val scissorTest = object : SecureBoolStack(false) {
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
    val renderers = Array(maxSize) { colorRenderer }

    val currentRenderer get() = renderers[framebuffer.size - 1]
    val currentBuffer get() = framebuffer.values[framebuffer.size - 1]

    val xs = IntArray(maxSize)
    val ys = IntArray(maxSize)
    val ws = IntArray(maxSize)
    val hs = IntArray(maxSize)
    val changeSizes = BooleanArray(maxSize)

    val framebuffer = object : SecureStack<IFramebuffer?>(null) {
        override fun onChangeValue(newValue: IFramebuffer?, oldValue: IFramebuffer?) {
            Frame.bind(newValue, changeSizes[index], xs[index], ys[index], ws[index], hs[index])
        }
    }

    inline fun useFrame(
        buffer: IFramebuffer?,
        renderer: Renderer,
        render: () -> Unit
    ) = useFrame(0, 0, -1, -1, false, buffer, renderer, render)

    inline fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        changeSize: Boolean,
        buffer: IFramebuffer?,
        renderer: Renderer,
        render: () -> Unit
    ) {
        val index = framebuffer.size
        xs[index] = x
        ys[index] = y
        ws[index] = w
        hs[index] = h
        changeSizes[index] = changeSize
        // todo depth must be cleared only once
        if (buffer is MultiFramebuffer) {
            val targets = buffer.targetsI
            for (targetIndex in targets.indices) {
                val target = targets[targetIndex]
                // split renderer by targets
                renderers[index] = renderer.split(targetIndex, buffer.div)
                framebuffer.use(target, render)
            }
        } else {
            renderers[index] = renderer
            framebuffer.use(buffer, render)
        }
    }

    inline fun useFrame(renderer: Renderer, render: () -> Unit) =
        useFrame(currentBuffer, renderer, render)

    inline fun useFrame(buffer: IFramebuffer?, render: () -> Unit) =
        useFrame(buffer, currentRenderer, render)

    inline fun useFrame(w: Int, h: Int, changeSize: Boolean, buffer: IFramebuffer?, render: () -> Unit) =
        useFrame(0, 0, w, h, changeSize, buffer, currentRenderer, render)

    inline fun useFrame(
        w: Int,
        h: Int,
        changeSize: Boolean,
        buffer: IFramebuffer?,
        renderer: Renderer,
        render: () -> Unit
    ) = useFrame(0, 0, w, h, changeSize, buffer, renderer, render)

    inline fun useFrame(x: Int, y: Int, w: Int, h: Int, changeSize: Boolean, render: () -> Unit) =
        useFrame(x, y, w, h, changeSize, currentBuffer, currentRenderer, render)

    inline fun useFrame(
        x: Int, y: Int, w: Int, h: Int, changeSize: Boolean,
        buffer: IFramebuffer?, render: () -> Unit
    ) = useFrame(x, y, w, h, changeSize, buffer, currentRenderer, render)

    inline fun useFrame(
        x: Int, y: Int, w: Int, h: Int, changeSize: Boolean,
        renderer: Renderer, render: () -> Unit
    ) = useFrame(x, y, w, h, changeSize, currentBuffer, renderer, render)

}