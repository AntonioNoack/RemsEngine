package me.anno.gpu

import me.anno.Build
import me.anno.Build.isDebug
import me.anno.Engine
import me.anno.audio.streams.AudioStream
import me.anno.config.DefaultConfig
import me.anno.gpu.GFXState.blendMode
import me.anno.gpu.GFXState.currentRenderer
import me.anno.gpu.GFXState.depthMode
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.FlatShaders.copyShader
import me.anno.gpu.shader.OpenGLShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.workEventTasks
import me.anno.utils.Clock
import me.anno.utils.OS
import me.anno.utils.structures.Task
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.ARBImaging.GL_TABLE_TOO_LARGE
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL43C.GL_MAX_UNIFORM_LOCATIONS
import org.lwjgl.opengl.GL45C.GL_CONTEXT_LOST
import org.lwjgl.opengl.GL46
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * graphics capabilities, clipping, copying, gpu work scheduling, main render loop
 * */
object GFX {

    // todo there is per-framebuffer blending since OpenGL 4.0 😍
    // todo and it somehow can be used for order-independent transparency
    // (Weighted Blended Order-Independent Transparency by Morgan McGuire Louis Bavoil)
    // glBlendFunci()

    @JvmStatic
    private val LOGGER = LogManager.getLogger(GFX::class)

    init {
        LOGGER.info("Initializing GFX")
    }

    // for final rendering we need to use the GPU anyway;
    // so just use a static variable
    @JvmField
    var isFinalRendering = false

    @JvmField
    val windows = ArrayList<OSWindow>()

    /**
     * current window, which is being rendered to by OpenGL
     * */
    @JvmField
    var activeWindow: OSWindow? = null

    /**
     * window, that is in focus; may be null
     * */
    @JvmStatic
    val focusedWindow get() = windows.firstOrNull2 { it.isInFocus }

    /**
     * window, that is in focus, or arbitrary window, if undefined
     * */
    @JvmStatic
    val someWindow get() = focusedWindow ?: windows[0] // we also could choose the one closest to the mouse :)

    @JvmStatic
    var idleFPS
        get() = DefaultConfig["ui.window.idleFPS", 10]
        set(value) {
            DefaultConfig["ui.window.idleFPS"] = value
        }

    @JvmField
    var supportsAnisotropicFiltering = false

    @JvmField
    var anisotropy = 1f

    @JvmField
    var maxSamples = 1

    @JvmField
    var supportsClipControl = !OS.isAndroid && !OS.isWeb

    @JvmField
    var supportsF32Targets = true

    @JvmField
    var supportsF16Targets = true

    @JvmField
    var maxFragmentUniformComponents = 0

    @JvmField
    var maxVertexUniformComponents = 0

    @JvmField
    var maxBoundTextures = 0

    @JvmField
    var maxUniforms = 0

    @JvmField
    var maxColorAttachments = 1

    @JvmField
    var maxTextureSize = 512 // assumption before loading anything

    @JvmField
    var canLooseContext = OS.isAndroid

    @JvmField
    val nextGPUTasks = ArrayList<Task>()

    @JvmField
    val gpuTasks: Queue<Task> = ConcurrentLinkedQueue()

    @JvmField
    val lowPriorityGPUTasks: Queue<Task> = ConcurrentLinkedQueue()

    @JvmField
    val loadTexturesSync = Stack<Boolean>()
        .apply { push(false) }

    /**
     * location of the current default framebuffer
     * */
    @JvmField
    var offsetX = 0

    @JvmField
    var offsetY = 0

    /**
     * location & size of the current panel
     * */
    @JvmField
    var viewportX = 0

    @JvmField
    var viewportY = 0

    @JvmField
    var viewportWidth = 0

    @JvmField
    var viewportHeight = 0

    @JvmField
    val flat01 = SimpleBuffer.flat01

    @JvmField
    var drawnId = 0

    @JvmField
    var glThread: Thread? = null

    @JvmStatic
    fun addGPUTask(name: String, w: Int, h: Int, task: () -> Unit) = addGPUTask(name, w, h, false, task)

    @JvmStatic
    fun addGPUTask(name: String, w: Int, h: Int, lowPriority: Boolean, task: () -> Unit) {
        addGPUTask(name, max(1, ((w * h.toLong()) / 10_000).toInt()), lowPriority, task)
    }

    @JvmStatic
    fun addGPUTask(name: String, weight: Int, task: () -> Unit) = addGPUTask(name, weight, false, task)

    @JvmStatic
    fun addGPUTask(name: String, weight: Int, lowPriority: Boolean, task: () -> Unit) {
        (if (lowPriority) lowPriorityGPUTasks else gpuTasks) += Task(name, weight, task)
    }

