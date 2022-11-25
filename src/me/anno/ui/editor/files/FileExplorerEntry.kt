package me.anno.ui.editor.files

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.audio.openal.AudioTasks
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.cache.instances.LastModifiedCache
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.shaders.effects.FSR
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.ui.render.Renderers
import me.anno.fonts.FontManager
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip2Dual
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageCPUCache
import me.anno.image.ImageGPUCache
import me.anno.image.ImageReadable
import me.anno.image.ImageScale.scaleMaxPreview
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.FileReference.Companion.getReferenceAsync
import me.anno.io.files.FileReference.Companion.getReferenceOrTimeout
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.utils.TrashManager.moveToTrash
import me.anno.io.zip.InnerLinkFile
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.roundDiv
import me.anno.maths.Maths.sq
import me.anno.studio.GFXSettings
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.style.Style
import me.anno.utils.Color.black
import me.anno.utils.Tabs
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.pooling.JomlPools
import me.anno.utils.strings.StringHelper.setNumber
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Strings.formatTime
import me.anno.utils.types.Strings.getImportType
import me.anno.utils.types.Strings.isBlank2
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import kotlin.math.*

// todo right click to get all meta information? (properties panel in windows)

// done images: show extra information: width, height
open class FileExplorerEntry(
    private val explorer: FileExplorer?,
    val isParent: Boolean, file: FileReference, style: Style
) : PanelGroup(style.getChild("fileEntry")) {

    constructor(isParent: Boolean, file: FileReference, style: Style) :
            this(null, isParent, file, style)

    constructor(file: FileReference, style: Style) :
            this(null, false, file, style)

    // todo small file type (signature) icons
    // todo use search bar for sort parameters :)
    // todo or right click menu for sorting

    val path = file.absolutePath
    val ref1 get() = getReferenceAsync(path)

    // todo when entering a json file, and leaving it, the icon should not be a folder!

    // todo check: do url files work (link + icon)?

    // done icons for 3d meshes
    // done icons for project files
    // done asset files like unity, and then icons for them? (we want a unity-like engine, just with Kotlin)

    // done play mesh animations

    private var startTime = 0L

    var time = 0.0
    var frameIndex = 0
    var maxFrameIndex = 0
    val hoverPlaybackDelay = 0.5
    var scale = 1
    var previewFPS = 1.0
    var meta: FFMPEGMetadata? = null

    var showTitle = true
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    private val originalBackgroundColor = backgroundColor
    private val hoverBackgroundColor = mixARGB(black, originalBackgroundColor, 0.85f)
    private val darkerBackgroundColor = mixARGB(black, originalBackgroundColor, 0.7f)

    private val importType = file.extension.getImportType()
    private var iconPath = if (isParent || file.isDirectory) {
        if (isParent) {
            folderPath
        } else {
            when (file.name.lowercase()) {
                "music", "musik", "videos", "movies" -> musicPath
                "documents", "dokumente", "downloads" -> textPath
                "images", "pictures", "bilder" -> imagePath
                else -> if (file.hasChildren())
                    folderPath else emptyFolderPath
            }
        }
    } else {
        // actually checking the type would need to be done async, because it's slow to ready many, many files
        when (importType) {
            "Container" -> zipPath
            "Image", "Cubemap", "Cubemap-Equ" -> imagePath
            "Text" -> textPath
            "Audio", "Video" -> musicPath
            "Executable" -> exePath
            // todo link icon for .lnk and .url, and maybe .desktop
            else -> docsPath
        }
    }

    val titlePanel = TextPanel(
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

    private var audio: AudioFileStreamOpenAL? = null

    fun stopPlayback() {
        val audio = audio
        if (audio != null && audio.isPlaying) {
            AudioTasks.addTask("stop", 1) { audio.stop() }
            this.audio = null
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val titleSize = if (showTitle) titlePanel.font.sizeInt * 5 / 2 else 0
        val size = min(minW, minH - titleSize)
        minW = size
        minH = size + titleSize
        this.w = minW
        this.h = minH
    }

    // is null
    // override fun getLayoutState(): Any? = titlePanel.getLayoutState()
    private var lastTex: Any? = null
    private var lastMeta: Any? = null

    override fun onUpdate() {
        super.onUpdate()

        val meta = meta
        val tex = if (canBeSeen) when (val tex = getTexKey()) {
            is GPUFrame -> if (tex.isCreated) tex else null
            is Texture2D -> tex.state
            else -> tex
        } else null
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
        if (ref1?.isHidden == true) {
            isVisible = false
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
                val meta = getMeta(path, true)
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
                            startTime = Engine.gameTime
                            val file = getReferenceOrTimeout(path)
                            stopPlayback()
                            if (meta.hasAudio) {
                                this.audio = AudioFileStreamOpenAL(
                                    file, LoopingState.PLAY_LOOP,
                                    -hoverPlaybackDelay, meta, 1.0
                                )
                                AudioTasks.addTask("start", 5) {
                                    audio?.start()
                                }
                            }
                            0
                        } else {
                            time = (Engine.gameTime - startTime) * 1e-9 - hoverPlaybackDelay
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
        val image = ImageGPUCache[iconPath, true] ?: whiteTexture
        drawTexture(x0, y0, x1, y1, image)
    }

    private fun drawTexture(x0: Int, y0: Int, x1: Int, y1: Int, image: ITexture2D) {
        val w = x1 - x0
        val h = y1 - y0
        // if aspect ratio is extreme, use a different scale
        var (iw, ih) = scaleMaxPreview(image.w, image.h, abs(w), abs(h), 5)
        iw *= w.sign
        ih *= h.sign
        val isHDR = image.isHDR
        // we can use FSR to upsample the images xD
        val x = x0 + (w - iw) / 2
        val y = y0 + (h - ih) / 2
        if (image is Texture2D) image.filtering = GPUFiltering.LINEAR
        if (iw > image.w && ih > image.h) {// maybe use fsr only, when scaling < 4x
            FSR.upscale(image, x, y, iw, ih, false, backgroundColor, isHDR)// ^^
        } else {
            drawTexture(x, y, iw, ih, image, -1, null, isHDR)
        }
    }

    private fun getDefaultIcon() = ImageGPUCache[iconPath, true]

    private fun getImage(): Any? {
        val ref1 = ref1 ?: return null
        val thumb = Thumbs.getThumbnail(ref1, w, true)
        return thumb ?: getDefaultIcon()
    }

    private fun getTexKey(): Any? {
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

    private fun drawImageOrThumb(
        x0: Int, y0: Int,
        x1: Int, y1: Int
    ) {
        val w = x1 - x0
        val h = y1 - y0
        val file = ref1 ?: InvalidRef
        if (isHovered) {
            // todo reset time when not hovered
            val animSample = try {
                if (when (file.lcExtension) {
                        "json", "gltf", "fbx" -> true
                        else -> false
                    }
                ) PrefabCache.getPrefabInstance(file, true) else null
            } catch (e: Exception) {
                null // just not an animation
            }
            if (animSample is Animation) {
                val time = (Engine.gameTime / 1e9) % animSample.duration
                val samples = min(
                    GFX.maxSamples, when (StudioBase.instance?.gfxSettings) {
                        GFXSettings.HIGH -> 8
                        GFXSettings.MEDIUM -> 2
                        else -> 1
                    }
                )
                if (samples > 1) {
                    val tmp = FBStack["tmp", w, h, 4, false, 8, true] // msaa; probably should depend on gfx settings
                    GFXState.useFrame(0, 0, w, h, tmp, Renderers.simpleNormalRenderer) {
                        GFXState.depthMode.use(DepthMode.CLOSER) {
                            tmp.clearColor(backgroundColor, true)
                            Thumbs.drawAnimatedSkeleton(animSample, time.toFloat(), w.toFloat() / h)
                        }
                    }
                    GFX.copy(tmp)
                } else {
                    // use current buffer directly
                    GFXState.useFrame(
                        x0, y0, w, h,
                        GFXState.currentBuffer,
                        Renderers.simpleNormalRenderer
                    ) {
                        // todo clip to correct area
                        GFXState.depthMode.use(DepthMode.CLOSER) {
                            GFXState.currentBuffer.clearDepth()
                            Thumbs.drawAnimatedSkeleton(animSample, time.toFloat(), w.toFloat() / h)
                        }
                    }
                }
                invalidateDrawing() // make sure to draw it next frame
                return
            }
        }
        val img0 = Thumbs.getThumbnail(file, w, true)
        val img1 = img0 ?: getDefaultIcon()
        val image = img1 ?: whiteTexture
        val rot = (image as? Texture2D)?.rotation
        image.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
        if (rot == null || rot.isNull()) {
            drawTexture(x0, y0, x1, y1, image)
        } else if (rot.angleCW == 0 && rot.mirrorVertical && !rot.mirrorHorizontal) {
            drawTexture(x0, y1, x1, y0, image)
        } else {
            // todo draw image without overflowing into other things
            // todo maybe use transform for that :)
            val transform = GFXx2D.transform
            transform.pushMatrix()

            rot.apply(transform)

            // transform.rotateY(45f.toRadians())

            drawTexture(x0, y0, x1, y1, image)

            transform.popMatrix()
            /* GFX.clip2(x0, y0, x1, y1) {
                 val stack = Matrix4fArrayList()
                 rot.apply(stack)
                 stack.scale(1f, -1f, 0f)
                 draw3D(
                     stack, image, (y1 - y0) * image.w, (x1 - x0) * image.h, -1,
                     Filtering.LINEAR, Clamping.CLAMP, null, UVProjection.Planar
                 )
             }*/
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
        ref1 ?: InvalidRef, scale, frameIndex + offset,
        videoBufferLength, previewFPS, 1000, true
    )

    private fun drawVideo(x0: Int, y0: Int, x1: Int, y1: Int) {

        // todo something with the states is broken...
        // todo only white is visible, even if there should be colors...


        val image = getFrame(0)
        if (frameIndex > 0) getFrame(videoBufferLength)
        if (image != null && image.isCreated) {
            drawTexture(
                GFX.viewportWidth, GFX.viewportHeight,
                image, -1, null
            )
            drawCircle(x0, y0, x1, y1)
        } else drawDefaultIcon(x0, y0, x1, y1)

        // show video progress on playback, e.g. hh:mm:ss/hh:mm:ss
        if (h >= 3 * titlePanel.font.sizeInt) {
            val meta = getMeta(path, true)
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

    private fun drawThumb(
        x0: Int, y0: Int,
        x1: Int, y1: Int
    ) {
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

    open fun updateTooltip() {

        // todo add created & modified information

        // if is selected, and there are multiple files selected, show group stats
        if (isInFocus && siblings.count { (it.isInFocus && it is FileExplorerEntry) || it === this } > 1) {
            val files = siblings
                .filter { it.isInFocus || it === this }
                .mapNotNull { (it as? FileExplorerEntry)?.path }
                .mapNotNull { getReferenceAsync(it) }
            tooltip = "${files.count { it.isDirectory }} folders + ${files.count { !it.isDirectory }} files\n" +
                    files.sumOf { it.length() }.formatFileSize()

        } else {

            fun getTooltip(file: FileReference): String {
                return when {
                    file.isDirectory -> {
                        // todo add number of children?, or summed size
                        file.name
                    }
                    file is InnerLinkFile -> "Link to " + getTooltip(file.link)
                    file is PrefabReadable -> {
                        val prefab = file.readPrefab()
                        val name = prefab.instanceName
                        val base = prefab.prefab
                        "" +
                                "${file.name}\n" +
                                (if (base != InvalidRef) "${base.nameWithoutExtension}\n" else "") +
                                (if (name != null) "\"$name\"\n" else "") +
                                "${prefab.clazzName}, ${prefab.countTotalChanges(true)} Changes"
                    }
                    file is ImageReadable -> {
                        val image = file.readImage()
                        file.name + "\n" +
                                "${image.width} x ${image.height}"
                    }
                    else -> {
                        val meta = getMeta(path, true)
                        val ttt = StringBuilder()
                        ttt.append(file.name).append('\n')
                        ttt.append(file.length().formatFileSize())
                        if (meta != null) {
                            if (meta.hasVideo) {
                                ttt.append('\n').append(meta.videoWidth).append(" x ").append(meta.videoHeight)
                                if (meta.videoFrameCount > 1) ttt.append(" @").append(meta.videoFPS.f1()).append(" fps")
                            } else {
                                val image = ImageCPUCache.getImageWithoutGenerator(file)
                                if (image != null) {
                                    ttt.append('\n').append(image.width).append(" x ").append(image.height)
                                }
                            }
                            if (meta.hasAudio) {
                                ttt.append('\n').append(roundDiv(meta.audioSampleRate, 1000)).append(" kHz")
                                when (meta.audioChannels) {
                                    1 -> ttt.append(" Mono")
                                    2 -> ttt.append(" Stereo")
                                    else -> ttt.append(" ").append(meta.audioChannels).append(" Ch")
                                }
                            }
                            if (meta.duration > 0 && (meta.hasAudio || (meta.hasVideo && meta.videoFrameCount > 1))) {
                                val duration = meta.duration
                                val digits = if (duration < 60) max((1.5 - log10(duration)).roundToInt(), 0) else 0
                                ttt.append('\n').append(meta.duration.formatTime(digits))
                            }
                        }
                        ttt.toString()
                    }
                }
            }

            val ref1 = ref1
            tooltip = if (ref1 != null) getTooltip(ref1) else "Loading..."

        }
    }

    private var lines = 0
    private var padding = 0
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        if (isHovered || isInFocus) {
            updateTooltip()
        }

        drawBackground(x0, y0, x1, y1)

        val font0 = titlePanel.font
        val font1 = FontManager.getFont(font0)
        val fontSize = font1.actualFontSize

        val x = x
        val y = y
        val w = w
        val h = h

        val extraHeight = h - w
        lines = if (showTitle) max(ceil(extraHeight / fontSize).toInt(), 1) else 0

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

        if (showTitle) clip2Dual(
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
                // why was that condition there?
                // inFocus.any { it.contains(mouseDownX, mouseDownY) } && StudioBase.dragged?.getOriginal() != file
                val selectedFiles = siblings
                    .filterIsInstance<FileExplorerEntry>()
                    .filter { it.isInFocus || it === this }
                    .map { getReferenceOrTimeout(it.path) }
                val title = selectedFiles.joinToString("\n") { it.nameWithoutExtension }
                val stringContent = selectedFiles.joinToString("\n") { it.toString() }
                val original: Any = if (selectedFiles.size == 1) selectedFiles[0] else selectedFiles
                StudioBase.dragged = Draggable(stringContent, "File", original, title, style)
            }
            "Enter" -> {
                if (explorer != null) {
                    val file = getReferenceOrTimeout(path)
                    if (explorer.canSensiblyEnter(file)) {
                        explorer.switchTo(file)
                    } else return false
                } else return false
            }
            "Rename" -> {
                val file = getReferenceOrTimeout(path)
                val title = NameDesc("Rename To...", "", "ui.file.rename2")
                askName(windowStack, x.toInt(), y.toInt(), title, file.name, NameDesc("Rename"), { -1 }, ::renameTo)
            }
            "OpenInExplorer" -> getReferenceOrTimeout(path).openInExplorer()
            "OpenInStandardProgram" -> getReferenceOrTimeout(path).openInStandardProgram()
            "EditInStandardProgram" -> getReferenceOrTimeout(path).editInStandardProgram()
            "Delete" -> deleteFileMaybe(this, getReferenceOrTimeout(path))
            "OpenOptions" -> explorer?.openOptions(getReferenceOrTimeout(path)) ?: return false
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun renameTo(newName: String) {
        val allowed = newName.toAllowedFilename()
        if (allowed != null) {
            val file = getReferenceOrTimeout(path)
            val dst = file.getParent()!!.getChild(allowed)
            if (dst.exists && !allowed.equals(file.name, true)) {
                ask(windowStack, NameDesc("Override existing file?", "", "ui.file.override")) {
                    file.renameTo(dst)
                    explorer?.invalidate()
                }
            } else {
                file.renameTo(dst)
                explorer?.invalidate()
            }
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft && explorer != null) {
            val file = getReferenceOrTimeout(path)
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
        val file = getReferenceOrTimeout(path)
        // todo in Rem's Engine, we first should check, whether there are prefabs, which depend on this file
        val files = parent!!.children.mapNotNull {
            if (it is FileExplorerEntry && it.isInFocus)
                getReferenceOrTimeout(it.path) else null
        }
        if (files.size <= 1) {
            // ask, then delete (or cancel)
            deleteFileMaybe(this, file)
        } else if (files.first() === file) {
            // ask, then delete all (or cancel)
            val matches = siblings.count { (it is FileExplorerEntry && it.isInFocus) || it === this }
            val title = NameDesc(
                "Delete these files? (${matches}x, ${
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
                moveToTrash(files.map { it.toFile() }.toTypedArray())
                explorer?.invalidate()
            }
            val deletePermanently = MenuOption(
                NameDesc(
                    "Yes, permanently",
                    "Deletes all selected files; forever; files cannot be recovered",
                    "ui.file.delete.many.permanently"
                )
            ) {
                files.forEach { it.deleteRecursively() }
                explorer?.invalidate()
            }
            openMenu(windowStack, title, listOf(moveToTrash, dontDelete, deletePermanently))
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        val files = if (isInFocus) {// multiple files maybe
            siblings.filterIsInstance<FileExplorerEntry>()
                .filter { it.isInFocus }
                .map { getReferenceOrTimeout(it.path) }
        } else listOf(getReferenceOrTimeout(path))
        Input.copyFiles(files)
        return null
    }

    override fun getMultiSelectablePanel() = this

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Tabs.spaces(tabDepth * 2 + 2)} ${getReferenceOrTimeout(path).name}")
    }

    override val className get() = "FileEntry"

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FileExplorerEntry::class)

        @JvmField
        var videoBufferLength = 64

        @JvmStatic
        private val charHHMMSS = "hh:mm:ss/hh:mm:ss".toCharArray()
        @JvmStatic
        private val charMMSS = "mm:ss/mm:ss".toCharArray()

        @JvmField
        val folderPath = getReference("res://file/folder.png")
        @JvmField
        val musicPath = getReference("res://file/music.png")
        @JvmField
        val textPath = getReference("res://file/text.png")
        @JvmField
        val imagePath = getReference("res://file/image.png")
        @JvmField
        val emptyFolderPath = getReference("res://file/empty_folder.png")
        @JvmField
        val exePath = getReference("res://file/executable.png")
        @JvmField
        val docsPath = getReference("res://file/document.png")
        @JvmField
        val zipPath = getReference("res://file/compressed.png")

        @JvmStatic
        val dontDelete
            get() = MenuOption(
                NameDesc(
                    "No",
                    "Deletes none of the selected file; keeps them all",
                    "ui.file.delete.many.no"
                )
            ) {}

        @JvmStatic
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
                val file2 = file.toFile()
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

        @JvmStatic
        fun drawLoadingCircle(relativeTime: Float, x0: Int, x1: Int, y0: Int, y1: Int) {
            val r = 1f - sq(relativeTime * 2 - 1)
            val radius = min(y1 - y0, x1 - x0) / 2f
            val color = JomlPools.vec4f.borrow()
            GFXx2D.drawCircleOld(
                (x0 + x1) / 2, (y0 + y1) / 2, radius, radius, 0f,
                relativeTime * 360f * 4 / 3,
                relativeTime * 360f * 2,
                color.set(1f, 1f, 1f, r * 0.2f)
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun drawLoadingCircle(stack: Matrix4fArrayList, relativeTime: Float) {
            GFXx3D.draw3DCircle(
                stack, 0f,
                relativeTime * 360f * 4 / 3,
                relativeTime * 360f * 2,
                Vector4f(1f, 1f, 1f, 0.2f)
            )
        }

    }


}