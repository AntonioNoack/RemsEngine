package me.anno.ui.editor.files

import me.anno.audio.openal.AudioTasks
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.config.DefaultStyle.black
import me.anno.fonts.FontManager
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip2Dual
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.ImageGPUCache.getInternalTexture
import me.anno.input.Input
import me.anno.input.Input.mouseDownX
import me.anno.input.Input.mouseDownY
import me.anno.input.MouseButton
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.trash.TrashManager.moveToTrash
import me.anno.language.translation.NameDesc
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.Video
import me.anno.objects.modes.LoopingState
import me.anno.studio.StudioBase
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.thumbs.Thumbs
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.files.Files.listFiles2
import me.anno.utils.image.ImageScale.scale
import me.anno.utils.io.Streams.copy
import me.anno.utils.maths.Maths.mixARGB
import me.anno.utils.maths.Maths.sq
import me.anno.utils.types.Strings.getImportType
import me.anno.video.FFMPEGMetadata
import me.anno.video.VFrame
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File
import java.nio.file.Files
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FileExplorerEntry(
    private val explorer: FileExplorer,
    val isParent: Boolean, val file: FileReference, style: Style
) : PanelGroup(style.getChild("fileEntry")) {


    // todo when entering a json file, and leaving it, the icon should not be a folder!


    // todo .lnk files for windows
    // todo .url files
    // todo icons from exe files?

    // done icons for 3d meshes
    // todo icons for project files
    // todo asset files like unity, and then icons for them? (we want a unity-like engine, just with Kotlin)


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
                else -> if (file.listFiles2().isNotEmpty())
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
            // todo link icon for .lnk and .url, and maybe .desktop
            // todo icon for asset
            else -> "file/document.png"
        }
    }


    private val titlePanel = TextPanel(
        if (isParent) ".." else if (file.name.isEmpty()) file.toString() else file.name,
        style
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
        minH = size * 4 / 3
        this.w = size
        this.h = size * 4 / 3
    }

    override fun getLayoutState(): Any? = titlePanel.getLayoutState()
    override fun getVisualState(): Any {
        val tex = when (val tex = getTexKey()) {
            is VFrame -> if (tex.isCreated) tex else null
            is Texture2D -> tex.state
            else -> tex
        }
        titlePanel.canBeSeen = canBeSeen
        return Pair(tex, meta)
    }

    override fun tickUpdate() {
        super.tickUpdate()
        wasInFocus = isInFocus
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
                val meta = FFMPEGMetadata.getMeta(file, true)
                this.meta = meta
                if (meta != null) {
                    val w = w
                    val h = h
                    previewFPS = min(meta.videoFPS, 120.0)
                    maxFrameIndex = max(1, (previewFPS * meta.videoDuration).toInt())
                    time = 0.0
                    frameIndex = if (isHovered) {
                        if (startTime == 0L) {
                            startTime = GFX.gameTime
                            val audio = Video(file)
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
        val (iw, ih) = scale(image.w, image.h, w, h)
        // todo if texture is HDR, then use reinhard tonemapping for preview, with factor of 5
        drawTexture(x0 + (w - iw) / 2, y0 + (h - ih) / 2, iw, ih, image, -1, null)
    }

    private fun getDefaultIcon() = getInternalTexture(iconPath, true)

    private fun getTexKey(): Any? {
        fun getImage(): Any? {
            val thumb = Thumbs.getThumbnail(file, w, true)
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
        val image = Thumbs.getThumbnail(file, w, true) ?: getDefaultIcon() ?: whiteTexture
        val tex2D = image as? Texture2D
        val rot = tex2D?.rotation
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


    private fun drawVideo(x0: Int, y0: Int, x1: Int, y1: Int) {

        // todo something with the states is broken...
        // todo only white is visible, even if there should be colors...

        val bufferLength = 64
        fun getFrame(offset: Int) = getVideoFrame(
            file, scale, frameIndex + offset,
            bufferLength, previewFPS, 1000, true
        )

        val image = getFrame(0)
        if (frameIndex > 0) getFrame(bufferLength)
        if (image != null && image.isCreated) {
            drawTexture(
                GFX.windowWidth, GFX.windowHeight,
                image, -1, null
            )
            drawCircle(x0, y0, x1, y1)
        } else drawDefaultIcon(x0, y0, x1, y1)
    }

    private fun drawThumb(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (file.isDirectory) {
            return drawDefaultIcon(x0, y0, x1, y1)
        }
        when (importType) {
            // todo audio preview???
            "Video", "Audio" -> {
                val meta = meta
                if (meta != null) {
                    if (meta.videoWidth > 0) {
                        if (time == 0.0) { // not playing
                            drawImageOrThumb(x0, y0, x1, y1)
                        } else drawVideo(x0, y0, x1, y1)
                    } else {
                        drawDefaultIcon(x0, y0, x1, y1)
                        drawCircle(x0, y0, x1, y1)
                    }
                } else drawDefaultIcon(x0, y0, x1, y1)
            }
            else -> drawImageOrThumb(x0, y0, x1, y1)
        }
    }

    private var lines = 0
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        tooltip = file.name

        drawBackground()

        val font0 = titlePanel.font
        val font1 = FontManager.getFont(font0)
        val fontSize = font1.actualFontSize

        lines = max(ceil((h - w) / fontSize).toInt(), 1)

        val padding = w / 20

        val remainingW = w - padding * 2
        val remainingH = h - padding * 2

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
            y + h - padding,
            ::drawText
        )

        return

        // todo extra start button for Isabell, and disabled auto-play
        // todo change this in the settings xD

        // todo tiles on background to show transparency? ofc only in the area of the image

    }

    /**
     * draws the title
     * */
    private fun drawText(x0: Int, y0: Int, x1: Int, y1: Int) {
        // GFXx2D.drawSimpleTextCharByChar(x0 + max(0, deltaX), y0, 0, titlePanel.text)
        titlePanel.w = x1 - x0
        titlePanel.minW = x1 - x0
        titlePanel.calculateSize(x1 - x0, y1 - y0)
        titlePanel.backgroundColor = backgroundColor and 0xffffff
        val deltaX = ((x1 - x0) - titlePanel.minW) / 2
        titlePanel.x = x0 + max(0, deltaX)
        titlePanel.y = y0
        titlePanel.w = x1 - x0
        titlePanel.minW = x1 - x0
        titlePanel.h = y1 - y0
        titlePanel.drawText(0, 0, titlePanel.textColor)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                // todo select the file, if the mouse goes up, not down
                if (inFocus.any { it.contains(mouseDownX, mouseDownY) } && StudioBase.dragged?.getOriginal() != file) {
                    val inFocus = inFocus.filterIsInstance<FileExplorerEntry>().map { it.file }
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
                if (file.isSomeKindOfDirectory) {
                    explorer.switchTo(file)
                } else return false
            }
            "Rename" -> {
                val title = NameDesc("Rename To...", "", "ui.file.rename2")
                askName(x.toInt(), y.toInt(), title, file.name, NameDesc("Rename"), { -1 }, ::renameTo)
            }
            "OpenInExplorer" -> file.openInExplorer()
            "Delete" -> deleteFileMaybe(file)
            "OpenOptions" -> explorer.openOptions(file)
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun openInExplorer() {
        file.openInExplorer()
    }

    fun renameTo(newName: String) {
        val allowed = newName.toAllowedFilename()
        if (allowed != null) {
            val dst = file.getParent()!!.getChild(allowed)!!
            if (dst.exists && !allowed.equals(file.name, true)) {
                ask(NameDesc("Override existing file?", "", "ui.file.override")) {
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
            if (file.isSomeKindOfDirectory) {
                explorer.switchTo(file)
                // super.onDoubleClick(x, y, button)
            } else {
                explorer.onDoubleClick(file)
            }
        } else super.onDoubleClick(x, y, button)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        // todo in Rem's Engine, we first should check, whether there are prefabs, which depend on this file
        val files = inFocus.mapNotNull { (it as? FileExplorerEntry)?.file }
        if (files.size <= 1) {
            // ask, then delete (or cancel)
            deleteFileMaybe(file)
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
            openMenu(title, listOf(moveToTrash, dontDelete, deletePermanently))
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        val rawFiles = if (this in inFocus) {// multiple files maybe
            inFocus.filterIsInstance<FileExplorerEntry>().map { it.file }
        } else listOf(file)
        // we need this folder, when we have temporary copies,
        // because just File.createTempFile() changes the name,
        // and we need the original file name
        val tmpFolder = lazy { Files.createTempDirectory("tmp").toFile() }
        val tmpFiles = rawFiles.map {
            if (it is FileFileRef) it.file
            else {
                // create a temporary copy, that the OS understands
                val tmp = File(tmpFolder.value, it.name)
                copyHierarchy(it, tmp)
                tmp
            }
        }
        Input.copyFiles2(tmpFiles)
        return null
    }

    fun copyHierarchy(src: FileReference, dst: File) {
        if (src.isDirectory) {
            dst.mkdirs()
            for (child in src.listChildren() ?: emptyList()) {
                copyHierarchy(child, File(dst, child.name))
            }
        } else {
            src.inputStream().copy(dst.outputStream())
        }
    }

    override fun getMultiSelectablePanel() = this

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Tabs.spaces(tabDepth * 2 + 2)} ${file.name}")
    }

    override val className get() = "FileEntry"

    companion object {

        val dontDelete
            get() = MenuOption(
                NameDesc(
                    "No",
                    "Deletes none of the selected file; keeps them all",
                    "ui.file.delete.many.no"
                )
            ) {}

        fun deleteFileMaybe(file: FileReference) {
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
                moveToTrash(file.unsafeFile)
                FileExplorer.invalidateFileExplorers()
            }
            val deletePermanently = MenuOption(
                NameDesc(
                    "Yes, permanently",
                    "Deletes the file; file cannot be recovered",
                    "ui.file.delete.permanent"
                )
            ) {
                file.deleteRecursively()
                FileExplorer.invalidateFileExplorers()
            }
            openMenu(
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
                (x0 + x1) / 2, (y0 + y1) / 2, radius, radius, 0f, relativeTime * 360f * 4 / 3, relativeTime * 360f * 2,
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