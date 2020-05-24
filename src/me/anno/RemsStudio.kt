package me.anno

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.run.startTime
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.impl.*
import java.io.File
import java.util.*

class RemsStudio {

    var lastWidth = 0
    var lastHeight = 0

    val windowStack = Stack<Panel>()

    lateinit var inspector: PropertyInspector

    fun run(){

        // val src = File("C:\\Users\\Antonio\\Videos\\Captures", "Cities_ Skylines 2020-01-06 19-32-23.mp4")
        // FFMPEGStream.getImageSequence(src)

        createUI()
        GFX.windowStack = windowStack
        GFX.gameLoop = { w, h ->
            check()

            if(GFX.previousSelectedTransform != GFX.selectedTransform){
                inspector.list.clear()
                GFX.previousSelectedTransform = GFX.selectedTransform
                GFX.selectedTransform?.createInspector(inspector.list)
            }

            windowStack.forEach {
                val t0 = System.nanoTime()
                it.calculateSize(w,h)
                val t1 = System.nanoTime()
                it.placeInParent(0, 0)
                val t2 = System.nanoTime()
                val dt1 = (t1-t0)*1e-9f
                val dt2 = (t2-t1)*1e-9f
                if(dt1 > 0.1f) println("Warn: Used ${dt1}s + ${dt2}s for layout")
                it.draw(0,0,w,h)
            }

            check()

            if(frameCtr == 0){
                println("Used ${(System.nanoTime()-startTime)*1e-9f}s from start to finishing the first frame")
            }
            frameCtr++

            false
        }
        GFX.shutdown = {

        }
        // GFX.init()
        GFX.run()
    }

    var frameCtr = 0

    lateinit var startMenu: Panel
    lateinit var ui: Panel

    fun createUI(){

        val style = DefaultConfig.style

        val ui = PanelListY(null, style)
        this.ui = ui
        // todo top ui bar
        // todo show the file location up there, too?
        // todo fully customizable content
        val options = OptionBar(style)
        options.addMajor("File")
        options.addMajor("Edit")
        options.addMajor("View")
        options.addMajor("Navigate")
        options.addMajor("Code")
        ui += options

        val root = Transform(null, null, null, null)
        root.name = "Root"
        // val a = Transform(Vector3f(10f, 50f, 0f), Vector3f(1f,1f,1f), Quaternionf(1f,0f,0f,0f), root)
        // for(i in 0 until 3) Transform(null, null, null, a)
        // val b = Transform(null, null, null, root)
        // for(i in 0 until 2) Transform(null, null, null, b)

        val video = Video(File("C:\\Users\\Antonio\\Videos\\Captures\\Cities_ Skylines 2020-01-06 19-32-23.mp4"), root)

        val animationWindow = PanelListX(null, style)
        ui += animationWindow

        inspector = PropertyInspector(style, Padding(3,3,3,3))

        sceneView = SceneView(root, style)
        animationWindow += TreeView(root, style)
        animationWindow += sceneView
        animationWindow += inspector
        animationWindow.setWeight(1f)

        val explorer = FileExplorer(style)
        ui += explorer



        val timeline = Timeline(style)
        ui += timeline

        windowStack.clear()
        windowStack += ui

        // todo debug line at the bottom?
    }

    fun check() = GFX.check()

    companion object {
        lateinit var sceneView: SceneView
    }


}