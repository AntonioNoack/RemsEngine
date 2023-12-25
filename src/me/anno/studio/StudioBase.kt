package me.anno.studio

import me.anno.Build
import me.anno.Engine
import me.anno.Engine.projectName
import me.anno.Time
import me.anno.audio.openal.AudioManager
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.OSWindow
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.renderer.Renderer
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.io.config.ConfigBasics.cacheFolder
import me.anno.io.config.ConfigBasics.configFolder
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.language.Language
import me.anno.language.translation.Dict
import me.anno.maths.Maths
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
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * base class for UI setup;
 * manages audio, graphics, settings, game loop, ui
 * */
abstract class StudioBase(
    val title: String,
    val configName: String,
    val versionNumber: Int,
    versionSuffix: String? = null,
    val needsAudio: Boolean = true,
) {

    constructor(title: String, versionNumber: Int, versionSuffix: String?, needsAudio: Boolean) :
            this(title, filterAlphaNumeric(title), versionNumber, versionSuffix, needsAudio)

    constructor(title: String, configName: String, versionNumber: Int, needsAudio: Boolean) :
            this(title, configName, versionNumber, null, needsAudio)

    constructor(title: String, versionNumber: Int, needsAudio: Boolean) :
            this(title, filterAlphaNumeric(title), versionNumber, null, needsAudio)

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
    open fun onGameInit() {}
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

        ExtensionLoader.load()

        createUI()
    }

    open fun setupNames() {
        GFX.windows.firstOrNull()?.title = title
        projectName = configName
        configFolder = getReference(OS.home, ".config/$configName")
        cacheFolder = getReference(OS.home, ".cache/$configName")
    }

    open fun run(runGraphics: Boolean = !OS.isWeb && !OS.isAndroid) {

        if (Engine.shutdown) {
            LOGGER.warn("Engine restart is experimental!")
            Engine.cancelShutdown()
        }

        instance = this

        setupNames()

        tick("run")

        Logging.setup()

        tick("logging")

        loadConfig()

        tick("config")

        if (runGraphics) {
            GFXBase.run(title)
        }
    }

    var hoveredPanel: Panel? = null
    var hoveredWindow: Window? = null

    private var lastMouseX = 0f
    private var lastMouseY = 0f

    open fun onShutdown() {
        ExtensionLoader.unload()
        Engine.requestShutdown()
        onGameClose()
    }

    open fun onGameLoop(window: OSWindow, w: Int, h: Int) {

        GFX.check()

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

        GFX.check()

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
        (hoveredPanel?.getCursor() ?: Cursor.default).useCursor(window)
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
            val time = Time.nanoTime
            for (index in progressBars.indices) {
                val progressBar = progressBars.getOrNull(index) ?: break
                val x = 0
                val y = ph * index
                progressBar.draw(
                    x, y, w, ph,
                    x, y, x + w, y + ph,
                    time
                )
            }
            val changed = progressBars.removeIf { it.canBeRemoved(time) }
            if (changed) window.invalidateLayout()
        }

        // dragging can be a nice way to work, but dragging values to change them,
        // and copying by ctrl+c/v is probably better -> no, we need both
        // dragging files for example
        val dragged = dragged
        if (dragged != null) {
            // todo if base below is sensitive, draw this transparent
            //  (text is blocking view when dragging a scene item into DraggingControls/RenderView)
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

    open fun clearAll() {
        CacheSection.clearAll()
    }

    open fun isSelected(obj: Any?) = false

    open var language = Language.get(Dict["en-US", "lang.spellcheck"])

    companion object {

        var instance: StudioBase? = null

        var workspace = OS.documents

        private val LOGGER = LogManager.getLogger(StudioBase::class)

        var dragged: IDraggable? = null

        val shiftSlowdown get() = if (Input.isAltDown) 5f else if (Input.isShiftDown) 0.2f else 1f

    }
}