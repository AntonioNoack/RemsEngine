package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.GFX.windowStack
import me.anno.input.Input
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.FileRootRef
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform.Companion.toTransform
import me.anno.studio.StudioBase.Companion.addEvent
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
import me.anno.ui.editor.files.FileExplorerEntry.Companion.deleteFileMaybe
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.pow
import me.anno.utils.OS
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.files.Files.listFiles2
import me.anno.utils.hpc.UpdatingTask
import me.anno.utils.structures.Compare.ifDifferent
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

abstract class FileExplorer(
    initialLocation: FileReference?,
    style: Style
) : PanelListY(style.getChild("fileExplorer")) {

    abstract fun getRightClickOptions(): List<FileExplorerOption>

    open fun openOptions(file: FileReference) {
        openMenu(getFileOptions().map {
            MenuOption(it.nameDesc) {
                it.onClick(file)
            }
        })
    }

    open fun getFileOptions(): List<FileExplorerOption> {
        // todo additional options for the game engine, e.g. create prefab, open as scene
        // todo add option to open json in specialized json editor...
        val rename = FileExplorerOption(NameDesc("Rename", "Change the name of this file", "ui.file.rename")) {
            onGotAction(0f, 0f, 0f, 0f, "Rename", false)
        }
        val openInExplorer = FileExplorerOption(
            NameDesc(
                "Open in Explorer",
                "Open the file in your default file explorer",
                "ui.file.openInExplorer"
            )
        ) { it.openInExplorer() }
        val delete = FileExplorerOption(
            NameDesc("Delete", "Delete this file", "ui.file.delete"),
        ) { deleteFileMaybe(it) }
        return listOf(rename, openInExplorer, delete)
    }

    abstract fun onDoubleClick(file: FileReference)

    val searchBar = TextInput("Search Term", "", false, style)
        .setChangeListener {
            searchTerm = it
            invalidate()
        }
        .setWeight(1f)

    var historyIndex = 0
    val history = arrayListOf(initialLocation ?: OS.documents)

    val folder get() = history[historyIndex]

    var searchTerm = ""
    var isValid = 0f

    var entrySize = 64f
    val minEntrySize = 32f

    val uContent = PanelListX(style)
    val content = PanelListMultiline({ a, b ->
        // define the order for the file entries:
        // first .., then folders, then files
        // first a, then z, ...
        // not all folders may be sorted
        a as FileExplorerEntry
        b as FileExplorerEntry
        (b.isParent.compareTo(a.isParent)).ifDifferent {
            val af = a.file
            val bf = b.file
            bf.isDirectory.compareTo(af.isDirectory).ifDifferent {
                a.file.name.compareTo(b.file.name, true)
            }
        }
    }, style)

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

    fun invalidate() {
        isValid = 0f
        invalidateLayout()
    }

    fun removeOldFiles() {
        content.children.forEach { (it as? FileExplorerEntry)?.stopPlayback() }
        content.clear()
    }

    // todo when searching, use a thread for that
    // todo regularly sleep 0ms inside of it:
    // todo when the search term changes, kill the thread

    val searchTask = UpdatingTask("FileExplorer-Query") {}

    fun createResults() {
        searchTask.compute {

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
                                    "bz2", "lz", "lz4", "lzma", "lzo", "z", "zst",
                                    "unitypackage" -> file.isPacked.value
                                    else -> false
                                }
                            ) {
                                nextLevel.addAll(file.listChildren() ?: continue)
                            }
                            Thread.sleep(0)
                        }
                        level0 = level0 + nextLevel
                        if (level0.size > fileLimit) break
                        lastLevel = nextLevel
                        nextLevel = ArrayList()
                        Thread.sleep(0)
                    }
                }

                Thread.sleep(0)

                GFX.addGPUTask(1) {
                    removeOldFiles()
                }

                val parent = folder.getParent()
                if (parent != null) {
                    GFX.addGPUTask(1) {
                        // option to go up a folder
                        val fe = FileExplorerEntry(this, true, parent, style)
                        content += fe
                    }
                }

                val tmpCount = 64
                var tmpList = ArrayList<FileReference>(tmpCount)

                fun put() {
                    if (tmpList.isNotEmpty()) {
                        val list = tmpList
                        tmpList = ArrayList(tmpCount)
                        addEvent {
                            for (file in list) {
                                content += FileExplorerEntry(this, false, file, style)
                            }
                            // force layout update
                            Input.invalidateLayout()
                        }
                    }
                }

                for (file in level0.filter { it.isDirectory }) {
                    tmpList.add(file)
                    if (tmpList.size >= tmpCount) {
                        put()
                        Thread.sleep(0)
                    }
                }

                for (file in level0.filter { !it.isDirectory }) {
                    tmpList.add(file)
                    if (tmpList.size >= tmpCount) {
                        put()
                        Thread.sleep(0)
                    }
                }

                put()

                Thread.sleep(0)

            } else {

                val fe = content.children.filterIsInstance<FileExplorerEntry>()
                for (it in fe) {
                    it.visibility = Visibility[search.matches(it.file.name)]
                }

            }
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
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "Transform" -> pasteTransform(data)
            "PrefabSaveable" -> pastePrefab(data)
            else -> {
                if (!pasteTransform(data)) {
                    if (data.length < 2048) {
                        val ref = getReference(data)
                        if (ref.exists) {
                            switchTo(ref)
                        } else super.onPaste(x, y, data, type)
                    } else super.onPaste(x, y, data, type)
                }
            }
        }
    }

    fun pastePrefab(data: String): Boolean {
        val saveable = TextReader.read(data)[0] as? PrefabSaveable ?: return false
        var name = saveable.name.toAllowedFilename()
        name = name ?: saveable.defaultDisplayName.toAllowedFilename()
        name = name ?: saveable.className
        name = name.toAllowedFilename() ?: "Something"
        // make .json lowercase
        if (name.endsWith(".json", true)) {
            name = name.substring(0, name.length - 5)
        }
        name += ".json"
        val file = findNextFileName(folder.getChild(name), 1, '-')
        file.writeText(data)
        invalidate()
        return true
    }

    fun pasteTransform(data: String): Boolean {
        val transform = data.toTransform() ?: return false
        var name = transform.name.toAllowedFilename() ?: transform.defaultDisplayName
        // make .json lowercase
        if (name.endsWith(".json", true)) {
            name = name.substring(0, name.length - 5)
        }
        name += ".json"
        findNextFileName(folder.getChild(name), 1, '-').writeText(data)
        invalidate()
        return true
    }

    override fun onGotAction(
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        action: String,
        isContinuous: Boolean
    ): Boolean {
        when (action) {
            "OpenOptions" -> {
                val home = folder
                openMenu(
                    listOf(
                        MenuOption(NameDesc("Create Folder", "Creates a new directory", "ui.newFolder")) {
                            askName(
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
                        MenuOption(
                            NameDesc(
                                "Open In Explorer",
                                "Show the file in your default file explorer",
                                "ui.file.openInExplorer"
                            )
                        ) {
                            folder.openInExplorer()
                        }
                    ) + getRightClickOptions().map {
                        MenuOption(it.nameDesc) {
                            it.onClick(folder)
                        }
                    })
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
        if (!folder.isSomeKindOfDirectory) {
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

        fun invalidateFileExplorers() {
            windowStack.forEach { window ->
                window.panel.listOfAll {
                    if (it is FileExplorer) {
                        it.invalidate()
                    }
                }
            }
        }

    }

}