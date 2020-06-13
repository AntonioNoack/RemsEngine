package me.anno.ui.editor

import me.anno.config.DefaultStyle.white
import me.anno.gpu.GFX
import me.anno.objects.cache.Cache
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.editor.explorer.FileExplorer
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.style.Style

class CustomContainer(default: Panel, style: Style): PanelContainer(default, Padding(0), style){

    override fun calculateSize(w: Int, h: Int) {
        child.calculateSize(w, h)
        minW = child.minW
        minH = child.minH
    }

    override fun applyConstraints() {
        child.applyConstraints()
        w = child.w
        h = child.h
    }

    override fun placeInParent(x: Int, y: Int) {
        child.placeInParent(x, y)
        this.x = x
        this.y = y
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val icon = Cache.getIcon("cross.png")
        GFX.drawTexture(x+w-14, y+2, 12, 12, icon, white)
    }

    fun changeType(){
        fun action(action: () -> Panel) = { b: Int, l: Boolean ->
            child = action()
            child.parent = this
            true
        }
        GFX.openMenu(x+w-16, y, "", listOf(
            "Scene View" to action { SceneView(style) },
            "Tree View" to action { TreeView(style) },
            "Inspector" to action { PropertyInspector(style) },
            "Graph Editor" to action { GraphEditor(style) },
            "Files" to action { FileExplorer(style) }
        ))
    }

    override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
        clicked(x, y)
    }

    fun clicked(x: Float, y: Float): Boolean {
        return if(x-(this.x+w-16f) in 0f .. 16f && y-this.y in 0f .. 16f){
            changeType()
            true
        } else false
    }

}