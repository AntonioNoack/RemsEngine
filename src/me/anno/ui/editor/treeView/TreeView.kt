package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXx2D.drawRect
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.objects.Transform
import me.anno.studio.RemsStudio.nullCamera
import me.anno.studio.RemsStudio.root
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.editor.files.addChildFromFile
import me.anno.ui.style.Style
import java.io.File

// todo select multiple elements, filter for common properties, and apply them all together :)

class TreeView(style: Style) :
    ScrollPanelXY(Padding(5), style.getChild("treeView")) {

    val list = content as PanelList

    init {
        padding.top = 16
    }

    val transformByIndex = ArrayList<Transform>()
    var inset = style.getSize("treeView.inset", style.getSize("textSize", 12) / 3)

    var index = 0

    fun updateTree() {
        val open = ArrayList<Pair<Transform, Int>>()
        open.add(root to 0)
        open.add(nullCamera to 0)
        // open.add(RenderSettings to 0)
        index = 0
        while (open.isNotEmpty()) {
            val (transform, depth) = open.removeAt(open.lastIndex)
            val panel = getOrCreateChild(index++, transform)
            //(panel.parent!!.children[0] as SpacePanel).minW = inset * depth + panel.padding.right
            val symbol =
                if (transform.isCollapsed) DefaultConfig["ui.symbol.collapsed", "\uD83D\uDDBF"]
                else transform.getSymbol()
            panel.text = if (symbol.isEmpty()) transform.name else "$symbol ${transform.name}"
            panel.padding.left = inset * depth + panel.padding.right
            if (!transform.isCollapsed) {
                open.addAll(transform.children.map { it to (depth + 1) }.reversed())
            }
        }
        for (i in index until list.children.size) {
            list.children[i].visibility = Visibility.GONE
        }
    }

    override fun tickUpdate() {
        super.tickUpdate()
        updateTree()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        if (focused?.isInFocus != true) {
            takenElement = null
        }

        val focused = focused
        if (focused != null && takenElement != null) {
            val h = focused.h
            val mx = mouseX.toInt()
            val my = mouseY.toInt()
            val hoveredTransformIndex = (my - (list.children.firstOrNull()?.y ?: 0)).toFloat() / (h + list.spacing)
            val fractionalHTI = hoveredTransformIndex % 1f
            if (fractionalHTI in 0.25f..0.75f) {
                // on top
                // add as child
                val targetY = my - 1 + h / 2 - (fractionalHTI * h).toInt()
                drawRect(this.x + 2, targetY, 3, 1, -1)
            } else {
                // in between
                // add in between elements
                val targetY = my - 1 + h / 2 - (((hoveredTransformIndex + 0.5f) % 1f) * h).toInt()
                drawRect(this.x + 2, targetY, 3, 1, -1)
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
        if (index < list.children.size) {
            transformByIndex[index] = transform0
            val panel = list.children[index] as TextPanel
            panel.visibility = Visibility.VISIBLE
            return panel
        }
        transformByIndex += transform0
        val child = TreeViewPanel({ transformByIndex[index] }, style)
        child.padding.left = 4
        // todo checkbox with custom icons
        list += child
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

}