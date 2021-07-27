package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderState.blendMode
import me.anno.gpu.RenderState.depthMode
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib.copyShader
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.Build.isDebug
import me.anno.studio.StudioBase.Companion.eventTasks
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.editorTimeDilation
import me.anno.studio.rems.RemsStudio.root
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.debug.FrameTimes
import me.anno.utils.Clock
import me.anno.utils.Maths.pow
import me.anno.utils.types.Vectors.minus
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4fc
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.ARBImaging.GL_TABLE_TOO_LARGE
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL45.GL_CONTEXT_LOST
import java.nio.FloatBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

// todo split the rendering in two parts:
// todo - without blending (no alpha, video or polygons)
// todo - with blending
// todo enqueue all objects for rendering
// todo sort blended objects by depth, if rendering with depth

object GFX : GFXBase1() {

    private val LOGGER = LogManager.getLogger(GFX::class)!!

    // for final rendering we need to use the GPU anyways;
    // so just use a static variable
    var isFinalRendering = false

    // var drawMode = ShaderPlus.DrawMode.COLOR_SQUARED
    val drawMode get() = RenderState.currentRenderer.drawMode

    var supportsAnisotropicFiltering = false
    var anisotropy = 1f

    var maxFragmentUniforms = 0
    var maxVertexUniforms = 0

    lateinit var currentCamera: Camera
    var lastTouchedCamera: Camera? = null

    var hoveredPanel: Panel? = null
    var hoveredWindow: Window? = null

    val gpuTasks = ConcurrentLinkedQueue<Task>()
    val lowPriorityGPUTasks = ConcurrentLinkedQueue<Task>()

    fun addGPUTask(w: Int, h: Int, task: () -> Unit) {
        addGPUTask(w, h, false, task)
    }

    fun addGPUTask(weight: Int, task: () -> Unit) {
        addGPUTask(weight, false, task)
    }

    fun addGPUTask(w: Int, h: Int, lowPriority: Boolean, task: () -> Unit) {
        addGPUTask((w * h / 1e5).toInt(), lowPriority, task)
    }

    fun addGPUTask(weight: Int, lowPriority: Boolean, task: () -> Unit) {
        (if (lowPriority) lowPriorityGPUTasks else gpuTasks) += weight to task
    }

    lateinit var gameInit: () -> Unit
    lateinit var gameLoop: (w: Int, h: Int) -> Boolean
    lateinit var onShutdown: () -> Unit

    val loadTexturesSync = Stack<Boolean>()

    init {
        loadTexturesSync.push(false)
    }

    var deltaX = 0
    var deltaY = 0

    var windowX = 0
    var windowY = 0
    var windowWidth = 0
    var windowHeight = 0

    val flat01 = SimpleBuffer.flat01

    val matrixBufferFBX: FloatBuffer =
        BufferUtils.createFloatBuffer(16 * 256)

    var rawDeltaTime = 0f
    var deltaTime = 0f

    var currentEditorFPS = 60f

    val startTime = System.nanoTime()
    private var lastTime = startTime

    val startDateTime = System.currentTimeMillis()

    /**
     * time since the engine started;
     * System.nanoTime() is relative to the start time of
     * the computer anyways;
     * */
    val gameTime get() = lastTime - startTime

    var editorHoverTime = 0.0

    var smoothSin = 0.0
    var smoothCos = 0.0

    var drawnTransform: Transform? = null

    val inFocus = HashSet<Panel>()
    val inFocus0 get() = inFocus.firstOrNull()

    fun requestFocus(panel: Panel?, exclusive: Boolean) {
        if (exclusive) inFocus.clear()
        if (panel != null) inFocus += panel
    }

    inline fun clip(x: Int, y: Int, w: Int, h: Int, render: () -> Unit) {
        // from the bottom to the top
        check()
        if (w < 1 || h < 1) throw java.lang.RuntimeException("w < 1 || h < 1 not allowed, got $w x $h")
        // val height = RenderState.currentBuffer?.h ?: height
        // val realY = height - (y + h)
        useFrame(x, y, w, h, false) {
            render()
        }
    }

