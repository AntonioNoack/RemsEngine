package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.FileRootRef
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.objects.Transform.Companion.toTransform
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.rems.RemsStudio.project
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListMultiline
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.pow
import me.anno.utils.OS
import me.anno.utils.Threads.threadWithName
import me.anno.utils.files.Files.listFiles2
import org.apache.logging.log4j.LogManager
import kotlin.math.max

// done the text size is quite small on my x360 -> get the font size for the ui from the OS :)
// todo double click is not working in touch mode?
// todo make file path clickable to quickly move to a grandparent folder :)

// todo buttons for filters, then dir name, search over it?, ...
// todo drag n drop; links or copy?
// done search options
// done search results below
// todo search in text files
// todo search in meta data for audio and video

// todo list view

// done a stack or history to know where we were
// todo left list of relevant places? todo drag stuff in there

class FileExplorer(style: Style) : PanelListY(style.getChild("fileExplorer")) {

    val searchBar = TextInput("Search Term", false, style)
        .setChangeListener {
            searchTerm = it
            invalidate()
        }
        .setWeight(1f)

    var historyIndex = 0
    val history = arrayListOf(project?.scenes ?: OS.documents)

    val folder get() = history[historyIndex]

    var searchTerm = ""
    var isValid = 0f

    var entrySize = 64f
    val minEntrySize = 32f

    fun invalidate() {
        isValid = 0f
        invalidateLayout()
    }

    val uContent = PanelListX(style)
    val content = PanelListMultiline(style)
    var lastFiles = emptyList<String>()
    var lastSearch = true

    val favourites = PanelListY(style)

    val title = PathPanel(folder, style)

