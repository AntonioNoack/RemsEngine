package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.objects.Transform.Companion.toTransform
import me.anno.studio.RemsStudio.project
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListMultiline
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.OS
import me.anno.utils.clamp
import me.anno.utils.listFiles2
import me.anno.utils.pow
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max

// todo the text size is quite small on my x360 -> get the font size for the ui from the OS :)
// todo double click is not working in touch mode?
// todo make file path clickable to quickly move to a grandparent folder :)
class FileExplorer(style: Style): PanelListY(style.getChild("fileExplorer")){

    // todo somehow gets assigned a huge height... -.-

    // todo a stack or history to know where we were...
    // todo left list of relevant places? todo drag stuff in there

    var folder: File? = project?.scenes ?: File(OS.home, "Documents")

    override fun getLayoutState() = Pair(super.getLayoutState(), folder)

    val searchBar = TextInput("Search Term", style)
        .setChangeListener {
            searchTerm = it
            invalidate()
        }
        .setWeight(1f)

    var searchTerm = ""
    var isValid = 0f

    var entrySize = 100f

    fun invalidate(){
        isValid = 0f
        invalidateLayout()
    }

    val uContent = PanelListX(style)
    val content = PanelListMultiline(style)
    var lastFiles = ""
    val favourites = PanelListY(style)

    val title = PathPanel(folder, style)

    init {
        val topBar = PanelListX(style)
        this += topBar
        topBar += title
        topBar += searchBar
        this += uContent
        title.onChangeListener = {
            folder = it
            invalidate()
        }
        uContent += ScrollPanelY(
            favourites,
            Padding(1),
            style,
            AxisAlignment.MIN
        ).setWeight(1f)
        uContent += content.setWeight(3f)
    }

    var isWorking = false
    fun createResults(){
        if(isWorking) return
        isWorking = true
        thread {
            val children = folder?.listFiles2() ?: File.listRoots().toList()
            val newFiles = children.joinToString { it.name }
            if(lastFiles != newFiles){
                lastFiles = newFiles
                val parent = folder?.parentFile
                if(parent != null){
                    val fe = FileEntry(this, true, parent, style)
                    GFX.addGPUTask(1){ content.clear(); content += fe }
                } else {
                    GFX.addGPUTask(1){ content.clear() }
                }
                val tmpCount = 64
                var tmpList = ArrayList<FileEntry>(tmpCount)
                fun put(){
                    if(tmpList.isNotEmpty()){
                        val list = tmpList
                        GFX.addGPUTask(1){
                            list.forEach { content += it }
                            // force layout update
                            Input.invalidateLayout()
                        }
                        tmpList = ArrayList(tmpCount)
                    }
                }
                children.sortedBy { !it.isDirectory }.forEach { file ->
                    val name = file.name
                    if(!name.startsWith(".")){
                        // todo check if this file is valid, part of the search results
                        // do this async for large folders and slow drives...
                        // todo only display the first ... entries maybe...
                        val fe = FileEntry(this, false, file, style)
                        tmpList.add(fe)
                        if(tmpList.size >= tmpCount) put()
                    }
                }
                put()
            }
            isWorking = false
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if(isValid <= 0f){
            isValid = 5f // depending on amount of files?
            title.file = folder// ?.toString() ?: "This Computer"
            createResults()
        } else isValid -= GFX.deltaTime
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        // todo create links? or truly copy them?
        // todo or just switch?
        folder = files.first()
        if(!folder!!.isDirectory){
            folder = folder!!.parentFile
        }
        invalidate()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when(type){
            "Transform" -> {
                var name = data.toTransform()!!.name.toAllowedFilename()
                if(name != null){
                    // make .json lowercase
                    if(name.endsWith(".json", true)){
                        name = name.substring(0, name.length-5)
                    }
                    name += ".json"
                    // todo replace vs add new?
                    File(folder, name).writeText(data)
                    invalidate()
                }
            }
            else -> super.onPaste(x, y, data, type)
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "Back" -> {
                folder = folder?.parentFile
                invalidate()
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if(Input.isControlDown){
            entrySize = clamp(entrySize * pow(1.05f, dy), 20f, max(w/2f, 20f))
            val esi = entrySize.toInt()
            content.childWidth = esi
            content.childHeight = esi
        } else super.onMouseWheel(x, y, dx, dy)
    }

    companion object {
        val forbiddenCharacters = DefaultConfig["files.forbiddenCharacters", "<>:\"/\\|?*" + (0 .. 31).map { it.toChar() }.joinToString("")].toHashSet()
    }

    // todo buttons for filters, then dir name, search over it?, ...
    // todo drag n drop; links or copy?
    // todo search options
    // todo search results below
    // todo search in text files
    // todo search in meta data for audio and video

    // multiple elements can be selected
    override fun getMultiSelectablePanel() = this

}