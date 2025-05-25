package me.anno.gpu

import me.anno.Build.isDebug
import me.anno.Engine.shutdown
import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase
import me.anno.engine.Events
import me.anno.engine.Events.addEvent
import me.anno.engine.NamedTask
import me.anno.engine.WindowRenderFlags
import me.anno.gpu.GFX.checkIsGFXThread
import me.anno.gpu.GFX.focusedWindow
import me.anno.gpu.GLNames.getErrorTypeName
import me.anno.gpu.RenderDoc.loadRenderDoc
import me.anno.gpu.RenderStep.renderStep
import me.anno.gpu.debug.LWJGLDebugCallback
import me.anno.gpu.debug.OpenGLDebug.getDebugSeverityName
import me.anno.gpu.debug.OpenGLDebug.getDebugSourceName
import me.anno.gpu.debug.OpenGLDebug.getDebugTypeName
import me.anno.gpu.framebuffer.NullFramebuffer.setFrameNullSize
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.input.GLFWListeners
import me.anno.input.Input
import me.anno.input.Input.isMouseLocked
import me.anno.input.Input.mouseLockWindow
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.SECONDS_TO_MILLIS
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.input.InputPanel
import me.anno.utils.Clock
import me.anno.utils.Color
import me.anno.utils.GFXFeatures
import me.anno.utils.OS
import me.anno.utils.OS.res
import me.anno.utils.OSFeatures
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.structures.lists.Lists.all2
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.none2
import org.apache.logging.log4j.LogManager.getLogger
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL46C.GL_MAX_SAMPLES
import org.lwjgl.opengl.GL46C.GL_MULTISAMPLE
import org.lwjgl.opengl.GL46C.glEnable
import org.lwjgl.opengl.GL46C.glGetInteger
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.opengl.GLUtil
import org.lwjgl.opengl.KHRDebug
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

/**
 * Manages creating windows, and a LWJGL/OpenGL context
 *
 * todo rebuild and recompile the glfw driver, which handles the touch input, so the input can be assigned to the window
 * (e.g., add 1 to the pointer)
 */
object WindowManagement {

    @JvmStatic
    private val LOGGER = getLogger(WindowManagement::class)

    @JvmStatic
    private val windows get() = GFX.windows

    @JvmStatic
    val glfwTasks: Queue<NamedTask> = ConcurrentLinkedQueue()

    /**
     * must be used when calling GLFW, because it's not thread-safe
     * */
    @JvmField
    val glfwLock = Any()

    /**
     * must be used to prevent RenderDoc crashes when the engine shuts down
     * */
    @JvmField
    val openglLock = Any()

    @JvmField
    var destroyed = false

    @JvmField
    var capabilities: GLCapabilities? = null

    @JvmField
    var usesRenderDoc = false

    @JvmField
    var useSeparateGLFWThread = true

    @JvmStatic
    fun run(title: String) {
        try {
            loadRenderDoc()
            val clock = initLWJGL()
            val window0 = createFirstWindow(title, clock)
            runLoops(window0)
            shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            LOGGER.info("glfwTerminate")
            GLFW.glfwTerminate()
        }
    }

    private fun createFirstWindow(title: String, clock: Clock): OSWindow {
        val window = if (GFX.windows.isEmpty() && !GFX.someWindow.shouldClose) {
            val window = GFX.someWindow // first window is being reused
            window.title = title
            window
        } else OSWindow(title)
        return createWindow(window, clock)
    }

    private fun shutdown() {
        val clock = Clock(LOGGER)
        synchronized(openglLock) {
            // wait for the last frame to be finished,
            // before we actually destroy the window and its framebuffer
            destroyed = true
            GFX.glThread = null // no longer valid after closing all windows
            clock.stop("Finishing last frame", 0.0)
        }
        synchronized(glfwLock) {
            val size = windows.size
            for (index in 0 until size) {
                close(windows.getOrNull(index) ?: break)
            }
            windows.clear()
            clock.stop("Closing $size window(s)", 0.0)
        }
    }

