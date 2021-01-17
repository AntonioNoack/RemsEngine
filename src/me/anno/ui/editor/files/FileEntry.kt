package me.anno.ui.editor.files

import me.anno.cache.instances.ImageCache.getInternalTexture
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.config.DefaultStyle.black
import me.anno.fonts.FontManager
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip2Dual
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.GFXx2D
import me.anno.gpu.GFXx2D.drawTexture
import me.anno.gpu.GFXx3D
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.Video
import me.anno.studio.StudioBase
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.thumbs.Thumbs
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.style.Style
import me.anno.utils.FileHelper.formatFileSize
import me.anno.utils.FileHelper.listFiles2
import me.anno.utils.FileHelper.openInExplorer
import me.anno.utils.Maths.mixARGB
import me.anno.utils.Maths.sq
import me.anno.utils.Quad
import me.anno.utils.StringHelper.getImportType
import me.anno.utils.Tabs
import me.anno.video.FFMPEGMetadata
import me.anno.video.VFrame
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FileEntry(
    private val explorer: FileExplorer,
    isParent: Boolean, val file: File, style: Style
) :
    PanelGroup(style.getChild("fileEntry")) {

    // todo why is .. split into two lines???

    // todo sometimes the title is missing... or its color... why ever...

    var audio: Audio? = null

    val size get() = explorer.entrySize.toInt()

    val importType = file.extension.getImportType()
    var iconPath = if (file.isDirectory) {
        when (file.name.toLowerCase()) {
            "music", "musik", "videos", "movies" -> "file/music.png"
            "documents", "dokumente", "downloads" -> "file/text.png"
            "images", "pictures" -> "file/image.png"
            else -> if (file.listFiles2().isNotEmpty())
                "file/folder.png" else "file/empty_folder.png"
        }
    } else {
        when (importType) {
            "Image", "Cubemap" -> "file/image.png"
            "Text" -> "file/text.png"
            "Audio", "Video" -> "file/music.png"
            else -> "file/document.png"
        }
    }

    val title = TextPanel(if (isParent) ".." else if (file.name.isEmpty()) file.toString() else file.name, style)

    override val children: List<Panel> = listOf(title)
    override fun remove(child: Panel) {}

    init {
        title.breaksIntoMultiline = true
        title.parent = this
        title.instantTextLoading = true
    }

    fun stopPlayback() {
        val audio = audio
        if (audio != null && audio.component?.isPlaying == true) {
            this.audio = null
            GFX.addAudioTask(1) { audio.stopPlayback() }
        }
    }

    var wasInFocus = false
    val originalBackgroundColor = backgroundColor
    val hoverBackgroundColor = mixARGB(black, originalBackgroundColor, 0.85f)
    val darkerBackgroundColor = mixARGB(black, originalBackgroundColor, 0.7f)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = size
        minW = size
        minH = size * 4 / 3
        this.w = size
        this.h = size * 4 / 3
    }

    var startTime = 0L

    override fun getLayoutState(): Any? = Pair(super.getLayoutState(), title.getLayoutState())
    override fun getVisualState(): Any? {
        val tex = when (val tex = getTexKey()) {
            is VFrame -> if (tex.isLoaded) tex else null
            is Texture2D -> tex.state
            else -> tex
        }
        title.canBeSeen = canBeSeen
        return Quad(super.getVisualState(), title.getVisualState(), tex, meta)
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

    var time = 0.0
    var frameIndex = 0
    var maxFrameIndex = 0
    val hoverPlaybackDelay = 0.5
    var scale = 1
    var previewFPS = 1.0
    var meta: FFMPEGMetadata? = null

    fun updatePlaybackTime() {
        when (importType) {
            "Video", "Audio" -> {
                val meta = FFMPEGMetadata.getMeta(file, true)
                this.meta = meta
                if (meta != null) {
                    val w = w
                    val h = h
                    previewFPS = min(meta.videoFPS, 30.0)
                    maxFrameIndex = max(1, (previewFPS * meta.videoDuration).toInt())
                    time = 0.0
                    frameIndex = if (isHovered) {
                        if (startTime == 0L) {
                            startTime = GFX.gameTime
                            val audio = Video(file)
                            this.audio = audio
                            GFX.addAudioTask(5) { audio.startPlayback(-hoverPlaybackDelay, 1.0, Camera()) }
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

    fun drawDefaultIcon(x0: Int, y0: Int, x1: Int, y1: Int) {
        val image = getInternalTexture(iconPath, true) ?: whiteTexture
        drawTexture(x0, y0, x1, y1, image)
    }

    fun drawTexture(x0: Int, y0: Int, x1: Int, y1: Int, image: ITexture2D) {
        val w = x1 - x0
        val h = y1 - y0
        var iw = image.w
        var ih = image.h
        val scale = min(w.toFloat() / iw, h.toFloat() / ih)
        iw = (iw * scale).roundToInt()
        ih = (ih * scale).roundToInt()
        drawTexture(x0 + (w - iw) / 2, y0 + (h - ih) / 2, iw, ih, image, -1, null)
    }

    fun getDefaultIcon() = getInternalTexture(iconPath, true)

    fun getTexKey(): Any? {
        fun getImage(): Any? {
            val thumb = Thumbs.getThumbnail(file, w)
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
            "Image" -> getImage()
            else -> getDefaultIcon()
        }
    }

    fun drawImageOrThumb(x0: Int, y0: Int, x1: Int, y1: Int) {
        val w = x1 - x0
        val h = y1 - y0
        val image = Thumbs.getThumbnail(file, w) ?: getDefaultIcon() ?: whiteTexture
        val tex2D = image as? Texture2D
        val rot = tex2D?.rotation
        tex2D?.ensureFilterAndClamping(GPUFiltering.LINEAR, Clamping.CLAMP)
        if (rot == null) {
            drawTexture(x0, y0, x1, y1, image)
        } else {
            val m = Matrix4fArrayList()
            rot.apply(m)
            drawTexture(m, w, h, image, -1, null)
        }
    }

    fun drawCircle(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (time < 0.0) {
            // countdown-circle, pseudo-loading
            // saves us some computations
            // todo when we have precomputed images, already here preload the video
            // load directly from windows? https://en.wikipedia.org/wiki/Windows_thumbnail_cache#:~:text=locality%20of%20Thumbs.-,db%20files.,thumbnails%20in%20each%20sized%20database.
            // https://stackoverflow.com/questions/1439719/c-sharp-get-thumbnail-from-file-via-windows-api?
            // how about Linux/Mac?
            // (maybe after half of the waiting time)
            val relativeTime = ((hoverPlaybackDelay + time) / hoverPlaybackDelay).toFloat()
            drawLoadingCircle(relativeTime, x0, x1, y0, y1)
        }
    }

    companion object {

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

    fun drawVideo(x0: Int, y0: Int, x1: Int, y1: Int) {

        // todo something with the states is broken...
        // todo only white is visible, even if there should be colors...

        val w = x1 - x0
        val bufferLength = 64
        fun getFrame(offset: Int) = getVideoFrame(
            file, scale, frameIndex + offset,
            bufferLength, previewFPS, 1000, true
        )

        val image = getFrame(0)
        if (frameIndex > 0) getFrame(bufferLength)
        if (image != null && image.isLoaded) {
            drawTexture(w, w, image, -1, null)
            drawCircle(x0, y0, x1, y1)
        } else drawDefaultIcon(x0, y0, x1, y1)
    }

    fun drawThumb(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (file.extension.equals("svg", true)) {
            drawDefaultIcon(x0, y0, x1, y1)
        } else {
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
                "Image" -> drawImageOrThumb(x0, y0, x1, y1)
                else -> drawDefaultIcon(x0, y0, x1, y1)
            }
        }
    }

    private var lines = 0
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        tooltip = file.name

        drawBackground()

        val font0 = title.font
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
        // todo settings xD

        // todo tiles on background to show transparency? ofc only in the area of the image

    }

    /**
     * draws the title
     * */
    fun drawText(x0: Int, y0: Int, x1: Int, y1: Int) {
        title.w = x1 - x0
        title.minW = x1 - x0
        title.calculateSize(x1 - x0, y1 - y0)
        title.backgroundColor = backgroundColor and 0xffffff
        val deltaX = ((x1 - x0) - title.minW) / 2
        title.x = x0 + max(0, deltaX)
        title.y = y0
        title.w = x1 - x0
        title.minW = x1 - x0
        title.h = y1 - y0
        title.drawText(0, 0, title.text, title.textColor)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                if (StudioBase.dragged?.getOriginal() != file) {
                    StudioBase.dragged =
                        Draggable(file.toString(), "File", file, TextPanel(file.nameWithoutExtension, style))
                }
            }
            "Enter" -> {
                if (file.isDirectory) {
                    explorer.folder = file
                    explorer.invalidate()
                } else {// todo check if it's a compressed thing we can enter
                    return false
                }
            }
            "Rename" -> {
                askName(x.toInt(), y.toInt(), NameDesc("Rename To...", "", "ui.file.rename2"), "Rename", { -1 }) {
                    val allowed = it.toAllowedFilename()
                    if (allowed != null) {
                        val dst = File(file.parentFile, allowed)
                        if (dst.exists() && !allowed.equals(file.name, true)) {
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
            }
            "OpenInExplorer" -> file.openInExplorer()
            "Delete" -> deleteFileMaybe()
            "OpenOptions" -> {
                // todo add option to open json in specialized json editor...
                openMenu(
                    listOf(
                        MenuOption(NameDesc("Rename", "Change the name of this file", "ui.file.rename")) {
                            onGotAction(x, y, dx, dy, "Rename", false)
                        },
                        MenuOption(
                            NameDesc(
                                "Open in Explorer",
                                "Open the file in your default file explorer",
                                "ui.file.openInExplorer"
                            )
                        ) { file.openInExplorer() },
                        MenuOption(
                            NameDesc(
                                "Delete", "Delete this file", "ui.file.delete"
                            ),
                            this::deleteFileMaybe
                        )
                    )
                )
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (file.isDirectory) {
            super.onDoubleClick(x, y, button)
        } else {
            SceneTabs.open(file)
        }
    }

    fun deleteFileMaybe() {
        openMenu(
            NameDesc(
                "Delete this file? (${file.length().formatFileSize()})",
                "",
                "ui.file.delete.ask"
            ), listOf(
                /* "Yes" to {
                     // todo move to OS trash
                     file.deleteRecursively()
                     explorer.invalidate()
                 },*/
                MenuOption(
                    NameDesc(
                        "No",
                        "Don't delete the file, keep it",
                        "ui.file.delete.no"
                    )
                ) {},
                MenuOption(
                    NameDesc(
                        "Yes, permanently",
                        "Deletes the file; file cannot be recovered",
                        "ui.file.delete.permanent"
                    )
                ) {
                    file.deleteRecursively()
                    explorer.invalidate()
                }
            ))
    }

    override fun onDeleteKey(x: Float, y: Float) {
        if (inFocus.size == 1) {
            // ask, then delete (or cancel)
            deleteFileMaybe()
        } else if (inFocus.firstOrNull() == this) {
            // ask, then delete all (or cancel)
            openMenu(NameDesc(
                "Delete these files? (${inFocus.size}x, ${
                inFocus
                    .sumByDouble { (it as? FileEntry)?.file?.length()?.toDouble() ?: 0.0 }
                    .toLong()
                    .formatFileSize()
                })", "", "ui.file.delete.ask.many"
            ), listOf(
                /*"Yes" to {
                    // todo put history state or move to OS-trash
                    inFocus.forEach { (it as? FileEntry)?.file?.deleteRecursively() }
                    explorer.invalidate()
                },*/
                MenuOption(
                    NameDesc(
                        "No",
                        "Deletes none of the selected file; keeps them all",
                        "ui.file.delete.many.no"
                    )
                ) {},
                MenuOption(
                    NameDesc(
                        "Yes, permanently",
                        "Deletes all selected files; forever; files cannot be recovered",
                        "ui.file.delete.many.permanently"
                    )
                ) {
                    inFocus.forEach { (it as? FileEntry)?.file?.deleteRecursively() }
                    explorer.invalidate()
                }
            ))
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if (this in inFocus) {// multiple files maybe
            Input.copyFiles(inFocus.filterIsInstance<FileEntry>().map { it.file })
        } else Input.copyFiles(listOf(file))
        return null
    }

    override fun getMultiSelectablePanel() = this

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Tabs.spaces(tabDepth * 2 + 2)} ${file.name}")
    }

}