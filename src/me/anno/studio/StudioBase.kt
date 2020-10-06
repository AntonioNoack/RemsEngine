package me.anno.studio

import me.anno.audio.ALBase
import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.Window
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.ShowKeys
import me.anno.objects.cache.Cache
import me.anno.studio.history.SceneState
import me.anno.studio.project.Project
import me.anno.ui.base.Panel
import me.anno.ui.base.Tooltips
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.dragging.IDraggable
import me.anno.ui.editor.UILayouts
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.utils.OS
import me.anno.utils.clamp
import me.anno.utils.f3
import me.anno.utils.listFiles2
import org.apache.logging.log4j.LogManager
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class StudioBase(val needsAudio: Boolean){

    abstract fun createUI()

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

    fun onSmallChange(cause: String){
        saveIsRequested = true
        SceneTabs.currentTab?.hasChanged = true
    }

    fun onLargeChange(){
        saveIsRequested = true
        forceSave = true
        SceneTabs.currentTab?.hasChanged = true
    }

    fun saveStateMaybe(){
        val historySaveDuration = 1e9
        if(saveIsRequested && !isSaving.get()){
            val current = System.nanoTime()
            if(forceSave || abs(current - lastSave) > historySaveDuration){
                lastSave = current
                forceSave = false
                saveIsRequested = false
                saveState()
            }
        }
    }

    fun saveState(){
        // saving state
        if(RemsStudio.project == null) return
        isSaving.set(true)
        thread {
            try {
                val state = SceneState()
                state.update()
                RemsStudio.history.put(state)
            } catch (e: Exception){
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

    open fun gameInit(){

        mt("game init")

        if(needsAudio){
            AudioManager.startRunning()
            mt("audio manager")
        }

        Cursor.init()

        createUI()

    }

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

            val lastFullscreenIndex = windowStack.indexOfLast { it.isFullscreen }
            windowStack.forEachIndexed { index, window ->
                if(index >= lastFullscreenIndex){

                    // todo only draw what is needed
                    val allPanels = window.panel.listOfAll.toList()
                    val needsRedraw = window.needsRedraw

                    GFX.loadTexturesSync.clear()
                    GFX.loadTexturesSync.push(false)
                    val panel = window.panel
                    if (Input.needsLayoutUpdate()) {
                        window.calculateFullLayout(w, h, isFirstFrame)
                    }

                    GFX.ensureEmptyStack()
                    Framebuffer.stack.push(null)
                    Frame.reset()
                    Frame(panel.x, panel.y, panel.w, panel.h, null){
                        panel.draw(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h)
                    }
                    GFX.ensureEmptyStack()
                }
            }

            Input.framesSinceLastInteraction++

            if (isFirstFrame) mt("window drawing")

            Frame(0, 0, GFX.width, GFX.height, null){

                Tooltips.draw()

                if (showFPS) GFX.showFPS()
                if (showTutorialKeys) ShowKeys.draw(0, 0, GFX.width, GFX.height)

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
                    GFX.clip(x, y, ui.w, ui.h){
                        dragged.draw(x, y)
                    }
                }

            }

            FBStack.reset()

            check()

            if (isFirstFrame) mt("first frame finished")

            if (isFirstFrame) { LOGGER.info("Used ${((System.nanoTime() - startTime) * 1e-9f).f3()}s from start to finishing the first frame") }
            isFirstFrame = false

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
        GFX.addGPUTask(1){ GFX.updateTitle() }
    }

    private var isFirstFrame = true

    lateinit var startMenu: Panel
    lateinit var ui: Panel

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

        fun updateAudio(){
            GFX.addAudioTask(100){
                // update the audio player...
                if(RemsStudio.isPlaying){
                    AudioManager.requestUpdate()
                } else {
                    AudioManager.stop()
                }
                ALBase.check()
            }
        }

        fun addEvent(event: () -> Unit){
            eventTasks += event
        }

        fun warn(msg: String){
            LOGGER.warn(msg)
        }

        val eventTasks = ConcurrentLinkedQueue<() -> Unit>()

        val shiftSlowdown get() = if(Input.isAltDown) 5f else if(Input.isShiftDown) 0.2f else 1f

    }

}