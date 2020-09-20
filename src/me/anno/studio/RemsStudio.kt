package me.anno.studio

import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.getPanelAndWindowAt
import me.anno.gpu.GFX.hoveredPanel
import me.anno.gpu.GFX.hoveredWindow
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFX.showFPS
import me.anno.gpu.GFX.updateTitle
import me.anno.gpu.Window
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.ShowKeys
import me.anno.objects.cache.Cache
import me.anno.studio.Studio.addEvent
import me.anno.studio.Studio.dragged
import me.anno.studio.Studio.project
import me.anno.studio.history.History
import me.anno.studio.history.SceneState
import me.anno.studio.project.Project
import me.anno.ui.base.*
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.FrameTimes
import me.anno.ui.editor.UILayouts
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.input.TextInput
import me.anno.utils.OS
import me.anno.utils.clamp
import me.anno.utils.f3
import me.anno.utils.listFiles2
import org.apache.logging.log4j.LogManager
import org.omg.CORBA.FREE_MEM
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

// todo operation to cut an image, video, or similar?

// todo scene screenshot/editor screenshot

// todo draw frame by frame, only save x,y,radius?

// todo somehow reenable scrubbing
// todo small preview?

object RemsStudio {

    val startTime = System.nanoTime()

    val isSaving = AtomicBoolean(false)
    var forceSave = false
    var lastSave = System.nanoTime()
    var saveIsRequested = true

    private val LOGGER = LogManager.getLogger(RemsStudio::class)

    val originalOutput = System.out!!

    val windowStack = Stack<Window>()

    var showTutorialKeys = DefaultConfig["tutorial.keys.show", true]
    var showFPS = DefaultConfig["debug.fps.show", false]

    var workspace = DefaultConfig["workspace.dir", File(OS.home, "Documents/RemsStudio")]
    // todo load last project, vs create new one?
    // todo just create a new one?

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
        isSaving.set(true)
        thread {
            try {
                val state = SceneState()
                state.update()
                History.put(state)
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
                originalOutput.write(b)
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

    fun run() {

        // Library.JNI_LIBRARY_NAME.toLowerCase()

        mt("run")

        setupLogging()

        mt("logging")

        var lmx = mouseX
        var lmy = mouseY

        UILayouts.createLoadingUI()

        mt("loading ui")

        GFX.windowStack = windowStack
        GFX.gameInit = {

            mt("game init")

            AudioManager.startRunning()

            mt("audio manager")

            Cursor.init()

            UILayouts.createWelcomeUI()

        }

        GFX.gameLoop = { w, h ->

            check()

            saveStateMaybe()

            if (frameCtr == 0L) mt("game loop")

            val hovered = getPanelAndWindowAt(mouseX, mouseY)
            hoveredPanel = hovered?.first
            hoveredWindow = hovered?.second

            if (lmx == mouseX && lmy == mouseY) {
                ActionManager.onMouseIdle()
            } else {
                lmx = mouseX
                lmy = mouseY
            }

            hoveredPanel?.getCursor()?.useCursor()

            if (frameCtr == 0L) mt("before window drawing")

            smallestIndexNeedingRendering = 0
            windowStack.forEachIndexed { index, window ->
                loadTexturesSync.clear()
                loadTexturesSync.push(false)
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
                panel.draw(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h)
                GFX.ensureEmptyStack()
            }

            Input.framesSinceLastInteraction++

            if (frameCtr == 0L) mt("window drawing")

            Tooltips.draw()

            if (showFPS) {
                FrameTimes.placeInParent(0, 0)
                FrameTimes.applyPlacement(FrameTimes.width, FrameTimes.height)
                FrameTimes.draw()
                showFPS()
            }

            if (showTutorialKeys) ShowKeys.draw(0, 0, GFX.width, GFX.height)

            // dragging can be a nice way to work, but dragging values to change them,
            // and copying by ctrl+c/v is probably better -> no, we need both
            // dragging files for example
            val dragged = dragged
            if (dragged != null) {
                val (rw, rh) = dragged.getSize(GFX.width / 5, GFX.height / 5)
                var x = mouseX.roundToInt() - rw / 2
                var y = mouseY.roundToInt() - rh / 2
                x = clamp(x, 0, GFX.width - rw)
                y = clamp(y, 0, GFX.height - rh)
                GFX.clip(x, y, ui.w, ui.h)
                dragged.draw(x, y)
            }

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
        project = Project(name.trim(), folder)
        project!!.open()
        GFX.addGPUTask(1){ updateTitle() }
    }

    // would overflow as 32 bit after 2.5 months on a 300 fps display ;D
    private var frameCtr = 0L

    lateinit var startMenu: Panel
    lateinit var ui: Panel
    var console: TextPanel? = null

    val lastConsoleLines = LinkedList<String>()
    var lastConsoleLineCount = 500

    fun check() = GFX.check()

}