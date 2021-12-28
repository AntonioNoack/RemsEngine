package me.anno.ui.editor.files

import me.anno.audio.openal.AudioTasks
import me.anno.cache.instances.LastModifiedCache
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.config.DefaultStyle.black
import me.anno.ecs.components.shaders.effects.FSR
import me.anno.ecs.prefab.PrefabReadable
import me.anno.fonts.FontManager
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip2Dual
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageGPUCache.getInternalTexture
import me.anno.image.ImageReadable
import me.anno.image.ImageScale.scaleMax
import me.anno.input.Input
import me.anno.input.Input.mouseDownX
import me.anno.input.Input.mouseDownY
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.trash.TrashManager.moveToTrash
import me.anno.io.zip.InnerLinkFile
import me.anno.language.translation.NameDesc
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.Video
import me.anno.objects.modes.LoopingState
import me.anno.studio.StudioBase
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.maths.Maths.mixARGB
import me.anno.utils.maths.Maths.sq
import me.anno.utils.strings.StringHelper.setNumber
import me.anno.utils.types.Strings.getImportType
import me.anno.utils.types.Strings.isBlank2
import me.anno.video.FFMPEGMetadata
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// todo when the aspect ratio is extreme (e.g. > 50), stretch the image artificially to maybe 10 aspect ratio

// todo cannot enter mtl file

// todo when is audio, and hovered, we need to draw the loading animation continuously as well

// todo right click to get all meta information? (properties panel in windows)

