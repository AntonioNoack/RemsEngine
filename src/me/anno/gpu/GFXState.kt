package me.anno.gpu

import me.anno.Build
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.fonts.FontManager.TextCache
import me.anno.gpu.GFX.supportsClipControl
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.BakedLayout
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.debug.TimeRecord
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.MultiFramebuffer
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.utils.InternalAPI
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.stacks.SecureStack
import me.anno.video.VideoCache
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_BACK
import org.lwjgl.opengl.GL46C.GL_BLEND
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_CULL_FACE
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRONT
import org.lwjgl.opengl.GL46C.GL_LOWER_LEFT
import org.lwjgl.opengl.GL46C.GL_NEGATIVE_ONE_TO_ONE
import org.lwjgl.opengl.GL46C.GL_RENDERBUFFER
import org.lwjgl.opengl.GL46C.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL46C.GL_STENCIL_TEST
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL46C.GL_ZERO_TO_ONE
import org.lwjgl.opengl.GL46C.glClipControl
import org.lwjgl.opengl.GL46C.glCullFace
import org.lwjgl.opengl.GL46C.glDepthFunc
import org.lwjgl.opengl.GL46C.glDepthMask
import org.lwjgl.opengl.GL46C.glDisable
import org.lwjgl.opengl.GL46C.glEnable
import org.lwjgl.opengl.GL46C.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL46C.glFramebufferTexture2D
import org.lwjgl.opengl.GL46C.glGenFramebuffers

/**
 * holds rendering-related state,
 * currently, with OpenGL, this must be changed from a single thread only!
 * all functions feature rendering-callbacks, so you can change settings without having to worry about the previously set state by your caller
 * */
object GFXState {

    private val LOGGER = LogManager.getLogger(GFXState::class)

    var session = 0
        private set

    fun invalidateState() {
        lastBlendMode = Unit
        lastDepthMode = null
        lastDepthMask = null
        lastCullMode = null
    }

