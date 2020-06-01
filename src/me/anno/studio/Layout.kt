package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.root
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.objects.*
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.RemsStudio.console
import me.anno.studio.RemsStudio.inspector
import me.anno.studio.RemsStudio.originalOutput
import me.anno.studio.RemsStudio.windowStack
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomListX
import me.anno.ui.custom.CustomListY
import me.anno.ui.editor.*
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.editor.timeline.Timeline
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

object Layout {

    fun createUI(){

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


        ui += options

        GFX.root = Transform(null)
        GFX.root.name = "Root"
        // val a = Transform(Vector3f(10f, 50f, 0f), Vector3f(1f,1f,1f), Quaternionf(1f,0f,0f,0f), root)
        // for(i in 0 until 3) Transform(null, null, null, a)
        // val b = Transform(null, null, null, root)
        // for(i in 0 until 2) Transform(null, null, null, b)

        Camera(GFX.root)
        Video(File("C:\\Users\\Antonio\\Videos\\Captures\\Cities_ Skylines 2020-01-06 19-32-23.mp4"), GFX.root)
        val simpleText = Text("Hi! \uD83D\uDE09", GFX.root)
        simpleText.color = AnimatedProperty.color().set(Vector4f(1f, 0.3f, 0.3f, 1f))

        Image(File("C:/Users/Antonio/Downloads/tiger.svg"), root).position.addKeyframe(0f, Vector3f(0f, 0f, 0.01f), 0.1f)

        val animationWindow = CustomListX(style)
        customUI.add(animationWindow, 200f)

        inspector = PropertyInspector(style, Padding(3,3,3,3))

        RemsStudio.sceneView = SceneView(style)
        animationWindow.add(CustomContainer(TreeView(style), style), 50f)
        animationWindow.add(CustomContainer(RemsStudio.sceneView, style), 200f)
        animationWindow.add(CustomContainer(inspector, style), 50f)
        animationWindow.setWeight(1f)

        val timeline = Timeline(style)
        customUI.add(timeline, 50f)

        val explorer = FileExplorer(style)
        customUI.add(explorer, 50f)

        ui += SpacePanel(0, 1, style)
        ui += customUI
        ui += SpacePanel(0, 1, style)


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

    }

}