    init {
        val esi = entrySize.toInt()
        content.childWidth = esi
        content.childHeight = esi * 4 / 3
        val topBar = PanelListX(style)
        this += topBar
        topBar += title
        topBar += searchBar
        this += uContent
        title.onChangeListener = {
            switchTo(it)
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

    fun removeOldFiles() {
        content.children.forEach { (it as? FileExplorerEntry)?.stopPlayback() }
        content.clear()
    }

    var isWorking = false
    fun createResults() {
        if (isWorking) return
        isWorking = true
        threadWithName("FileExplorer-Query") {

            val search = Search(searchTerm)

            var level0: List<FileReference> = folder.listFiles2()
                .filter { !it.name.startsWith('.') }
            val newFiles = level0.map { it.name }
            val newSearch = search.isNotEmpty()

            if (lastFiles != newFiles || lastSearch != newSearch) {

                lastFiles = newFiles
                lastSearch = newSearch

                // when searching something, also include sub-folders up to depth of xyz
                val searchDepth = 3
                val fileLimit = 10000
                if (search.isNotEmpty() && level0.size < fileLimit) {
                    var lastLevel = level0
                    var nextLevel = ArrayList<FileReference>()
                    for (i in 0 until searchDepth) {
                        for (file in lastLevel) {
                            if (file.name.startsWith('.')) continue
                            if (file.isDirectory || when (file.extension.lowercase()) {
                                    "zip", "rar", "7z", "s7z", "jar", "tar", "gz", "xz",
                                    "bz2", "lz", "lz4", "lzma", "lzo", "z", "zst" -> file.isPacked.value
                                    else -> false
                                }
                            ) {
                                nextLevel.addAll(file.listChildren() ?: continue)
                            }
                        }
                        level0 = level0 + nextLevel
                        if (level0.size > fileLimit) break
                        lastLevel = nextLevel
                        nextLevel = ArrayList()
                    }
                }

                val parent = folder.getParent()
                if (parent != null) {
                    val fe = FileExplorerEntry(this, true, parent, style)
                    GFX.addGPUTask(1) { removeOldFiles(); content += fe }
                } else {
                    GFX.addGPUTask(1) { removeOldFiles() }
                }

                val tmpCount = 64
                var tmpList = ArrayList<FileExplorerEntry>(tmpCount)

                fun put() {
                    if (tmpList.isNotEmpty()) {
                        val list = tmpList
                        addEvent {
                            for (it in list) {
                                content += it
                            }
                            // force layout update
                            Input.invalidateLayout()
                        }
                        tmpList = ArrayList(tmpCount)
                    }
                }

                for (file in level0.filter { it.isDirectory }) {
                    val fe = FileExplorerEntry(this, false, file, style)
                    tmpList.add(fe)
                    if (tmpList.size >= tmpCount) put()
                }

                for (file in level0.filter { !it.isDirectory }) {
                    val fe = FileExplorerEntry(this, false, file, style)
                    tmpList.add(fe)
                    if (tmpList.size >= tmpCount) put()
                }

                put()

            } else {
                val fe = content.children.filterIsInstance<FileExplorerEntry>()
                for (it in fe) {
                    it.visibility = Visibility[search.matches(it.file.name)]
                }
            }
            isWorking = false
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if (isValid <= 0f) {
            isValid = 5f // depending on amount of files?
            title.file = folder// ?.toString() ?: "This Computer"
            title.tooltip = if (folder == FileRootRef) "This Computer" else folder.toString()
            createResults()
        } else isValid -= GFX.deltaTime
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        // todo create links? or truly copy them?
        // todo or just switch?
        switchTo(files.first())
        invalidate()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "Transform" -> {
                pasteTransform(data)
            }
            else -> {
                if (!pasteTransform(data)) {
                    super.onPaste(x, y, data, type)
                }
            }
        }
    }

    fun pasteTransform(data: String): Boolean {
        val transform = data.toTransform() ?: return false
        var name = transform.name.toAllowedFilename() ?: transform.defaultDisplayName
        // make .json lowercase
        if (name.endsWith(".json", true)) {
            name = name.substring(0, name.length - 5)
        }
        name += ".json"
        // todo replace vs add new?
        folder.getChild(name).writeText(data)
        invalidate()
        return true
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "OpenOptions" -> {
                val home = folder
                openMenu(
                    listOf(
                        MenuOption(NameDesc("Create Folder", "Creates a new directory", "ui.newFolder")) {
                            askName(
                                x.toInt(),
                                y.toInt(),
                                NameDesc("Name", "", "ui.newFolder.askName"),
                                "",
                                NameDesc("Create"),
                                { -1 }) {
                                val validName = it.toAllowedFilename()
                                if (validName != null) {
                                    getReference(home, validName).mkdirs()
                                    invalidate()
                                }
                            }
                        },
                        MenuOption(NameDesc("Create Component", "Create a new folder component", "ui.newComponent")) {
                            askName(
                                x.toInt(),
                                y.toInt(),
                                NameDesc("Name", "", "ui.newComponent.askName"),
                                "",
                                NameDesc("Create"),
                                { -1 }) {
                                val validName = it.toAllowedFilename()
                                if (validName != null) {
                                    getReference(home, "${validName}.json").writeText(Transform()
                                        .apply { name = it }
                                        .toString())
                                    invalidate()
                                }
                            }
                        },
                        MenuOption(
                            NameDesc(
                                "Open In Explorer",
                                "Show the file in your default file explorer",
                                "ui.file.openInExplorer"
                            )
                        ) {
                            folder.openInExplorer()
                        }
                    ))
            }
            "Back", "Backward" -> back()
            "Forward" -> forward()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun back() {
        if (historyIndex > 0) {
            historyIndex--
            invalidate()
        } else {
            val element = folder.getParent() ?: return
            history.clear()
            history.add(element)
            historyIndex = 0
            invalidate()
        }
    }

    fun forward() {
        if (historyIndex + 1 < history.size) {
            historyIndex++
            invalidate()
        } else {
            LOGGER.info("End of history reached!")
        }
    }

    fun switchTo(folder: FileReference?) {
        folder ?: return
        if (!folder.isDirectoryOrPacked) {
            switchTo(folder.getParent())
        } else {
            while (history.lastIndex > historyIndex) history.removeAt(history.lastIndex)
            history.add(folder)
            historyIndex++
            invalidate()
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isControlDown) {
            entrySize = clamp(entrySize * pow(1.05f, dy), minEntrySize, max(w / 2f, 20f))
            val esi = entrySize.toInt()
            content.childWidth = esi
            content.childHeight = esi * 4 / 3
        } else super.onMouseWheel(x, y, dx, dy)
    }

    // multiple elements can be selected
    override fun getMultiSelectablePanel() = this

    override val className get() = "FileExplorer"

    companion object {
        private val LOGGER = LogManager.getLogger(FileExplorer::class)
        private val forbiddenConfig =
            DefaultConfig["files.forbiddenCharacters", "<>:\"/\\|?*"] + String(CharArray(32) { it.toChar() })
        val forbiddenCharacters = forbiddenConfig.toHashSet()
    }

}