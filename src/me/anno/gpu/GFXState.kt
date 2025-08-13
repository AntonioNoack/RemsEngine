package me.anno.gpu

import me.anno.Build
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.fonts.FontManager.textAtlasCache
import me.anno.gpu.GPUTasks.gpuTasks
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.AttributeLayout
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
import me.anno.utils.assertions.assertFail
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.stacks.SecureStack
import me.anno.utils.types.Booleans.hasFlag
import me.anno.video.VideoCache
import org.apache.logging.log4j.LogManager
import org.joml.Vector4i
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_BACK
import org.lwjgl.opengl.GL46C.GL_BLEND
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_CULL_FACE
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_FILL
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRONT
import org.lwjgl.opengl.GL46C.GL_FRONT_AND_BACK
import org.lwjgl.opengl.GL46C.GL_LINE
import org.lwjgl.opengl.GL46C.GL_LOWER_LEFT
import org.lwjgl.opengl.GL46C.GL_NEGATIVE_ONE_TO_ONE
import org.lwjgl.opengl.GL46C.GL_RENDERBUFFER
import org.lwjgl.opengl.GL46C.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL46C.GL_STENCIL_TEST
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL46C.GL_ZERO_TO_ONE
import org.lwjgl.opengl.GL46C.glBindVertexArray
import org.lwjgl.opengl.GL46C.glClipControl
import org.lwjgl.opengl.GL46C.glColorMask
import org.lwjgl.opengl.GL46C.glCreateVertexArrays
import org.lwjgl.opengl.GL46C.glCullFace
import org.lwjgl.opengl.GL46C.glDepthFunc
import org.lwjgl.opengl.GL46C.glDepthMask
import org.lwjgl.opengl.GL46C.glDisable
import org.lwjgl.opengl.GL46C.glEnable
import org.lwjgl.opengl.GL46C.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL46C.glFramebufferTexture2D
import org.lwjgl.opengl.GL46C.glGenFramebuffers
import org.lwjgl.opengl.GL46C.glPolygonMode
import org.lwjgl.opengl.GL46C.glScissor

