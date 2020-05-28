package me.anno

import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.gpu.Cursor
import me.anno.gpu.Cursor.useCursor
import me.anno.gpu.GFX
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.objects.SimpleText
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.run.startTime
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.impl.*
import me.anno.ui.impl.sceneView.SceneView
import me.anno.ui.impl.timeline.Timeline
import org.joml.Vector4f
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.util.*

class RemsStudio {

    val originalOutput = System.out

    var lastWidth = 0
    var lastHeight = 0

    val windowStack = Stack<Window>()

    lateinit var inspector: PropertyInspector

    fun run(){

        // val src = File("C:\\Users\\Antonio\\Videos\\Captures", "Cities_ Skylines 2020-01-06 19-32-23.mp4")
        // FFMPEGStream.getImageSequence(src)


        createUI()
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

            GFX.getClickedPanel(Input.mouseX, Input.mouseY)?.getCursor()?.useCursor()

            windowStack.forEach { window ->
                val panel = window.panel
                val t0 = System.nanoTime()
                panel.calculateSize(w-window.x,h-window.y)
                val t1 = System.nanoTime()
                panel.placeInParent(window.x, window.y)
                val t2 = System.nanoTime()
                val dt1 = (t1-t0)*1e-9f
                val dt2 = (t2-t1)*1e-9f
                if(dt1 > 0.1f) println("Warn: Used ${dt1}s + ${dt2}s for layout")
                panel.draw(window.x,window.y,w,h)
            }

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

    fun createUI(){

        val style = DefaultConfig.style

        val ui = PanelListY(null, style)
        this.ui = ui
        // todo top ui bar
        // todo show the file location up there, too?
        // todo fully customizable content
        val options = OptionBar(style)
        options.addAction("File", "Save"){

        }
        options.addMajor("File")
        options.addMajor("Edit")
        options.addMajor("View")
        options.addMajor("Navigate")
        options.addMajor("Code")
        ui += options

        val root = Transform(null)
        root.name = "Root"
        // val a = Transform(Vector3f(10f, 50f, 0f), Vector3f(1f,1f,1f), Quaternionf(1f,0f,0f,0f), root)
        // for(i in 0 until 3) Transform(null, null, null, a)
        // val b = Transform(null, null, null, root)
        // for(i in 0 until 2) Transform(null, null, null, b)

        val video = Video(File("C:\\Users\\Antonio\\Videos\\Captures\\Cities_ Skylines 2020-01-06 19-32-23.mp4"), root)
        val simpleText = SimpleText("Hi! \uD83D\uDE09", root)
        simpleText.color = AnimatedProperty.color().set(Vector4f(1f, 0.3f, 0.3f, 1f))

        val animationWindow = PanelListX(null, style)
        ui += animationWindow

        inspector = PropertyInspector(style, Padding(3,3,3,3))

        sceneView = SceneView(root, style)
        animationWindow += TreeView(root, style)
        animationWindow += sceneView
        animationWindow += inspector
        animationWindow.setWeight(1f)

        val timeline = Timeline(style)
        ui += timeline

        val explorer = FileExplorer(style)
        ui += explorer

        console = TextPanel("Welcome to Rem's Studio!", style.getChild("small"))
        ui += console

        System.setOut(PrintStream(object: OutputStream(){
            var line = ""
            override fun write(b: Int) {
                when {
                    b == '\n'.toInt() -> {
                        console.text = line
                        line = ""
                    }
                    line.length < 100 -> {
                        line += b.toChar()
                    }
                    line.length == 100 -> {
                        line += "..."
                    }
                }
                originalOutput.write(b)
            }
        }))

        windowStack.clear()
        windowStack += Window(ui, 0, 0)

        // todo debug line at the bottom?
    }

    fun check() = GFX.check()

    companion object {
        lateinit var sceneView: SceneView
    }


}