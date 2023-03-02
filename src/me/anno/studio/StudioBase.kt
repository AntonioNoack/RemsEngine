package me.anno.studio

import me.anno.Build
import me.anno.Engine
import me.anno.Engine.projectName
import me.anno.audio.openal.AudioManager
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.Renderer
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.Language
import me.anno.language.translation.Dict
import me.anno.maths.Maths
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.Tooltips
import me.anno.ui.debug.FrameTimings
import me.anno.ui.dragging.IDraggable
import me.anno.utils.Clock
import me.anno.utils.Logging
import me.anno.utils.OS
import me.anno.utils.types.Strings.addSuffix
import me.anno.utils.types.Strings.filterAlphaNumeric
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.PriorityBlockingQueue
import kotlin.math.min
import kotlin.math.roundToInt

// which parts of the engine can be split into modules? would be good for compile times and for shipping small, compact apps (e.g. on the web)
// todo generate dependency graph visualization/computation? :)

/**
 * base class for UI setup;
 * manages audio, graphics, settings, game loop, ui
 * */
abstract class StudioBase(
    val needsAudio: Boolean,
    val title: String,
    val configName: String,
    val versionNumber: Int,
    versionSuffix: String? = null
) {

    constructor(needsAudio: Boolean, title: String, versionNumber: Int, versionSuffix: String? = null) :
            this(needsAudio, title, filterAlphaNumeric(title), versionNumber, versionSuffix)

    init {
        LOGGER.info("Process ID: ${OS.getProcessID()}")
    }

    /**
     * version of program as string,
     * x.yy.zz
     * */
    val versionName =
        addSuffix("${versionNumber / 10000}.${(versionNumber / 100) % 100}.${versionNumber % 100}", versionSuffix)

    open fun loadConfig() {}

    abstract fun createUI()

    open fun onGameLoopStart() {}
    open fun onGameLoopEnd() {}

    open fun onGameInit() {
    }

    open fun onGameClose() {}

    open fun openHistory() {}

    open fun save() {}

    open fun getDefaultFileLocation(): FileReference = InvalidRef

    open fun importFile(file: FileReference) {
        LOGGER.warn("Ignored $file")
    }

    open fun getPersistentStorage(): FileReference {
        return OS.documents
    }

    val startClock = Clock()
    fun tick(name: String) {
        startClock.stop(name)
    }

    var showTutorialKeys
        get() = DefaultConfig["ui.tutorial.showKeys", true]
        set(value) {
            DefaultConfig["ui.tutorial.showKeys"] = value
        }

    var showFPS
        get() = DefaultConfig["debug.ui.showFPS", Build.isDebug]
        set(value) {
            DefaultConfig["debug.ui.showFPS"] = value
        }

    var gfxSettings = GFXSettings.LOW
        set(value) {
            field = value
            DefaultConfig["editor.gfx"] = value.id
            DefaultConfig.putAll(value.data)
        }

    open fun gameInit() {

        GFX.check()

        tick("Pre game init")

        onGameInit()

        tick("Game init")

        if (needsAudio) {
            AudioManager.startRunning()
            tick("Audio manager")
        }

        Cursor.init()

        ExtensionLoader.load()

        createUI()

    }

    open fun setupNames() {
        GFX.windows.firstOrNull()?.title = title
        projectName = configName
    }

    open fun run(
        runGraphics: Boolean = !OS.isWeb && !OS.isAndroid
    ) {

        instance = this

        setupNames()

        tick("run")

        Logging.setup()

        tick("logging")

        loadConfig()

        tick("config")

        if (runGraphics) {
            GFXBase.run()
        }

    }

    var hoveredPanel: Panel? = null
    var hoveredWindow: Window? = null

    private var lastMouseX = 0f
    private var lastMouseY = 0f

    open fun onShutdown() {
        shallStop = true
        ExtensionLoader.unload()
        Cursor.destroy()
        Engine.requestShutdown()
        onGameClose()
    }

    open fun onGameLoop(window: OSWindow, w: Int, h: Int) {

        check()

        onGameLoopStart()

        if (isFirstFrame) tick("Game loop")

        if (Maths.random() < 0.1) FileReference.updateCache()

        updateVSync(window)
        updateHoveredAndCursor(window)
        processMouseMovement(window)

        if (isFirstFrame) tick("Before window drawing")

        // be sure always something is drawn
        var didSomething = window.needsRefresh || Input.needsLayoutUpdate(window)
        window.needsRefresh = false
        val windowStack = window.windowStack

        val dy = window.progressbarHeightSum
        // when the frame is minimized, nothing needs to be drawn
        if (!window.isMinimized) {

            windowStack.updateTransform(window, 0, 0, w, h)
            didSomething = windowStack.draw(
                0, dy, w, h, didSomething,
                didSomething || window.didNothingCounter < 3
            )

            window.framesSinceLastInteraction++

            if (isFirstFrame) tick("Window drawing")

            useFrame(0, 0, w, h, NullFramebuffer, Renderer.colorRenderer) {
                if (drawUIOverlay(window, w, h)) didSomething = true
            }

        }

        if (didSomething) window.didNothingCounter = 0
        else window.didNothingCounter++

        FBStack.reset()

        check()

        if (isFirstFrame) {
            startClock.total("First frame finished")
            isFirstFrame = false
        }

        CacheSection.updateAll()

        onGameLoopEnd()

    }

    fun updateVSync(window: OSWindow) {
        val vsync = DefaultConfig["debug.ui.enableVsync", !Build.isDebug]
        if (vsync != window.enableVsync) {
            window.setVsyncEnabled(vsync)
        }
    }

    /**
     * prevents tearing, but also increase input-latency
     * */
    var enableVSync
        get() = DefaultConfig["debug.ui.enableVsync", !Build.isDebug]
        set(value) {
            DefaultConfig["debug.ui.enableVsync"] = value
        }

    fun toggleVsync() {
        DefaultConfig["debug.ui.enableVsync"] = !DefaultConfig["debug.ui.enableVsync", true]
    }

    fun processMouseMovement(window: OSWindow) {
        if (!Input.hadMouseMovement && window.isInFocus) {
            // if our window doesn't have focus, or the cursor is outside,
            // we need to ask for updates manually
            window.updateMousePosition()
            if (!Input.hadMouseMovement) {
                ActionManager.onMouseIdle(window)
            }
        }
        lastMouseX = window.mouseX
        lastMouseY = window.mouseY
        Input.hadMouseMovement = false
    }

    fun updateHoveredAndCursor(window: OSWindow) {
        val hovered = window.windowStack
            .getPanelAndWindowAt(window.mouseX, window.mouseY)
        hoveredPanel = hovered?.first
        hoveredWindow = hovered?.second
        updateCursor(window, hovered?.first)
    }

    fun updateCursor(window: OSWindow, hoveredPanel: Panel?) {
        hoveredPanel?.getCursor()?.useCursor(window)
    }

    open fun drawUIOverlay(window: OSWindow, w: Int, h: Int): Boolean {

        var didSomething = false

        if (showFPS) {
            FrameTimings.showFPS(window)
        }

        if (showTutorialKeys) {
            if (ShowKeys.draw(0, 0, h)) {
                didSomething = true
            }
        }

        if (Tooltips.draw(window)) {
            didSomething = true
        }

        val progressBars = window.progressBars
        if (progressBars.isNotEmpty()) {
            val ph = window.progressbarHeight
            val time = Engine.nanoTime
            for (index in progressBars.indices) {
                val bar = progressBars[index]
                bar.draw(0, ph * index, w, ph, time)
            }
            val changed = progressBars.removeIf { it.canBeRemoved(time) }
            if (changed) {
                for (window1 in window.windowStack) {
                    if (window1.isFullscreen) window1.panel.invalidateLayout()
                }
            }
        }

        // dragging can be a nice way to work, but dragging values to change them,
        // and copying by ctrl+c/v is probably better -> no, we need both
        // dragging files for example
        val dragged = dragged
        if (dragged != null) {
            val (rw, rh) = dragged.getSize(w / 5, h / 5)
            var x = lastMouseX.roundToInt() - rw / 2
            var y = lastMouseY.roundToInt() - rh / 2
            x = clamp(x, 0, w - rw)
            y = clamp(y, 0, h - rh)
            GFX.clip(x, y, min(rw, w), min(rh, h)) {
                dragged.draw(x, y)
                didSomething = true
            }
        }

        return didSomething

    }

    private var isFirstFrame = true

    fun check() = GFX.check()

    open fun clearAll() {
        CacheSection.clearAll()
    }

    open fun isSelected(obj: Any?) = false

    open var language = Language.get(Dict["en-US", "lang.spellcheck"])

    companion object {

        var shallStop = false

        var instance: StudioBase? = null

        var workspace = OS.documents

        private val LOGGER = LogManager.getLogger(StudioBase::class.java)

        var dragged: IDraggable? = null

        // val defaultWindowStack get() = instance?.windowStack

        /**
         * schedules a task that will be executed on the main loop
         * */
        fun addEvent(event: () -> Unit) {
            eventTasks += event
        }

        /**
         * schedules a task that will be executed on the main loop;
         * will wait at least deltaMillis before it is executed
         * */
        fun addEvent(deltaMillis: Long, event: () -> Unit) {
            scheduledTasks.add(Pair(Engine.nanoTime + deltaMillis * MILLIS_TO_NANOS, event))
        }

        fun warn(msg: String) {
            LOGGER.warn(msg)
        }

        private val eventTasks: Queue<() -> Unit> = ConcurrentLinkedQueue()
        private val scheduledTasks: Queue<Pair<Long, () -> Unit>> =
            PriorityBlockingQueue(16) { a, b -> a.first.compareTo(b.first) }

        val shiftSlowdown get() = if (Input.isAltDown) 5f else if (Input.isShiftDown) 0.2f else 1f

        fun workEventTasks() {
            while (scheduledTasks.isNotEmpty()) {
                try {
                    val time = Engine.nanoTime
                    val peeked = scheduledTasks.peek()!!
                    if (time >= peeked.first) {
                        scheduledTasks.poll()!!.second.invoke()
                    } else break
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            while (eventTasks.isNotEmpty()) {
                try {
                    eventTasks.poll()!!.invoke()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

    }

}