package me.anno.ui.impl

import me.anno.ui.base.ScrollPanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListMultiline
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.impl.explorer.FileEntry
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import java.io.File

class FileExplorer(style: Style): PanelListY(style.getChild("fileExplorer")){

    var folder = File(System.getProperty("user.home")+"/Desktop/")

    val searchBar = TextInput("Search Term", style)
        .setChangeListener {
            searchTerm = it
            invalidate()
        }
        .setWeight(1f)

    var searchTerm = ""
    var isValid = false

    fun invalidate(){
        isValid = false
    }

    val content = PanelListMultiline(null, style)

    init {
        val topBar = PanelListX(style)
        this += topBar
        topBar += TextPanel("Xplorer", style)
        topBar += searchBar
        this += ScrollPanel(content, Padding(1), style, WrapAlign.AxisAlignment.MIN)
    }

    fun createResults(){
        content.clear()
        val parent = folder.parentFile
        if(parent != null){
            content += FileEntry(folder.parentFile, style)
        }
        folder.listFiles()?.forEach { file ->
            val name = file.name
            if(!name.startsWith(".")){
                // todo check if this file is valid, part of the search results
                // todo do this async for large folders
                // todo only display the first ... entries maybe...
                content += FileEntry(file, style)
            }
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        if(!isValid){
            isValid = true
            createResults()
        }
    }

    // todo buttons for filters, then dir name, search over it?, ...
    // todo drag n drop; links or copy?
    // todo search options
    // todo search results below
    // todo search in text files
    // todo search in meta data for audio and video

}