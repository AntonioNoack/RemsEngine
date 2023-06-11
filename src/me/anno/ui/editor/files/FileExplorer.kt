package me.anno.ui.editor.files

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.input.Input
import me.anno.input.Input.setClipboardContent
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.FileReference.Companion.getReferenceOrTimeout
import me.anno.io.files.FileRootRef
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerFolderCache
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.menuSeparator1
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.files.FileExplorerEntry.Companion.deleteFileMaybe
import me.anno.ui.editor.files.FileExplorerEntry.Companion.drawLoadingCircle
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.OS.home
import me.anno.utils.OS.music
import me.anno.utils.OS.pictures
import me.anno.utils.OS.videos
import me.anno.utils.ShutdownException
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.files.Files.listFiles2
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.hpc.UpdatingTask
import me.anno.utils.process.BetterProcessBuilder
import me.anno.utils.structures.History
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.roundToInt

// done, kind of: zoom: keep mouse at item in question
// done change side ratio based on: border + 1:1 frame + 2 lines of text
// todo dynamically change aspect ratio based on content for better coverage?
// issue: then the size is no longer constant
// solution: can we get the image size quickly? using ffmpeg maybe, or implemented ourselves

// done the text size is quite small on my x360 -> get the font size for the ui from the OS :)
// todo double click is not working in touch mode?
// done make file path clickable to quickly move to a grandparent folder :)

// done drag n drop; links or copy?
// done search options
// done search results below
// todo search in text files
// todo search in meta data for audio and video

