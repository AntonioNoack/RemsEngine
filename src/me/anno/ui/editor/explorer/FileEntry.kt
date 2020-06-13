package me.anno.ui.editor.explorer

import me.anno.studio.Studio
import me.anno.ui.base.IconPanel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.dragging.Draggable
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import java.io.File
import kotlin.math.max
import kotlin.math.min

class FileEntry(val explorer: FileExplorer, val file: File, style: Style): PanelListY(style.getChild("fileEntry")){

    val icon = object: IconPanel("cross.png", style){
        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            val size = max(10, min(w, h))
            minW = size
            minH = size
        }
    }
    val title = TextPanel(file.name, style)

    init {
        val iconBar = PanelListX(style)
        iconBar += SpacePanel(1,1,style).setWeight(1f)
        iconBar += icon.setWeight(3f)
        iconBar += SpacePanel(1,1,style).setWeight(1f)
        this += iconBar
        this += title
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "DragStart" -> {
                Studio.dragged = Draggable(file.toString(), "File", TextPanel(file.nameWithoutExtension, style))
            }
            "Enter" -> {
                if(file.isDirectory){
                    explorer.folder = file
                    explorer.invalidate()
                } else {// todo check if it's a compressed thing we can enter
                    return false
                }
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun printLayout(depth: Int) {
        super.printLayout(depth)
        println("${Tabs.spaces(depth*2+2)} ${file.name}")
    }

    override fun getClassName() = "FileEntry"

}