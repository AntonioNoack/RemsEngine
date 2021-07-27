package me.anno.studio

import me.anno.Engine
import me.anno.audio.openal.ALBase
import me.anno.audio.openal.AudioManager
import me.anno.audio.openal.AudioTasks
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.GFX.isMinimized
import me.anno.gpu.GFXBase0
import me.anno.gpu.RenderState.renderDefault
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.Window
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.shader.Renderer
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.studio.project.Project
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.Panel
import me.anno.ui.base.Tooltips
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.debug.FPSPanel
import me.anno.ui.dragging.IDraggable
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.Maths.clamp
import me.anno.utils.OS
import me.anno.utils.types.Strings.addSuffix
import me.anno.utils.types.Strings.filterAlphaNumeric
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt

abstract class StudioBase(
    val needsAudio: Boolean,
    val title: String,
    val configName: String,
    val versionNumber: Int,
    versionSuffix: String? = null
) {

    constructor(needsAudio: Boolean, title: String, versionNumber: Int, versionSuffix: String? = null) :
            this(needsAudio, title, filterAlphaNumeric(title), versionNumber, versionSuffix)

    /**
     * version of program as string,
     * x.yy.zz
     * */
    val versionName =
        addSuffix("${versionNumber / 10000}.${(versionNumber / 100) % 100}.${versionNumber % 100}", versionSuffix)

    abstract fun createUI()

    open fun onGameLoopStart() {}
    open fun onGameLoopEnd() {}

    open fun onGameInit() {
    }

    open fun onGameClose() {}

    val startClock = Clock()
    fun tick(name: String) {
        startClock.stop(name)
    }

    val showTutorialKeys get() = DefaultConfig["ui.tutorial.showKeys", true]
    val showFPS get() = DefaultConfig["debug.ui.showFPS", Build.isDebug]
    val showRedraws get() = DefaultConfig["debug.ui.showRedraws", false]

    open fun gameInit() {

        GFX.check()

        onGameInit()

        tick("game init")

        if (needsAudio) {
            AudioManager.startRunning()
            tick("audio manager")
        }

        Cursor.init()

        createUI()

        ExtensionLoader.load()

    }

    var didNothingCounter = 0

    fun setupNames() {
        GFX.title = title
        GFXBase0.projectName = configName
        instance = this
    }

    fun run() {

        setupNames()

        tick("run")

        Logging.setup()

        tick("logging")

        tick("loading ui")

        GFX.gameInit = this::gameInit
        GFX.gameLoop = this::onGameLoop
        GFX.onShutdown = this::onShutdown

        GFX.run()

    }

    fun shallDraw(didSomething: Boolean) = didSomething || didNothingCounter < 3

    private var lastMouseX = Input.mouseX
    private var lastMouseY = Input.mouseY

    open fun onShutdown() {
        shallStop = true
        ExtensionLoader.unload()
        Cursor.destroy()
        Engine.shutdown()
        onGameClose()
    }

    private val needsRedraw = HashSet<Panel>()
    open fun onGameLoop(w: Int, h: Int): Boolean {

        check()

        onGameLoopStart()

        if (isFirstFrame) tick("game loop")

        updateVSync()
        updateHoveredAndCursor()
        processMouseMovement()

        if (isFirstFrame) tick("before window drawing")

        // be sure that always something is drawn
        var didSomething = GFX.needsRefresh
        GFX.needsRefresh = false

        // when the frame is minimized, nothing needs to be drawn
        if (!isMinimized) {

            val sparseRedraw = DefaultConfig["ui.sparseRedraw", true]

            val windowStack = GFX.windowStack
            val lastFullscreenIndex = windowStack.indexOfLast { it.isFullscreen }
            for (index in windowStack.indices) {
                val window = windowStack[index]
                if (index >= lastFullscreenIndex) {
                    didSomething = drawWindow(w, h, window, needsRedraw, sparseRedraw, didSomething)
                }
            }

            Input.framesSinceLastInteraction++

            if (isFirstFrame) tick("window drawing")

            useFrame(0, 0, w, h, false, null, Renderer.colorRenderer) {
                if (drawUIOverlay(w, h)) didSomething = true
            }

        }

        if (didSomething) didNothingCounter = 0
        else didNothingCounter++

        FBStack.reset()

        check()

        if (isFirstFrame) {
            startClock.total("first frame finished")
            isFirstFrame = false
        }

        CacheSection.updateAll()

        onGameLoopEnd()

        return false

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
        if (lastMouseX == Input.mouseX && lastMouseY == Input.mouseY) {
            ActionManager.onMouseIdle()
        } else {
            lastMouseX = Input.mouseX
            lastMouseY = Input.mouseY
        }
    }

    fun updateHoveredAndCursor() {
        val hovered = GFX.getPanelAndWindowAt(Input.mouseX, Input.mouseY)
        GFX.hoveredPanel = hovered?.first
        GFX.hoveredWindow = hovered?.second
        updateCursor(hovered?.first)
    }

    fun updateCursor(hoveredPanel: Panel?) {
        hoveredPanel?.getCursor()?.useCursor()
    }

    fun drawWindow(
        w: Int, h: Int, window: Window,
        needsRedraw: MutableSet<Panel>,
        sparseRedraw: Boolean,
        didSomething0: Boolean
    ): Boolean {

        var didSomething = didSomething0
        val panel0 = window.panel

        panel0.updateVisibility(lastMouseX.toInt(), lastMouseY.toInt())
        for (panel in inFocus) panel.isInFocus = true

        // resolve missing parents...
        // which still happens...
        panel0.findMissingParents()

        panel0.listOfAll { panel -> if (panel.canBeSeen) panel.tickUpdate() }
        panel0.listOfAll { panel -> if (panel.canBeSeen) panel.tick() }

        findRedraws(window, needsRedraw)

        validateLayouts(w, h, window, panel0, needsRedraw)

        if (panel0.w > 0 && panel0.h > 0) {

            // overlays get missing...
            // this somehow needs to be circumvented...
            if (sparseRedraw) {
                didSomething = sparseRedraw(w, h, window, panel0, needsRedraw, didSomething)
            } else {
                if (shallDraw(didSomething)) {
                    needsRedraw.clear()
                    fullRedraw(w, h, window, panel0)
                }// else no buffer needs to be updated
            }
        }

        return didSomething

    }

    fun findRedraws(window: Window, needsRedraw: MutableSet<Panel>) {
        needsRedraw.clear()
        try {
            for (panel in window.needsRedraw) {
                if (panel.canBeSeen) {
                    val panel2 = panel.getOverlayParent() ?: panel
                    needsRedraw.add(panel2)
                }
            }
        } catch (e: ConcurrentModificationException) {
            // something async has changed stuff... should not happen
        }
    }

    fun validateLayouts(w: Int, h: Int, window: Window, panel0: Panel, needsRedraw: MutableSet<Panel>) {
        val needsLayout = window.needsLayout
        if (panel0 in needsLayout || window.lastW != w || window.lastH != h) {
            window.lastW = w
            window.lastH = h
            window.calculateFullLayout(w, h, isFirstFrame)
            needsRedraw.add(panel0)
            needsLayout.clear()
        } else {
            while (needsLayout.isNotEmpty()) {
                val panel = needsLayout.minByOrNull { it.depth }!!
                // recalculate layout
                panel.calculateSize(panel.lx1 - panel.lx0, panel.ly1 - panel.ly0)
                panel.place(panel.lx0, panel.ly0, panel.lx1 - panel.lx0, panel.ly1 - panel.ly0)
                needsLayout.removeAll(panel.listOfAll.toList())
                needsRedraw.add(panel.getOverlayParent() ?: panel)
            }
        }
    }

    fun fullRedraw(
        w: Int, h: Int, window: Window,
        panel0: Panel
    ) {

        GFX.ensureEmptyStack()
        // Framebuffer.stack.push(null)
        Frame.reset()

        GFX.loadTexturesSync.clear()
        GFX.loadTexturesSync.push(false)
        if (Input.needsLayoutUpdate()) {
            window.calculateFullLayout(w, h, isFirstFrame)
        }

        useFrame(panel0.x, panel0.y, panel0.w, panel0.h, false, null, Renderer.colorRenderer) {
            panel0.canBeSeen = true
            panel0.draw(panel0.x, panel0.y, panel0.x + panel0.w, panel0.y + panel0.h)
        }

    }

    fun sparseRedraw(
        w: Int, h: Int, window: Window,
        panel0: Panel,
        needsRedraw: MutableSet<Panel>,
        didSomething0: Boolean
    ): Boolean {

        var didSomething = didSomething0

        val wasRedrawn = ArrayList<Panel>()

        if (needsRedraw.isNotEmpty()) {

            didSomething = true

            GFX.ensureEmptyStack()
            Frame.reset()

            GFX.deltaX = panel0.x
            GFX.deltaY = panel0.y

            renderDefault {

                val buffer = window.buffer
                if (panel0 in needsRedraw) {

                    wasRedrawn += panel0

                    GFX.loadTexturesSync.clear()
                    GFX.loadTexturesSync.push(true)

                    useFrame(panel0.x, panel0.y, panel0.w, panel0.h, true, buffer, Renderer.colorRenderer) {
                        Frame.bind()
                        glClearColor(0f, 0f, 0f, 0f)
                        glClear(GL_COLOR_BUFFER_BIT)
                        panel0.canBeSeen = true
                        panel0.draw(panel0.x, panel0.y, panel0.x + panel0.w, panel0.y + panel0.h)
                    }

                } else {

                    while (needsRedraw.isNotEmpty()) {
                        val panel = needsRedraw.minByOrNull { it.depth }!!
                        GFX.loadTexturesSync.clear()
                        GFX.loadTexturesSync.push(false)
                        if (panel.canBeSeen) {
                            useFrame(
                                panel.lx0, panel.ly0,
                                panel.lx1 - panel.lx0,
                                panel.ly1 - panel.ly0,
                                false, buffer,
                                Renderer.colorRenderer
                            ) { panel.redraw() }
                        }
                        wasRedrawn += panel
                        panel.listOfAll {
                            needsRedraw.remove(it)
                        }
                    }

                }

                window.needsRedraw.clear()

            }

            GFX.deltaX = 0
            GFX.deltaY = 0

        }

        if (shallDraw(didSomething)) {
            drawCachedImage(w, h, window, panel0, wasRedrawn)
        }// else no buffer needs to be updated

        return didSomething

    }

    open fun drawCachedImage(w: Int, h: Int, window: Window, panel0: Panel, wasRedrawn: Collection<Panel>) {
        /*useFrame(panel0.x, h - (panel0.y + panel0.h), panel0.w, panel0.h, false, null) {
            renderDefault {

                window.buffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                GFX.copy()

                if (showRedraws) {
                    showRedraws(wasRedrawn)
                }
            }
        }*/
        useFrame(panel0.x, panel0.y, panel0.w, panel0.h, false, null) {
            renderDefault {

                GFX.copy(window.buffer)

                if (showRedraws) {
                    showRedraws(wasRedrawn)
                }
            }
        }
    }

    open fun showRedraws(wasRedrawn: Collection<Panel>) {
        for (panel in wasRedrawn) {
            drawRect(
                panel.lx0,
                panel.ly0,
                panel.lx1 - panel.lx0,
                panel.ly1 - panel.ly0,
                0x33ff0000
            )
        }
    }

    open fun drawUIOverlay(w: Int, h: Int): Boolean {

        var didSomething = false

        if (Tooltips.draw()) {
            didSomething = true
        }

        if (showFPS) {
            FPSPanel.showFPS()
        }

        if (showTutorialKeys) {
            if (ShowKeys.draw(0, 0, GFX.height)) {
                didSomething = true
            }
        }

        // dragging can be a nice way to work, but dragging values to change them,
        // and copying by ctrl+c/v is probably better -> no, we need both
        // dragging files for example
        val dragged = dragged
        if (dragged != null) {
            val (rw, rh) = dragged.getSize(GFX.width / 5, GFX.height / 5)
            var x = Input.mouseX.roundToInt() - rw / 2
            var y = Input.mouseY.roundToInt() - rh / 2
            x = clamp(x, 0, GFX.width - rw)
            y = clamp(y, 0, GFX.height - rh)
            GFX.clip(x, y, w, h) {
                dragged.draw(x, y)
                didSomething = true
            }
        }

        return didSomething

    }

    fun loadProject(name: String, folder: File) {
        loadProject(name, getReference(folder))
    }

    fun loadProject(name: String, folder: FileReference) {
        val project = Project(name.trim(), folder)
        RemsStudio.project = project
        project.open()
        GFX.addGPUTask(1) {
            GFX.setTitle("Rem's Studio: ${project.name}")
        }
    }

    private var isFirstFrame = true
    var hideUnusedProperties = false

    fun check() = GFX.check()

    fun createConsole(style: Style): ConsoleOutputPanel {
        val console = ConsoleOutputPanel(style.getChild("small"))
        // console.fontName = "Segoe UI"
        Logging.console = console
        console.setTooltip("Double-click to open history")
        console.text = Logging.lastConsoleLines.lastOrNull() ?: ""
        return console
    }

    companion object {

        var shallStop = false

        lateinit var instance: StudioBase

        var workspace = OS.documents

        private val LOGGER = LogManager.getLogger(StudioBase::class.java)

        var dragged: IDraggable? = null

        fun updateAudio() {
            AudioTasks.addTask(100) {
                // update the audio player...
                if (RemsStudio.isPlaying) {
                    AudioManager.requestUpdate()
                } else {
                    AudioManager.stop()
                }
                ALBase.check()
            }
        }

        fun addEvent(event: () -> Unit) {
            eventTasks += event
        }

        fun warn(msg: String) {
            LOGGER.warn(msg)
        }

        val eventTasks = ConcurrentLinkedQueue<() -> Unit>()

        val shiftSlowdown get() = if (Input.isAltDown) 5f else if (Input.isShiftDown) 0.2f else 1f

        init {
            System.setProperty("joml.format", "false")
        }

    }

}