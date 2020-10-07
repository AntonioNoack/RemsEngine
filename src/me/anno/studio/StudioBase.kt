package me.anno.studio

import me.anno.audio.ALBase
import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.Window
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.NearestMode
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.objects.cache.Cache
import me.anno.studio.RemsStudio.selectedTransform
import me.anno.studio.history.SceneState
import me.anno.studio.project.Project
import me.anno.ui.base.Panel
import me.anno.ui.base.Tooltips
import me.anno.ui.base.Visibility
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.dragging.IDraggable
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.UILayouts
import me.anno.ui.editor.graphs.GraphEditorBody
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.editor.sceneView.ISceneView
import me.anno.ui.editor.treeView.TreeView
import me.anno.utils.OS
import me.anno.utils.clamp
import me.anno.utils.f3
import me.anno.utils.listFiles2
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class StudioBase(val needsAudio: Boolean) {

    // todo fix overlay views...

    abstract fun createUI()

    var showRedraws = DefaultConfig["debug.ui.showRedraws", false]
    val startTime = System.nanoTime()

    val isSaving = AtomicBoolean(false)
    var forceSave = false
    var lastSave = System.nanoTime()
    var saveIsRequested = true

    private val LOGGER = LogManager.getLogger(StudioBase::class)

    val windowStack = Stack<Window>()

    var showTutorialKeys = DefaultConfig["tutorial.keys.show", true]
    var showFPS = DefaultConfig["debug.fps.show", Build.isDebug]

    var workspace = DefaultConfig["workspace.dir", File(OS.home, "Documents/RemsStudio")]

    fun onSmallChange(cause: String) {
        saveIsRequested = true
        SceneTabs.currentTab?.hasChanged = true
        updateSceneViews()
        // println(cause)
    }

    fun onLargeChange() {
        saveIsRequested = true
        forceSave = true
        SceneTabs.currentTab?.hasChanged = true
        updateSceneViews()
    }

    fun updateSceneViews() {
        windowStack.forEach { window ->
            window.panel.listOfVisible
                .forEach {
                    when (it) {
                        is TreeView, is ISceneView -> {
                            it.invalidateDrawing()
                        }
                        is GraphEditorBody -> {
                            if (selectedTransform != null) {
                                it.invalidateDrawing()
                            }
                        }
                        is PropertyInspector -> {
                            it.needsUpdate = true
                        }
                    }
                }
        }
    }

    fun saveStateMaybe() {
        val historySaveDuration = 1e9
        if (saveIsRequested && !isSaving.get()) {
            val current = System.nanoTime()
            if (forceSave || abs(current - lastSave) > historySaveDuration) {
                lastSave = current
                forceSave = false
                saveIsRequested = false
                saveState()
            }
        }
    }

    fun saveState() {
        // saving state
        if (RemsStudio.project == null) return
        isSaving.set(true)
        thread {
            try {
                val state = SceneState()
                state.update()
                RemsStudio.history.put(state)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isSaving.set(false)
        }
    }

    fun clear(file: File) {
        if (file.isDirectory) {
            for (file2 in file.listFiles2()) {
                file2.delete()
            }
            file.delete()
        } else file.delete()
    }

    var lastT = startTime
    fun mt(name: String) {
        val t = System.nanoTime()
        val dt = t - lastT
        lastT = t
        if (dt > 500_000) {// 0.5 ms
            LOGGER.info("Used ${(dt * 1e-9).f3()}s for $name")
        }
    }

    open fun gameInit() {

        mt("game init")

        if (needsAudio) {
            AudioManager.startRunning()
            mt("audio manager")
        }

        Cursor.init()

        createUI()

    }

    var didNothingCounter = 0

    fun run() {

        instance = this

        mt("run")

        Logging.setup()

        mt("logging")

        var lmx = Input.mouseX
        var lmy = Input.mouseY

        UILayouts.createLoadingUI()

        mt("loading ui")

        GFX.windowStack = windowStack
        GFX.gameInit = { gameInit() }

        GFX.gameLoop = { w, h ->

            check()

            saveStateMaybe()

            if (isFirstFrame) mt("game loop")

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

            if (isFirstFrame) mt("before window drawing")

            var didSomething = false
            fun shallDraw() = didSomething || didNothingCounter < 3

            val lastFullscreenIndex = windowStack.indexOfLast { it.isFullscreen }
            windowStack.forEachIndexed { index, window ->
                if (index >= lastFullscreenIndex) {

                    val panel0 = window.panel
                    val allPanels = panel0.listOfVisible.toList()
                    // should be faster than a lot of HashSet-tests
                    val mx = lmx.toInt()
                    val my = lmy.toInt()
                    allPanels.forEach {
                        it.apply {
                            isInFocus = false
                            canBeSeen = (parent?.canBeSeen != false) &&
                                    visibility == Visibility.VISIBLE &&
                                    lx1 > lx0 && ly1 > ly0
                            isHovered = mx in lx0 until lx1 && my in ly0 until ly1
                        }
                    }

                    val visiblePanels = allPanels.filter { it.canBeSeen }

                    inFocus.forEach {
                        it.isInFocus = true
                    }

                    visiblePanels.forEach { it.tickUpdate() }
                    visiblePanels.forEach { it.tick() }

                    val needsRedraw = window.needsRedraw
                        .map { it.getOverlayParent() ?: it }
                        .toHashSet()

                    val needsLayout = window.needsLayout
                    needsRedraw.retainAll(visiblePanels)

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
                            needsLayout.removeAll(panel.listOfAll)
                            needsRedraw.add(panel.getOverlayParent() ?: panel)
                        }
                    }

                    if(panel0.w > 0 && panel0.h > 0){

                        val sparseRedraw = DefaultConfig["ui.sparseRedraw", true]

                        // overlays get missing...
                        // this somehow needs to be circumvented...
                        if (sparseRedraw) {

                            val wasRedrawn = ArrayList<Panel>()

                            if (needsRedraw.isNotEmpty()) {

                                didSomething = true

                                GFX.ensureEmptyStack()
                                Framebuffer.stack.push(null)
                                Frame.reset()

                                GFX.deltaX = panel0.x
                                GFX.deltaY = h - (panel0.y + panel0.h)

                                BlendDepth(BlendMode.DEFAULT, false).use {

                                    val buffer = window.buffer
                                    if (panel0 in needsRedraw) {

                                        wasRedrawn += panel0

                                        GFX.loadTexturesSync.clear()
                                        GFX.loadTexturesSync.push(true)

                                        Frame(panel0.x, panel0.y, panel0.w, panel0.h, true, buffer) {
                                            Frame.currentFrame!!.bind()
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
                                                Frame(panel.lx0, h - (y + h2), panel.lx1 - panel.lx0, h2, false, buffer) {
                                                    panel.redraw()
                                                }
                                            }
                                            wasRedrawn += panel
                                            needsRedraw.removeAll(panel.listOfAll)
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

                                    BlendDepth(BlendMode.DEFAULT, false).use {

                                        window.buffer.bindTexture0(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                                        GFX.copy()

                                        if(showRedraws){
                                            wasRedrawn.forEach {
                                                GFX.drawRect(it.lx0, it.ly0, it.lx1 - it.lx0, it.ly1 - it.ly0, 0x33ff0000)
                                            }
                                        }

                                    }

                                }

                            }// else no buffer needs to be updated

                        } else {

                            if (shallDraw()) {

                                needsRedraw.clear()

                                GFX.ensureEmptyStack()
                                Framebuffer.stack.push(null)
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

            if (isFirstFrame) mt("window drawing")

            Frame(0, 0, GFX.width, GFX.height, false, null) {

                if (Tooltips.draw()) {
                    didSomething = true
                }

                if (showFPS) {
                    GFX.showFPS()
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
                mt("first frame finished")
                LOGGER.info("Used ${((System.nanoTime() - startTime) * 1e-9f).f3()}s from start to finishing the first frame")
                isFirstFrame = false
            }

            Cache.update()

            false
        }

        GFX.shutdown = {
            AudioManager.requestDestruction()
            Cursor.destroy()
        }

        GFX.run()

    }

    fun loadProject(name: String, folder: File) {
        val project = Project(name.trim(), folder)
        RemsStudio.project = project
        project.open()
        GFX.addGPUTask(1) { GFX.updateTitle() }
    }

    private var isFirstFrame = true
    var hideUnusedProperties = false

    fun check() = GFX.check()

    fun createConsole(): ConsoleOutputPanel {
        val console = ConsoleOutputPanel(style.getChild("small"))
        // console.fontName = "Segoe UI"
        Logging.console = console
        console.setTooltip("Double-click to open history")
        console.instantTextLoading = true
        console.text = Logging.lastConsoleLines.lastOrNull() ?: ""
        return console
    }

    companion object {

        lateinit var instance: StudioBase

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

    }

}