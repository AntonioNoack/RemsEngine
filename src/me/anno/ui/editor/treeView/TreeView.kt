package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.nullCamera
import me.anno.gpu.GFX.root
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.objects.*
import me.anno.objects.Transform.Companion.toTransform
import me.anno.ui.base.*
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style
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
    ScrollPanelXY(Padding(1), style.getChild("treeView")) {

    val list = content as PanelList

    init { padding.top = 16 }

    val transformByIndex = ArrayList<Transform>()
    var inset = style.getSize("treeView.inset", style.getSize("textSize", 12)/3)

    var index = 0

    fun updateTree(){
        val todo = ArrayList<Pair<Transform, Int>>()
        todo.add(root to 0)
        todo.add(nullCamera to 0)
        index = 0
        while(todo.isNotEmpty()){
            val (transform, depth) = todo.removeAt(todo.lastIndex)
            val panel = getOrCreateChild(index++, transform)
            (panel.parent!!.children[0] as SpacePanel).minW = inset * depth + panel.padding.right
            panel.text = transform.name
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
            val list2 = list.children[index] as PanelGroup
            val panel = list2.children[2] as TextPanel
            list2.visibility = Visibility.VISIBLE
            return panel
        }
        transformByIndex += transform0
        val list2 = PanelListX(style)
        list2 += SpacePanel(1, 1, style)
        val child = TreeViewPanel({ transformByIndex[index] }, style)
        child.padding.left = 4
        // todo checkbox with custom icons
        list2 += Checkbox(transform0.isCollapsed, child.textSize, style)
            .setChangeListener {
                transformByIndex[index].isCollapsed = it
                updateTree()
            }
        list2 += child
        list += list2
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
                val ending = file.name.split('.').last()
                val name = file.name
                val type0 = DefaultConfig["import.mapping.$ending"]
                val type1 = DefaultConfig["import.mapping.${ending.toLowerCase()}"]
                val type2 = DefaultConfig["import.mapping.*"] ?: "Text"
                when((type0 ?: type1 ?: type2).toString()){
                    "Transform" -> thread { parent.addChild(file.readText().toTransform()) }
                    "Image" -> Image(file, parent).name = name
                    "Cubemap" -> {
                        val cube = Cubemap(file, parent)
                        cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                        cube.name = name
                    }
                    "Video" -> Video(file, parent).name = name
                    "Text" -> {
                        try {
                            var all = file.readText()
                            if(all.length > 500) all = all.substring(0, 500)
                            Text(all, parent).name = name
                        } catch (e: Exception){
                            e.printStackTrace()
                            return
                        }
                    }
                    "Markdown" -> {
                        // todo parse, and create constructs?
                        println("Markdown is not yet implemented!")
                    }
                    "Audio" -> Audio(file, parent).name = name
                    else -> println("Unknown file type: $ending")
                }
            }
        }
    }

}