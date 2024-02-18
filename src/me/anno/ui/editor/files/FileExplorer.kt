package me.anno.ui.editor.files

import me.anno.Time
import me.anno.cache.IgnoredException
import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.Events.addEvent
import me.anno.gpu.GFX
import me.anno.input.Clipboard.setClipboardContent
import me.anno.input.Input
import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolderCache
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.menuSeparator1
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.FileExplorerEntry.Companion.drawLoadingCircle
import me.anno.ui.editor.files.FileExplorerOptions.copyName
import me.anno.ui.editor.files.FileExplorerOptions.copyNameDesc
import me.anno.ui.editor.files.FileExplorerOptions.copyPath
import me.anno.ui.editor.files.FileExplorerOptions.copyPathDesc
import me.anno.ui.editor.files.FileExplorerOptions.delete
import me.anno.ui.editor.files.FileExplorerOptions.editInStandardProgram
import me.anno.ui.editor.files.FileExplorerOptions.editInStandardProgramDesc
import me.anno.ui.editor.files.FileExplorerOptions.invalidateThumbnails
import me.anno.ui.editor.files.FileExplorerOptions.openImageViewer
import me.anno.ui.editor.files.FileExplorerOptions.openInExplorer
import me.anno.ui.editor.files.FileExplorerOptions.openInExplorerDesc
import me.anno.ui.editor.files.FileExplorerOptions.openInStandardProgram
import me.anno.ui.editor.files.FileExplorerOptions.openInStandardProgramDesc
import me.anno.ui.editor.files.FileExplorerOptions.pinToFavourites
import me.anno.ui.editor.files.FileExplorerOptions.rename
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.ui.editor.files.SearchAlgorithm.createResults
import me.anno.ui.input.TextInput
import me.anno.utils.Color.hex32
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.OS.home
import me.anno.utils.OS.music
import me.anno.utils.OS.pictures
import me.anno.utils.OS.videos
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.files.OpenFileExternally.editInStandardProgram
import me.anno.utils.files.OpenFileExternally.openInExplorer
import me.anno.utils.files.OpenFileExternally.openInStandardProgram
import me.anno.utils.hpc.UpdatingTask
import me.anno.utils.structures.History
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// todo star assets, and they will always (?) come first in sorting

// todo dynamically change aspect ratio based on content for better coverage?

// todo double click is not working in touch mode?

// todo search in text files
// todo search in meta data for audio and video

