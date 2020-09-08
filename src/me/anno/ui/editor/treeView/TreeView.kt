package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.select
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.objects.*
import me.anno.objects.Transform.Companion.toTransform
import me.anno.objects.modes.UVProjection
import me.anno.objects.rendering.RenderSettings
import me.anno.studio.Studio.nullCamera
import me.anno.studio.Studio.root
import me.anno.ui.base.*
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.style.Style
import me.anno.utils.getImportType
import org.joml.Vector3f
import java.io.File
import java.lang.Exception
import kotlin.concurrent.thread

// todo support for multiple cameras? -> just use scenes?
// todo switch back and forth? how -> multiple cameras... how?

// todo a panel for linear video editing
// todo maybe control the cameras there...

// todo select multiple elements, filter for common properties, and apply them all together :)

// todo collapse elements

class TreeView(style: Style):
    ScrollPanelXY(Padding(5), style.getChild("treeView")) {

    val list = content as PanelList

    init { padding.top = 16 }

    val transformByIndex = ArrayList<Transform>()
    var inset = style.getSize("treeView.inset", style.getSize("textSize", 12)/3)

    var index = 0

    fun updateTree(){
        val todo = ArrayList<Pair<Transform, Int>>()
        todo.add(root to 0)
        todo.add(nullCamera to 0)
        todo.add(RenderSettings to 0)
        index = 0
        while(todo.isNotEmpty()){
            val (transform, depth) = todo.removeAt(todo.lastIndex)
            val panel = getOrCreateChild(index++, transform)
            //(panel.parent!!.children[0] as SpacePanel).minW = inset * depth + panel.padding.right
            panel.text = transform.name
            panel.padding.left = inset * depth + panel.padding.right
            if(!transform.isCollapsed){
                todo.addAll(transform.children.map { it to (depth+1) }.reversed())
            }
        }
        for(i in index until list.children.size){
            list.children[i].visibility = Visibility.GONE
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        updateTree()

        if(focused?.isInFocus != true){
            takenElement = null
        }

        val focused = focused
        if(focused != null && takenElement != null){
            val h = focused.h
            val mx = mouseX.toInt()
            val my = mouseY.toInt()
            val hoveredTransformIndex = (my - (list.children.firstOrNull()?.y ?: 0)).toFloat() / (h + list.spacing)
            val fractionalHTI = hoveredTransformIndex % 1f
            if(fractionalHTI in 0.25f .. 0.75f){
                // on top
                // todo add as child
                val targetY = my - 1 + h/2 - (fractionalHTI * h).toInt()
                GFX.drawRect(this.x+2, targetY, 3, 1, -1)
                /*addHereFunction = {
                    transformByIndex.getOrNull(hoveredTransformIndex.toInt())?.addChild(it)
                }*/
            } else {
                // in between
                // todo add in between elements
                val targetY = my - 1 + h/2 - (((hoveredTransformIndex + 0.5f) % 1f) * h).toInt()
                GFX.drawRect(this.x+2, targetY, 3, 1, -1)
                /*addHereFunction = {
                    val inQuestion = transformByIndex.getOrNull(hoveredTransformIndex.roundToInt()) ?: transformByIndex.last()
                    val parent = inQuestion.parent
                    if(parent != null){
                        val index = parent.children.indexOf(inQuestion)
                        parent.children.add(index, it)
                        it.parent = parent
                    }
                    //?.addChild(it)
                }*/
            }
            val x = focused.x
            val y = focused.y
            focused.x = mx - focused.w / 2
            focused.y = my - focused.h / 2
            focused.draw(x0, y0, x1, y1)
            focused.x = x
            focused.y = y
        }

    }

    var focused: Panel? = null
    var takenElement: Transform? = null

    fun getOrCreateChild(index: Int, transform0: Transform): TextPanel {
        if(index < list.children.size){
            transformByIndex[index] = transform0
            val panel = list.children[index] as TextPanel
            panel.visibility = Visibility.VISIBLE
            return panel
        }
        transformByIndex += transform0
        // val list2 = PanelListX(style)
        // list2 += SpacePanel(1, 1, style)
        val child = TreeViewPanel({ transformByIndex[index] }, style)
        child.padding.left = 4
        // todo checkbox with custom icons
        /*list2 += Checkbox(transform0.isCollapsed, child.textSize, style)
            .setChangeListener {
                transformByIndex[index].isCollapsed = it
                updateTree()
            }*/
        //list2 += child
        list += child// += list2
        return child
    }

    // todo display, where we'd move it
    // todo between vs at the end vs at the start
    // todo use the arrow keys to move elements left, right, up, down?
    // todo always give the user hints? :D
    // todo we'd need a selection mode with the arrow keys, too...

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        files.forEach {
            addChildFromFile(root, it)
        }
    }

    override fun getClassName(): String = "TreeView"

    companion object {

        fun addText(name: String, parent: Transform, text: String){
            // important ;)
            // should maybe be done sometimes in object as well ;)
            if(text.length > 500){
                GFX.addGPUTask {
                    GFX.ask("Text has ${text.codePoints().count()} characters, import?"){
                        val textNode = Text(text, parent)
                        textNode.name = name
                        select(textNode)
                    }
                    1
                }
                return
            }
            val textNode = Text(text, parent)
            textNode.name = name
            select(textNode)
        }

        fun addChildFromFile(parent: Transform, file: File, depth: Int = 0){
            if(file.isDirectory){
                val directory = Transform(parent)
                directory.name = file.name
                if(depth < DefaultConfig["import.depth.max", 3]){
                    file.listFiles()?.filter { !it.name.startsWith(".") }?.forEach {
                        addChildFromFile(directory, it, depth+1)
                    }
                }
            } else {
                val name = file.name
                when(file.extension.getImportType()){
                    "Transform" -> thread {
                        val text = file.readText()
                        try {
                            val transform = text.toTransform()
                            parent.addChild(transform)
                            select(transform)
                        } catch (e: Exception){
                            e.printStackTrace()
                            println("Didn't understand json! ${e.message}")
                            addText(name, parent, text)
                        }
                    }
                    "Cubemap-Equ" -> {
                        val cube = Video(file, parent)
                        cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                        cube.uvProjection = UVProjection.Equirectangular
                        cube.name = name
                        select(cube)
                    }
                    "Cubemap-Tiles" -> {
                        val cube = Video(file, parent)
                        cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                        cube.uvProjection = UVProjection.TiledCubemap
                        cube.name = name
                        select(cube)
                    }
                    "Video", "Image" -> {// the same, really ;)
                        // rather use a list of keywords?
                        if(DefaultConfig["import.decideCubemap", true]){
                            val video = Video(file, parent)
                            val fName = file.name
                            if(fName.contains("360", true)){
                                video.scale.set(Vector3f(1000f, 1000f, 1000f))
                                video.uvProjection = UVProjection.Equirectangular
                            } else
                            if(fName.contains("cubemap", true)){
                                video.scale.set(Vector3f(1000f, 1000f, 1000f))
                                video.uvProjection = UVProjection.TiledCubemap
                            }
                            video.name = fName
                            select(video)
                        } else {
                            val video = Video(file, parent)
                            video.name = name
                            select(video)
                        }
                    }
                    "Text" -> {
                        try {
                            addText(name, parent, file.readText())
                        } catch (e: Exception){
                            e.printStackTrace()
                            return
                        }
                    }
                    "HTML" -> {
                        // parse html? maybe, but html and css are complicated
                        // rather use screenshots or svg...
                        // integrated browser?
                    }
                    "Markdeep", "Markdown" -> {
                        // execute markdeep script or interpret markdown to convert it to html? no
                        // I see few use-cases
                    }
                    "Audio" -> {
                        val audio = Audio(file, parent)
                        audio.name = name
                        select(audio)
                    }
                    else -> println("Unknown file type: ${file.extension}")
                }
            }
        }
    }


}