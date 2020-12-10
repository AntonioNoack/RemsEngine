package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXx2D.drawText
import me.anno.gpu.ShaderLib.copyShader
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.Build.isDebug
import me.anno.studio.RemsStudio
import me.anno.studio.RemsStudio.editorTime
import me.anno.studio.RemsStudio.editorTimeDilation
import me.anno.studio.RemsStudio.lastT
import me.anno.studio.RemsStudio.nullCamera
import me.anno.studio.RemsStudio.root
import me.anno.studio.RemsStudio.selectedInspectable
import me.anno.studio.RemsStudio.selectedTransform
import me.anno.studio.StudioBase.Companion.eventTasks
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.FrameTimes
import me.anno.ui.input.components.PureTextInput
import me.anno.utils.*
import me.anno.utils.Maths.clamp
import me.anno.utils.Vectors.minus
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

// todo split the rendering in two parts:
// todo - without blending (no alpha, video or polygons)
// todo - with blending
// todo enqueue all objects for rendering
// todo sort blended objects by depth, if rendering with depth

// todo ffmpeg requires 100MB RAM per instance -> do we really need multiple instances, or does one work fine?
// todo or keep only a certain amount of ffmpeg instances running?

// todo gpu task priority? (low=destroy,rendering/medium=playback/high=ui)

object GFX : GFXBase1() {

    private val LOGGER = LogManager.getLogger(GFX::class)!!

    // for final rendering we need to use the GPU anyways;
    // so just use a static variable
    var isFinalRendering = false
    var drawMode = ShaderPlus.DrawMode.COLOR_SQUARED
    var supportsAnisotropicFiltering = false
    var anisotropy = 1f

    var maxFragmentUniforms = 0
    var maxVertexUniforms = 0

    var currentCamera = nullCamera

    var hoveredPanel: Panel? = null
    var hoveredWindow: Window? = null

    fun select(transform: Transform?) {
        if (selectedTransform != transform || selectedInspectable != transform) {
            selectedInspectable = transform
            selectedTransform = transform
            RemsStudio.updateSceneViews()
        }
    }

    val gpuTasks = ConcurrentLinkedQueue<Task>()
    val audioTasks = ConcurrentLinkedQueue<Task>()

    fun addAudioTask(weight: Int, task: () -> Unit) {
        // could be optimized for release...
        audioTasks += weight to task
    }

    fun addGPUTask(w: Int, h: Int, task: () -> Unit) {
        gpuTasks += (w * h / 1e5).toInt() to task
    }

    fun addGPUTask(weight: Int, task: () -> Unit) {
        gpuTasks += weight to task
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

    val matrixBufferFBX = BufferUtils.createFloatBuffer(16 * 256)

    var rawDeltaTime = 0f
    var deltaTime = 0f

    var currentEditorFPS = 60f

    val startTime = System.nanoTime()
    var lastTime = startTime

    val gameTime get() = lastTime - startTime

    var editorHoverTime = 0.0

    var smoothSin = 0.0
    var smoothCos = 0.0

    var drawnTransform: Transform? = null

    val menuSeparator = "-----"

    val inFocus = HashSet<Panel>()
    val inFocus0 get() = inFocus.firstOrNull()

    fun requestFocus(panel: Panel?, exclusive: Boolean) {
        if (exclusive) inFocus.clear()
        if (panel != null) inFocus += panel
    }

    fun clip(x: Int, y: Int, w: Int, h: Int, render: () -> Unit) {
        // from the bottom to the top
        check()
        if (w < 1 || h < 1) throw java.lang.RuntimeException("w < 1 || h < 1 not allowed, got $w x $h")
        val realY = height - (y + h)
        Frame(x, realY, w, h, false) {
            render()
        }
    }

    fun clip(size: me.anno.gpu.size.WindowSize, render: () -> Unit) = clip(size.x, size.y, size.w, size.h, render)

    fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) = clip(x0, y0, x1 - x0, y1 - y0, render)