    @JvmStatic
    fun addNextGPUTask(name: String, w: Int, h: Int, task: () -> Unit) =
        addNextGPUTask(name, max(1, ((w * h.toLong()) / 10_000).toInt()), task)

    @JvmStatic
    fun addNextGPUTask(name: String, weight: Int, task: () -> Unit) {
        nextGPUTasks += Task(name, weight, task)
    }

    @JvmStatic
    inline fun useWindowXY(x: Int, y: Int, buffer: Framebuffer?, process: () -> Unit) {
        if (buffer == null) {
            val ox = offsetX
            val oy = offsetY
            offsetX = x
            offsetY = y
            try {
                process()
            } finally {
                offsetX = ox
                offsetY = oy
            }
        } else {
            val ox = buffer.offsetX
            val oy = buffer.offsetY
            buffer.offsetX = x
            buffer.offsetY = y
            try {
                process()
            } finally {
                buffer.offsetX = ox
                buffer.offsetY = oy
            }
        }
    }

    @JvmStatic
    fun clip(x: Int, y: Int, w: Int, h: Int, render: () -> Unit) {
        // from the bottom to the top
        check()
        if (w < 1 || h < 1) throw RuntimeException("w < 1 || h < 1 not allowed, got $w x $h")
        // val height = RenderState.currentBuffer?.h ?: height
        // val realY = height - (y + h)
        useFrame(x, y, w, h) {
            render()
        }
    }