    private var lastBlendMode: Any? = Unit
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
                if (lastBlendMode == Unit || lastBlendMode == null) {
                    glEnable(GL_BLEND)
                }
                newValue.forceApply()
            }
            is List<*> -> {
                if (lastBlendMode == Unit || lastBlendMode == null) {
                    glEnable(GL_BLEND)
                }
                for (i in newValue.indices) {
                    val v = newValue[i] as BlendMode
                    v.forceApply(i)
                }
            }
            else -> throw IllegalArgumentException("Unknown blend mode type")
        }
        lastBlendMode = newValue
    }

    private fun bindDepthMode() {
        val newValue = depthMode.currentValue
        if (lastDepthMode == newValue) return
        glDepthFunc(newValue.id)
        val reversedDepth = newValue.reversedDepth
        if (lastDepthMode?.reversedDepth != reversedDepth) {
            if (supportsClipControl) {
                glClipControl(GL_LOWER_LEFT, if (reversedDepth) GL_ZERO_TO_ONE else GL_NEGATIVE_ONE_TO_ONE)
            } else {
                LOGGER.warn("Reversed depth is not supported (because it's pointless without glClipControl")
            }
        }
        lastDepthMode = newValue
    }

    fun bindDepthMask() {
        val newValue = depthMask.currentValue
        if (lastDepthMask == newValue) return
        glDepthMask(newValue)
        lastDepthMask = newValue
    }

    private fun bindCullMode() {
        val newValue = cullMode.currentValue
        if (lastCullMode == newValue) return
        when (newValue) {
            CullMode.BOTH -> {
                glDisable(GL_CULL_FACE) // both visible -> disabled
            }
            CullMode.FRONT -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_BACK) // front visible -> back hidden
            }
            CullMode.BACK -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_FRONT) // back visible -> front hidden
            }
        }
        lastCullMode = newValue
    }

    fun bind() {
        bindBlendMode(blendMode.currentValue)
        bindDepthMode()
        bindDepthMask()
        bindCullMode()
    }

    /**
     * in OpenGL ES (e.g. on Android),
     * the context can be destroyed, when the app
     * is closed temporarily to save resources & energy
     * then all loaded memory has become invalid
     * */
    fun newSession() {
        session++
        if (session != 1 && GFX.gpuTasks.isNotEmpty()) {
            LOGGER.warn("Discarding ${GFX.gpuTasks.size} GPUTasks")
            GFX.gpuTasks.clear() // they all have become invalid
        }
        GPUShader.invalidateBinding()
        Texture2D.invalidateBinding()
        OpenGLBuffer.invalidateBinding()
        invalidateState()
        val vao = GL46C.glCreateVertexArrays()
        GL46C.glBindVertexArray(vao)
        if (session != 1) {
            // clear all caches, which contain gpu data
            FBStack.clear()
            TextCache.clear()
            VideoCache.clear()
            TextureCache.clear()
        }
    }

    val blendMode = SecureStack<Any?>(BlendMode.DEFAULT)
    val depthMode = SecureStack(alwaysDepthMode)
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

    val ditherMode = SecureStack(DitherMode.DRAW_EVERYTHING)

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
            depthMode.use(alwaysDepthMode, render)
        }
    }

    /**
     * render without blending and without depth test
     * */
    fun <V> renderPurely2(render: () -> V): V {
        return blendMode.use(null) {
            depthMask.use(false) {
                depthMode.use(alwaysDepthMode, render)
            }
        }
    }

    /**
     * render with back-to-front alpha blending and without depth test
     * */
    fun <V> renderDefault(render: () -> V): V {
        return blendMode.use(BlendMode.DEFAULT) {
            depthMode.use(alwaysDepthMode, render)
        }
    }

    // this would allow us to specify per-model parameters :)
    val bakedMeshLayout = SecureStack<BakedLayout?>(null)
    val bakedInstLayout = SecureStack<BakedLayout?>(null)
    
    val alwaysDepthMode get() = if (supportsClipControl) DepthMode.ALWAYS else DepthMode.FORWARD_ALWAYS

    // maximum expected depth for OpenGL operations
    // could be changed, if needed...
    private const val maxSize = 512
    val renderers = createArrayList<Renderer>(maxSize, colorRenderer)

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
    ) = useFrame(0, 0, buffer.width, buffer.height, buffer, renderer, render)

    fun useFrame(
        x: Int, y: Int, w: Int, h: Int,
        buffer: IFramebuffer, renderer: Renderer, render: () -> Unit
    ) = useFrame(x, y, w, h, false, buffer, renderer, render)

    private fun useFrame(
        x: Int, y: Int, w: Int, h: Int, changeSize: Boolean,
        buffer: IFramebuffer, renderer: Renderer, render: () -> Unit
    ) {
        if (w > 0 && h > 0) {
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

    private val tmp = Framebuffer("tmp", 1, 1, 1, emptyList(), DepthBufferType.NONE)

    /**
     * render onto that texture
     * */
    fun useFrame(colorDst: Texture2D, render: (IFramebuffer) -> Unit) {
        useFrameColorNDepth(colorDst.width, colorDst.height, colorDst.pointer, 0, render)
    }

    /**
     * render onto that texture, and the depth texture/renderbuffer of depthDst
     * */
    fun useFrame(colorDst: Texture2D?, depthDst: IFramebuffer?, render: (IFramebuffer) -> Unit) {
        if (colorDst == null && (depthDst !is Framebuffer && depthDst !is MultiFramebuffer)) return
        val depthDstI = depthDst as? Framebuffer ?: (depthDst as? MultiFramebuffer)?.targetsI?.get(0)
        val dt = depthDstI?.depthTexture
        val dr = depthDstI?.depthRenderbuffer?.pointer ?: 0
        val dri = if (dr > 0) dr.inv() else 0
        val width = colorDst?.width ?: depthDstI!!.width
        val height = colorDst?.height ?: depthDstI!!.height
        useFrameColorNDepth(width, height, colorDst?.pointer ?: 0, dt?.pointer ?: dri, render)
    }

    /**
     * render onto that texture
     * */
    private fun useFrameColorNDepth(
        width: Int, height: Int,
        colorDstPointer: Int, depthDstPointer: Int,
        render: (IFramebuffer) -> Unit
    ) {
        tmp.width = width
        tmp.height = height
        if (tmp.pointer == 0 || tmp.session != session) {
            tmp.pointer = glGenFramebuffers()
            tmp.session = session
        }
        useFrame(tmp) {
            val target = GL_FRAMEBUFFER
            val attach = GL_DEPTH_ATTACHMENT
            // bind color, 0 = unbinding
            glFramebufferTexture2D(target, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorDstPointer, 0)
            // bind depth, 0 = unbinding
            if (depthDstPointer >= 0) glFramebufferTexture2D(target, attach, GL_TEXTURE_2D, depthDstPointer, 0)
            else glFramebufferRenderbuffer(target, attach, GL_RENDERBUFFER, depthDstPointer.inv())
            Framebuffer.drawBuffersN(1)
            tmp.checkIsComplete()
            render(tmp)
        }
    }

    fun pushDrawCallName(name: String) {
        if (Build.isDebug) {
            GFX.checkIsGFXThread()
            GL46C.glPushDebugGroup(GL46C.GL_DEBUG_SOURCE_APPLICATION, PUSH_DEBUG_GROUP_MAGIC, name)
        }
    }

    fun popDrawCallName() {
        if (Build.isDebug) {
            GFX.checkIsGFXThread()
            GL46C.glPopDebugGroup()
        }
    }

    inline fun timeRendering(name: String, timer: GPUClockNanos?, runRendering: () -> Unit) {
        pushDrawCallName(name)
        timer?.start()
        runRendering()
        stopTimer(name, timer)
        popDrawCallName()
    }

    @InternalAPI
    fun stopTimer(name: String, timer: GPUClockNanos?) {
        timer ?: return
        timer.stop()
        if (timer.lastResult >= 0L) {
            timeRecords.add(TimeRecord(name, timer.lastResult))
        }
    }

    const val PUSH_DEBUG_GROUP_MAGIC = -93 // just some random number, that's unlikely to appear otherwise

    val timeRecords = ArrayList<TimeRecord>()
}