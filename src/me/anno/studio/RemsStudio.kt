package me.anno.studio

import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.getPanelAndWindowAt
import me.anno.gpu.GFX.hoveredPanel
import me.anno.gpu.GFX.hoveredWindow
import me.anno.gpu.GFX.showFPS
import me.anno.gpu.GFX.updateTitle
import me.anno.gpu.Window
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.ShowKeys
import me.anno.objects.cache.Cache
import me.anno.studio.Studio.addEvent
import me.anno.studio.Studio.dragged
import me.anno.studio.Studio.project
import me.anno.studio.project.Project
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Tooltips
import me.anno.utils.clamp
import me.anno.utils.f3
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt

// todo operation to cut an image, video, or similar?

object RemsStudio {

    val LOGGER = LogManager.getLogger(RemsStudio::class)

    val originalOutput = System.out

    val windowStack = Stack<Window>()

    var showTutorialKeys = DefaultConfig["tutorial.keys.show", true]
    var showFPS = DefaultConfig["debug.fps.show", true]

    var workspace = File(DefaultConfig["workspace.dir", "${System.getProperty("user.home")}/Documents/RemsStudio"])
    // todo load last project, vs create new one?
    // todo just create a new one?

    fun clear(file: File){
        if(file.isDirectory){
            for(file2 in file.listFiles() ?: return){
                file2.delete()
            }
            file.delete()
        } else file.delete()
    }

    fun run(){

        var lmx = mouseX
        var lmy = mouseY

        UILayouts.createLoadingUI()

        GFX.windowStack = windowStack
        GFX.gameInit = {
            AudioManager.startRunning()
            Cursor.init()
            thread {
                loadProject()
                addEvent {
                    UILayouts.createEditorUI()
                }
            }
        }
        GFX.gameLoop = { w, h ->
            check()

            val hovered = getPanelAndWindowAt(mouseX, mouseY)
            hoveredPanel = hovered?.first
            hoveredWindow = hovered?.second

            if(lmx == mouseX && lmy == mouseY){
                ActionManager.onMouseIdle()
            } else {
                lmx = mouseX
                lmy = mouseY
            }

            hoveredPanel?.getCursor()?.useCursor()

            windowStack.forEach { window ->
                GFX.loadTexturesSync = false
                val panel = window.panel
                // optimization is worth 0.5% of 3.4GHz * 12 ~ 200 MHz ST (13.06.2020)
                if(Input.needsLayoutUpdate()){
                    val t0 = System.nanoTime()
                    panel.calculateSize(w-window.x,h-window.y)
                    panel.applyConstraints()
                    val t1 = System.nanoTime()
                    panel.placeInParent(window.x, window.y)
                    val t2 = System.nanoTime()
                    val dt1 = (t1-t0)*1e-9f
                    val dt2 = (t2-t1)*1e-9f
                    if(dt1 > 0.01f && frameCtr > 0) LOGGER.warn("Used ${dt1.f3()}s + ${dt2.f3()}s for layout")
                    Input.framesSinceLastInteraction++
                }
                panel.draw(window.x,window.y,window.x+panel.w,window.y+panel.h)
            }

            Tooltips.draw()

            if(showFPS) showFPS()
            if(showTutorialKeys) ShowKeys.draw(0, 0, GFX.width, GFX.height)

            // dragging can be a nice way to work, but dragging values to change them,
            // and copying by ctrl+c/v is probably better -> no, we need both
            // dragging files for example
            val dragged = dragged
            if(dragged != null){
                val (rw, rh) = dragged.getSize(GFX.width/5, GFX.height/5)
                var x = mouseX.roundToInt() - rw/2
                var y = mouseY.roundToInt() - rh/2
                x = clamp(x, 0, GFX.width-rw)
                y = clamp(y, 0, GFX.height-rh)
                GFX.clip(x, y, ui.w, ui.h)
                dragged.draw(x, y)
            }

            check()

            if(frameCtr == 0L){
                LOGGER.info("Used ${((System.nanoTime()-startTime)*1e-9f).f3()}s from start to finishing the first frame")
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

    fun loadProject(){
        val newProjectName = "New Project"
        val lastProject = DefaultConfig["projects.last", newProjectName]
        val project0File = File(workspace, lastProject)
        if(lastProject == newProjectName) clear(project0File)
        project = Project(project0File)
        GFX.addGPUTask { updateTitle(); 1 }
    }

    // would overflow as 32 bit after 2.5 months on a 300 fps display ;D
    private var frameCtr = 0L

    lateinit var startMenu: Panel
    lateinit var ui: Panel
    lateinit var console: TextPanel

    fun check() = GFX.check()

    @JvmStatic
    fun main(args: Array<String>){
        run()
    }

}