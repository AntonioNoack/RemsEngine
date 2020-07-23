package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.objects.*
import me.anno.studio.RemsStudio.console
import me.anno.studio.RemsStudio.originalOutput
import me.anno.studio.RemsStudio.windowStack
import me.anno.studio.Studio.project
import me.anno.studio.Studio.root
import me.anno.studio.Studio.targetDuration
import me.anno.studio.Studio.targetFPS
import me.anno.studio.Studio.targetOutputFile
import me.anno.studio.Studio.targetHeight
import me.anno.studio.Studio.targetWidth
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomListX
import me.anno.ui.custom.CustomListY
import me.anno.ui.editor.*
import me.anno.ui.editor.explorer.FileExplorer
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.treeView.TreeView
import me.anno.video.VideoAudioCreator
import me.anno.video.VideoCreator
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.lang.RuntimeException
import kotlin.math.max
import kotlin.math.roundToInt

object UILayouts {

    fun createLoadingUI(){

        val style = DefaultConfig.style

        val ui = PanelListY(style)
        val customUI = CustomListY(style)
        customUI.setWeight(10f)

        RemsStudio.ui = ui

    }

    fun renderPart(size: Int){
        render(targetWidth/size, targetHeight/size)
    }

    fun render(width: Int, height: Int){
        val tmpFile = File(targetOutputFile.parentFile, targetOutputFile.nameWithoutExtension+".tmp."+targetOutputFile.extension)
        val fps = targetFPS
        val totalFrameCount = (fps * targetDuration).toInt()
        val sampleRate = 48000
        VideoAudioCreator(VideoCreator(width, height,
            targetFPS, totalFrameCount, tmpFile), sampleRate, targetOutputFile).start()
    }

    fun createEditorUI(){

        val style = DefaultConfig.style

        val ui = PanelListY(style)
        val customUI = CustomListY(style)
        customUI.setWeight(10f)

        RemsStudio.ui = ui

        // todo show the file location up there, too?
        // todo fully customizable content
        val options = OptionBar(style)
        options.addMajor("File")
        options.addMajor("Edit")
        options.addMajor("View")
        options.addMajor("Navigate")
        options.addMajor("Code")

        options.addAction("File", "Save"){ Input.save() }
        options.addAction("File", "Load"){  }

        options.addAction("Render", "Set%"){
            render(
                max(1, (project!!.targetWidth * project!!.targetSizePercentage / 100).roundToInt()),
                max(1, (project!!.targetHeight * project!!.targetSizePercentage / 100).roundToInt())) }
        options.addAction("Render", "Full"){ renderPart(1) }
        options.addAction("Render", "Half"){ renderPart(2) }
        options.addAction("Render", "Quarter"){ renderPart(4) }

        ui += options

        root = Transform(null)
        root.name = "Root"
        // val a = Transform(Vector3f(10f, 50f, 0f), Vector3f(1f,1f,1f), Quaternionf(1f,0f,0f,0f), root)
        // for(i in 0 until 3) Transform(null, null, null, a)
        // val b = Transform(null, null, null, root)
        // for(i in 0 until 2) Transform(null, null, null, b)

        Camera(root)
        // Video(File("C:\\Users\\Antonio\\Videos\\Captures\\Cities_ Skylines 2020-01-06 19-32-23.mp4"), GFX.root)
        // Text("Hi! \uD83D\uDE09", GFX.root)
        // Image(File("C:/Users/Antonio/Downloads/tiger.svg"), root).position.addKeyframe(0f, Vector3f(0f, 0f, 0.01f), 0.1f)

        val animationWindow = CustomListX(style)
        customUI.add(animationWindow, 200f)

        animationWindow.add(CustomContainer(TreeView(style), style), 50f)
        animationWindow.add(CustomContainer(SceneView(style), style), 200f)
        animationWindow.add(CustomContainer(PropertyInspector(style), style), 50f)
        animationWindow.setWeight(1f)

        val timeline = GraphEditor(style)
        customUI.add(CustomContainer(timeline, style), 50f)

        val explorer = FileExplorer(style)
        customUI.add(CustomContainer(explorer, style), 50f)

        ui += SpacePanel(0, 1, style)
        ui += customUI
        ui += SpacePanel(0, 1, style)
        
        console = TextPanel("Welcome to Rem's Studio!", style.getChild("small"))
        console.instantTextLoading = true
        // console.visibility = Visibility.GONE
        console.setOnClickListener { _, _, button, _ ->
                if(button == 0){
                    // todo open console in large with scrollbar
                }
            }
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

        windowStack.clear()
        windowStack += Window(ui, 0, 0)

    }

    fun printLayout(){
        println("Layout:")
        for (window1 in GFX.windowStack) {
            window1.panel.printLayout(1)
        }
    }

}