    lateinit var windowStack: Stack<Window>

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
        for (root in windowStack.reversed()) {
            val panel = getPanelAt(root.panel, x, y)
            if (panel != null) return panel
        }
        return null
    }

    fun getPanelAt(panel: Panel, x: Int, y: Int): Panel? {
        return if (panel.canBeSeen && (x - panel.x) in 0 until panel.w && (y - panel.y) in 0 until panel.h) {
            if (panel is PanelGroup) {
                for (child in panel.children.reversed()) {
                    val clickedByChild = getPanelAt(child, x, y)
                    if (clickedByChild != null) {
                        return clickedByChild
                    }
                }
            }
            panel
        } else null
    }

    fun requestExit() {
        glfwSetWindowShouldClose(window, true)
    }

    override fun addCallbacks() {
        super.addCallbacks()
        Input.initForGLFW()
    }

    fun applyCameraTransform(camera: Camera, time: Double, cameraTransform: Matrix4f, stack: Matrix4fArrayList) {
        val offset = camera.getEffectiveOffset(time)
        val cameraTransform2 = if (offset != 0f) {
            Matrix4f(cameraTransform).translate(0f, 0f, offset)
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
    }

    fun shaderColor(shader: Shader, name: String, color: Vector4f) {
        if (drawMode == ShaderPlus.DrawMode.ID) {
            val id = drawnTransform!!.clickId
            shader.v4(name, id.b() / 255f, id.g() / 255f, id.r() / 255f, 1f)
        } else {
            shader.v4(name, color.x, color.y, color.z, color.w)
        }
    }

    fun toRadians(f: Float) = Math.toRadians(f.toDouble()).toFloat()
    fun toRadians(f: Double) = Math.toRadians(f)

    fun copy(alpha: Float) {
        check()
        val shader = copyShader
        shader.v1("am1", 1f-alpha)
        flat01.draw(shader)
        check()
    }

    fun copy() {
        check()
        val shader = copyShader
        shader.v1("am1", 0f)
        flat01.draw(shader)
        check()
    }

    fun copyNoAlpha() {
        check()
        BlendDepth(BlendMode.DST_ALPHA, false) {
            val shader = copyShader
            shader.v1("am1", 0f)
            flat01.draw(shader)
        }
        check()
    }

    override fun renderStep0() {
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1) // opengl is evil ;), for optimizations, we might set it back
        supportsAnisotropicFiltering = GL.getCapabilities().GL_EXT_texture_filter_anisotropic
        LOGGER.info("OpenGL supports Anisotropic Filtering? $supportsAnisotropicFiltering")
        if (supportsAnisotropicFiltering) {
            val max = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)
            anisotropy = min(max, DefaultConfig["gpu.filtering.anisotropic.max", 16f])
        }
        maxVertexUniforms = glGetInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS)
        maxFragmentUniforms = glGetInteger(GL20.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS)
        LOGGER.info("Max Uniform Components: [Vertex: $maxVertexUniforms, Fragment: $maxFragmentUniforms]")
        TextureLib.init()
        ShaderLib.init()
        setIcon()
    }

    fun workQueue(queue: ConcurrentLinkedQueue<Task>) {
        // async work section

        // work 1/5th of the tasks by weight...

        // changing to 10 doesn't make the frame rate smoother :/
        val framesForWork = 5

        val workTodo = max(1000, queue.sumBy { it.first } / framesForWork)
        var workDone = 0
        val workTime0 = System.nanoTime()
        while (true) {
            val nextTask = queue.poll() ?: break
            nextTask.second()
            workDone += nextTask.first
            if (workDone >= workTodo) break
            val workTime1 = System.nanoTime()
            val workTime = abs(workTime1 - workTime0) * 1e-9f
            if (workTime * 60f > 1f) break // too much work
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

    fun workGPUTasks() {
        workQueue(gpuTasks)
    }

    fun workEventTasks() {
        while (eventTasks.isNotEmpty()) {
            try {
                eventTasks.poll()!!.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun renderStep() {

        ensureEmptyStack()

        workGPUTasks()

        ensureEmptyStack()

        // rendering and editor section

        updateTime()

        // updating the local times must be done before the events, because
        // the worker thread might have invalidated those
        updateLastLocalTime(root, editorTime)

        workEventTasks()

        Texture2D.textureBudgetUsed = 0

        check()

        glBindTexture(GL_TEXTURE_2D, 0)

        BlendDepth.reset()

        glDisable(GL_CULL_FACE)
        glDisable(GL_ALPHA_TEST)

        check()

        ensureEmptyStack()

        gameLoop(width, height)

        ensureEmptyStack()

        check()

    }

    fun updateLastLocalTime(parent: Transform, time: Double) {
        val localTime = parent.getLocalTime(time)
        parent.lastLocalTime = localTime
        parent.children.forEach { child ->
            updateLastLocalTime(child, localTime)
        }
    }

    fun updateTime() {

        val thisTime = System.nanoTime()
        rawDeltaTime = (thisTime - lastTime) * 1e-9f
        deltaTime = min(rawDeltaTime, 0.1f)
        FrameTimes.putValue(rawDeltaTime)

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

    fun askName(
        x: Int, y: Int,
        title: String,
        actionName: String,
        getColor: (String) -> Int,
        callback: (String) -> Unit
    ) {

        lateinit var window: Window
        fun close() {
            windowStack.remove(window)
            window.destroy()
        }

        val style = style.getChild("menu")
        val panel = PureTextInput(style)
        panel.placeholder = title
        panel.setEnterListener {
            callback(panel.text)
            close()
        }
        panel.setChangeListener {
            panel.textColor = getColor(it)
        }

        val submit = ButtonPanel(actionName, style)
            .setSimpleClickListener {
                callback(panel.text)
                close()
            }

        val cancel = ButtonPanel("Cancel", style)
            .setSimpleClickListener { close() }

        val buttons = PanelListX(style)
        buttons += cancel
        buttons += submit

        window = openMenuComplex2(x, y, title, listOf(panel, buttons))!!

    }

    fun openMenuComplex(
        x: Int,
        y: Int,
        title: String,
        options: List<Pair<String, (button: MouseButton, isLong: Boolean) -> Boolean>>
    ) {

        if (options.isEmpty()) return
        val style = style.getChild("menu")

        lateinit var window: Window
        fun close() {
            windowStack.remove(window)
            window.destroy()
        }

        val list = ArrayList<Panel>()

        val padding = 4
        for ((index, element) in options.withIndex()) {
            val (name, action) = element
            if (name == menuSeparator) {
                if (index != 0) {
                    list += SpacePanel(0, 1, style)
                }
            } else {
                val buttonView = TextPanel(name, style)
                buttonView.setOnClickListener { _, _, button, long ->
                    if (action(button, long)) {
                        close()
                    }
                }
                buttonView.enableHoverColor = true
                buttonView.padding.left = padding
                buttonView.padding.right = padding
                list += buttonView
            }
        }

        window = openMenuComplex2(x, y, title, list)!!

    }

    fun openMenuComplex2(
        x: Int,
        y: Int,
        title: String,
        panels: List<Panel>
    ): Window? {

        loadTexturesSync.push(true) // to calculate the correct size, which is needed for correct placement
        if (panels.isEmpty()) return null
        val style = style.getChild("menu")
        val list = PanelListY(style)
        list += WrapAlign.LeftTop
        val container = ScrollPanelY(list, Padding(1), style, AxisAlignment.MIN)
        container += WrapAlign.LeftTop
        lateinit var window: Window

        val padding = 4
        if (title.isNotEmpty()) {
            val titlePanel = TextPanel(title, style)
            titlePanel.padding.left = padding
            titlePanel.padding.right = padding
            list += titlePanel
            list += SpacePanel(0, 1, style)
        }

        for (panel in panels) {
            list += panel
        }

        val maxWidth = max(300, GFX.width)
        val maxHeight = max(300, GFX.height)
        container.calculateSize(maxWidth, maxHeight)
        container.applyPlacement(min(container.minW, maxWidth), min(container.minH, maxHeight))

        val wx = clamp(x, 0, max(GFX.width - container.w, 0))
        val wy = clamp(y, 0, max(GFX.height - container.h, 0))

        window = Window(container, false, wx, wy)
        windowStack.add(window)
        loadTexturesSync.pop()

        return window

    }

    fun openMenuComplex(
        x: Float,
        y: Float,
        title: String,
        options: List<Pair<String, (button: MouseButton, isLong: Boolean) -> Boolean>>,
        delta: Int = 10
    ) {
        openMenuComplex(x.roundToInt() - delta, y.roundToInt() - delta, title, options)
    }

    fun openMenu(options: List<Pair<String, () -> Any>>) {
        openMenu(mouseX, mouseY, "", options)
    }

    fun openMenu(title: String, options: List<Pair<String, () -> Any>>) {
        openMenu(mouseX, mouseY, title, options)
    }

    fun openMenu(x: Int, y: Int, title: String, options: List<Pair<String, () -> Any>>, delta: Int = 10) {
        return openMenu(x.toFloat(), y.toFloat(), title, options, delta)
    }

    fun openMenu(x: Float, y: Float, title: String, options: List<Pair<String, () -> Any>>, delta: Int = 10) {
        openMenuComplex(x.roundToInt() - delta, y.roundToInt() - delta, title, options.map { (key, value) ->
            Pair(key, { b: MouseButton, _: Boolean ->
                if (b.isLeft) {
                    value(); true
                } else false
            })
        })
    }

    var glThread: Thread? = null
    fun check() {
        if (isDebug) {
            val currentThread = Thread.currentThread()
            if (currentThread != glThread) {
                if (glThread == null) {
                    glThread = currentThread
                    currentThread.name = "OpenGL"
                } else {
                    throw RuntimeException("GFX.check() called from wrong thread! Always use GFX.addGPUTask { ... }")
                }
            }
            val error = glGetError()
            if (error != 0) {
                /*Framebuffer.stack.forEach {
                    LOGGER.info(it.toString())
                }*/
                throw RuntimeException(
                    "GLException: ${when (error) {
                        1280 -> "invalid enum"
                        1281 -> "invalid value"
                        1282 -> "invalid operation"
                        1283 -> "stack overflow"
                        1284 -> "stack underflow"
                        1285 -> "out of memory"
                        1286 -> "invalid framebuffer operation"
                        else -> "$error"
                    }}"
                )
            }
        }
    }

    fun msg(title: String) {
        openMenu(listOf(title to {}))
    }

    fun ask(question: String, onYes: () -> Unit) {
        openMenu(mouseX, mouseY, question, listOf(
            "Yes" to onYes,
            "No" to {}
        ))
    }

    fun ask(question: String, onYes: () -> Unit, onNo: () -> Unit) {
        openMenu(
            mouseX, mouseY, question, listOf(
                "Yes" to onYes,
                "No" to onNo
            )
        )
    }

    fun showFPS() {
        val x0 = max(0, GFX.width - FrameTimes.width)
        val y0 = max(0, GFX.height - FrameTimes.height)
        FrameTimes.place(x0, y0, FrameTimes.width, FrameTimes.height)
        FrameTimes.draw()
        loadTexturesSync.push(true)
        drawText(
            x0 + 1, y0 + 1,
            "Consolas", 12f, false, false,
            "${currentEditorFPS.f1()}, min: ${(1f / FrameTimes.maxValue).f1()}",
            FrameTimes.textColor,
            FrameTimes.backgroundColor,
            -1
        )
        loadTexturesSync.pop()
    }

}