    @JvmStatic
    fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) = clip(x0, y0, x1 - x0, y1 - y0, render)

    @JvmStatic
    fun clip2Save(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) {
        val w = x1 - x0
        val h = y1 - y0
        if (w > 0 && h > 0) {
            clip(x0, y0, w, h, render)
        }
    }

    @JvmStatic
    fun clip2Dual(
        x0: Int, y0: Int, x1: Int, y1: Int,
        x2: Int, y2: Int, x3: Int, y3: Int,
        render: (x0: Int, y0: Int, x1: Int, y1: Int) -> Unit
    ) {
        clip2Save(
            max(x0, x2),
            max(y0, y2),
            min(x1, x3),
            min(y1, y3)
        ) {
            render(x2, y2, x3, y3)
        }
    }

    @JvmStatic
    fun shaderColor(shader: Shader, name: String, color: Int) =
        currentRenderer.shaderColor(shader, name, color)

    @JvmStatic
    fun shaderColor(shader: Shader, name: String, color: Vector4f?) =
        currentRenderer.shaderColor(shader, name, color)

    @JvmStatic
    fun shaderColor(shader: Shader, name: String, r: Float, g: Float, b: Float, a: Float) =
        currentRenderer.shaderColor(shader, name, r, g, b, a)

    @JvmStatic
    fun shaderColor(shader: Shader, name: String, color: Vector3f?) =
        currentRenderer.shaderColor(shader, name, color)

    @JvmStatic
    fun copy(buffer: IFramebuffer) = copy(buffer.getTexture0())

    @JvmStatic
    fun copy(buffer: ITexture2D) {
        Frame.bind()
        buffer.bindTrulyNearest(0)
        copy()
    }

    @JvmStatic
    fun copy(alpha: Float) {
        check()
        val shader = copyShader
        shader.use()
        shader.v1f("alpha", alpha)
        flat01.draw(shader)
        check()
    }

    @JvmStatic
    fun copy() {
        check()
        val shader = copyShader
        shader.use()
        shader.v1f("alpha", 1f)
        flat01.draw(shader)
        check()
    }

    @JvmStatic
    fun copyNoAlpha(buffer: IFramebuffer) = copyNoAlpha(buffer.getTexture0())

    @JvmStatic
    fun copyNoAlpha(buffer: ITexture2D) {
        Frame.bind()
        buffer.bindTrulyNearest(0)
        copyNoAlpha()
    }

    @JvmStatic
    fun copyNoAlpha() {
        check()
        blendMode.use(BlendMode.DST_ALPHA) {
            depthMode.use(DepthMode.ALWAYS) {
                val shader = copyShader
                shader.use()
                shader.v1f("alpha", 1f)
                flat01.draw(shader)
            }
        }
        check()
    }

    @JvmStatic
    fun setupBasics(tick: Clock?) {
        glThread = Thread.currentThread()
        LOGGER.info("OpenGL Version " + glGetString(GL_VERSION))
        LOGGER.info("GLSL Version " + glGetString(GL_SHADING_LANGUAGE_VERSION))
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1) // OpenGL is evil ;), for optimizations, we might set it back
        val capabilities = GFXBase.capabilities
        supportsAnisotropicFiltering = capabilities?.GL_EXT_texture_filter_anisotropic ?: false
        LOGGER.info("OpenGL supports NV mesh shader? ${capabilities?.GL_NV_mesh_shader}")
        LOGGER.info("OpenGL supports Anisotropic Filtering? $supportsAnisotropicFiltering")
        if (supportsAnisotropicFiltering) {
            val max = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)
            anisotropy = min(max, DefaultConfig["gpu.filtering.anisotropic.max", 16f])
        }
        maxVertexUniformComponents = glGetInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS)
        maxFragmentUniformComponents = glGetInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS)
        maxBoundTextures = glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS)
        maxUniforms = glGetInteger(GL_MAX_UNIFORM_LOCATIONS)
        maxColorAttachments = glGetInteger(GL_MAX_COLOR_ATTACHMENTS)
        maxSamples = max(1, glGetInteger(GL_MAX_SAMPLES))
        maxTextureSize = max(256, glGetInteger(GL_MAX_TEXTURE_SIZE))
        LOGGER.info("Max Uniform Components: [Vertex: $maxVertexUniformComponents, Fragment: $maxFragmentUniformComponents]")
        LOGGER.info("Max Uniforms: $maxUniforms")
        LOGGER.info("Max Color Attachments: $maxColorAttachments")
        LOGGER.info("Max Samples: $maxSamples")
        LOGGER.info("Max Texture Size: $maxTextureSize")
        LOGGER.info("Max Bound Textures: $maxBoundTextures")
        tick?.stop("Checking OpenGL properties")
    }

    @JvmStatic
    fun setup(tick: Clock?) {
        setupBasics(tick)
        ShaderLib.init()
    }

    /**
     * time limit in seconds
     * returns whether time is left
     * */
    @JvmStatic
    fun workQueue(queue: Queue<Task>, timeLimit: Float, all: Boolean): Boolean {
        return workQueue(queue, if (all) Float.POSITIVE_INFINITY else timeLimit)
    }

    /**
     * time limit in seconds
     * returns whether time is left
     * */
    @JvmStatic
    fun workQueue(queue: Queue<Task>, timeLimit: Float): Boolean {

        // async work section
        val startTime = Engine.nanoTime

        // work 1/5th of the tasks by weight...

        // changing to 10 doesn't make the frame rate smoother :/
        val framesForWork = 5
        if (Thread.currentThread() == glThread) check()

        val workTodo = max(1000, queue.sumOf { it.cost } / framesForWork)
        var workDone = 0
        while (true) {
            val task = queue.poll() ?: return true
            try {
                task.work()
            } catch (e: Throwable) {
                RuntimeException(task.name, e)
                    .printStackTrace()
            }
            if (Thread.currentThread() == glThread) check()
            workDone += task.cost
            val currentTime = Engine.nanoTime
            val workTime = abs(currentTime - startTime) * 1e-9f
            if (workTime > 2f * timeLimit) {
                LOGGER.warn("Spent ${workTime}s on '${task.name}' with cost ${task.cost}")
            }
            if (workDone >= workTodo) return false
            if (workTime > timeLimit) return false // too much work
            FBStack.reset() // so we can reuse resources in different tasks
        }

    }

    @JvmStatic
    fun resetFBStack() {
        FBStack.reset()
    }

    @JvmField
    var gpuTaskBudget = 1f / 90f

    @JvmStatic
    fun workGPUTasks(all: Boolean) {
        val t0 = Engine.nanoTime
        synchronized(nextGPUTasks) {
            gpuTasks.addAll(nextGPUTasks)
            nextGPUTasks.clear()
        }
        if (workQueue(gpuTasks, gpuTaskBudget, all)) {
            val remainingTime = Engine.nanoTime - t0
            workQueue(lowPriorityGPUTasks, remainingTime * 1e-9f, all)
        }
        /*val dt = (Engine.nanoTime - t0) * 1e-9f
        if (dt > 1.5f * gpuTaskBudget) {
            LOGGER.warn("Spent too long in workGPUTasks(): ${dt}s")
        }*/
    }

    @JvmStatic
    fun workGPUTasksUntilShutdown() {
        while (!Engine.shutdown) {
            workGPUTasks(true)
        }
    }

    @JvmStatic
    fun setFrameNullSize(window: OSWindow) {
        setFrameNullSize(window.width, window.height)
    }

    @JvmStatic
    fun setFrameNullSize(width: Int, height: Int) {
        GFXState.apply {
            // this should be the state for the default framebuffer
            xs[0] = 0
            ys[0] = 0
            ws[0] = width
            hs[0] = height
            changeSizes[0] = false
            Frame.invalidate()
            viewportX = 0
            viewportY = 0
            viewportWidth = width
            viewportHeight = height
        }
    }

    @JvmStatic
    fun renderStep(window: OSWindow) {

        OpenGLShader.invalidateBinding()
        Texture2D.destroyTextures()
        Texture2D.invalidateBinding()
        OpenGLBuffer.invalidateBinding()

        Texture2D.freeUnusedEntries()
        AudioStream.bufferPool.freeUnusedEntries()

        setFrameNullSize(window)

        me.anno.utils.pooling.Stack.resetAll()

        resetFBStack()

        workGPUTasks(false)

        resetFBStack()

        // rendering and editor section

        Input.resetFrameSpecificKeyStates()

        workEventTasks()

        setFrameNullSize(window)

        Texture2D.resetBudget()

        check()

        Texture2D.bindTexture(GL_TEXTURE_2D, 0)

        // BlendDepth.reset()

        glDisable(GL_CULL_FACE)

        check()

        resetFBStack()

        val inst = StudioBase.instance
        if (inst != null) try {
            inst.onGameLoop(window, window.width, window.height)
        } catch (e: Exception) {
            e.printStackTrace()
            Thread.sleep(250)
        }

        resetFBStack()

        check()

    }

    @JvmStatic
    fun isGFXThread(): Boolean {
        if (glThread == null) return false
        val currentThread = Thread.currentThread()
        return currentThread == glThread
    }

    @JvmStatic
    fun checkIsGFXThread() {
        val currentThread = Thread.currentThread()
        if (currentThread != glThread) {
            if (glThread == null) {
                glThread = currentThread
                currentThread.name = "OpenGL"
            } else throw IllegalAccessException("GFX.check() called from wrong thread! Always use GFX.addGPUTask { ... }")
        }
    }

    @JvmStatic
    fun check() {
        // assumes that the first access is indeed from the OpenGL thread
        if (isDebug) {
            checkIsGFXThread()
            val error = glGetError()
            if (error != 0) {
                val title = "GLException: ${getErrorTypeName(error)}"
                throw RuntimeException(title)
            }
        }
    }

    @JvmStatic
    fun getErrorTypeName(error: Int): String {
        return when (error) {
            GL_INVALID_ENUM -> "invalid enum"
            GL_INVALID_VALUE -> "invalid value"
            GL_INVALID_OPERATION -> "invalid operation"
            GL_STACK_OVERFLOW -> throw StackOverflowError("OpenGL Exception")
            GL_STACK_UNDERFLOW -> "stack underflow"
            GL_OUT_OF_MEMORY -> throw OutOfMemoryError("OpenGL Exception")
            GL_INVALID_FRAMEBUFFER_OPERATION -> "invalid framebuffer operation"
            GL_CONTEXT_LOST -> "context lost"
            GL_TABLE_TOO_LARGE -> "table too large (arb imaging)"
            GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "incomplete attachment"
            GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "missing attachment"
            GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "incomplete draw buffer"
            GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "incomplete read buffer"
            GL_FRAMEBUFFER_UNSUPPORTED -> "framebuffer unsupported"
            GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "incomplete multisample"
            GL_FRAMEBUFFER_UNDEFINED -> "framebuffer undefined"
            else -> getName(error)
        }
    }

    @JvmStatic
    fun getName(i: Int): String {
        val constants = glConstants ?: return "$i"
        if (constants.isEmpty()) {
            discoverOpenGLNames()
        }
        return constants[i] ?: "$i"
    }

    // 1696 values in my testing
    @JvmStatic
    private val glConstants = if (Build.isShipped) null else HashMap<Int, String>(2048)

    @JvmStatic
    fun discoverOpenGLNames() {
        discoverOpenGLNames(GL46::class)
    }

    @JvmStatic
    fun discoverOpenGLNames(clazz: KClass<*>) {
        val glConstants = glConstants ?: return
        // literally 300 times faster than the Kotlin code... what is Kotlin doing???
        // 3.5 ms instead of 1000 ms
        val t2 = Engine.nanoTime
        discoverOpenGLNames(clazz.java)
        val t3 = Engine.nanoTime
        LOGGER.debug("took ${(t3 - t2) * 1e-9f}s for loading ${glConstants.size} OpenGL names")
        /*val t0 = Engine.nanoTime
        val properties = clazz.staticProperties // this call takes 1000 ms 
        val t1 = Engine.nanoTime
        println("took ${(t1 - t0) * 1e-9f}s for loading ${glConstants.size} OpenGL names")
        for (property in properties) {
            val name = property.name
            if (name.startsWith("GL_")) {
                val value = property.get()
                if (value is Int) {
                    glConstants[value] = name.substring(3)
                }
            }
        }*/
    }

    @JvmStatic
    fun discoverOpenGLNames(clazz: Class<*>) {
        val glConstants = glConstants ?: return
        val properties2 = clazz.declaredFields
        for (property in properties2) {
            val name = property.name
            if (name.startsWith("GL_")) {
                val value = property.get(null)
                if (value is Int) {
                    glConstants[value] = name.substring(3)
                }
            }
        }
        discoverOpenGLNames(clazz.superclass ?: return)
    }

}