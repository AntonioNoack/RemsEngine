package me.anno.engine

import me.anno.Engine
import me.anno.Time
import me.anno.audio.openal.AudioManager
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.systems.Systems
import me.anno.engine.ui.EditorState
import me.anno.extensions.ExtensionLoader
import me.anno.extensions.events.EventBroadcasting.callEvent
import me.anno.extensions.events.GameLoopStartEvent
import me.anno.gpu.Clipping
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.OSWindow
import me.anno.gpu.WindowManagement
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
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.Tooltips
import me.anno.ui.debug.FrameTimings
import me.anno.ui.dragging.IDraggable
import me.anno.utils.Clock
import me.anno.utils.OS
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings
import org.apache.logging.log4j.LogManager
import kotlin.math.min

/**
 * base class for UI setup;
 * manages audio, graphics, settings, game loop, and UI
 * */
abstract class EngineBase(
    val nameDesc: NameDesc,
    val configName: String,
    val versionNumber: Int,
    versionSuffix: String? = null,
    val needsAudio: Boolean = true,
) {

    constructor(nameDesc: NameDesc, versionNumber: Int, versionSuffix: String?, needsAudio: Boolean) :
            this(nameDesc, Strings.filterAlphaNumeric(nameDesc.englishName), versionNumber, versionSuffix, needsAudio)

    constructor(nameDesc: NameDesc, configName: String, versionNumber: Int, needsAudio: Boolean) :
            this(nameDesc, configName, versionNumber, null, needsAudio)

    constructor(nameDesc: NameDesc, versionNumber: Int, needsAudio: Boolean) :
            this(nameDesc, Strings.filterAlphaNumeric(nameDesc.englishName), versionNumber, null, needsAudio)

    /**
     * version of program as string,
     * x.yy.zz
     * */
    val versionName =
        Strings.addSuffix(
            "${versionNumber / 10000}.${(versionNumber / 100) % 100}.${versionNumber % 100}",
            versionSuffix
        )

    open fun loadConfig() {}

    abstract fun createUI()

    open fun onGameLoopStart() {
        val sampleInstance = EditorState.prefabAsync?.getSampleInstance()
        Systems.world = sampleInstance
        (sampleInstance as? Entity)?.create() // really do that here???
        Systems.onUpdate()
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
        GFX.windows.firstOrNull()?.title = nameDesc.name
        Engine.projectName = configName
        instance = this
        ConfigBasics.configFolder = OS.home.getChild(".config").getChild(configName)
        ConfigBasics.cacheFolder = OS.home.getChild(".cache").getChild(configName)
    }

    open fun run(runGraphics: Boolean = !OS.isWeb && !OS.isAndroid) {

        setupNames()

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
            WindowManagement.run(nameDesc.name)
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

        callEvent(GameLoopStartEvent())
        onGameLoopStart()

        if (isFirstFrame) tick("Game loop")

        window.setVsyncEnabled(WindowRenderFlags.enableVSync)
        updateHoveredAndCursor(window)
        processMouseMovement(window)

        if (isFirstFrame) tick("Before window drawing")

        // be sure always something is drawn
        val windowStack = window.windowStack

        val dy = window.progressbarHeightSum
        // when the frame is minimized, nothing needs to be drawn
        if (!window.isMinimized) {

            windowStack.updateTransform(window, 0, 0, w, h)
            windowStack.draw(0, dy, w, h)

            if (isFirstFrame) tick("Window drawing")

            GFXState.useFrame(0, 0, w, h, NullFramebuffer, Renderer.colorRenderer) {
                drawUIOverlay(window, w, h)
            }
        }

        FBStack.reset()

        GFX.check()

        if (isFirstFrame) {
            startClock.total("First frame finished")
            isFirstFrame = false
        }

        CacheSection.updateAll()

        onGameLoopEnd()
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
            .getPanelAt(window.mouseX, window.mouseY)
        hoveredPanel = hovered
        hoveredWindow = hovered?.window
        updateCursor(window, hovered)
    }

    fun updateCursor(window: OSWindow, hoveredPanel: Panel?) {
        (hoveredPanel?.getCursor() ?: Cursor.default).useCursor(window)
    }

    open fun drawUIOverlay(window: OSWindow, w: Int, h: Int) {

        if (WindowRenderFlags.showFPS && window.showFPS) {
            FrameTimings.showFPS(window)
        }

        if (WindowRenderFlags.showTutorialKeys) {
            ShowKeys.draw(0, 0, h)
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
            progressBars.removeAll { it.canBeRemoved(time) }
        }

        Tooltips.draw(window)

        renderDragged(w, h, dragged)
    }

    private fun renderDragged(w: Int, h: Int, dragged: IDraggable?) {
        dragged ?: return
        // todo if base below is sensitive, draw this transparent
        //  (text is blocking view when dragging a scene item into DraggingControls/RenderView)
        val (rw, rh) = dragged.getSize(w / 5, h / 5)
        var x = lastMouseX.roundToIntOr() - rw / 2
        var y = lastMouseY.roundToIntOr() - rh / 2
        x = Maths.clamp(x, 0, w - rw)
        y = Maths.clamp(y, 0, h - rh)
        val xw = min(rw, w)
        val xh = min(rh, h)
        Clipping.clip(x, y, xw, xh) {
            dragged.draw(x, y, x + xw, y + xh)
        }
    }

    private var isFirstFrame = true

    open fun clearAll() {
        CacheSection.clearAll()
    }

    open fun isSelected(obj: Any?) = false

    open val language get() = Language.get(Dict["en-US", "lang.spellcheck"])

    companion object {

        private val LOGGER = LogManager.getLogger(EngineBase::class)

        /**
         * Currently running instance of Engine.
         * */
        var instance: EngineBase? = null

        /**
         * In which folder the instance shall operate mainly, e.g., current project folder.
         * */
        var workspace = OS.documents

        /**
         * What's currently dragged by the user.
         * Will be rendered as an overlay when set, and can be dropped on some panels.
         * */
        var dragged: IDraggable? = null
    }
}