// todo images: show extra information: width, height
class FileExplorerEntry(
    private val explorer: FileExplorer,
    val isParent: Boolean, file: FileReference, style: Style
) : PanelGroup(style.getChild("fileEntry")) {

    // todo small file type (signature) icons
    // todo use search bar for sort parameters :)
    // todo or right click menu for sorting

    val path = file.absolutePath

    // todo when entering a json file, and leaving it, the icon should not be a folder!


    // todo .lnk files for windows
    // todo .url files
    // todo icons from exe files

    // done icons for 3d meshes
    // done icons for project files
    // done asset files like unity, and then icons for them? (we want a unity-like engine, just with Kotlin)


    // done load fbx files
    // todo load separate fbx animations
    // todo play them together

    private var audio: Audio? = null

    private var startTime = 0L

    var time = 0.0
    var frameIndex = 0
    var maxFrameIndex = 0
    val hoverPlaybackDelay = 0.5
    var scale = 1
    var previewFPS = 1.0
    var meta: FFMPEGMetadata? = null

    private val originalBackgroundColor = backgroundColor
    private val hoverBackgroundColor = mixARGB(black, originalBackgroundColor, 0.85f)
    private val darkerBackgroundColor = mixARGB(black, originalBackgroundColor, 0.7f)

    private val size get() = explorer.entrySize.toInt()

    private val importType = file.extension.getImportType()
    private var iconPath = if (isParent || file.isDirectory) {
        if (isParent) {
            "file/folder.png"
        } else {
            when (file.name.lowercase()) {
                "music", "musik", "videos", "movies" -> "file/music.png"
                "documents", "dokumente", "downloads" -> "file/text.png"
                "images", "pictures", "bilder" -> "file/image.png"
                else -> if (file.hasChildren())
                    "file/folder.png" else "file/empty_folder.png"
            }
        }
    } else {
        // actually checking the type would need to be done async, because it's slow to ready many, many files
        when (importType) {
            "Container" -> "file/compressed.png"
            "Image", "Cubemap", "Cubemap-Equ" -> "file/image.png"
            "Text" -> "file/text.png"
            "Audio", "Video" -> "file/music.png"
            "Executable" -> "file/executable.png"
            // todo link icon for .lnk and .url, and maybe .desktop
            else -> "file/document.png"
        }
    }

    private val titlePanel = TextPanel(
        when {
            isParent -> ".."
            file.nameWithoutExtension.isBlank2() && file.name.isBlank2() -> file.toString()
            file.nameWithoutExtension.isBlank2() -> file.name
            else -> file.nameWithoutExtension
        }, style
    )

    override val children: List<Panel> = listOf(titlePanel)
    override fun remove(child: Panel) {}

    init {
        titlePanel.breaksIntoMultiline = true
        titlePanel.parent = this
        titlePanel.instantTextLoading = true
    }

    fun stopPlayback() {
        val audio = audio
        if (audio != null && audio.component?.isPlaying == true) {
            this.audio = null
            AudioTasks.addTask(1) { audio.stopPlayback() }
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = size
        minW = size
        minH = size + (titlePanel.font.sizeInt * 5 / 2)
        this.w = minW
        this.h = minH
    }

    // is null
    // override fun getLayoutState(): Any? = titlePanel.getLayoutState()
    private var lastTex: Any? = null
    private var lastMeta: Any? = null

    override fun tickUpdate() {
        super.tickUpdate()

        val meta = meta
        val tex = when (val tex = getTexKey()) {
            is GPUFrame -> if (tex.isCreated) tex else null
            is Texture2D -> tex.state
            else -> tex
        }
        if (lastMeta !== meta || lastTex !== tex) {
            lastTex = tex
            lastMeta = meta
            invalidateDrawing()
        }

        titlePanel.canBeSeen = canBeSeen

        // todo instead invalidate all file explorers, if they contain that file
        /*val newFile = FileReference.getReference(file)
        if (newFile !== file) {
            file = newFile
            invalidateDrawing()
        }

        if (!file.exists) {
            explorer.invalidate()
        }*/

        // needs to be disabled in the future, I think
        if (getReference(path).isHidden) {
            visibility = Visibility.GONE
        }

        backgroundColor = when {
            isInFocus -> darkerBackgroundColor
            isHovered -> hoverBackgroundColor
            else -> originalBackgroundColor
        }
        updatePlaybackTime()

    }

    private fun updatePlaybackTime() {
        when (importType) {
            "Video", "Audio" -> {
                val meta = FFMPEGMetadata.getMeta(path, true)
                this.meta = meta
                if (meta != null) {
                    val w = w
                    val h = h
                    previewFPS = min(meta.videoFPS, 120.0)
                    maxFrameIndex = max(1, (previewFPS * meta.videoDuration).toInt())
                    time = 0.0
                    frameIndex = if (isHovered) {
                        invalidateDrawing()
                        if (startTime == 0L) {
                            startTime = GFX.gameTime
                            val audio = Video(getReference(path))
                            audio.isLooping.value = LoopingState.PLAY_LOOP
                            audio.update()// sets audio.type, which is required for startPlayback
                            this.audio = audio
                            AudioTasks.addTask(5) {
                                audio.startPlayback(-hoverPlaybackDelay, 1.0, Camera())
                            }
                            0
                        } else {
                            time = (GFX.gameTime - startTime) * 1e-9 - hoverPlaybackDelay
                            max(0, (time * previewFPS).toInt())
                        }
                    } else {
                        startTime = 0
                        stopPlayback()
                        0
                    } % maxFrameIndex
                    scale = max(min(meta.videoWidth / w, meta.videoHeight / h), 1)
                }
            }
        }
    }

    private fun drawDefaultIcon(x0: Int, y0: Int, x1: Int, y1: Int) {
        val image = getInternalTexture(iconPath, true) ?: whiteTexture
        drawTexture(x0, y0, x1, y1, image)
    }

    private fun drawTexture(x0: Int, y0: Int, x1: Int, y1: Int, image: ITexture2D) {
        val w = x1 - x0
        val h = y1 - y0
        val (iw, ih) = scaleMax(image.w, image.h, w, h)
        // todo if texture is HDR, then use reinhard tonemapping for preview, with factor of 5
        // we can use FSR to upsample the images LOL
        val x = x0 + (w - iw) / 2
        val y = y0 + (h - ih) / 2
        if (image is Texture2D) image.filtering = GPUFiltering.LINEAR
        if (iw > image.w && ih > image.h) {
            FSR.upscale(image, x, y, iw, ih, false, backgroundColor)// ^^
        } else {
            drawTexture(x, y, iw, ih, image, -1, null)
        }
    }

    private fun getDefaultIcon() = getInternalTexture(iconPath, true)

    private fun getTexKey(): Any? {
        fun getImage(): Any? {
            val thumb = Thumbs.getThumbnail(getReference(path), w, true)
            return thumb ?: getDefaultIcon()
        }
        return when (importType) {
            "Video", "Audio" -> {
                val meta = meta
                if (meta != null) {
                    if (meta.videoWidth > 0) {
                        if (time == 0.0) { // not playing
                            getImage()
                        } else time
                    } else getDefaultIcon()
                } else getDefaultIcon()
            }
            else -> getImage()
        }
    }

    private fun drawImageOrThumb(x0: Int, y0: Int, x1: Int, y1: Int) {
        val w = x1 - x0
        val h = y1 - y0
        val image = Thumbs.getThumbnail(getReference(path), w, true) ?: getDefaultIcon() ?: whiteTexture
        val rot = (image as? Texture2D)?.rotation
        image.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
        if (rot == null) {
            drawTexture(x0, y0, x1, y1, image)
        } else {
            val m = Matrix4fArrayList()
            rot.apply(m)
            drawTexture(m, w, h, image, -1, null)
        }
    }

    private fun drawCircle(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (time < 0.0) {
            // countdown-circle, pseudo-loading
            // saves us some computations
            val relativeTime = ((hoverPlaybackDelay + time) / hoverPlaybackDelay).toFloat()
            drawLoadingCircle(relativeTime, x0, x1, y0, y1)
        }
    }

    fun getFrame(offset: Int) = getVideoFrame(
        getReference(path), scale, frameIndex + offset,
        videoBufferLength, previewFPS, 1000, true
    )

    private fun drawVideo(x0: Int, y0: Int, x1: Int, y1: Int) {

        // todo something with the states is broken...
        // todo only white is visible, even if there should be colors...


        val image = getFrame(0)
        if (frameIndex > 0) getFrame(videoBufferLength)
        if (image != null && image.isCreated) {
            drawTexture(
                GFX.windowWidth, GFX.windowHeight,
                image, -1, null
            )
            drawCircle(x0, y0, x1, y1)
        } else drawDefaultIcon(x0, y0, x1, y1)

        // show video progress on playback, e.g. hh:mm:ss/hh:mm:ss
        if (h >= 3 * titlePanel.font.sizeInt) {
            val meta = FFMPEGMetadata.getMeta(path, true)
            if (meta != null) {

                val totalSeconds = (meta.videoDuration).roundToInt()
                val needsHours = totalSeconds >= 3600
                val seconds = max((frameIndex / previewFPS).toInt(), 0) % max(totalSeconds, 1)

                val format = if (needsHours) charHHMMSS else charMMSS
                if (needsHours) {
                    setNumber(15, totalSeconds % 60, format)
                    setNumber(12, (totalSeconds / 60) % 60, format)
                    setNumber(9, totalSeconds / 3600, format)
                    setNumber(6, seconds % 60, format)
                    setNumber(3, (seconds / 60) % 60, format)
                    setNumber(0, seconds / 3600, format)
                } else {
                    setNumber(9, totalSeconds % 60, format)
                    setNumber(6, (totalSeconds / 60) % 60, format)
                    setNumber(3, seconds % 60, format)
                    setNumber(0, seconds / 60, format)
                }

                // more clip space, and draw it a little more left and at the top
                val extra = padding / 2
                clip2Dual(
                    x0 - extra, y0 - extra, x1, y1,
                    this.lx0, this.ly0, this.lx1, this.ly1
                ) { _, _, _, _ ->
                    drawSimpleTextCharByChar(x + padding - extra, y + padding - extra, 1, format)
                }
            }
        }
    }

    private fun drawThumb(x0: Int, y0: Int, x1: Int, y1: Int) {
        /*if (file.isDirectory) {
            return drawDefaultIcon(x0, y0, x1, y1)
        }*/
        when (importType) {
            // todo audio preview???
            // todo animation preview: draw the animated skeleton
            "Video", "Audio" -> {
                val meta = meta
                if (meta != null) {
                    if (meta.videoWidth > 0) {
                        if (time == 0.0) { // not playing
                            drawImageOrThumb(x0, y0, x1, y1)
                        } else {
                            drawVideo(x0, y0, x1, y1)
                        }
                    } else {
                        drawDefaultIcon(x0, y0, x1, y1)
                        drawCircle(x0, y0, x1, y1)
                    }
                } else drawDefaultIcon(x0, y0, x1, y1)
            }
            else -> drawImageOrThumb(x0, y0, x1, y1)
        }
    }

    private fun updateTooltip() {

        // if is selected, and there are multiple files selected, show group stats
        if (isInFocus && inFocus.count { it is FileExplorerEntry } > 1) {

            val files = inFocus.mapNotNull { (it as? FileExplorerEntry)?.path }
                .map { getReference(it) }

            tooltip = "${files.count { it.isDirectory }} folders + ${files.count { !it.isDirectory }} files\n" +
                    files.sumOf { it.length() }.formatFileSize()

        } else {

            fun getTooltip(file: FileReference): String {
                return when {
                    file.isDirectory -> {
                        // todo add number of children?
                        file.name
                    }
                    file is InnerLinkFile -> "Link to " + getTooltip(file.link)
                    file is PrefabReadable -> {
                        val prefab = file.readPrefab()
                        file.name + "\n" +
                                "${prefab.clazzName}, ${prefab.countTotalChanges(true)} Changes"
                    }
                    file is ImageReadable -> {
                        val image = file.readImage()
                        file.name + "\n" +
                                "${image.width} x ${image.height}"
                    }
                    else -> {
                        file.name + "\n" +
                                file.length().formatFileSize()
                    }
                }
            }

            tooltip = getTooltip(getReference(path))

        }
    }

    private var lines = 0
    private var padding = 0
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        if (isHovered || isInFocus) {
            updateTooltip()
        }

        drawBackground()

        val font0 = titlePanel.font
        val font1 = FontManager.getFont(font0)
        val fontSize = font1.actualFontSize

        val x = x
        val y = y
        val w = w
        val h = h

        val extraHeight = h - w
        lines = max(ceil(extraHeight / fontSize).toInt(), 1)

        padding = w / 20

        // why not twice the padding?????
        // only once centers it...
        val remainingW = w - padding// * 2
        val remainingH = h - padding// * 2

        val textH = (lines * fontSize).toInt()
        val imageH = remainingH - textH

        clip2Dual(
            x0, y0, x1, y1,
            x + padding,
            y + padding,
            x + remainingW,
            y + padding + imageH,
            ::drawThumb
        )

        clip2Dual(
            x0, y0, x1, y1,
            x + padding,
            y + h - padding - textH,
            x + remainingW,
            y + h/* - padding*/, // only apply the padding, when not playing video?
            ::drawText
        )
    }

    /**
     * draws the title
     * */
    private fun drawText(x0: Int, y0: Int, x1: Int, y1: Int) {
        titlePanel.w = x1 - x0
        titlePanel.minW = x1 - x0
        titlePanel.calculateSize(x1 - x0, y1 - y0)
        titlePanel.backgroundColor = backgroundColor and 0xffffff
        val deltaX = ((x1 - x0) - titlePanel.minW) / 2 // centering the text
        titlePanel.x = x0 + max(0, deltaX)
        titlePanel.y = max(y0, (y0 + y1 - titlePanel.minH) / 2)
        titlePanel.w = x1 - x0
        titlePanel.h = titlePanel.minH
        titlePanel.drawText()
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                // todo select the file, if the mouse goes up, not down
                val file = getReference(path)
                if (inFocus.any { it.contains(mouseDownX, mouseDownY) } && StudioBase.dragged?.getOriginal() != file) {
                    val inFocus = inFocus.filterIsInstance<FileExplorerEntry>().map { getReference(it.path) }
                    if (inFocus.size == 1) {
                        val textPanel = TextPanel(file.nameWithoutExtension, style)
                        val draggable = Draggable(file.toString(), "File", file, textPanel)
                        StudioBase.dragged = draggable
                    } else {
                        val textPanel = TextPanel(inFocus.joinToString("\n") { it.nameWithoutExtension }, style)
                        val draggable =
                            Draggable(inFocus.joinToString("\n") { it.toString() }, "File", inFocus, textPanel)
                        StudioBase.dragged = draggable
                    }
                }
            }
            "Enter" -> {
                val file = getReference(path)
                if (explorer.canSensiblyEnter(file)) {
                    explorer.switchTo(file)
                } else return false
            }
            "Rename" -> {
                val file = getReference(path)
                val title = NameDesc("Rename To...", "", "ui.file.rename2")
                askName(windowStack, x.toInt(), y.toInt(), title, file.name, NameDesc("Rename"), { -1 }, ::renameTo)
            }
            "OpenInExplorer" -> getReference(path).openInExplorer()
            "Delete" -> deleteFileMaybe(this, getReference(path))
            "OpenOptions" -> explorer.openOptions(getReference(path))
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun renameTo(newName: String) {
        val allowed = newName.toAllowedFilename()
        if (allowed != null) {
            val file = getReference(path)
            val dst = file.getParent()!!.getChild(allowed)
            if (dst.exists && !allowed.equals(file.name, true)) {
                ask(windowStack, NameDesc("Override existing file?", "", "ui.file.override")) {
                    file.renameTo(dst)
                    explorer.invalidate()
                }
            } else {
                file.renameTo(dst)
                explorer.invalidate()
            }
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            val file = getReference(path)
            if (explorer.canSensiblyEnter(file)) {
                LOGGER.info("Can enter ${file.name}? Yes!")
                explorer.switchTo(file)
            } else {
                LOGGER.info("Can enter ${file.name}? No :/")
                explorer.onDoubleClick(file)
            }
        } else super.onDoubleClick(x, y, button)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        val file = getReference(path)
        // todo in Rem's Engine, we first should check, whether there are prefabs, which depend on this file
        val files = inFocus.mapNotNull { if (it is FileExplorerEntry) getReference(it.path) else null }
        if (files.size <= 1) {
            // ask, then delete (or cancel)
            deleteFileMaybe(this, file)
        } else if (files.first() === file) {
            // ask, then delete all (or cancel)
            val title = NameDesc(
                "Delete these files? (${inFocus.size}x, ${
                    files.sumOf { it.length() }.formatFileSize()
                })", "", "ui.file.delete.ask.many"
            )
            val moveToTrash = MenuOption(
                NameDesc(
                    "Yes",
                    "Move the file to the trash",
                    "ui.file.delete.yes"
                )
            ) {
                moveToTrash(files.map { it.unsafeFile }.toTypedArray())
                explorer.invalidate()
            }
            val deletePermanently = MenuOption(
                NameDesc(
                    "Yes, permanently",
                    "Deletes all selected files; forever; files cannot be recovered",
                    "ui.file.delete.many.permanently"
                )
            ) {
                files.forEach { it.deleteRecursively() }
                explorer.invalidate()
            }
            openMenu(windowStack, title, listOf(moveToTrash, dontDelete, deletePermanently))
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        val file = getReference(path)
        val files = if (this in inFocus) {// multiple files maybe
            inFocus.filterIsInstance<FileExplorerEntry>().map {
                getReference(it.path)
            }
        } else listOf(file)
        Input.copyFiles(files)
        return null
    }

    override fun getMultiSelectablePanel() = this

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Tabs.spaces(tabDepth * 2 + 2)} ${getReference(path).name}")
    }

    override val className get() = "FileEntry"

    companion object {

        private val LOGGER = LogManager.getLogger(FileExplorerEntry::class)

        val videoBufferLength = 64

        private val charHHMMSS = "hh:mm:ss/hh:mm:ss".toCharArray()
        private val charMMSS = "mm:ss/mm:ss".toCharArray()

        val dontDelete
            get() = MenuOption(
                NameDesc(
                    "No",
                    "Deletes none of the selected file; keeps them all",
                    "ui.file.delete.many.no"
                )
            ) {}

        fun deleteFileMaybe(panel: Panel, file: FileReference) {
            val title = NameDesc(
                "Delete this file? (${file.length().formatFileSize()})",
                "",
                "ui.file.delete.ask"
            )
            val moveToTrash = MenuOption(
                NameDesc(
                    "Yes",
                    "Move the file to the trash",
                    "ui.file.delete.yes"
                )
            ) {
                val file2 = file.unsafeFile
                moveToTrash(file2)
                FileExplorer.invalidateFileExplorers(panel)
                LastModifiedCache.invalidate(file2)
            }
            val deletePermanently = MenuOption(
                NameDesc(
                    "Yes, permanently",
                    "Deletes the file; file cannot be recovered",
                    "ui.file.delete.permanent"
                )
            ) {
                file.deleteRecursively()
                FileExplorer.invalidateFileExplorers(panel)
            }
            openMenu(
                panel.windowStack,
                title, listOf(
                    moveToTrash,
                    dontDelete,
                    deletePermanently
                )
            )
        }

        fun drawLoadingCircle(relativeTime: Float, x0: Int, x1: Int, y0: Int, y1: Int) {
            val r = 1f - sq(relativeTime * 2 - 1)
            val radius = min(y1 - y0, x1 - x0) / 2f
            GFXx2D.drawCircle(
                (x0 + x1) / 2, (y0 + y1) / 2, radius, radius, 0f,
                relativeTime * 360f * 4 / 3,
                relativeTime * 360f * 2,
                Vector4f(1f, 1f, 1f, r * 0.2f)
            )
        }

        fun drawLoadingCircle(stack: Matrix4fArrayList, relativeTime: Float) {
            GFXx3D.draw3DCircle(
                null, 0.0, stack, 0f,
                relativeTime * 360f * 4 / 3,
                relativeTime * 360f * 2,
                Vector4f(1f, 1f, 1f, 0.2f)
            )
        }

    }


}