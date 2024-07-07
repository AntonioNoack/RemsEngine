package me.anno.engine

import me.anno.Build
import me.anno.Engine
import me.anno.Time
import me.anno.audio.openal.AudioManager
import me.anno.cache.CacheSection
import me.anno.config.ConfigRef
import me.anno.config.DefaultConfig
import me.anno.ecs.systems.Systems
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.GFXState
import me.anno.gpu.OSWindow
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.renderer.Renderer
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.Language
import me.anno.language.translation.Dict
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.Tooltips
import me.anno.ui.debug.FrameTimings
import me.anno.ui.dragging.IDraggable
import me.anno.utils.Clock
import me.anno.utils.Logging
import me.anno.utils.OS
import me.anno.utils.types.Strings
import org.apache.logging.log4j.LogManager
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * base class for UI setup;
 * manages audio, graphics, settings, game loop, and UI
 * */
abstract class EngineBase(
    val title: String,
    val configName: String,
    val versionNumber: Int,
    versionSuffix: String? = null,
    val needsAudio: Boolean = true,
) {

    constructor(title: String, versionNumber: Int, versionSuffix: String?, needsAudio: Boolean) :
            this(title, Strings.filterAlphaNumeric(title), versionNumber, versionSuffix, needsAudio)

    constructor(title: String, configName: String, versionNumber: Int, needsAudio: Boolean) :
            this(title, configName, versionNumber, null, needsAudio)

    constructor(title: String, versionNumber: Int, needsAudio: Boolean) :
            this(title, Strings.filterAlphaNumeric(title), versionNumber, null, needsAudio)

    /**
     * version of program as string,
     * x.yy.zz
     * */
    val versionName =
        Strings.addSuffix(
            "${versionNumber / 10000}.${(versionNumber / 100) % 100}.${versionNumber % 100}",
            versionSuffix
        )

    val systems = Systems()

    open fun loadConfig() {}

    abstract fun createUI()

    open fun onGameLoopStart() {
        // todo can we beautify this?
        systems.world = ECSSceneTabs.currentTab?.inspector?.prefab?.getSampleInstance()
        systems.onUpdate()
    }

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

    val startClock = Clock(LOGGER)
    fun tick(name: String) {
        startClock.stop(name)
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

        createUI()
    }

    open fun setupNames() {
        GFX.windows.firstOrNull()?.title = title
        Engine.projectName = configName
        instance = this
        ConfigBasics.configFolder = OS.home.getChild(".config").getChild(configName)
        ConfigBasics.cacheFolder = OS.home.getChild(".cache").getChild(configName)
    }

    open fun run(runGraphics: Boolean = !OS.isWeb && !OS.isAndroid) {

        setupNames()

        Logging.setup()

        if (Engine.shutdown) {
            LOGGER.warn("Engine restart is experimental!")
            @Suppress("DEPRECATION")
            Engine.cancelShutdown()
        }

        tick("logging")

        loadConfig()

        tick("config")

        OfficialExtensions.register()
        ExtensionLoader.load()

        tick("extensions")

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

        window.setVsyncEnabled(enableVSync)
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

            GFXState.useFrame(0, 0, w, h, NullFramebuffer, Renderer.colorRenderer) {
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

    fun toggleVsync() {
        enableVSync = !enableVSync
    }

    fun processMouseMovement(window: OSWindow) {
        if (!Input.hadMouseMovement || !window.isInFocus) {
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

        if (showFPS && window.showFPS) {
            FrameTimings.showFPS(window)
        }

        if (showTutorialKeys) {
            if (ShowKeys.draw(0, 0, h)) {
                didSomething = true
            }
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
            val changed = progressBars.removeAll { it.canBeRemoved(time) }
            if (changed) window.invalidateLayout()
        }

        if (Tooltips.draw(window)) {
            didSomething = true
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
            x = Maths.clamp(x, 0, w - rw)
            y = Maths.clamp(y, 0, h - rh)
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

    open val language get() = Language.get(Dict["en-US", "lang.spellcheck"])

    companion object {

        @JvmStatic
        var idleFPS by ConfigRef("ui.window.idleFPS", 10)

        @JvmStatic
        var maxFPS by ConfigRef("ui.window.maxFPS", 0)

        /**
         * prevents tearing, but also increase input-latency
         * */
        var enableVSync by ConfigRef("debug.ui.enableVsync", !Build.isDebug)

        var instance: EngineBase? = null

        var showRedraws by ConfigRef("debug.ui.showRedraws", false)
        var showFPS by ConfigRef("debug.ui.showFPS", false)
        var showTutorialKeys by ConfigRef("ui.tutorial.showKeys", true)

        var workspace = OS.documents

        private val LOGGER = LogManager.getLogger(EngineBase::class)

        var dragged: IDraggable? = null

        val shiftSlowdown get() = if (Input.isAltDown) 5f else if (Input.isShiftDown) 0.2f else 1f
    }
}