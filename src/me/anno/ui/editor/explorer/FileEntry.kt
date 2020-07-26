package me.anno.ui.editor.explorer

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.objects.cache.Cache
import me.anno.studio.Studio
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import me.anno.utils.getImportType
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

class FileEntry(val explorer: FileExplorer, val isParent: Boolean, val file: File, style: Style): Panel(style.getChild("fileEntry")){

    val size = 100
    val importType = file.extension.getImportType()
    var iconPath = if(file.isDirectory){
        if(file.listFiles()?.isNotEmpty() == true)
            "file/folder.png" else "file/empty_folder.png"
    } else {
        when(importType){
            "Image", "Cubemap" -> "file/image.png"
            "Text" -> "file/text.png"
            // todo dark/bright styled images
            // todo dark image for video -> one of the first frames? :)
            "Audio", "Video" -> "file/music.png"
            else -> "file/document.png"
        }
    }

    val title = TextPanel(if(isParent) ".." else if(file.name.isEmpty()) file.toString() else file.name, style)
    init { title.backgroundColor = black }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = size
        minH = size
        this.w = size
        this.h = size
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val originalIcon = if(file.length() < 10e6){
            when(importType){
                // todo preview for video
                "Image" -> Cache.getImage(file, 1000, true)
                else -> null
            }
        } else null
        val image = originalIcon ?: Cache.getIcon(iconPath, true) ?: GFX.whiteTexture
        var iw = image.w
        var ih = image.h
        val scale = (size-20)/max(iw, ih).toFloat()
        iw = (iw * scale).roundToInt()
        ih = (ih * scale).roundToInt()
        image.ensureFiltering(false)
        GFX.drawTexture(x+(size-iw)/2, y+(size-ih)/2, iw, ih, image, -1, null)
        title.x = x
        title.y = y
        title.w = 1
        title.h = 1
        title.draw(x0, y0, x1, y1)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "DragStart" -> {
                if(Studio.dragged?.getOriginal() != file){
                    Studio.dragged = Draggable(file.toString(), "File", file, TextPanel(file.nameWithoutExtension, style))
                }
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

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Tabs.spaces(tabDepth*2+2)} ${file.name}")
    }

    override fun getClassName() = "FileEntry"

}