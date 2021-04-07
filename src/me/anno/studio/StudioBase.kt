package me.anno.studio

import me.anno.Engine
import me.anno.audio.ALBase
import me.anno.audio.AudioManager
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.GFXBase0
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.Window
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.studio.project.Project
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.Panel
import me.anno.ui.base.Tooltips
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.debug.FPSPanel
import me.anno.ui.dragging.IDraggable
import me.anno.utils.Clock
import me.anno.utils.Maths.clamp
import me.anno.utils.OS
import me.anno.utils.types.Strings.addSuffix
import me.anno.utils.types.Strings.filterAlphaNumeric
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt

// todo remove unnecessary checks, whether files exist and when they were created: much too expensive (0.2ms)

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
    abstract fun onGameLoopStart()
    abstract fun onGameLoopEnd()
    abstract fun onGameClose()
    abstract fun onGameInit()

    val startClock = Clock()
    fun tick(name: String) {
        startClock.stop(name)
    }

    val windowStack = Stack<Window>()

    val showTutorialKeys get() = DefaultConfig["ui.tutorial.showKeys", true]
    val showFPS get() = DefaultConfig["debug.ui.showFPS", Build.isDebug]
    val showRedraws get() = DefaultConfig["debug.ui.showRedraws", false]

    open fun gameInit() {

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

        var lmx = Input.mouseX
        var lmy = Input.mouseY

        tick("loading ui")

        GFX.windowStack = windowStack
        GFX.gameInit = {
            GFX.check()
            gameInit()
        }

        GFX.gameLoop = { w, h ->

            check()

            val vsync = DefaultConfig["debug.ui.enableVsync", Build.isDebug]
            if (vsync != GFXBase0.enableVsync) {
                GFXBase0.setVsyncEnabled(vsync)
            }

            onGameLoopStart()

            if (isFirstFrame) tick("game loop")

            val hovered = GFX.getPanelAndWindowAt(Input.mouseX, Input.mouseY)
            GFX.hoveredPanel = hovered?.first
            GFX.hoveredWindow = hovered?.second

            if (lmx == Input.mouseX && lmy == Input.mouseY) {
                ActionManager.onMouseIdle()
            } else {
                lmx = Input.mouseX
                lmy = Input.mouseY
            }

            GFX.hoveredPanel?.getCursor()?.useCursor()

            if (isFirstFrame) tick("before window drawing")

            var didSomething = false
            fun shallDraw() = didSomething || didNothingCounter < 3

            val sparseRedraw = DefaultConfig["ui.sparseRedraw", true]

            val needsRedraw = HashSet<Panel>()


            val lastFullscreenIndex = windowStack.indexOfLast { it.isFullscreen }
            windowStack.forEachIndexed { index, window ->
                if (index >= lastFullscreenIndex) {

                    val panel0 = window.panel
                    // val allPanels = panel0.listOfAll.toList()
                    // should be faster than a lot of HashSet-tests
                    val mx = lmx.toInt()
                    val my = lmy.toInt()
                    panel0.listOfAll {
                        it.apply {
                            isInFocus = false
                            canBeSeen = (parent?.canBeSeen != false) &&
                                    visibility == Visibility.VISIBLE &&
                                    lx1 > lx0 && ly1 > ly0
                            isHovered = mx in lx0 until lx1 && my in ly0 until ly1
                        }
                    }

                    // val visiblePanels = allPanels.filter { it.canBeSeen }

                    for (panel in inFocus) panel.isInFocus = true

                    // resolve missing parents...
                    // which still happen...
                    panel0.listOfAll { panel ->
                        if (panel.parent == null && panel !== panel0) {
                            panel.parent = panel0.listOfAll
                                .filter { it.canBeSeen }
                                .filterIsInstance<PanelGroup>()
                                .firstOrNull { parent -> panel in parent.children }
                        }
                    }

                    panel0.listOfAll { panel -> if (panel.canBeSeen) panel.tickUpdate() }
                    panel0.listOfAll { panel -> if (panel.canBeSeen) panel.tick() }

                    needsRedraw.clear()
                    for(panel in window.needsRedraw){
                        if(panel.canBeSeen){
                            val panel2 = panel.getOverlayParent() ?: panel
                            needsRedraw.add(panel2)
                        }
                    }

                    val needsLayout = window.needsLayout
                    if (panel0 in needsLayout || window.lastW != w || window.lastH != h) {
                        window.lastW = w
                        window.lastH = h
                        window.calculateFullLayout(w, h, isFirstFrame)
                        needsRedraw.add(panel0)
                        needsLayout.clear()
                    } else {
                        while (needsLayout.isNotEmpty()) {
                            val panel = needsLayout.minBy { it.depth }!!
                            // recalculate layout
                            panel.calculateSize(panel.lx1 - panel.lx0, panel.ly1 - panel.ly0)
                            panel.place(panel.lx0, panel.ly0, panel.lx1 - panel.lx0, panel.ly1 - panel.ly0)
                            panel.listOfAll { needsLayout.remove(it) }
                            needsRedraw.add(panel.getOverlayParent() ?: panel)
                        }
                    }

                    if (panel0.w > 0 && panel0.h > 0) {

                        // overlays get missing...
                        // this somehow needs to be circumvented...
                        if (sparseRedraw) {

                            val wasRedrawn = ArrayList<Panel>()

                            if (needsRedraw.isNotEmpty()) {

                                didSomething = true

                                GFX.ensureEmptyStack()
                                // Framebuffer.stack.push(null)
                                Frame.reset()

                                GFX.deltaX = panel0.x
                                GFX.deltaY = h - (panel0.y + panel0.h)

                                BlendDepth(BlendMode.DEFAULT, false) {

                                    val buffer = window.buffer
                                    if (panel0 in needsRedraw) {

                                        wasRedrawn += panel0

                                        GFX.loadTexturesSync.clear()
                                        GFX.loadTexturesSync.push(true)

                                        Frame(panel0.x, panel0.y, panel0.w, panel0.h, true, buffer) {
                                            Frame.bind()
                                            glClearColor(0f, 0f, 0f, 0f)
                                            glClear(GL_COLOR_BUFFER_BIT)
                                            panel0.canBeSeen = true
                                            panel0.draw(panel0.x, panel0.y, panel0.x + panel0.w, panel0.y + panel0.h)
                                        }

                                    } else {

                                        while (needsRedraw.isNotEmpty()) {
                                            val panel = needsRedraw.minBy { it.depth }!!
                                            GFX.loadTexturesSync.clear()
                                            GFX.loadTexturesSync.push(false)
                                            if (panel.canBeSeen) {
                                                val y = panel.ly0
                                                val h2 = panel.ly1 - panel.ly0
                                                Frame(
                                                    panel.lx0,
                                                    h - (y + h2),
                                                    panel.lx1 - panel.lx0,
                                                    h2,
                                                    false,
                                                    buffer
                                                ) {
                                                    panel.redraw()
                                                }
                                            }
                                            wasRedrawn += panel
                                            for (child in panel.listOfAll) {
                                                needsRedraw.remove(child)
                                            }
                                        }

                                    }

                                    window.needsRedraw.clear()

                                }

                                GFX.deltaX = 0
                                GFX.deltaY = 0

                            }

                            if (shallDraw()) {

                                // draw cached image
                                Frame(panel0.x, h - (panel0.y + panel0.h), panel0.w, panel0.h, false, null) {

                                    BlendDepth(BlendMode.DEFAULT, false) {

                                        window.buffer.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                                        GFX.copy()

                                        if (showRedraws) {
                                            wasRedrawn.forEach {
                                                drawRect(
                                                    it.lx0,
                                                    it.ly0,
                                                    it.lx1 - it.lx0,
                                                    it.ly1 - it.ly0,
                                                    0x33ff0000
                                                )
                                            }
                                        }

                                    }

                                }

                            }// else no buffer needs to be updated

                        } else {

                            if (shallDraw()) {

                                needsRedraw.clear()

                                GFX.ensureEmptyStack()
                                // Framebuffer.stack.push(null)
                                Frame.reset()

                                GFX.loadTexturesSync.clear()
                                GFX.loadTexturesSync.push(false)
                                if (Input.needsLayoutUpdate()) {
                                    window.calculateFullLayout(w, h, isFirstFrame)
                                }

                                Frame(panel0.x, panel0.y, panel0.w, panel0.h, false, null) {
                                    panel0.canBeSeen = true
                                    panel0.draw(panel0.x, panel0.y, panel0.x + panel0.w, panel0.y + panel0.h)
                                }

                            }// else no buffer needs to be updated
                        }
                    }

                }
            }

            Input.framesSinceLastInteraction++

            if (isFirstFrame) tick("window drawing")

            Frame(0, 0, GFX.width, GFX.height, false, null) {

                if (Tooltips.draw()) {
                    didSomething = true
                }

                if (showFPS) {
                    FPSPanel.showFPS()
                }

                if (showTutorialKeys) {
                    if (ShowKeys.draw(0, 0, GFX.width, GFX.height)) {
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

            }

            if (didSomething) {
                didNothingCounter = 0
            } else {
                didNothingCounter++
            }

            FBStack.reset()

            check()

            if (isFirstFrame) {
                startClock.total("first frame finished")
                isFirstFrame = false
            }

            CacheSection.updateAll()

            onGameLoopEnd()

            false
        }

        GFX.onShutdown = {
            shallStop = true
            ExtensionLoader.unload()
            Cursor.destroy()
            Engine.shutdown()
            onGameClose()
        }

        GFX.run()

    }


    fun loadProject(name: String, folder: File) {
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

    fun createConsole(): ConsoleOutputPanel {
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
            GFX.addAudioTask(100) {
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