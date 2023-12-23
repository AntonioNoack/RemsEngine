package me.anno.gpu

import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.ecs.components.mesh.MeshVertexData
import me.anno.fonts.FontManager.TextCache
import me.anno.gpu.GFX.supportsClipControl
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.framebuffer.*
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.utils.structures.stacks.SecureStack
import me.anno.video.VideoCache
import org.lwjgl.opengl.GL20C.GL_FRONT
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

    private var lastBlendMode: Any? = BlendMode.INHERIT
    private var lastDepthMode: DepthMode? = null
    private var lastDepthMask: Boolean? = null
    private var lastCullMode: CullMode? = null

    private fun bindBlendMode(newValue: Any?) {
        if (newValue == lastBlendMode) return
        when (newValue) {
            null -> glDisable(GL_BLEND)
            BlendMode.INHERIT -> {
                val stack = blendMode
                var index = stack.index
                var self: Any?
                do {
                    self = stack.values[index--]
                } while (self == BlendMode.INHERIT)
                return bindBlendMode(self)
            }
            is BlendMode -> {
                if (lastBlendMode == null) {
                    glEnable(GL_BLEND)
                }
                newValue.forceApply()
            }
            is Array<*> -> {
                if (lastBlendMode == null) {
                    glEnable(GL_BLEND)
                }
                for (i in newValue.indices) {
                    val v = newValue[i] as BlendMode
                    v.forceApply(i)
                }
            }
            else -> throw IllegalArgumentException()
        }
        lastBlendMode = newValue
    }

    private fun bindDepthMode(newValue: DepthMode) {
        if (lastDepthMode == newValue) return
        glDepthFunc(newValue.id)
        val reversedDepth = newValue.reversedDepth
        if (lastDepthMode?.reversedDepth != reversedDepth) {
            if (supportsClipControl) {
                glClipControl(GL_LOWER_LEFT, if (reversedDepth) GL_ZERO_TO_ONE else GL_NEGATIVE_ONE_TO_ONE)
            } else {
                // does this work??
                glDepthRange(0.0, 1.0)
            }
        }
        lastDepthMode = newValue
    }

    private fun bindDepthMask(newValue: Boolean) {
        if (lastDepthMask == newValue) return
        glDepthMask(newValue)
        lastDepthMask = newValue
    }

    private fun bindCullMode(newValue: CullMode) {
        if (lastCullMode == newValue) return
        when (newValue) {
            CullMode.BOTH -> {
                glDisable(GL_CULL_FACE)
            }
            CullMode.FRONT -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_FRONT)
            }
            CullMode.BACK -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_BACK)
            }
        }
        lastCullMode = newValue
    }

    fun bind() {
        bindBlendMode(blendMode.currentValue)
        bindDepthMode(depthMode.currentValue)
        bindDepthMask(depthMask.currentValue)
        bindCullMode(cullMode.currentValue)
    }

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
        lastBlendMode = BlendMode.INHERIT
        lastDepthMode = null
        lastDepthMask = null
        lastCullMode = null
        // clear all caches, which contain gpu data
        FBStack.clear()
        TextCache.clear()
        VideoCache.clear()
        TextureCache.clear()
    }

    // the renderer is set per framebuffer; makes the most sense
    // additionally, it would be possible to set the blend-mode and depth there in a game setting
    // (not that practical in RemsStudio)
    // val renderer = SecureStack(Renderer.colorRenderer)

    val blendMode = SecureStack<Any?>(BlendMode.DEFAULT)
    val depthMode = SecureStack(DepthMode.ALWAYS)
    val depthMask = SecureStack(true)

    /**
     * a flag for shaders whether their animated version (slower) is used
     * */
    val animated = SecureStack(false)

    /**
     * defines how localPosition, and normal are loaded from mesh attributes
     * */
    val vertexData = SecureStack(MeshVertexData.DEFAULT)

    /**
     * defines how the instanced transform is derived from available attributes (depends on InstancedStack)
     * */
    val instanceData = SecureStack(MeshInstanceData.DEFAULT)

    val cullMode = SecureStack(CullMode.BOTH)

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
    fun <V> renderPurely(render: () -> V): V {
        return blendMode.use(null) {
            depthMode.use(DepthMode.ALWAYS, render)
        }
    }

    /**
     * render without blending and without depth test
     * */
    fun <V> renderPurely2(render: () -> V): V {
        return blendMode.use(null) {
            depthMask.use(false) {
                depthMode.use(DepthMode.ALWAYS, render)
            }
        }
    }

    /**
     * render with back-to-front alpha blending and without depth test
     * */
    fun <V> renderDefault(render: () -> V): V {
        return blendMode.use(BlendMode.DEFAULT) {
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
        if ((w > 0 && h > 0) || (w == -1 && h == -1)) {
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
        } else buffer.ensure()
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

    private val tmp = Framebuffer("tmp", 1, 1, 1, 0, false, DepthBufferType.NONE)

    /**
     * render onto that texture
     * */
    fun useFrame(texture: Texture2D, level: Int, render: (IFramebuffer) -> Unit) {
        tmp.width = texture.width
        tmp.height = texture.height
        if (tmp.pointer == 0 || tmp.session != session) {
            tmp.pointer = glGenFramebuffers()
        }
        useFrame(tmp) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, texture.target, texture.pointer, level)
            Framebuffer.drawBuffers1(0)
            tmp.checkIsComplete()
            render(tmp)
        }
    }
}