package me.anno.ui.editor.files

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFX.openMenu
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.texture.ClampMode
import me.anno.input.MouseButton
import me.anno.objects.cache.Cache
import me.anno.objects.modes.LoopingState
import me.anno.studio.Studio
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.style.Style
import me.anno.utils.*
import me.anno.utils.OS.startProcess
import me.anno.video.FFMPEGMetadata
import org.joml.Vector4f
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FileEntry(val explorer: FileExplorer, val isParent: Boolean, val file: File, style: Style) :
    Panel(style.getChild("fileEntry")) {

    val size = 100
    val importType = file.extension.getImportType()
    var iconPath = if (file.isDirectory) {
        if (file.listFiles2().isNotEmpty())
            "file/folder.png" else "file/empty_folder.png"
    } else {
        when (importType) {
            "Image", "Cubemap" -> "file/image.png"
            "Text" -> "file/text.png"
            // todo dark/bright styled images
            // todo dark image for video -> one of the first frames? :)
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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        wasInFocus = isInFocus
        backgroundColor = if (isInFocus) darkerBackgroundColor else originalBackgroundColor
        drawBackground()
        // todo tiles on background to show transparency? ofc only in the area of the image
        if (file.extension.equals("svg", true)) {

        } else {
            val w = w
            val h = h
            val needsDefault = when (importType) {
                // todo audio preview???
                "Video" -> {
                    // todo faster calculation of preview images
                    // todo maybe just cache them (statically, in files), once they were downloaded?
                    val hoverPlaybackDelay = 0.5
                    val meta = FFMPEGMetadata.getMeta(file, true)
                    if(meta != null){
                        val previewFPS = min(meta.videoFPS, 30.0)
                        val maxFrameIndex = max(1, (previewFPS * meta.videoDuration).toInt())
                        var time = 0.0
                        val frameIndex = if(isHovered){
                            if(startTime == 0L){
                                startTime = System.nanoTime()
                                0
                            } else {
                                time = (System.nanoTime() - startTime) * 1e-9 - hoverPlaybackDelay
                                max(0, (time * previewFPS).toInt())
                            }
                        } else {
                            startTime = 0
                            0
                        } % maxFrameIndex
                        val scale = min(meta.videoWidth / w, meta.videoHeight / h)
                        val image = Cache.getVideoFrame(file, scale, frameIndex, if(frameIndex == 0) 16 else 64, previewFPS, 1000, LoopingState.PLAY_LOOP)
                        if(image != null && image.isLoaded){
                            var iw = image.w
                            var ih = image.h
                            val scale2 = (size) / max(iw, ih).toFloat()
                            iw = (iw * scale2).roundToInt()
                            ih = (ih * scale2).roundToInt()
                            // image.ensureFiltering(false)
                            GFX.drawTexture(x + (size - iw) / 2, y + (size - ih) / 2, iw, ih, image, -1, null)
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
                                GFX.drawCircle(x, y, iw, ih, 0f, relativeTime * 360f * 4 / 3, relativeTime * 360f * 2, Vector4f(1f, 1f, 1f, r * 0.2f))
                            }
                            false
                        } else true
                    } else true
                }
                "Image" -> {
                    val image = if (file.length() < 10e6) Cache.getImage(file, 1000, true) else null
                    if(image != null){
                        var iw = image.w
                        var ih = image.h
                        val scale = (size - 20) / max(iw, ih).toFloat()
                        iw = (iw * scale).roundToInt()
                        ih = (ih * scale).roundToInt()
                        image.ensureFilterAndClamping(false, ClampMode.CLAMP)
                        GFX.drawTexture(x + (size - iw) / 2, y + (size - ih) / 2, iw, ih, image, -1, null)
                    }
                    image == null
                }
                else -> true
            }
            if(needsDefault){
                val image = Cache.getIcon(iconPath, true) ?: whiteTexture
                var iw = image.w
                var ih = image.h
                val scale = (size - 20) / max(iw, ih).toFloat()
                iw = (iw * scale).roundToInt()
                ih = (ih * scale).roundToInt()
                image.ensureFilterAndClamping(false, ClampMode.CLAMP)
                GFX.drawTexture(x + (size - iw) / 2, y + (size - ih) / 2, iw, ih, image, -1, null)
            }

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
                    },
                    "Open in Explorer" to {
                        when {
                            OS.isWindows -> {// https://stackoverflow.com/questions/2829501/implement-open-containing-folder-and-highlight-file
                                startProcess("explorer.exe", "/select,", file.absolutePath)
                            }
                            OS.isLinux -> {// https://askubuntu.com/questions/31069/how-to-open-a-file-manager-of-the-current-directory-in-the-terminal
                                startProcess("xdg-open", file.absolutePath)
                            }
                        }
                    },
                    "Delete" to { deleteFileMaybe() }
                ))
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when (button) {
            MouseButton.RIGHT -> {
                // todo get all meta data you ever need
                // todo or get more options? probably better... delete, new folder, new file,
                // todo rename, open in explorer, open in editor, ...
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        when (button) {
            MouseButton.RIGHT -> {
                // todo open the file in the editor, or add it to the scene?
                // todo or open it using windows?
            }
            else -> super.onDoubleClick(x, y, button)
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
                    GFX.inFocus.forEach { (it as? FileEntry)?.file?.deleteRecursively() }
                    explorer.invalidate()
                },
                "No" to {},
                "Yes, permanently" to {
                    GFX.inFocus.forEach { (it as? FileEntry)?.file?.deleteRecursively() }
                    explorer.invalidate()
                }
            ))
        }
    }

    override fun getMultiSelectablePanel() = this

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Tabs.spaces(tabDepth * 2 + 2)} ${file.name}")
    }

    override fun getClassName() = "FileEntry"

}