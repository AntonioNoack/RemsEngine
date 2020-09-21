package me.anno.ui.editor.treeView

import me.anno.gpu.GFX
import me.anno.gpu.GFX.select
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.objects.*
import me.anno.objects.rendering.RenderSettings
import me.anno.studio.Studio.nullCamera
import me.anno.studio.Studio.root
import me.anno.ui.base.*
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.editor.files.addChildFromFile
import me.anno.ui.style.Style
import java.io.File

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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

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
                // add as child
                val targetY = my - 1 + h/2 - (fractionalHTI * h).toInt()
                GFX.drawRect(this.x+2, targetY, 3, 1, -1)
                /*addHereFunction = {
                    transformByIndex.getOrNull(hoveredTransformIndex.toInt())?.addChild(it)
                }*/
            } else {
                // in between
                // add in between elements
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
            addChildFromFile(root, it, {})
        }
    }

    companion object {

        fun addText(name: String, parent: Transform?, text: String, callback: (Transform) -> Unit){
            // important ;)
            // should maybe be done sometimes in object as well ;)
            if(text.length > 500){
                GFX.addGPUTask(text.length * 15, 30){
                    GFX.ask("Text has ${text.codePoints().count()} characters, import?"){
                        val textNode = Text(text, parent)
                        textNode.name = name
                        select(textNode)
                        callback(textNode)
                    }
                }
                return
            }
            val textNode = Text(text, parent)
            textNode.name = name
            select(textNode)
            callback(textNode)
        }


    }


}