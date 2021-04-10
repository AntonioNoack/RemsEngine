package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.gpu.GFXx2D.drawRect
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.objects.Transform
import me.anno.objects.Transform.Companion.toTransform
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.root
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.editor.files.ImportFromFile.addChildFromFile
import me.anno.ui.editor.treeView.TreeViewPanel.Companion.openAddMenu
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

    private val inset = style.getSize("textSize", 12) / 3
    private val collapsedSymbol = DefaultConfig["ui.symbol.collapsed", "\uD83D\uDDBF"]

    private fun addToTree(transform: Transform, depth: Int, index0: Int): Int {
        var index = index0
        val panel = getOrCreateChild(index++, transform)
        //(panel.parent!!.children[0] as SpacePanel).minW = inset * depth + panel.padding.right
        val symbol = if (transform.isCollapsed) collapsedSymbol else transform.getSymbol()
        panel.setText(symbol.trim(), transform.name)
        val padding = panel.padding
        val left = inset * depth + padding.right
        if(padding.left != left){
            padding.left = left
            invalidateLayout()
        }
        if (!transform.isCollapsed) {
            val children = transform.children
            for (child in children) {
                index = addToTree(child, depth + 1, index)
            }
        }
        invalidateLayout()
        return index
    }

    private fun updateTree() {
        val camIndex = addToTree(nullCamera!!, 0, 0)
        val index = addToTree(root, 0, camIndex)
        for (i in index until list.children.size) {
            val child = list.children[i]
            if(child.visibility != Visibility.GONE){
                child.visibility = Visibility.GONE
                invalidateLayout()
            }
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

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if (button.isRight) {
            openAddMenu(root)
        } else super.onMouseClicked(x, y, button, long)
    }

    var focused: Panel? = null
    var takenElement: Transform? = null

    fun getOrCreateChild(index: Int, transform0: Transform): TreeViewPanel {
        if (index < list.children.size) {
            transformByIndex[index] = transform0
            val panel = list.children[index] as TreeViewPanel
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

    // done display, where we'd move it
    // done between vs at the end vs at the start
    // todo we'd need a selection mode with the arrow keys, too...

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if (!pasteTransform(data)) {
            super.onPaste(x, y, data, type)
        }
    }

    fun pasteTransform(data: String): Boolean {
        val transform = data.toTransform() ?: return false
        RemsStudio.largeChange("Pasted ${transform.name}") {
            root.addChild(transform)
        }
        return true
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        files.forEach { addChildFromFile(root, it, null, true) {} }
    }

}