    @JvmStatic
    fun initLWJGL(): Clock {
        LOGGER.info("Using LWJGL Version " + Version.getVersion())
        val tick = Clock(LOGGER)
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err))
        tick.stop("Error callback")
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        tick.stop("GLFW initialization")
        setWindowFlags()
        if (false) {
            // things go wrong when using the following; we're probably using something old...
            // I also don't really want to exclude any GPU
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0)
        }
        setDebugFlag()
        // removes scaling options -> how could we replace them?
        // glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        // tick.stop("window hints");// 0s
        return tick
    }

    fun setWindowFlags() {
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
    }

    var hasOpenGLDebugContext = false

    fun setDebugFlag() {
        if (isDebug) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)
            hasOpenGLDebugContext = true
        }
    }

    @JvmStatic
    fun addCallbacks(window: OSWindow) {
        window.addCallbacks()
        GLFWListeners.registerCallbacks(window)
    }

    @JvmStatic
    fun createWindow(title: String, panel: Panel): OSWindow {
        if (!GFXFeatures.canOpenNewWindows) return windows.first()
        val window = OSWindow(title)
        createWindow(window, null)
        window.windowStack.push(panel)
        return window
    }

    @JvmStatic
    fun createWindow(title: String, panel: Panel, width: Int, height: Int): OSWindow {
        if (!GFXFeatures.canOpenNewWindows) return windows.first()
        val window = OSWindow(title)
        window.width = width
        window.height = height
        createWindow(window, null)
        window.windowStack.push(panel)
        return window
    }

    @JvmStatic
    fun createWindow(instance: OSWindow, tick: Clock?): OSWindow {
        synchronized(glfwLock) {
            createBlankWindow(instance)
            windows.add(instance)
            tick?.stop("Create window")
            addCallbacks(instance)
            tick?.stop("Adding callbacks")
            centerWindowOnScreen(instance)
            updateActualSize(instance)

            tick?.stop("Window position")
            GLFW.glfwSetWindowTitle(instance.pointer, instance.title)

            // tick.stop("window title"); // 0s
            GLFW.glfwShowWindow(instance.pointer)
            tick?.stop("Show window")
            setIcon(instance)
            tick?.stop("Setting icon")
            updateMousePosition(instance)
        }
        return instance
    }

    private fun createBlankWindow(instance: OSWindow): Long {
        val sharedWindow = findSharedWindow()
        val window = GLFW.glfwCreateWindow(instance.width, instance.height, instance.title, 0L, sharedWindow)
        assertNotEquals(0L, window, "Failed to create GLFW window")
        instance.pointer = window
        return window
    }

    /**
     * windows sharing a context means we only need to support a single GFX context at a time
     * */
    private fun findSharedWindow(): Long {
        val firstWindow = windows.firstOrNull { it.pointer != 0L } ?: return 0L
        return firstWindow.pointer
    }

    private fun centerWindowOnScreen(instance: OSWindow) {
        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()) ?: return
        val alignment = AxisAlignment.CENTER
        GLFW.glfwSetWindowPos(
            instance.pointer,
            alignment.getOffset(videoMode.width(), instance.width),
            alignment.getOffset(videoMode.height(), instance.height)
        )
    }

    private fun updateActualSize(instance: OSWindow) {
        val w = intArrayOf(0)
        val h = intArrayOf(1)
        GLFW.glfwGetFramebufferSize(instance.pointer, w, h)
        instance.width = w[0]
        instance.height = h[0]
    }

    private fun updateMousePosition(instance: OSWindow) {
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        GLFW.glfwGetCursorPos(instance.pointer, x, y)
        instance.mouseX = x[0].toFloat()
        instance.mouseY = y[0].toFloat()
    }

    @JvmStatic
    private var neverStarveWindows = DefaultConfig["ux.neverStarveWindows", false]

    @JvmStatic
    fun prepareForRendering(tick: Clock?) {
        capabilities = GL.createCapabilities()
        GFXState.newSession()
        tick?.stop("OpenGL initialization")
        GLUtil.setupDebugMessageCallback(LWJGLDebugCallback)
        tick?.stop("Debugging Setup")
        // render first frames = render logo
        // the engine will still be loading,
        // so it has to be a still image
        // alternatively we could play a small animation
        GFX.maxSamples = max(1, glGetInteger(GL_MAX_SAMPLES))
        GFX.check()
        // nice features :3
        // cause issues in FrameGen -> not enabled everywhere
        // glEnable(GL_LINE_SMOOTH)
        // glEnable(GL_POINT_SMOOTH)
        glEnable(GL_MULTISAMPLE)
        glEnable(GL_DEPTH_TEST)
        checkIsGFXThread()
    }

    @JvmField
    var numLogoFrames = 2

    @JvmStatic
    fun runRenderLoop0(window0: OSWindow) {
        LOGGER.info("Running RenderLoop")
        val tick = Clock(LOGGER)
        window0.makeCurrent()
        window0.forceUpdateVsync()
        tick.stop("Make context current + vsync")
        prepareForRendering(tick)
        setFrameNullSize(window0)
        val timeout = Time.nanoTime + 1000 * MILLIS_TO_NANOS
        val logoFrames = numLogoFrames
        var frameIdx = 0
        while (Time.nanoTime < timeout) {
            Logo.drawLogo(window0.width, window0.height)
            GFX.check()
            GLFW.glfwSwapBuffers(window0.pointer)
            val err = GL46C.glGetError()
            if (err != 0) {
                val errName = getErrorTypeName(err)
                LOGGER.warn("Got awkward OpenGL error from calling glfwSwapBuffers: $errName")
            }
            if (Logo.hasMesh) {
                frameIdx++
                if (frameIdx >= logoFrames) {
                    break
                }
            } else Thread.sleep(10)
        }
        Logo.destroy()
        tick.stop("Render frame zero")
        if (isDebug && (LOGGER.isInfoEnabled() || LOGGER.isWarnEnabled())) {
            setupDebugCallback()
        }
        init2(tick)
    }

    private fun setupDebugCallback() {
        GL46C.glDebugMessageCallback({ source: Int, type: Int, id: Int, severity: Int, _: Int, msgPtr: Long, _: Long ->
            handleDebugCallback(source, type, id, severity, msgPtr)
        }, 0)
        glEnable(KHRDebug.GL_DEBUG_OUTPUT)
    }

    private fun handleDebugCallback(source: Int, type: Int, id: Int, severity: Int, msgPtr: Long) {
        var msg = if (msgPtr != 0L) MemoryUtil.memUTF8(msgPtr) else null
        if (msg != null && "will use VIDEO memory as the source for buffer object operations" !in msg &&
            "detailed info: Based on the usage hint and actual usage," !in msg &&
            // this could be fixed by creating a shader for each attribute-configuration
            // todo we want to be able to use our own buffer formats anyway, so somehow implement it that we load/create the shader based on the actually used layout
            // todo after that's done, disable this check (?)
            "Program/shader state performance warning: Vertex shader in program" !in msg &&
            id != GFXState.PUSH_DEBUG_GROUP_MAGIC // spam that we can ignore
        ) {
            msg += "" +
                    ", source: " + getDebugSourceName(source) +
                    ", type: " + getDebugTypeName(type) + // mmh, not correct, at least for my simple sample I got a non-mapped code
                    ", id: " + getErrorTypeName(id) +
                    ", severity: " + getDebugSeverityName(severity)
            if (type == KHRDebug.GL_DEBUG_TYPE_OTHER) LOGGER.info(msg)
            else LOGGER.warn(msg)
        }
    }

    @JvmStatic
    fun init2(tick: Clock?) {
        GFX.setupBasics(tick)
        GFX.check()
        tick?.stop("Render step zero")
        EngineBase.instance?.gameInit()
        tick?.stop("Game Init")
    }

    @JvmStatic
    fun runRenderLoop() {
        var lastTime = Time.nanoTime - 60 * MILLIS_TO_NANOS
        while (shouldContinueUpdates()) {
            val currTime = Time.nanoTime
            Time.updateTime(currTime, lastTime)
            renderFrame()
            lastTime = currTime
        }
        EngineBase.instance?.onShutdown()
    }

    @JvmStatic
    fun runLoops(window0: OSWindow) {

        Thread.currentThread().name = "GLFW"

        // Start new thread to have the OpenGL context current in and that does the rendering.
        if (useSeparateGLFWThread) {
            thread(name = "OpenGL") {
                GFX.glThread = Thread.currentThread()
                runRenderLoop0(window0)
                runRenderLoop()
            }
            runWindowUpdateLoop()
        } else {
            GFX.glThread = Thread.currentThread()
            runRenderLoop0(window0)
            runRenderLoopWithWindowUpdates()
            EngineBase.instance?.onShutdown()
        }
    }

    private fun runWindowUpdateLoop() {
        var lastTime = Time.nanoTime
        while (shouldContinueUpdates()) {
            updateWindows()
            val currTime = Time.nanoTime
            lastTime = if (currTime - lastTime < MILLIS_TO_NANOS) {
                // reduce load on CPU if the method call is very lightweight
                Thread.sleep(1)
                Time.nanoTime
            } else currTime
        }
    }

    private fun runRenderLoopWithWindowUpdates() {
        var lastTime = Time.nanoTime - 60 * MILLIS_TO_NANOS
        while (shouldContinueUpdates()) {
            val currTime = Time.nanoTime
            Time.updateTime(currTime, lastTime)
            updateWindows()
            renderFrame()
            lastTime = currTime
        }
    }

    private fun shouldContinueUpdates(): Boolean {
        return !destroyed && !shutdown && !windows.all2 { it.shouldClose }
    }

    @JvmField
    var lastTime = Time.nanoTime

    @JvmStatic
    fun renderFrame() {
        val time = Time.nanoTime
        RenderStep.beforeRenderSteps()
        if (!renderWindows(time)) {
            keepProcessingHiddenWindows()
        }
        closeWindowsIfTheyShouldBeClosed()
        if (mayIdle1()) {
            waitIfIdle()
        }
    }

    private fun mayIdle1(): Boolean {
        return mayIdle && OSFeatures.canSleep
    }

    private fun closeWindowsIfTheyShouldBeClosed() {
        val windows = windows
        for (index in windows.indices) {
            val window = windows.getOrNull(index) ?: break
            if (window.shouldClose) close(window)
        }
    }

    private fun shouldRenderWindow(window: OSWindow, time: Long): Boolean {
        return window.isInFocus ||
                window.hasActiveMouseTargets() ||
                neverStarveWindows ||
                abs(window.lastUpdate - time) * WindowRenderFlags.idleFPS > SECONDS_TO_NANOS
    }

    private fun renderWindows(time: Long): Boolean {
        var workedTasks = false
        val windows = windows
        for (index in windows.indices) {
            val window = windows.getOrNull(index) ?: break
            if (shouldRenderWindow(window, time)) {
                window.lastUpdate = time
                // this is hopefully ok (calling it async to other glfw stuff)
                if (window.makeCurrent()) {
                    renderWindow(window)
                    workedTasks = true
                }
            }
        }
        return workedTasks
    }

    private fun renderWindow(window: OSWindow) {
        synchronized(openglLock) {
            if (destroyed) return
            GFX.activeWindow = window
            renderStep(window, true)
        }
        synchronized(glfwLock) {
            if (destroyed) return
            GLFW.glfwWaitEventsTimeout(0.0) // needed?
            GLFW.glfwSwapBuffers(window.pointer)
            // works in reducing input latency by 1 frame 😊
            // https://www.reddit.com/r/GraphicsProgramming/comments/tkpdhd/minimising_input_latency_in_opengl/
            if (DefaultConfig["gpu.glFinishForLatency", OS.isWindows]) {
                GL46C.glFinish()
            }
            window.updateVsync()
        }
    }

    /**
     * to keep processing, e.g., for Rem's Studio
     * */
    private fun keepProcessingHiddenWindows() {
        val window = windows.getOrNull(0)
        if (window != null && window.makeCurrent()) {
            synchronized(openglLock) {
                GFX.activeWindow = window
                renderStep(window, false)
            }
        }
    }

    private fun waitIfIdle() {
        val isIdle = windows.isNotEmpty() &&
                windows.none2 { (it.isInFocus && !it.isMinimized) || it.hasActiveMouseTargets() }
        val maxFPS = if (isIdle) WindowRenderFlags.idleFPS else WindowRenderFlags.maxFPS
        if (maxFPS > 0) {
            // enforce X fps, because we don't need more
            // and don't want to waste energy
            val currentTime = Time.nanoTime
            val waitingTime = SECONDS_TO_MILLIS / max(1, maxFPS) - (currentTime - lastTime) / MILLIS_TO_NANOS
            if (waitingTime > 0) try {
                // wait does not work, causes IllegalMonitorState exception
                Thread.sleep(waitingTime)
            } catch (_: InterruptedException) {
            }
            lastTime = Time.nanoTime
        }
    }

    @JvmField
    var mayIdle = true

    @JvmField
    var lastTrapWindow: OSWindow? = null

    private fun handleClose(window: OSWindow) {
        if (shouldCloseWindowImmediately(window)) {
            window.requestClose()
        } else {
            deferClosingWindow(window)
        }
    }

    private fun shouldCloseWindowImmediately(window: OSWindow): Boolean {
        val ws = window.windowStack
        return ws.isEmpty() ||
                DefaultConfig["window.close.directly", false] ||
                ws.last().isAskingUserAboutClosing ||
                window.shouldClose
    }

    private fun deferClosingWindow(window: OSWindow) {
        val ws = window.windowStack
        GLFW.glfwSetWindowShouldClose(window.pointer, false)
        addEvent {
            ask(
                ws, NameDesc("Close %1?", "", "ui.closeProgram")
                    .with("%1", window.title),
                window::requestClose
            )?.isAskingUserAboutClosing = true
            ws.peek()?.setAcceptsClickAway(false)
        }
    }

    @JvmStatic
    fun updateWindows() {

        Events.workTasks(glfwTasks)

        for (index in 0 until windows.size) {
            val window = windows[index]
            if (!window.shouldClose) {
                if (GLFW.glfwWindowShouldClose(window.pointer)) {
                    handleClose(window)
                } else {
                    // update small stuff, that may need to be updated;
                    // currently only the title
                    window.updateTitle()
                }
            }
        }

        val trapMouseWindow = mouseLockWindow
        if (trapMouseWindow != null && !trapMouseWindow.shouldClose && isMouseLocked) {
            if (lastTrapWindow == null) {
                LOGGER.debug("Locking Mouse")
                GLFW.glfwSetInputMode(trapMouseWindow.pointer, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
                lastTrapWindow = trapMouseWindow
            }
            /*val x = trapWindow.mouseX
            val y = trapWindow.mouseY
            val centerX = trapWindow.width * 0.5
            val centerY = trapWindow.height * 0.5
            val dx = x - centerX
            val dy = y - centerY
            if (dx * dx + dy * dy > trapMouseRadius * trapMouseRadius) {
                GLFW.glfwSetCursorPos(trapWindow.pointer, centerX, centerY)
            }*/
        } else if (lastTrapWindow?.shouldClose == false) {
            GLFW.glfwSetInputMode(lastTrapWindow!!.pointer, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
            LOGGER.debug("Unlocking Mouse")
            lastTrapWindow = null
        } else if (Input.mouseHasMoved && Input.mouseKeysDown.isNotEmpty() && DefaultConfig["ui.enableMouseJumping", true]) {
            // when dragging a value (dragging + selected.isInput), and cursor is on the border, respawn it in the middle of the screen
            // for that, the cursor should be within 2 frames of reaching the border...
            // for that, we need the last mouse movement :)
            val window = focusedWindow
            if (window != null) {
                val margin = 10f
                if (window.mouseX !in margin..window.width - margin || window.mouseY !in margin..window.height - margin) {
                    val inFocus = window.windowStack.inFocus
                    if (inFocus.any2 { p -> p.anyInHierarchy { h -> h is InputPanel<*> && h.wantsMouseTeleport() } }) {
                        val centerX = window.width * 0.5
                        val centerY = window.height * 0.5
                        synchronized(window) {
                            GLFW.glfwSetCursorPos(window.pointer, centerX, centerY)
                            window.mouseX = centerX.toFloat()
                            window.mouseY = centerY.toFloat()
                            window.lastMouseTeleport = Time.nanoTime + 5_000_000 // 5ms safety delay
                        }
                    }
                }
            }
        }

        for (index in windows.indices) {
            val window = windows[index]
            if (!window.shouldClose && window.updateMouseTarget()) break
        }

        // glfwWaitEventsTimeout() without args only terminates, if keyboard or mouse state is changed
        GLFW.glfwWaitEventsTimeout(0.0)
    }

    @JvmStatic
    fun setIcon(instance: OSWindow) {
        val src = res.getChild("icon.png")
        val srcImage = ImageCache[src, false]
        if (srcImage != null) {
            setIcon(instance.pointer, srcImage)
        }
    }

    @JvmStatic
    fun setIcon(window: Long, srcImage: Image) {
        val (image, pixels) = imageToGLFW(srcImage)
        val buffer = GLFWImage.malloc(1)
        buffer.put(0, image)
        GLFW.glfwSetWindowIcon(window, buffer)
        buffer.free()
        ByteBufferPool.free(pixels)
    }

    @JvmStatic
    fun imageToGLFW(srcImage: Image): Pair<GLFWImage, ByteBuffer> {
        val image = GLFWImage.malloc()
        val w = srcImage.width
        val h = srcImage.height
        val pixels = ByteBufferPool.allocateDirect(w * h * 4)
        val pixelsAsInt = pixels.asIntBuffer()
        val srcIntImage = srcImage.asIntImage()
        for (y in 0 until h) {
            pixelsAsInt.put(srcIntImage.data, srcIntImage.getIndex(0, y), w)
        }
        pixelsAsInt.flip()
        if (pixels.order() == ByteOrder.BIG_ENDIAN) {
            Color.convertARGB2RGBA(pixelsAsInt)
        } else {
            Color.convertARGB2ABGR(pixelsAsInt)
        }
        image.set(w, h, pixels)
        return image to pixels
    }

    @JvmStatic
    fun close(window: OSWindow) {
        synchronized(glfwLock) {
            if (window.pointer != 0L) {
                GLFW.glfwDestroyWindow(window.pointer)
                window.pointer = 0L
            }
        }
    }
}