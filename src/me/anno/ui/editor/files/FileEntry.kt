package me.anno.ui.editor.files

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.GFX.openMenu
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.texture.ClampMode
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.Video
import me.anno.objects.cache.Cache
import me.anno.studio.Studio
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.thumbs.Thumbs
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.style.Style
import me.anno.utils.*
import me.anno.video.FFMPEGMetadata
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// todo special icons for music, documents, videos, ... like in Windows and any other OS

class FileEntry(val explorer: FileExplorer, val isParent: Boolean, val file: File, style: Style) :
    Panel(style.getChild("fileEntry")) {

    // todo don't select stuff with secondary mouse keys

    var audio: Audio? = null

    val size = 100
    val importType = file.extension.getImportType()
    var iconPath = if (file.isDirectory) {
        if (file.listFiles2().isNotEmpty())
            "file/folder.png" else "file/empty_folder.png"
    } else {
        when (importType) {
            "Image", "Cubemap" -> "file/image.png"
            "Text" -> "file/text.png"
            "Audio", "Video" -> "file/music.png"
            else -> "file/document.png"
        }
    }

    val title = TextPanel(if (isParent) ".." else if (file.name.isEmpty()) file.toString() else file.name, style)

    init {
        title.backgroundColor = black
        title.breaksIntoMultiline = true
    }

    var wasInFocus = false
    val originalBackgroundColor = backgroundColor
    val darkerBackgroundColor = mixARGB(0, originalBackgroundColor, 0.7f)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = size
        minH = size
        this.w = size
        this.h = size
    }

    var startTime = 0L

    fun drawDefaultIcon(){
        val image = Cache.getIcon(iconPath, true) ?: whiteTexture
        var iw = image.w
        var ih = image.h
        val scale = (size - 20) / max(iw, ih).toFloat()
        iw = (iw * scale).roundToInt()
        ih = (ih * scale).roundToInt()
        image.ensureFilterAndClamping(false, ClampMode.CLAMP)
        GFX.drawTexture(x + (size - iw) / 2, y + (size - ih) / 2, iw, ih, image, -1, null)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        wasInFocus = isInFocus
        backgroundColor = if (isInFocus) darkerBackgroundColor else originalBackgroundColor
        drawBackground()
        // todo tiles on background to show transparency? ofc only in the area of the image

        fun drawImage(): Boolean {
            val image = Thumbs.getThumbnail(file, w)
            // val image = if (file.length() < 10e6) Cache.getImage(file, 1000, true) else null
            return if(image != null){
                var iw = image.w
                var ih = image.h
                val rot = image.rotation
                image.ensureFilterAndClamping(false, ClampMode.CLAMP)
                if(rot == null){
                    val scale = (size - 20) / max(iw, ih).toFloat()
                    iw = (iw * scale).roundToInt()
                    ih = (ih * scale).roundToInt()
                    GFX.drawTexture(x + (size - iw) / 2, y + (size - ih) / 2, iw, ih, image, -1, null)
                } else {
                    val m = Matrix4fArrayList()
                    rot.apply(m)
                    GFX.drawTexture(m, w, h, image, -1, null)
                }
                false
            } else true
        }

        if (file.extension.equals("svg", true)) {

        } else {
            val w = w
            val h = h
            val needsDefault = when (importType) {
                // todo audio preview???
                "Video", "Audio" -> {
                    val hoverPlaybackDelay = 0.5
                    val meta = FFMPEGMetadata.getMeta(file, true)
                    if(meta != null){
                        val previewFPS = min(meta.videoFPS, 30.0)
                        val maxFrameIndex = max(1, (previewFPS * meta.videoDuration).toInt())
                        var time = 0.0
                        val frameIndex = if(isHovered){
                            if(startTime == 0L){
                                startTime = System.nanoTime()
                                val audio = Video(file)
                                this.audio = audio
                                GFX.addAudioTask(5){ audio.startPlayback(-hoverPlaybackDelay, 1.0, Camera()) }
                                0
                            } else {
                                time = (System.nanoTime() - startTime) * 1e-9 - hoverPlaybackDelay
                                max(0, (time * previewFPS).toInt())
                            }
                        } else {
                            startTime = 0
                            val audio = audio
                            if(audio != null && audio.component?.isPlaying == true) {
                                this.audio = null
                                GFX.addAudioTask(1){ audio.stopPlayback() }
                            }
                            0
                        } % maxFrameIndex
                        val scale = max(min(meta.videoWidth / w, meta.videoHeight / h), 1)
                        fun drawCircle(){
                            if(time < 0.0){
                                // countdown-circle, pseudo-loading
                                // saves us some computations
                                // todo when we have precomputed images, already here preload the video
                                // load directly from windows? https://en.wikipedia.org/wiki/Windows_thumbnail_cache#:~:text=locality%20of%20Thumbs.-,db%20files.,thumbnails%20in%20each%20sized%20database.
                                // https://stackoverflow.com/questions/1439719/c-sharp-get-thumbnail-from-file-via-windows-api?
                                // how about Linux/Mac?
                                // (maybe after half of the waiting time)
                                val relativeTime = ((hoverPlaybackDelay+time)/hoverPlaybackDelay).toFloat()
                                val r = 1f-sq(relativeTime*2-1)
                                GFX.drawCircle(w, h, 0f, relativeTime * 360f * 4 / 3, relativeTime * 360f * 2,
                                    Vector4f(1f, 1f, 1f, r * 0.2f))
                            }
                        }
                        if(meta.videoWidth > 0){
                            fun drawVideo(): Boolean {
                                val bufferLength = 64
                                val image = Cache.getVideoFrame(file, scale, frameIndex, bufferLength, previewFPS, 1000, true)
                                if(frameIndex > 0) Cache.getVideoFrame(file, scale, frameIndex + bufferLength, bufferLength, previewFPS, 1000, true)
                                return if(image != null && image.isLoaded){
                                    GFX.drawTexture(w, h, image, -1, null)
                                    drawCircle()
                                    false
                                } else true
                            }
                            if(time == 0.0){
                                // not playing
                                drawImage()// && drawVideo()
                            } else {
                                drawVideo()
                            }
                        } else {
                            drawCircle()
                            drawDefaultIcon()
                            false
                        }
                    } else true
                }
                "Image" -> {
                    drawImage()
                }
                else -> true
            }
            if(needsDefault) drawDefaultIcon()
        }
        title.x = x
        title.y = y
        title.w = 1
        title.h = 1
        title.draw(x0, y0, x1, y1)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                if (Studio.dragged?.getOriginal() != file) {
                    Studio.dragged =
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
            "OpenOptions" -> {
                openMenu(listOf(
                    "Rename" to {
                        // todo on F2
                        // todo ask new name
                        // todo change name
                        // todo ok and cancel button
                        // todo check if name is valid
                        // todo rename the file...
                        LOGGER.warn("Renaming not yet implemented!")
                    },
                    "Open in Explorer" to file::openInExplorer,
                    "Delete" to this::deleteFileMaybe
                ))
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if(file.isDirectory){
            super.onDoubleClick(x, y, button)
        } else {
            SceneTabs.open(file)
        }
    }

    fun deleteFileMaybe(){
        openMenu("Delete this file? (${file.length().formatFileSize()})", listOf(
            "Yes" to {
                // todo put history state...
                file.deleteRecursively()
                explorer.invalidate()
            },
            "No" to {},
            "Yes, permanently" to {
                file.deleteRecursively()
                explorer.invalidate()
            }
        ))
    }

    override fun onDeleteKey(x: Float, y: Float) {
        if (GFX.inFocus.size == 1) {
            // ask, then delete (or cancel)
            deleteFileMaybe()
        } else if (GFX.inFocus.firstOrNull() == this) {
            // ask, then delete all (or cancel)
            openMenu("Delete these files? (${GFX.inFocus.size}x, ${
            GFX.inFocus
                .sumByDouble { (it as? FileEntry)?.file?.length()?.toDouble() ?: 0.0 }
                .toLong()
                .formatFileSize()
            })", listOf(
                "Yes" to {
                    // todo put history state...
                    inFocus.forEach { (it as? FileEntry)?.file?.deleteRecursively() }
                    explorer.invalidate()
                },
                "No" to {},
                "Yes, permanently" to {
                    inFocus.forEach { (it as? FileEntry)?.file?.deleteRecursively() }
                    explorer.invalidate()
                }
            ))
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if(this in inFocus){// multiple files maybe
            Input.pasteFiles(inFocus.filterIsInstance<FileEntry>().map { it.file })
        } else Input.pasteFiles(listOf(file))
        return null
    }

    override fun getMultiSelectablePanel() = this

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Tabs.spaces(tabDepth * 2 + 2)} ${file.name}")
    }

}