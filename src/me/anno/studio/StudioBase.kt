package me.anno.studio

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
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Tooltips
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.editor.UILayouts
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.utils.OS
import me.anno.utils.clamp
import me.anno.utils.f3
import me.anno.utils.listFiles2
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.lang.Exception
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

open class StudioBase {

    val startTime = System.nanoTime()

    val isSaving = AtomicBoolean(false)
    var forceSave = false
    var lastSave = System.nanoTime()
    var saveIsRequested = true

    private val LOGGER = LogManager.getLogger(StudioBase::class)

    val needsLayout = HashSet<Panel>()
    val needsDrawing = HashSet<Panel>()

    val originalOut = System.out!!
    val originalErr = System.err!!

    val windowStack = Stack<Window>()

    var showTutorialKeys = DefaultConfig["tutorial.keys.show", true]
    var showFPS = DefaultConfig["debug.fps.show", Build.isDebug]

    var workspace = DefaultConfig["workspace.dir", File(OS.home, "Documents/RemsStudio")]

    fun onSmallChange(cause: String){
        saveIsRequested = true
        SceneTabs.currentTab?.hasChanged = true
        // (cause)
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
        if(Studio.project == null) return
        isSaving.set(true)
        thread {
            try {
                val state = SceneState()
                state.update()
                Studio.history.put(state)
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

    fun setupLogging() {
        System.setOut(PrintStream(object : OutputStream() {
            var line = ""
            override fun write(b: Int) {
                when {
                    b == '\n'.toInt() -> {
                        // only accept non-empty lines?
                        val lines = lastConsoleLines
                        if (lines.size > lastConsoleLineCount) lines.removeFirst()
                        lines.push(line)
                        console?.text = line
                        line = ""
                    }
                    line.length < 100 -> {
                        // enable for
                        /*if(line.isEmpty() && b != '['.toInt()){
                            throw RuntimeException("Please use the LogManager.getLogger(YourClass)!")
                        }*/
                        line += b.toChar()
                    }
                    line.length == 100 -> {
                        line += "..."
                    }
                }
                originalOut.write(b)
            }
        }))
        System.setErr(PrintStream(object : OutputStream() {
            var line = ""
            override fun write(b: Int) {
                when {
                    b == '\n'.toInt() -> {
                        if(line.isNotBlank()){
                            // only accept non-empty lines?
                            val lines = lastConsoleLines
                            if (lines.size > lastConsoleLineCount) lines.removeFirst()
                            line = "[ERR] $line"
                            lines.push(line)
                            console?.text = line
                        }
                        line = ""
                    }
                    line.length < 100 -> {
                        // enable for
                        /*if(line.isEmpty() && b != '['.toInt()){
                            throw RuntimeException("Please use the LogManager.getLogger(YourClass)!")
                        }*/
                        line += b.toChar()
                    }
                    line.length == 100 -> {
                        line += "..."
                    }
                }
                originalErr.write(b)
            }
        }))
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

    var smallestIndexNeedingRendering = 0

    open fun gameInit(){

        mt("game init")

        AudioManager.startRunning()

        mt("audio manager")

        Cursor.init()

        UILayouts.createWelcomeUI()

    }

    fun run() {

        instance = this

        // Library.JNI_LIBRARY_NAME.toLowerCase()

        mt("run")

        setupLogging()

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

            if (frameCtr == 0L) mt("game loop")

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

            if (frameCtr == 0L) mt("before window drawing")

            smallestIndexNeedingRendering = 0
            windowStack.forEachIndexed { index, window ->
                GFX.loadTexturesSync.clear()
                GFX.loadTexturesSync.push(false)
                val panel = window.panel
                // optimization is worth 0.5% of 3.4GHz * 12 ~ 200 MHz ST (13.06.2020)
                if (Input.needsLayoutUpdate()) {
                    // ("layouting")
                    val t0 = System.nanoTime()
                    panel.calculateSize(min(w - window.x, w), min(h - window.y, h))
                    // panel.applyPlacement(min(w - window.x, w), min(h - window.y, h))
                    if(window.isFullscreen){
                        smallestIndexNeedingRendering = index
                    }
                    if(panel.w > w || panel.h > h) throw RuntimeException("Panel is too large...")
                    // panel.applyConstraints()
                    val t1 = System.nanoTime()
                    panel.place(window.x, window.y, w, h)
                    val t2 = System.nanoTime()
                    val dt1 = (t1 - t0) * 1e-9f
                    val dt2 = (t2 - t1) * 1e-9f
                    if (dt1 > 0.01f && frameCtr > 0) LOGGER.warn("Used ${dt1.f3()}s + ${dt2.f3()}s for layout")
                }
                GFX.ensureEmptyStack()
                Framebuffer.stack.push(null)
                Frame.reset()
                Frame(panel.x, panel.y, panel.w, panel.h, null){
                    panel.draw(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h)
                }
                GFX.ensureEmptyStack()
            }

            Input.framesSinceLastInteraction++

            if (frameCtr == 0L) mt("window drawing")

            Frame(0, 0, GFX.width, GFX.height, null){

                Tooltips.draw()

                if (showFPS) GFX.showFPS()
                if (showTutorialKeys) ShowKeys.draw(0, 0, GFX.width, GFX.height)

                // dragging can be a nice way to work, but dragging values to change them,
                // and copying by ctrl+c/v is probably better -> no, we need both
                // dragging files for example
                val dragged = Studio.dragged
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

            if (frameCtr == 0L) mt("first frame finished")

            if (frameCtr == 0L) {
                LOGGER.info("Used ${((System.nanoTime() - startTime) * 1e-9f).f3()}s from start to finishing the first frame")
            }
            frameCtr++

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
        Studio.project = Project(name.trim(), folder)
        Studio.project!!.open()
        GFX.addGPUTask(1){ GFX.updateTitle() }
    }

    // would overflow as 32 bit after 2.5 months on a 300 fps display ;D
    private var frameCtr = 0L

    lateinit var startMenu: Panel
    lateinit var ui: Panel
    var console: TextPanel? = null

    val lastConsoleLines = LinkedList<String>()
    var lastConsoleLineCount = 500

    var hideUnusedProperties = false

    fun check() = GFX.check()

    fun createConsole(): ConsoleOutputPanel {
        val console = ConsoleOutputPanel(style.getChild("small"))
        // console.fontName = "Segoe UI"
        instance.console = console
        console.setTooltip("Double-click to open history")
        console.instantTextLoading = true
        console.text = RemsStudio.lastConsoleLines.lastOrNull() ?: ""
        return console
    }

    companion object {
        lateinit var instance: StudioBase
    }

}