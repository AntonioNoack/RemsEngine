package me.anno.studio

import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.getClickedPanelAndWindow
import me.anno.gpu.GFX.hoveredPanel
import me.anno.gpu.GFX.hoveredWindow
import me.anno.gpu.GFX.showFPS
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.ShowKeys
import me.anno.objects.cache.Cache
import me.anno.studio.Studio.dragged
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Tooltips
import me.anno.ui.editor.*
import me.anno.ui.editor.sceneView.SceneView
import me.anno.utils.clamp
import me.anno.utils.f3
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

// todo open full log when clicking on the bottom;
// todo or get more details?

object RemsStudio {

    val originalOutput = System.out

    val windowStack = Stack<Window>()

    var showTutorialKeys = DefaultConfig["tutorial.keys.show", true]
    var showFPS = DefaultConfig["debug.fps.show", true]

    fun run(){

        Layout.createUI()
        GFX.windowStack = windowStack
        GFX.gameInit = {
            AudioManager.init()
            Cursor.init()
        }
        GFX.gameLoop = { w, h ->
            check()

            val hovered = getClickedPanelAndWindow(Input.mouseX, Input.mouseY)
            hoveredPanel = hovered?.first
            hoveredWindow = hovered?.second

            hoveredPanel?.getCursor()?.useCursor()

            windowStack.forEach { window ->
                val panel = window.panel
                val t0 = System.nanoTime()
                panel.calculateSize(w-window.x,h-window.y)
                panel.applyConstraints()
                val t1 = System.nanoTime()
                panel.placeInParent(window.x, window.y)
                val t2 = System.nanoTime()
                val dt1 = (t1-t0)*1e-9f
                val dt2 = (t2-t1)*1e-9f
                if(dt1 > 0.01f && frameCtr > 0) println("[WARN] Used ${dt1}s + ${dt2}s for layout")
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
                var x = mouseX.roundToInt()
                var y = mouseY.roundToInt()
                x = min(x, GFX.width-rw)
                y = min(y, GFX.height-rh)
                GFX.clip(x, y, ui.w, ui.h)
                println("d $x $y")
                dragged.draw(x, y)
            }

            check()

            if(frameCtr == 0L){
                println("[INFO] Used ${((System.nanoTime()-startTime)*1e-9f).f3()}s from start to finishing the first frame")
            }
            frameCtr++

            Cache.update()

            false
        }
        GFX.shutdown = {
            AudioManager.destroy()
            Cursor.destroy()
        }
        GFX.run()
    }

    // would overflow as 32 bit after 2.5 months on a 300 fps display ;D
    private var frameCtr = 0L

    lateinit var startMenu: Panel
    lateinit var ui: Panel
    lateinit var console: TextPanel

    fun check() = GFX.check()


}