    inline fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) = clip(x0, y0, x1 - x0, y1 - y0, render)
    inline fun clip2Save(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) {
        val w = x1 - x0
        val h = y1 - y0
        if (w > 0 && h > 0) {
            clip(x0, y0, w, h, render)
        }
    }

    inline fun clip2Dual(
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

    val windowStack = Stack<Window>()

    fun getPanelAndWindowAt(x: Float, y: Float) = getPanelAndWindowAt(x.toInt(), y.toInt())
    fun getPanelAndWindowAt(x: Int, y: Int): Pair<Panel, Window>? {
        for (root in windowStack.reversed()) {
            val panel = getPanelAt(root.panel, x, y)
            if (panel != null) return panel to root
        }
        return null
    }

    fun getPanelAt(x: Float, y: Float) = getPanelAt(x.toInt(), y.toInt())
    fun getPanelAt(x: Int, y: Int): Panel? {
        val windowStack = windowStack
        for (i in windowStack.size - 1 downTo 0) {
            val root = windowStack[i]
            val panel = getPanelAt(root.panel, x, y)
            if (panel != null) return panel
        }
        return null
    }

    fun getPanelAt(panel: Panel, x: Int, y: Int): Panel? {
        return if (panel.canBeSeen && (x - panel.x) in 0 until panel.w && (y - panel.y) in 0 until panel.h) {
            if (panel is PanelGroup) {
                val children = panel.children
                for (i in children.size - 1 downTo 0) {
                    val child = children[i]
                    val clickedByChild = getPanelAt(child, x, y)
                    if (clickedByChild != null) {
                        return clickedByChild
                    }
                }
            }
            panel
        } else null
    }

    override fun addCallbacks() {
        super.addCallbacks()
        Input.initForGLFW()
    }

    private val tmpMatrix0 = Matrix4f()
    fun applyCameraTransform(camera: Camera, time: Double, cameraTransform: Matrix4f, stack: Matrix4fArrayList) {
        val offset = camera.getEffectiveOffset(time)
        cameraTransform.translate(0f, 0f, camera.orbitRadius[time])
        val cameraTransform2 = if (offset != 0f) {
            tmpMatrix0.set(cameraTransform).translate(0f, 0f, offset)
        } else cameraTransform
        val fov = camera.getEffectiveFOV(time, offset)
        val near = camera.getEffectiveNear(time, offset)
        val far = camera.getEffectiveFar(time, offset)
        val position = cameraTransform2.transformProject(Vector3f(0f, 0f, 0f))
        val up = cameraTransform2.transformProject(Vector3f(0f, 1f, 0f)) - position
        val lookAt = cameraTransform2.transformProject(Vector3f(0f, 0f, -1f))
        stack
            .perspective(
                Math.toRadians(fov.toDouble()).toFloat(),
                windowWidth * 1f / windowHeight, near, far
            )
            .lookAt(position, lookAt, up.normalize())
        val scale = pow(1f / camera.orbitRadius[time], camera.orthographicness[time])
        if (scale != 0f && scale.isFinite()) stack.scale(scale)
    }

    fun shaderColor(shader: Shader, name: String, color: Int) {
        if (drawMode == ShaderPlus.DrawMode.ID) {
            val id = drawnTransform!!.clickId
            shader.v4(name, id.b() / 255f, id.g() / 255f, id.r() / 255f, 1f)
        } else {
            shader.v4(name, color)
        }
    }

    fun shaderColor(shader: Shader, name: String, color: Vector4fc?) {
        if (drawMode == ShaderPlus.DrawMode.ID) {
            val id = drawnTransform!!.clickId
            shader.v4(name, id.b() / 255f, id.g() / 255f, id.r() / 255f, 1f)
        } else if (color != null) {
            shader.v4(name, color)
        } else {
            shader.v4(name, 1f)
        }
    }

    fun toRadians(f: Float) = Math.toRadians(f.toDouble()).toFloat()
    fun toRadians(f: Double) = Math.toRadians(f)

    fun copy(buffer: Framebuffer) {
        Frame.bind()
        buffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        copy()
    }

    fun copy(alpha: Float) {
        check()
        val shader = copyShader.value
        shader.use()
        shader.v1("am1", 1f - alpha)
        flat01.draw(shader)
        check()
    }

    fun copy() {
        check()
        val shader = copyShader.value
        shader.use()
        shader.v1("am1", 0f)
        flat01.draw(shader)
        check()
    }

    fun copyNoAlpha() {
        check()
        blendMode.use(BlendMode.DST_ALPHA) {
            depthMode.use(DepthMode.ALWAYS) {
                val shader = copyShader.value
                shader.use()
                shader.v1("am1", 0f)
                flat01.draw(shader)
            }
        }
        check()
    }

    override fun renderStep0() {
        super.renderStep0()
        val tick = Clock()
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1) // OpenGL is evil ;), for optimizations, we might set it back
        supportsAnisotropicFiltering = GL.getCapabilities().GL_EXT_texture_filter_anisotropic
        LOGGER.info("OpenGL supports Anisotropic Filtering? $supportsAnisotropicFiltering")
        if (supportsAnisotropicFiltering) {
            val max = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)
            anisotropy = min(max, DefaultConfig["gpu.filtering.anisotropic.max", 16f])
        }
        maxVertexUniforms = glGetInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS)
        maxFragmentUniforms = glGetInteger(GL20.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS)
        LOGGER.info("Max Uniform Components: [Vertex: $maxVertexUniforms, Fragment: $maxFragmentUniforms]")
        tick.stop("render step zero")
        TextureLib.init()
        ShaderLib.init()
    }

    /**
     * time limit in seconds
     * returns whether time is left
     * */
    fun workQueue(queue: ConcurrentLinkedQueue<Task>, timeLimit: Float, all: Boolean): Boolean {
        // async work section

        // work 1/5th of the tasks by weight...

        // changing to 10 doesn't make the frame rate smoother :/
        val framesForWork = 5
        if (Thread.currentThread() == glThread) check()

        val workTodo = max(1000, queue.sumOf { it.first } / framesForWork)
        var workDone = 0
        val workTime0 = System.nanoTime()
        while (true) {
            val nextTask = queue.poll() ?: return true
            try {
                nextTask.second()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            if (Thread.currentThread() == glThread) check()
            workDone += nextTask.first
            if (workDone >= workTodo && !all) return false
            val workTime1 = System.nanoTime()
            val workTime = abs(workTime1 - workTime0) * 1e-9f
            if (workTime > timeLimit && !all) return false// too much work
        }

    }

    fun ensureEmptyStack() {
        /*if (Framebuffer.stack.size > 0) {
            /*Framebuffer.stack.forEach {
                println(it)
            }
            throw RuntimeException("Catched ${Framebuffer.stack.size} items on the Framebuffer.stack")
            exitProcess(1)*/
        }
        Framebuffer.stack.clear()*/
    }

    fun workGPUTasks(all: Boolean) {
        if (workQueue(gpuTasks, 1f / 60f, all)) {
            workQueue(lowPriorityGPUTasks, 1f / 120f, all)
        }
    }

    fun workEventTasks() {
        while (eventTasks.isNotEmpty()) {
            try {
                eventTasks.poll()!!.invoke()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun renderStep() {

        Texture2D.destroyTextures()

        ensureEmptyStack()

        workGPUTasks(false)

        ensureEmptyStack()

        // rendering and editor section

        updateTime()

        // updating the local times must be done before the events, because
        // the worker thread might have invalidated those
        updateLastLocalTime(root, editorTime)

        workEventTasks()

        Texture2D.textureBudgetUsed = 0

        check()

        Texture2D.bindTexture(GL_TEXTURE_2D, 0)

        // BlendDepth.reset()

        glDisable(GL_CULL_FACE)
        glDisable(GL_ALPHA_TEST)

        check()

        ensureEmptyStack()

        try {
            gameLoop(width, height)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ensureEmptyStack()

        check()

    }

    fun updateLastLocalTime(parent: Transform, time: Double) {
        val localTime = parent.getLocalTime(time)
        parent.lastLocalTime = localTime
        val children = parent.children
        for (i in children.indices) {
            val child = children[i]
            updateLastLocalTime(child, localTime)
        }
    }

    fun updateTime() {

        val thisTime = System.nanoTime()
        rawDeltaTime = (thisTime - lastTime) * 1e-9f
        deltaTime = min(rawDeltaTime, 0.1f)
        FrameTimes.putTime(rawDeltaTime)

        val newFPS = 1f / rawDeltaTime
        currentEditorFPS = min(currentEditorFPS + (newFPS - currentEditorFPS) * 0.05f, newFPS)
        lastTime = thisTime

        editorTime += deltaTime * editorTimeDilation
        if (editorTime <= 0.0 && editorTimeDilation < 0.0) {
            editorTimeDilation = 0.0
            editorTime = 0.0
        }

        smoothSin = sin(editorTime)
        smoothCos = cos(editorTime)

    }

    var glThread: Thread? = null

    fun checkIsNotGFXThread() {
        if (isGFXThread()) {
            throw IllegalAccessException("Cannot call from OpenGL thread")
        }
    }

    fun isGFXThread(): Boolean {
        if (glThread == null) return false
        val currentThread = Thread.currentThread()
        return currentThread == glThread
    }

    fun checkIsGFXThread() {
        val currentThread = Thread.currentThread()
        if (currentThread != glThread) {
            if (glThread == null) {
                glThread = currentThread
                currentThread.name = "OpenGL"
            } else {
                throw IllegalAccessException("GFX.check() called from wrong thread! Always use GFX.addGPUTask { ... }")
            }
        }
    }

    fun check() {
        // assumes that the first access is indeed from the OpenGL thread
        if (isDebug) {
            checkIsGFXThread()
            val error = glGetError()
            if (error != 0) {
                /*Framebuffer.stack.forEach {
                    LOGGER.info(it.toString())
                }*/
                val title = "GLException: ${getErrorTypeName(error)}"
                throw RuntimeException(title)
            }
        }
    }

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
            else -> "$error"
        }
    }

    //override fun cleanUp() {
    // destroy all used meshes, shaders, ...
    // not that dearly needed, as the memory
    // is freed anyways, when the process is killed
    // Cache.clear()
    // workGPUTasks(true)
    /*SimpleBuffer.destroy()
    CameraModel.destroy()
    ArrowModel.destroy()
    CubemapModel.destroy()
    SpeakerModel.destroy()
    SphereAxesModel.destroy()
    SphereModel.destroy()
    TextureLib.destroy()
    GL.setCapabilities(null)*/
    /*    super.cleanUp()
    }*/

}