/**
 * holds rendering-related state,
 * currently, with OpenGL, this must be used from a single thread only!
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
        lastDrawLines = null
        lastColorMask = -1
    }

    private var lastBlendMode: Any? = Unit
    private var lastDepthMode: DepthMode? = null
    private var lastDepthMask: Boolean? = null
    private var lastCullMode: CullMode? = null
    private var lastDrawLines: Boolean? = null
    private var lastColorMask: Int = -1

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
            else -> assertFail("Unknown blend mode type")
        }
        lastBlendMode = newValue
    }

    private fun bindDepthMode() {
        val newValue = depthMode.currentValue
        if (lastDepthMode == newValue) return
        glDepthFunc(newValue.id)
        val reversedDepth = newValue.reversedDepth
        if (lastDepthMode?.reversedDepth != reversedDepth) {
            if (GFX.supportsClipControl) {
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

    fun bindColorMask() {
        val newValue = colorMask.currentValue and 15
        if (lastColorMask == newValue) return
        glColorMask(
            newValue.hasFlag(COLOR_MASK_R),
            newValue.hasFlag(COLOR_MASK_G),
            newValue.hasFlag(COLOR_MASK_B),
            newValue.hasFlag(COLOR_MASK_A)
        )
        lastColorMask = newValue
    }

    private fun bindDrawLines() {
        val newValue = drawLines.currentValue
        if (lastDrawLines == newValue) return
        glPolygonMode(GL_FRONT_AND_BACK, if (newValue) GL_LINE else GL_FILL)
        lastDrawLines = newValue
    }

    private fun bindCullMode() {
        var newValue = cullMode.currentValue
        if (drawLines.currentValue) newValue = CullMode.BOTH
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
        bindColorMask()
        bindCullMode()
        bindDrawLines()
    }

    /**
     * in OpenGL ES (e.g. on Android),
     * the context can be destroyed, when the app
     * is closed temporarily to save resources & energy
     * then all loaded memory has become invalid
     * */
    fun newSession() {
        session++
        if (session != 1 && gpuTasks.isNotEmpty()) {
            LOGGER.warn("Discarding ${gpuTasks.size} GPUTasks")
            gpuTasks.clear() // they all have become invalid
        }
        GPUShader.invalidateBinding()
        Texture2D.invalidateBinding()
        OpenGLBuffer.invalidateBinding()
        invalidateState()
        val vao = glCreateVertexArrays()
        glBindVertexArray(vao)
        if (session != 1) {
            clearGPUCaches()
        }
    }

    fun onDestroyContext() {
        session++
    }

    private fun clearGPUCaches() {
        // clear all caches, which contain gpu data
        FBStack.clear()
        textAtlasCache.clear()
        VideoCache.clear()
        TextureCache.clear()
    }

    /**
     * how shader pixels are combined with the underlying framebuffer
     * */
    val blendMode = SecureStack<Any?>(BlendMode.DEFAULT)

    /**
     * mode used for depth-testing, e.g. CLOSER
     * */
    val depthMode = SecureStack(alwaysDepthMode)

    /**
     * whether depth is written
     * */
    val depthMask = SecureStack(true)

    const val COLOR_MASK_R = 8
    const val COLOR_MASK_G = 4
    const val COLOR_MASK_B = 2
    const val COLOR_MASK_A = 1

    /**
     * whether R=8,G=4,B=2,A=1 shall be stored
     * */
    val colorMask = SecureStack(15)

    /**
     * whether lines should be rendered instead of triangles
     * */
    val drawLines = SecureStack(false)

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

    /**
     * whether lines/triangles shall be dithered
     * */
    val ditherMode = SecureStack(DitherMode.DRAW_EVERYTHING)

    /**
     * whether front/back/or both of triangles shall be drawn
     * */
    val cullMode = SecureStack(CullMode.BOTH)

    /**
     * whether sky is currently being drawn; necessary for previewRenderer
     * */
    val drawingSky = SecureStack(false)

    @Suppress("unused")
    val stencilTest = object : SecureStack<Boolean>(false) {
        override fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
            if (newValue) glEnable(GL_STENCIL_TEST)
            else glDisable(GL_STENCIL_TEST)
        }
    }

    /**
     * Binds the current scissor window or null.
     * Components: x,y,width,height.
     * */
    val scissorTest = object : SecureStack<Vector4i?>(null) {
        override fun onChangeValue(newValue: Vector4i?, oldValue: Vector4i?) {
            if (newValue != null) {
                glEnable(GL_SCISSOR_TEST)
                glScissor(newValue.x, newValue.y, newValue.z, newValue.w)
            } else glDisable(GL_SCISSOR_TEST)
        }
    }

    /**
     * render without blending and without depth test
     * */
    inline fun <V> renderPurely(crossinline render: () -> V): V {
        return blendMode.use(null) {
            depthMode.use(alwaysDepthMode, render)
        }
    }

    /**
     * render without blending and without depth test
     * */
    inline fun <V> renderPurely2(crossinline render: () -> V): V {
        return blendMode.use(null) {
            depthMask.use(false) {
                depthMode.use(alwaysDepthMode, render)
            }
        }
    }

    /**
     * render with back-to-front alpha blending and without depth test
     * */
    inline fun <V> renderDefault(crossinline render: () -> V): V {
        return blendMode.use(BlendMode.DEFAULT) {
            depthMode.use(alwaysDepthMode, render)
        }
    }

    // this would allow us to specify per-model parameters :)
    val bakedMeshLayout = SecureStack<AttributeLayout?>(null)
    val bakedInstLayout = SecureStack<AttributeLayout?>(null)

    val alwaysDepthMode
        get() =
            if (GFX.supportsClipControl) DepthMode.ALWAYS
            else DepthMode.FORWARD_ALWAYS

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

    fun usePushDebugGroups(): Boolean {
        GFX.checkIsGFXThread()
        return Build.isDebug && WindowManagement.hasOpenGLDebugContext
    }

    fun pushDrawCallName(name: String) {
        if (usePushDebugGroups()) {
            GL46C.glPushDebugGroup(GL46C.GL_DEBUG_SOURCE_APPLICATION, PUSH_DEBUG_GROUP_MAGIC, name)
        }
    }

    inline fun drawCall(name: String, renderCall: () -> Unit) {
        pushDrawCallName(name)
        renderCall()
        popDrawCallName()
    }

    fun popDrawCallName() {
        if (usePushDebugGroups()) {
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
        if (timer.result >= 0L) {
            val last = timeRecords.lastOrNull()
            if (last?.name != name) {
                timeRecords.add(TimeRecord(name, timer.result, 1))
            } else {
                last.deltaNanos += timer.result
                last.divisor++
            }
        }
    }

    const val PUSH_DEBUG_GROUP_MAGIC = -93 // just some random number, that's unlikely to appear otherwise

    val timeRecords = ArrayList<TimeRecord>()
}