open class FileExplorer(
    initialLocation: FileReference?,
    style: Style
) : PanelListY(style.getChild("fileExplorer")) {

    enum class FolderSorting(val v: Int) {
        FIRST(-2), MIXED(0), LAST(2)
    }

    enum class FileSorting {
        NAME,
        SIZE,
        LAST_MODIFIED,
    }

    // todo group files by stuff?

    var folderSorting = FolderSorting.FIRST
        set(value) {
            if (field != value) {
                content2d.invalidateSorting()
                field = value
            }
        }

    var fileSorting = FileSorting.NAME
        set(value) {
            if (field != value) {
                content2d.invalidateSorting()
                field = value
            }
        }

    var ascendingSorting = true
        set(value) {
            if (field != value) {
                content2d.invalidateSorting()
                field = value
            }
        }

    open fun getFolderOptions(): List<FileExplorerOption> = emptyList()

    open fun openOptions(file: FileReference) {
        if (file.exists) {
            openMenu(windowStack, getFileOptions().map {
                MenuOption(it.nameDesc) {
                    it.onClick(this, file)
                }
            })
        } else LOGGER.warn("File cannot be accessed currently")
    }

    open fun getFileOptions(): List<FileExplorerOption> {
        // todo add option to open json in specialized json editor...
        val rename = FileExplorerOption(renameDesc) { _, _ -> onGotAction(0f, 0f, 0f, 0f, "Rename", false) }
        val openInExplorer = FileExplorerOption(openInExplorerDesc) { _, it -> it.openInExplorer() }
        val openInStandard = FileExplorerOption(openInStandardProgramDesc) { _, it -> it.openInStandardProgram() }
        val editInStandard = FileExplorerOption(editInStandardProgramDesc) { _, it -> it.editInStandardProgram() }
        val copyPath = FileExplorerOption(copyPathDesc) { _, it -> setClipboardContent(it.absolutePath) }
        val copyName = FileExplorerOption(copyNameDesc) { _, it -> setClipboardContent(it.name) }
        val delete = FileExplorerOption(deleteDesc) { p, it -> deleteFileMaybe(p, it) }
        return listOf(rename, openInExplorer, openInStandard, editInStandard, copyPath, copyName, delete)
    }

    open fun onDoubleClick(file: FileReference) {}

    val searchBar = TextInput("Search Term", "", false, style)

    init {
        searchBar.addChangeListener {
            searchTerm = it
            invalidate()
        }
        searchBar.setEnterListener {
            if (it.length > 3) {
                val ref = getReference(it)
                if (ref.exists) {
                    switchTo(ref)
                    searchBar.setValue("", true)
                }
            }
        }
        searchBar.weight = 1f
    }

    val history = History(initialLocation ?: documents)
    val folder get() = history.value

    var searchTerm = ""
    var isValid = 0f

    var entrySize = 64f
    val minEntrySize = 32f

    val uContent = PanelListX(style)
    val content2d = PanelList2D({ p0, p1 ->
        p0 as FileExplorerEntry
        p1 as FileExplorerEntry
        when {
            p0 === p1 -> 0
            p0.isParent -> -1
            p1.isParent -> +1
            else -> {
                val a = p0.ref1s
                val b = p1.ref1s
                val base = clamp(
                    when (fileSorting) {
                        FileSorting.NAME -> a.name.compareTo(b.name, true)
                        FileSorting.SIZE -> a.length().compareTo(b.length())
                        FileSorting.LAST_MODIFIED -> b.lastModified.compareTo(a.lastModified)
                    }, -1, +1
                ) * (if (ascendingSorting) +1 else -1)
                if (folderSorting == FolderSorting.MIXED) base
                else base + a.isDirectory.compareTo(b.isDirectory) * folderSorting.v
            }
        }
    }, style)

    var lastFiles = emptyList<String>()
    var lastSearch = true

    val favourites = PanelListY(style)

    init {
        fun addFavourite(folder: FileReference) {
            favourites.add(object : FileExplorerEntry(this@FileExplorer, false, folder, style) {
                override fun calculateSize(w: Int, h: Int) {
                    val size = 64
                    minW = size
                    minH = size
                }
            })
        }
        addFavourite(home)
        addFavourite(downloads)
        addFavourite(documents)
        addFavourite(workspace)
        addFavourite(pictures)
        addFavourite(videos)
        addFavourite(music)
        addFavourite(FileRootRef)
        // todo custom favourites
        /*favourites.add(TextButton("+", false, style)
            .addLeftClickListener {
                FileExplorerSelectWrapper.selectFolder(null) { file ->
                }
            })*/
    }

    val title = PathPanel(folder, style)

    override fun calculateSize(w: Int, h: Int) {
        // a try...
        if (listMode) content2d.childWidth = w
        super.calculateSize(w, h)
    }

    fun getShortcutFolders(): List<FileReference> {
        val raw = DefaultConfig["files.shortcuts",
                listOf(
                    home,
                    desktop,
                    documents,
                    downloads,
                    pictures,
                    videos,
                    music,
                    workspace,
                    FileRootRef
                ).joinToString("|") { it.toLocalPath() }
        ]
        return raw
            .split('|')
            .map { it.toGlobalFile() }
            .filter { it != InvalidRef }
    }

    // file explorer
    // - top bar
    //   - title
    //   - search bar
    // - uContent
    //   - favourites
    //   - content
    //

    init {

        val esi = entrySize.toInt()
        content2d.childWidth = esi
        content2d.childHeight = esi * 4 / 3
        val topBar = PanelListX(style)
        this += topBar
        topBar += title

        title.addRightClickListener {
            val shortCutFolders = getShortcutFolders()
            openMenu(windowStack, NameDesc("Switch To"), listOf(
                MenuOption(NameDesc("Add Current To This List")) {
                    DefaultConfig["files.shortcuts"] = (getShortcutFolders() + folder)
                        .joinToString("|") { it.toLocalPath() }
                }
            ) + shortCutFolders.map { file ->
                MenuOption(NameDesc(file.name)) { switchTo(file) }
            })
        }

        topBar += searchBar
        this += uContent

        title.onChangeListener = {
            switchTo(it)
            invalidate()
        }

        uContent += ScrollPanelY(favourites, Padding(1), style, AxisAlignment.MIN)
        uContent += ScrollPanelY(content2d, Padding(1), style, AxisAlignment.MIN).apply {
            weight = 1f
        }

    }

    fun invalidate() {
        isValid = 0f
        lastFiles = listOf("!")
        invalidateLayout()
    }

    fun removeOldFiles() {
        FileExplorerEntry.stopAnyPlayback()
        content2d.clear()
    }

    val searchTask = UpdatingTask("FileExplorer-Query") {}

    override fun onDestroy() {
        super.onDestroy()
        searchTask.destroy()
    }

    /**
     * decides whether a file shall be shown; e.g., to only show images
     * */
    open fun filterShownFiles(file: FileReference): Boolean {
        return true
    }

    fun createResults() {
        searchTask.compute {

            val search = Search(searchTerm)

            var level0: List<FileReference> = folder.listFiles2()
                .filter { !it.isHidden }
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
                            if (file.isHidden) continue
                            if (file.isDirectory || when (file.lcExtension) {
                                    "zip", "rar", "7z", "s7z", "jar", "tar", "gz", "xz",
                                    "bz2", "lz", "lz4", "lzma", "lzo", "z", "zst",
                                    "unitypackage" -> file.isPacked.value
                                    else -> false
                                }
                            ) nextLevel.addAll(file.listChildren() ?: continue)
                            Thread.sleep(0)
                        }
                        level0 = level0 + nextLevel
                        if (level0.size > fileLimit) break
                        lastLevel = nextLevel
                        nextLevel = ArrayList()
                        Thread.sleep(0)
                    }
                }

                // be cancellable
                Thread.sleep(0)

                addEvent {
                    removeOldFiles()
                }

                val parent = folder.getParent()
                if (parent != null) {
                    if (filterShownFiles(parent)) addEvent {
                        // option to go up a folder
                        val entry = FileExplorerEntry(this, true, parent, style)
                        entry.listMode = listMode
                        content2d += entry
                        invalidateLayout()
                    }
                }

                val tmpCount = 64
                var tmpList = ArrayList<FileReference>(tmpCount)

                fun put() {
                    if (tmpList.isNotEmpty()) {
                        val list = tmpList
                        tmpList = ArrayList(tmpCount)
                        addEvent {
                            // check if the folder is still the same
                            if (lastFiles === newFiles && lastSearch == newSearch) {
                                for (idx in list.indices) {
                                    val file = list[idx]
                                    if (filterShownFiles(file)) {
                                        val entry = FileExplorerEntry(this, false, file, style)
                                        entry.listMode = listMode
                                        content2d += entry
                                    }
                                }
                                // force layout update
                                val windows = GFX.windows
                                for (i in windows.indices) {
                                    windows[i].framesSinceLastInteraction = 0
                                }
                                invalidateLayout()
                            }
                        }
                    }
                }

                for (file in level0) {
                    if (file.isDirectory) {
                        tmpList.add(file)
                        if (tmpList.size >= tmpCount) {
                            put()
                            Thread.sleep(0)
                        }
                    }
                }

                for (file in level0) {
                    if (!file.isDirectory) {
                        tmpList.add(file)
                        if (tmpList.size >= tmpCount) {
                            put()
                            Thread.sleep(0)
                        }
                    }
                }

                put()

                Thread.sleep(0)

            } else {
                val fe = content2d.children.filterIsInstance<FileExplorerEntry>()
                for (it in fe) {
                    it.isVisible = search.matches(getReferenceOrTimeout(it.path).name)
                }
                invalidateLayout()
            }

            // reset query time
            isValid = 5f

        }
    }

    var loading = 0L

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return loading != 0L
    }

    override fun onUpdate() {
        super.onUpdate()
        if (isValid <= 0f) {
            isValid = Float.POSITIVE_INFINITY
            title.file = folder// ?.toString() ?: "This Computer"
            title.tooltip = if (folder == FileRootRef) "This Computer" else folder.toString()
            createResults()
        } else isValid -= Engine.deltaTime
        if (loading != 0L) invalidateDrawing()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if (loading != 0L) {
            drawLoadingCircle((Engine.gameTime - loading) / 1e9f, x0, y0, x1, y1)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        // create links? or truly copy them?
        // or just switch?
        if (files.size > 1) {
            copyIntoCurrent(files)
        } else {
            openMenu(windowStack, listOf(
                MenuOption(NameDesc("Switch To")) {
                    switchTo(files.first())
                },
                MenuOption(NameDesc("Switch To Folder")) {
                    switchTo(files.first())
                }.setEnabled(!files.first().isDirectory),
                MenuOption(NameDesc("Copy")) {
                    thread(name = "copying files") {
                        copyIntoCurrent(files)
                    }
                },
                MenuOption(NameDesc("Create Links")) {
                    thread(name = "creating links") {
                        createLinksIntoCurrent(files)
                    }
                },
                MenuOption(NameDesc("Cancel")) {}
            ))
        }
    }

    fun copyIntoCurrent(files: List<FileReference>) {
        val progress = GFX.someWindow.addProgressBar("Copying", "Bytes", files.sumOf { it.length() }.toDouble())
        for (file in files) {
            if (progress.isCancelled) break
            val newFile = findNextFile(folder, file, 1, '-', 1)
            newFile.writeFile(file, { progress.progress += it }, { it?.printStackTrace() })
        }
        progress.finish()
        invalidate()
    }

    fun createLinksIntoCurrent(files: List<FileReference>) {
        val progress = GFX.someWindow.addProgressBar("Creating Links", "Files", files.size.toDouble())
        var tmp: FileReference? = null
        loop@ for (file in files) {
            if (progress.isCancelled) break
            when {
                OS.isWindows -> {
                    val newFile = findNextFile(folder, file, "lnk", 1, '-', 1)
                    if (tmp == null) tmp = FileFileRef.createTempFile("create-link", ".ps1")
                    tmp.writeText(
                        "" + // param ( [string]$SourceExe, [string]$DestinationPath )
                                "\$WshShell = New-Object -comObject WScript.Shell\n" +
                                "\$Shortcut = \$WshShell.CreateShortcut(\"${newFile.absolutePath}\")\n" +
                                "\$Shortcut.TargetPath = \"${file.absolutePath}\"\n" +
                                "\$Shortcut.Save()"
                    )
                    // PowerShell.exe -ExecutionPolicy Unrestricted -command "C:\temp\TestPS.ps1"
                    val builder = BetterProcessBuilder("PowerShell.exe", 16, false)
                    builder.add("-ExecutionPolicy")
                    builder.add("Unrestricted")
                    builder.add("-command")
                    builder.add(tmp.absolutePath)
                    builder.startAndPrint().waitFor()
                    invalidate()
                    progress.progress += 1.0
                }
                OS.isLinux || OS.isMacOS -> {
                    val newFile = findNextFile(folder, file, 1, '-', 1)
                    // create symbolic link
                    // ln -s target_file link_name
                    val builder = BetterProcessBuilder("ln", 3, false)
                    builder.add("-s") // symbolic link
                    builder.add(file.absolutePath)
                    builder.add(newFile.absolutePath)
                    builder.startAndPrint()
                    invalidate()
                    progress.progress += 1.0
                }
                OS.isAndroid -> {
                    LOGGER.warn("Unsupported OS for creating links.. how would you do that?")
                    progress.cancel(true)
                    break@loop
                }
                else -> {
                    LOGGER.warn("Unknown OS, don't know how to create links")
                    progress.cancel(true)
                    break@loop
                }
            }
        }
        progress.finish()
        try {
            tmp?.delete()
        } catch (_: Exception) {
        }
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
                val base = listOf(
                    MenuOption(NameDesc("Create Folder", "Creates a new directory", "ui.newFolder")) {
                        askName(windowStack,
                            NameDesc("Name", "", "ui.newFolder.askName"),
                            "",
                            NameDesc("Create"),
                            { -1 }) {
                            val validName = it.toAllowedFilename()
                            if (validName != null) {
                                home.getChild(validName).tryMkdirs()
                                invalidate()
                            }
                        }
                    },
                    MenuOption(openInExplorerDesc) { folder.openInExplorer() },
                    MenuOption(openInStandardProgramDesc) { folder.openInStandardProgram() },
                    MenuOption(editInStandardProgramDesc) { folder.editInStandardProgram() },
                    MenuOption(copyPathDesc) { setClipboardContent(folder.absolutePath) },
                    MenuOption(copyNameDesc) { setClipboardContent(folder.name) },
                    menuSeparator1,
                    MenuOption(NameDesc("Sort by Name")) { fileSorting = FileSorting.NAME }
                        .setEnabled(fileSorting != FileSorting.NAME),
                    MenuOption(NameDesc("Sort by Size")) { fileSorting = FileSorting.SIZE }
                        .setEnabled(fileSorting != FileSorting.SIZE),
                    MenuOption(NameDesc("Sort by Last-Modified")) { fileSorting = FileSorting.LAST_MODIFIED }
                        .setEnabled(fileSorting != FileSorting.LAST_MODIFIED),
                    menuSeparator1,
                    MenuOption(NameDesc("Sort Ascending")) { ascendingSorting = true }
                        .setEnabled(!ascendingSorting),
                    MenuOption(NameDesc("Sort Descending")) { ascendingSorting = false }
                        .setEnabled(ascendingSorting),
                    menuSeparator1,
                    MenuOption(NameDesc("Folders First")) { folderSorting = FolderSorting.FIRST }
                        .setEnabled(folderSorting != FolderSorting.FIRST),
                    MenuOption(NameDesc("Folders Mixed")) { folderSorting = FolderSorting.MIXED }
                        .setEnabled(folderSorting != FolderSorting.MIXED),
                    MenuOption(NameDesc("Folders Last")) { folderSorting = FolderSorting.LAST }
                        .setEnabled(folderSorting != FolderSorting.LAST),
                )
                val folder = getFolderOptions().map {
                    MenuOption(it.nameDesc) {
                        it.onClick(this, folder)
                    }
                }
                openMenu(windowStack, if (folder.isEmpty()) base else base + menuSeparator1 + folder)
            }
            "Refresh" -> {
                LOGGER.info("Refreshing")
                invalidate()
            }
            "Back", "Backward" -> back()
            "Forward" -> forward()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun back() {
        history.back { folder.getParent() }
        invalidate()
    }

    fun forward() {
        if (history.forward()) {
            invalidate()
        } else {
            LOGGER.info("End of history reached!")
        }
    }

    fun switchTo(folder: FileReference?) {
        folder ?: return
        val windowsLink = folder.windowsLnk.value
        when {
            windowsLink != null -> {
                val dst = getReferenceOrTimeout(windowsLink.absolutePath)
                if (dst.exists) {
                    switchTo(dst)
                } else {
                    switchTo(folder.getParent())
                }
            }
            GFX.isGFXThread() && !OS.isWeb -> {
                loading = Engine.gameTime
                invalidateDrawing()
                thread(name = "switchTo($folder)") {
                    try {
                        switchTo1(folder)
                    } catch (e: ShutdownException) {
                        // ignored
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    loading = 0L
                    addEvent { invalidateDrawing() }
                }
            }
            else -> switchTo1(folder)
        }
    }

    private fun switchTo1(folder: FileReference) {
        if (!canSensiblyEnter(folder)) {
            switchTo(folder.getParent())
        } else {
            addEvent {
                history.add(folder)
                invalidate()
            }
        }
    }

    fun canSensiblyEnter(file: FileReference): Boolean {
        return file.isDirectory || (file.isSomeKindOfDirectory &&
                InnerFolderCache.readAsFolder(file, false)?.listChildren()?.isEmpty() == false)
    }

    var hoveredItemIndex = 0
    var hoverFractionY = 0f

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (!Input.isControlDown) {
            // find, which item is being hovered
            hoveredItemIndex = content2d.getItemIndexAt(x.toInt(), y.toInt())
            hoverFractionY = clamp(content2d.getItemFractionY(y).toFloat(), 0.25f, 0.75f)
        }
    }

    var listMode = false

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (Input.isControlDown) {
            val newEntrySize = entrySize * pow(1.05f, dy - dx)
            entrySize = clamp(
                newEntrySize,
                minEntrySize,
                max(w - content2d.spacing * 2f - 1f, 20f)
            )
            onUpdateEntrySize(newEntrySize)
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    fun onUpdateEntrySize(newEntrySize: Float = entrySize) {
        listMode = newEntrySize < minEntrySize
        val esi = entrySize.toInt()
        content2d.maxColumns = if (listMode) 1 else Int.MAX_VALUE
        content2d.childWidth = if (listMode) max(w, 65536) else esi
        favourites.invalidateLayout()
        // define the aspect ratio by 2 lines of space for the name
        val sample = content2d.firstOfAll { it is TextPanel } as? TextPanel
        val sampleFont = sample?.font ?: style.getFont("text")
        val textSize = sampleFont.sizeInt
        content2d.childHeight = if (listMode) (textSize * 1.5f).roundToInt()
        else esi + (textSize * 2.5f).roundToInt()
        // scroll to hoverItemIndex, hoverFractionY
        // todo restore that (?)
        // content2d.scrollTo(hoveredItemIndex, hoverFractionY)
    }

    // multiple elements can be selected
    override fun getMultiSelectablePanel() = this

    override val className: String get() = "FileExplorer"

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FileExplorer::class)

        @JvmStatic
        private val forbiddenConfig =
            DefaultConfig["files.forbiddenCharacters", "<>:\"/\\|?*"] + String(CharArray(32) { it.toChar() })

        @JvmField
        val forbiddenCharacters = forbiddenConfig.toHashSet()

        @JvmStatic
        fun invalidateFileExplorers(panel: Panel) {
            for (window in panel.windowStack) {
                window.panel.forAll {
                    if (it is FileExplorer) {
                        it.invalidate()
                    }
                }
            }
        }

        @JvmField
        val renameDesc = NameDesc(
            "Rename",
            "Change the name of this file",
            "ui.file.rename"
        )

        @JvmField
        val openInExplorerDesc = NameDesc(
            "Open In Explorer",
            "Show the file in your default file explorer",
            "ui.file.openInExplorer"
        )

        @JvmField
        val openInStandardProgramDesc = NameDesc(
            "Show In Standard Program",
            "Open the file using your default viewer",
            "ui.file.openInStandardProgram"
        )

        @JvmField
        val editInStandardProgramDesc = NameDesc(
            "Edit In Standard Program",
            "Edit the file using your default editor",
            "ui.file.editInStandardProgram"
        )

        @JvmField
        val copyPathDesc = NameDesc(
            "Copy Path",
            "Copy the path of the file to clipboard",
            "ui.file.copyPath"
        )

        @JvmField
        val copyNameDesc = NameDesc(
            "Copy Name",
            "Copy the name of the file to clipboard",
            "ui.file.copyName"
        )

        @JvmField
        val deleteDesc = NameDesc(
            "Delete",
            "Delete this file",
            "ui.file.delete"
        )

    }

}