open class FileExplorer(initialLocation: FileReference?, isY: Boolean, style: Style) :
    PanelListY(style.getChild("fileExplorer")) {

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

    @Suppress("unused")
    var isY: Boolean = isY
        set(value) {
            if (field != value) {
                field = value
                // switch out content2d's parent
                content2d.isY = value
                content2d.uiParent!!.removeFromParent()
                uContent.add(wrapInScrollPanel(value, content2d))
            }
        }

    open fun getFolderOptions(): List<FileExplorerOption> = emptyList()

    open fun openOptions(files: List<FileReference>) {
        rightClickedFiles = files.toHashSet()
        val title = NameDesc(
            if (files.size == 1) "For ${files.first().name}:"
            else "For ${files.size} files:"
        )
        openMenu(windowStack, title, getFileOptions().map {
            it.toMenu(this, files)
        })?.addClosingListener {
            rightClickedFiles = emptySet()
        }
    }

    open fun getFileOptions(): List<FileExplorerOption> {
        // todo add option to open json in specialized json editor...
        return listOf(
            rename,
            openInExplorer,
            openInStandardProgram,
            editInStandardProgram,
            pinToFavourites,
            invalidateThumbnails,
            openImageViewer,
            copyPath,
            copyName,
            delete,
        )
    }

    open fun onDoubleClick(file: FileReference) {}

    val searchBar = TextInput(
        Dict["Search Term", "ui.general.searchTerm"], "", false,
        style.getChild("deep")
    )
    var searchDepth = 3
    var isValid = 0f

    init {
        searchBar.tooltip =
            Dict["Enter search terms, or paste a path, press enter to go there", "ui.fileExplorer.searchTerm.desc"]
        searchBar.weight = 1f
        searchBar.addChangeListener { invalidate() }
        searchBar.setEnterListener {
            val child = folder.getChild(it)
            if (child.exists) {
                switchTo(child)
            } else if (it.length > 3) {
                val ref = getReference(it)
                if (ref.exists) {
                    switchTo(ref)
                    searchBar.setValue("", true)
                }
            }
        }
    }

    val history: History<FileReference> = History(initialLocation ?: documents)
    val folder: FileReference get() = history.value
    private var lastFolder: FileReference = InvalidRef
    private var fileToScrollTo: FileReference = InvalidRef
    private var panelToScrollTo: FileExplorerEntry? = null

    var entrySize = 64f
    val minEntrySize = 32f

    private fun checkForScrollingTo(panel: FileExplorerEntry) {
        if (fileToScrollTo.absolutePath == panel.path) {
            fileToScrollTo = InvalidRef
            panelToScrollTo = panel
        }
    }

    val uContent = PanelListX(style)
    val content2d: PanelList2D = PanelList2D(isY, { p0, p1 ->

        p0 as FileExplorerEntry
        p1 as FileExplorerEntry

        // if is first time, and p0 is the last file that was scrolled to, scroll to it
        if (fileToScrollTo != InvalidRef) {
            checkForScrollingTo(p0)
            checkForScrollingTo(p1)
        }

        when {
            p0 === p1 -> 0
            p0.isParent -> -1
            p1.isParent -> +1
            else -> {
                val a = p0.ref1s
                val b = p1.ref1s
                val base = clamp(fileSorting.compare(a, b), -1, +1) * (if (ascendingSorting) +1 else -1)
                if (folderSorting == FolderSorting.MIXED) base
                else base + p0.isDirectory.compareTo(p1.isDirectory) * folderSorting.weight
            }
        }
    }, style)

    var lastFiles = emptyList<String>()
    var lastSearch: Search? = null
    var calcIndex = 0

    val favourites = PanelListY(style)

    init {
        content2d.makeBackgroundTransparent()
        uContent.makeBackgroundTransparent()
        favourites.makeBackgroundTransparent()
        validateFavourites(Favourites.getFavouriteFiles())
    }

    fun addFavourite(folder: FileReference) {
        val entry = object : FileExplorerEntry(this@FileExplorer, false, folder, style) {
            override fun onGotAction(
                x: Float, y: Float, dx: Float, dy: Float,
                action: String, isContinuous: Boolean
            ): Boolean {
                return if (action == "OpenOptions") {
                    openMenu(windowStack, listOf(
                        MenuOption(NameDesc(Dict["Remove from favourites", "ui.fileExplorer.removeFromFavourites"])) {
                            Favourites.removeFavouriteFiles(listOf(folder))
                        }
                    ))
                    true
                } else super.onGotAction(x, y, dx, dy, action, isContinuous)
            }

            override fun calculateSize(w: Int, h: Int) {
                val size = 64
                minW = size
                minH = size
            }
        }
        favourites.add(entry)
    }

    fun validateFavourites(favourites1: List<FileReference>) {
        favourites.clear()
        for (fav in favourites1) {
            addFavourite(fav)
        }
    }

    val pathPanel = PathPanel(folder, style)

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
        // I prefer: scaleChildren > scaleSpaces > nothing
        content2d.scaleChildren = true
        val topBar = PanelListX(style)
        this += topBar
        topBar += pathPanel

        val padLR = 4
        pathPanel.padding.add(padLR, 0, padLR, 0)
        searchBar.padding.add(padLR, 0, padLR, 0)
        pathPanel.addRightClickListener {
            val shortCutFolders = getShortcutFolders()
            openMenu(windowStack, NameDesc("Options"),
                listOf(
                    MenuOption(NameDesc("Add Current To Favourites")) {
                        DefaultConfig["files.shortcuts"] = (getShortcutFolders() + folder)
                            .joinToString("|") { it.toLocalPath() }
                    }
                ) + shortCutFolders.map { file ->
                    MenuOption(NameDesc(file.name)) { switchTo(file) }
                })
        }

        topBar += searchBar
        this += uContent

        uContent.weight = 1f

        pathPanel.onChangeListener = {
            switchTo(it)
            invalidate()
        }

        uContent += ScrollPanelY(favourites, Padding(1), style).apply {
            makeBackgroundTransparent()
            alwaysShowShadowY = true
        }
        uContent += wrapInScrollPanel(isY, content2d)

        content2d.weight = 1f // expand to the right
        uContent.weight = 1f // expand to the right
        content2d.uiParent!!.weight = 1f
    }

    private fun wrapInScrollPanel(isY: Boolean, content2d: Panel): Panel {
        val scroll = if (isY) ScrollPanelY(content2d, Padding(1), style)
        else ScrollPanelX(content2d, Padding(1), style)
        scroll.makeBackgroundTransparent()
        scroll.fill(1f)
        scroll.alwaysShowShadowY = true
        return scroll
    }

    fun invalidate(force: Boolean = false) {
        isValid = 0f
        if (force) {
            lastFiles = listOf("!")
        }
        onUpdate()
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
    open fun shouldShowFile(file: FileReference): Boolean {
        return true
    }

    open fun createEntry(isParent: Boolean, file: FileReference): FileExplorerEntry {
        val entry = FileExplorerEntry(this, isParent, file, style)
        entry.alignmentX = AxisAlignment.CENTER
        entry.listMode = listMode
        return entry
    }

    var loading = 0L

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return loading != 0L
    }

    private fun getClosestFileKey(file: FileReference): String {
        return "ui.fileExplorer.last.${hex32(file.hashCode())}"
    }

    private fun getClosestPanel(): FileExplorerEntry? {
        // save preferred file by lastFolder
        val window = window ?: return null
        val x0 = window.mouseXi * 2
        val y0 = window.mouseYi * 2
        return content2d.children.minByOrNull {
            abs(it.x * 2 + it.width - x0) + abs(it.y * 2 + it.height - y0)
        } as? FileExplorerEntry
    }

    private fun saveClosestFile() {
        // save preferred file by lastFolder
        val closestEntry = getClosestPanel()
        val preferredFile = closestEntry?.ref1s
        if (preferredFile != null) {
            DefaultConfig[getClosestFileKey(lastFolder)] = preferredFile.name
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        if (folder != lastFolder) {
            saveClosestFile()
            lastFolder = folder
            val fileName = DefaultConfig[getClosestFileKey(folder), ""]
            fileToScrollTo =
                if (fileName.isEmpty()) folder.getParent() ?: InvalidRef
                else folder.getChild(fileName)
            panelToScrollTo = null // first must be created
        }
        if (isValid <= 0f) {
            isValid = Float.POSITIVE_INFINITY
            pathPanel.file = folder
            pathPanel.tooltip = if (folder == FileRootRef) "This Computer" else folder.toString()
            createResults(this)
        } else isValid -= Time.deltaTime.toFloat()
        if (loading != 0L) invalidateDrawing()
        val pts = panelToScrollTo
        if (pts != null) {
            pts.scrollTo()
            panelToScrollTo = null
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if (loading != 0L) {
            drawLoadingCircle((Time.nanoTime - loading) / 1e9f, x0, y0, x1, y1)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        pasteFiles(files, folder)
    }

    fun pasteFiles(files: List<FileReference>, folder: FileReference, extraOptions: List<MenuOption> = emptyList()) {
        // create links? or truly copy them?
        // or just switch?
        openMenu(windowStack, listOf(
            MenuOption(NameDesc("Move")) {
                thread(name = "moving files") {
                    moveInto(files, folder)
                }
            },
            MenuOption(NameDesc("Copy")) {
                thread(name = "copying files") {
                    copyInto(files, folder)
                }
            },
            MenuOption(NameDesc("Switch To")) {
                switchTo(files.first())
            }.setEnabled(files.size == 1),
            MenuOption(NameDesc("Switch To Folder")) {
                switchTo(files.first())
            }.setEnabled(files.size == 1 && !files.first().isDirectory),
            MenuOption(NameDesc("Create Links")) {
                thread(name = "creating links") {
                    createLinksInto(files, folder)
                }
            }
        ) + extraOptions + listOf(
            MenuOption(NameDesc("Cancel")) {}
        ))
    }

    fun copyInto(files: List<FileReference>, folder: FileReference) {
        val progress = GFX.someWindow.addProgressBar("Copying", "Bytes", files.sumOf { it.length() }.toDouble())
        for (srcFile in files) {
            if (progress.isCancelled) break
            val dstFile = findNextFile(folder, srcFile, 1, '-', 1)
            dstFile.writeFile(srcFile, { delta, _ -> progress.progress += delta }, { it?.printStackTrace() })
        }
        progress.finish()
        invalidate()
    }

    fun moveInto(files: List<FileReference>, folder: FileReference) {
        val progress = GFX.someWindow.addProgressBar("Copying", "Bytes", files.sumOf { it.length() }.toDouble())
        for (srcFile in files) {
            if (progress.isCancelled) break
            if (folder == srcFile.getParent()) continue
            val dstFile = findNextFile(folder, srcFile, 1, '-', 1)
            srcFile.renameTo(dstFile)
        }
        progress.finish()
        invalidate()
    }

    fun createLinksInto(files: List<FileReference>, folder: FileReference) {
        val progress = GFX.someWindow.addProgressBar("Creating Links", "Files", files.size.toDouble())
        val createLink = createLink
        if (createLink != null) {
            var tmp: FileReference? = null
            loop@ for (dst in files) {
                if (progress.isCancelled) break
                val src = findNextFile(folder, dst, "lnk", 1, '-', 1)
                tmp = createLink(src, dst, tmp)
                invalidate()
                progress.progress += 1.0
            }
            progress.finish()
            try {
                tmp?.delete()
            } catch (_: Exception) {
            }
        } else {
            LOGGER.warn("Don't know how to create links")
            progress.cancel(false)
        }
    }

    override fun onGotAction(
        x: Float, y: Float, dx: Float, dy: Float,
        action: String, isContinuous: Boolean
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
                    MenuOption(openInExplorerDesc) { openInExplorer(folder) },
                    MenuOption(openInStandardProgramDesc) { openInStandardProgram(folder) },
                    MenuOption(editInStandardProgramDesc) { editInStandardProgram(folder) },
                    MenuOption(copyPathDesc) { setClipboardContent(folder.absolutePath) },
                    MenuOption(copyNameDesc) { setClipboardContent(folder.name) },
                    menuSeparator1,
                    MenuOption(NameDesc("Sort by Name")) { fileSorting = FileSorting.NAME }
                        .setEnabled(fileSorting != FileSorting.NAME),
                    MenuOption(NameDesc("Sort by Size")) { fileSorting = FileSorting.SIZE }
                        .setEnabled(fileSorting != FileSorting.SIZE),
                    MenuOption(NameDesc("Sort by Last-Modified")) { fileSorting = FileSorting.LAST_MODIFIED }
                        .setEnabled(fileSorting != FileSorting.LAST_MODIFIED),
                    MenuOption(NameDesc("Sort by Creation-Time")) { fileSorting = FileSorting.CREATION_TIME }
                        .setEnabled(fileSorting != FileSorting.CREATION_TIME),
                    MenuOption(NameDesc("Sort by Extension")) { fileSorting = FileSorting.EXTENSION }
                        .setEnabled(fileSorting != FileSorting.EXTENSION),
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
                val folderFiles = listOf(folder)
                val folder = getFolderOptions().map {
                    it.toMenu(this, folderFiles)
                }
                val list = if (folder.isEmpty()) base
                else base + menuSeparator1 + folder
                openMenu(windowStack, list)
            }
            "Refresh" -> {
                LOGGER.info("Refreshing")
                invalidate()
            }
            "Back", "Backward" -> back()
            "Forward" -> forward()
            "OpenSearchBar" -> searchBar.requestFocus()
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
        if (GFX.isGFXThread() && !OS.isWeb) {
            loading = Time.nanoTime
            invalidateDrawing()
            thread(name = "switchTo($folder)") {
                try {
                    switchTo1(folder)
                } catch (_: IgnoredException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                loading = 0L
                addEvent { invalidateDrawing() }
            }
        } else switchTo1(folder)
    }

    private fun switchTo1(folder: FileReference) {
        if (!canSensiblyEnter(folder)) {
            switchTo(folder.getParent())
        } else {
            addEvent {
                history.add(folder)
                searchBar.setValue("", true)
                invalidate(true)
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
            hoverFractionY = clamp(content2d.getItemFractionY(y), 0.25f, 0.75f)
        }
    }

    override fun onEscapeKey(x: Float, y: Float) {
        switchTo(folder.getParent())
    }

    var listMode = false

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (Input.isControlDown) {
            val newEntrySize = entrySize * pow(1.05f, dy - dx)
            entrySize = clamp(
                newEntrySize,
                minEntrySize,
                max(width - content2d.spacing * 2f - 1f, 20f)
            )
            onUpdateEntrySize(newEntrySize)
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    private var lastScrollChangedPanel: FileExplorerEntry? = null
    private var lastScrollChangeTime = 0L
    fun onUpdateEntrySize(newEntrySize: Float = entrySize) {
        listMode = newEntrySize < minEntrySize
        val esi = entrySize.toInt()
        content2d.maxColumns = if (listMode) 1 else Int.MAX_VALUE
        content2d.childWidth = if (listMode) max(width, 65536) else esi
        favourites.invalidateLayout()
        // define the aspect ratio by 2 lines of space for the name
        val sample = content2d.firstOfAll { it is TextPanel } as? TextPanel
        val sampleFont = sample?.font ?: style.getFont("text")
        val textSize = sampleFont.sizeInt
        content2d.childHeight = if (listMode) (textSize * 1.5f).roundToInt()
        else esi + (textSize * 2.5f).roundToInt()
        val time = Time.nanoTime
        if (lastScrollChangedPanel != null && abs(time - lastScrollChangeTime) < 700 * MILLIS_TO_NANOS) {
            panelToScrollTo = lastScrollChangedPanel
        } else {
            panelToScrollTo = getClosestPanel()
            lastScrollChangedPanel = panelToScrollTo
        }
        lastScrollChangeTime = time
        addEvent(500) {
            // delay a little, until layout is calculated
            // could be shorter, but it's most satisfying this way :D
            panelToScrollTo = lastScrollChangedPanel
        }
    }

    // multiple elements can be selected
    override fun getMultiSelectablePanel() = this

    override val className: String get() = "FileExplorer"

    companion object {

        var rightClickedFiles: Set<FileReference> = emptySet()

        var createLink: ((src: FileReference, dst: FileReference, tmp: FileReference?) -> FileReference?)? = null

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FileExplorer::class)

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
    }
}