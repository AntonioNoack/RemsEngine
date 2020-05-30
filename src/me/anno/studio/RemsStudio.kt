package me.anno.studio

import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.getClickedPanelAndWindow
import me.anno.gpu.GFX.hoveredPanel
import me.anno.gpu.GFX.hoveredWindow
import me.anno.gpu.GFX.root
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.input.Input.save
import me.anno.objects.Camera
import me.anno.objects.Text
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.run.startTime
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Tooltips
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomListX
import me.anno.ui.custom.CustomListY
import me.anno.ui.impl.*
import me.anno.ui.impl.sceneView.SceneView
import me.anno.ui.impl.timeline.Timeline
import org.joml.Vector4f
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.util.*

// todo close properties, when another one is clicked

object RemsStudio {

    val originalOutput = System.out

    var lastWidth = 0
    var lastHeight = 0

    val windowStack = Stack<Window>()

    lateinit var inspector: PropertyInspector

    fun run(){

        // val src = File("C:\\Users\\Antonio\\Videos\\Captures", "Cities_ Skylines 2020-01-06 19-32-23.mp4")
        // FFMPEGStream.getImageSequence(src)


        Layout.createUI()
        GFX.windowStack = windowStack
        GFX.gameInit = {
            AudioManager.init()
            Cursor.init()
        }
        GFX.gameLoop = { w, h ->
            check()

            if(GFX.previousSelectedTransform != GFX.selectedTransform){
                inspector.list.clear()
                GFX.previousSelectedTransform = GFX.selectedTransform
                val list = inspector.list
                GFX.selectedTransform?.createInspector(list, list.style)
            }

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
                if(dt1 > 0.1f) println("Warn: Used ${dt1}s + ${dt2}s for layout")
                panel.draw(window.x,window.y,window.x+panel.w,window.y+panel.h)
            }

            Tooltips.draw()

            /* dragging can be a nice way to work, but dragging values to change them,
            // and copying by ctrl+c/v is probably better :)
            val dragged = GFX.draggedObject
            if(dragged != null && dragged.isNotBlank()){
                val maxSize = 10
                var displayed = dragged.trim()
                if(displayed.length > maxSize) displayed = displayed.substring(0, maxSize - 3) + "..."
                GFX.drawText(GFX.mx.roundToInt() - 5, GFX.my.roundToInt() - 5,
                    style.getSize("dragging.textSize", 10), displayed,
                    style.getColor("dragging.textColor", -1),
                    style.getColor("dragging.background", 0))
            }*/

            check()

            if(frameCtr == 0){
                println("Used ${(System.nanoTime()-startTime)*1e-9f}s from start to finishing the first frame")
            }
            frameCtr++

            Cache.update()

            false
        }
        GFX.shutdown = {
            AudioManager.destroy()
            Cursor.destroy()
        }
        // GFX.init()
        GFX.run()
    }

    var frameCtr = 0

    lateinit var startMenu: Panel
    lateinit var ui: Panel
    lateinit var console: TextPanel



    fun check() = GFX.check()

    lateinit var sceneView: SceneView


}