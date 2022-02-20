package me.anno.studio

import me.anno.Build
import me.anno.Engine
import me.anno.Logging
import me.anno.audio.openal.AudioManager
import me.anno.cache.CacheSection
import me.anno.cache.instances.LastModifiedCache
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isMinimized
import me.anno.gpu.GFXBase0
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.Renderer
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.Language
import me.anno.language.translation.Dict
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.base.Tooltips
import me.anno.ui.base.progress.ProgressBar
import me.anno.ui.debug.FPSPanel
import me.anno.ui.dragging.IDraggable
import me.anno.ui.utils.WindowStack
import me.anno.utils.Clock
import me.anno.utils.OS
import me.anno.utils.types.Strings.addSuffix
import me.anno.utils.types.Strings.filterAlphaNumeric
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt

// todo which parts of the engine can be split into modules? would be good for compile times
// todo generate dependency graph visualization/computation? :)

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

    open fun supportsFileImport(): Boolean {
        return true
    }

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

    val showTutorialKeys get() = DefaultConfig["ui.tutorial.showKeys", true]
    val showFPS get() = DefaultConfig["debug.ui.showFPS", Build.isDebug]

    val windowStack = WindowStack()

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

    var didNothingCounter = 0

    open fun setupNames() {
        GFX.title = title
        GFXBase0.projectName = configName
    }

    open fun run() {

        instance = this

        setupNames()

        tick("run")

        Logging.setup()

        tick("logging")

        GFX.gameInit = this::gameInit
        GFX.gameLoop = this::onGameLoop
        GFX.onShutdown = this::onShutdown

        loadConfig()

        tick("config")

        GFX.run()

    }

    fun shallDraw(didSomething: Boolean) = didSomething || didNothingCounter < 3

    private var lastMouseX = Input.mouseX
    private var lastMouseY = Input.mouseY

    open fun onShutdown() {
        shallStop = true
        ExtensionLoader.unload()
        Cursor.destroy()
        Engine.requestShutdown()
        onGameClose()
    }

    open fun onGameLoop(w: Int, h: Int) {

        check()

        onGameLoopStart()

        if (isFirstFrame) tick("Game loop")

        if (Math.random() < 0.1) FileReference.updateCache()

        updateVSync()
        updateHoveredAndCursor()
        processMouseMovement()

        if (isFirstFrame) tick("Before window drawing")

        // be sure that always something is drawn
        var didSomething = GFX.needsRefresh || Input.needsLayoutUpdate()
        GFX.needsRefresh = false

        // when the frame is minimized, nothing needs to be drawn
        if (!isMinimized) {

            windowStack.updateTransform(w, h)
            didSomething = windowStack.draw(w, h, didSomething, shallDraw(didSomething), null)

            Input.framesSinceLastInteraction++

            if (isFirstFrame) tick("Window drawing")

            useFrame(0, 0, w, h, false, null, Renderer.colorRenderer) {
                if (drawUIOverlay(w, h)) didSomething = true
            }

        }

        if (didSomething) didNothingCounter = 0
        else didNothingCounter++

        FBStack.reset()

        check()

        if (isFirstFrame) {
            startClock.total("First frame finished")
            isFirstFrame = false
        }

        CacheSection.updateAll()

        onGameLoopEnd()

    }

    fun updateVSync() {
        val vsync = DefaultConfig["debug.ui.enableVsync", Build.isDebug]
        if (vsync != GFXBase0.enableVsync) {
            GFXBase0.setVsyncEnabled(vsync)
        }
    }

    fun setVsyncEnabled(enabled: Boolean) {
        DefaultConfig["debug.ui.enableVsync"] = enabled
    }

    fun processMouseMovement() {
        if (!Input.hadMouseMovement && GFX.isInFocus) {
            // if our window doesn't have focus or the cursor is outside,
            // we need to ask for updates manually
            GFX.updateMousePosition()
            if (!Input.hadMouseMovement) {
                ActionManager.onMouseIdle()
            }
        }
        lastMouseX = Input.mouseX
        lastMouseY = Input.mouseY
        Input.hadMouseMovement = false
    }

    fun updateHoveredAndCursor() {
        val hovered = windowStack.getPanelAndWindowAt(Input.mouseX, Input.mouseY)
        GFX.hoveredPanel = hovered?.first
        GFX.hoveredWindow = hovered?.second
        updateCursor(hovered?.first)
    }

    fun updateCursor(hoveredPanel: Panel?) {
        hoveredPanel?.getCursor()?.useCursor()
    }

    open fun drawUIOverlay(w: Int, h: Int): Boolean {

        var didSomething = false

        if (showFPS) {
            FPSPanel.showFPS()
        }

        if (showTutorialKeys) {
            if (ShowKeys.draw(0, 0, GFX.height)) {
                didSomething = true
            }
        }

        if (Tooltips.draw()) {
            didSomething = true
        }

        synchronized(progressBars) {
            if (progressBars.isNotEmpty()) {
                val ph = style.getSize("progressbarHeight", 8)
                val time = GFX.gameTime
                for (index in progressBars.indices) {
                    val bar = progressBars[index]
                    bar.draw(0, ph * index, w, ph, time)
                }
                progressBars.removeIf { it.canBeRemoved(time) }
            }
        }

        // dragging can be a nice way to work, but dragging values to change them,
        // and copying by ctrl+c/v is probably better -> no, we need both
        // dragging files for example
        val dragged = dragged
        if (dragged != null) {
            val (rw, rh) = dragged.getSize(w / 5, h / 5)
            var x = Input.mouseX.roundToInt() - rw / 2
            var y = Input.mouseY.roundToInt() - rh / 2
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
    var hideUnusedProperties = false

    fun check() = GFX.check()

    val progressBars = ArrayList<ProgressBar>()

    open fun clearAll() {
        CacheSection.clearAll()
        LastModifiedCache.clear()
    }

    open fun isSelected(obj: Any?) = false

    open var language = Language.get(Dict["en-US", "lang.spellcheck"])

    companion object {

        var shallStop = false

        var instance: StudioBase? = null

        var workspace = OS.documents

        private val LOGGER = LogManager.getLogger(StudioBase::class.java)

        var dragged: IDraggable? = null

        val defaultWindowStack get() = instance?.windowStack

        fun addEvent(event: () -> Unit) {
            eventTasks += event
        }

        fun warn(msg: String) {
            LOGGER.warn(msg)
        }

        fun addProgressBar(unit: String, total: Double): ProgressBar {
            val bar = ProgressBar(unit, total)
            val instance = instance ?: return bar
            synchronized(instance.progressBars) {
                instance.progressBars.add(bar)
            }
            return bar
        }

        val eventTasks = ConcurrentLinkedQueue<() -> Unit>()

        val shiftSlowdown get() = if (Input.isAltDown) 5f else if (Input.isShiftDown) 0.2f else 1f

        init {
            System.setProperty("joml.format", "false")
        }

    }

}