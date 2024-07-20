package me.anno.ui.editor.files

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.EngineBase
import me.anno.engine.Events.addEvent
import me.anno.engine.GFXSettings
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip2Dual
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.drawTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTexts.popBetterBlending
import me.anno.gpu.drawing.DrawTexts.pushBetterBlending
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.effects.FSR
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.ImageScale
import me.anno.image.ImageScale.scaleMaxPreview
import me.anno.image.thumbs.AssetThumbnails
import me.anno.image.thumbs.Thumbs
import me.anno.input.Clipboard
import me.anno.input.Key
import me.anno.io.MediaMetadata
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.files.Reference.getReferenceAsync
import me.anno.io.files.Reference.getReferenceOrTimeout
import me.anno.io.files.inner.InnerLinkFile
import me.anno.io.utils.TrashManager.moveToTrash
import me.anno.io.xml.ComparableStringBuilder
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.roundDiv
import me.anno.maths.Maths.sq
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.WindowStack
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileExplorer.Companion.rightClickedFiles
import me.anno.ui.editor.files.FileExplorerIcons.docsPath
import me.anno.ui.editor.files.FileExplorerIcons.emptyFolderPath
import me.anno.ui.editor.files.FileExplorerIcons.exePath
import me.anno.ui.editor.files.FileExplorerIcons.folderPath
import me.anno.ui.editor.files.FileExplorerIcons.imagePath
import me.anno.ui.editor.files.FileExplorerIcons.linkPath
import me.anno.ui.editor.files.FileExplorerIcons.metadataPath
import me.anno.ui.editor.files.FileExplorerIcons.musicPath
import me.anno.ui.editor.files.FileExplorerIcons.textPath
import me.anno.ui.editor.files.FileExplorerIcons.videoPath
import me.anno.ui.editor.files.FileExplorerIcons.zipPath
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.files.OpenFileExternally.editInStandardProgram
import me.anno.utils.files.OpenFileExternally.openInExplorer
import me.anno.utils.files.OpenFileExternally.openInStandardProgram
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.formatTime
import me.anno.utils.types.Strings.getImportTypeByExtension
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.setNumber
import me.anno.video.VideoStream
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

// todo right click to get all meta information? (properties panel in windows)

open class FileExplorerEntry(
    private val explorer: FileExplorer?,
    val isParent: Boolean, file: FileReference, style: Style
) : PanelGroup(style.getChild("fileEntry")) {

    constructor(file: FileReference, style: Style) :
            this(null, false, file, style)

    // todo small file type (signature) icons

    val path = file.absolutePath
    val ref1 get() = getReferenceAsync(path)
    val ref1s get() = getReference(path)

    private var startTime = 0L

    var time = 0.0
    var frameIndex = 0
    var maxFrameIndex = 0
    var scale = 1
    var previewFPS = 1.0
    var meta: MediaMetadata? = null
    var listMode = false

    var showTitle = true
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    var originalBackgroundColor = backgroundColor
    var hoverBackgroundColor = mixARGB(black, originalBackgroundColor, 0.85f)
    var darkerBackgroundColor = mixARGB(black, originalBackgroundColor, 0.7f)

    private val importType = getImportTypeByExtension(file.lcExtension)
    private var iconPath = if (isParent || file.isDirectory) {
        if (isParent) {
            folderPath
        } else {
            when (file.name.lowercase()) {
                "music", "musik" -> musicPath
                "videos", "movies" -> videoPath
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
            "Audio" -> musicPath
            "Video" -> videoPath
            "Executable" -> exePath
            "Metadata" -> metadataPath
            "Link" -> linkPath
            else -> docsPath
        }
    }

    val isDirectory = isParent || file.isDirectory

    val titlePanel = TextPanel(
        when {
            isParent -> ".."
            file.nameWithoutExtension.isBlank2() && file.name.isBlank2() -> file.toString()
            showFileExtensions || file.nameWithoutExtension.isBlank2() || file.isDirectory -> file.name
            else -> file.nameWithoutExtension
        }, style
    )

    override val children: List<Panel> = listOf(titlePanel)
    override fun remove(child: Panel) {}

    init {
        titlePanel.breaksIntoMultiline = true
        titlePanel.parent = this
        titlePanel.instantTextLoading = false
    }

    override fun calculateSize(w: Int, h: Int) {
        val titleSize = if (showTitle) titlePanel.font.sizeInt * 5 / 2 else 0
        if (listMode) {
            minW = titleSize * 5 // idk
            minH = titleSize
        } else {
            val size = min(minW, minH - titleSize)
            minW = size
            minH = size + titleSize
        }
    }

    // is null
    // override fun getLayoutState(): Any? = titlePanel.getLayoutState()
    private var lastTex: Any? = null
    private var lastMeta: Any? = null

    override fun onUpdate() {
        super.onUpdate()

        if (explorer != null && parent == explorer.content2d) {
            listMode = explorer.listMode
        }

        val meta = meta
        val tex = if (canBeSeen) when (val tex = getTexKey()) {
            is GPUFrame -> if (tex.isCreated) tex else null
            is Texture2D -> tex.state
            else -> tex
        } else null
        if (lastMeta != meta || lastTex != tex) {
            lastTex = tex
            lastMeta = meta
            invalidateDrawing()
        }

        titlePanel.canBeSeen = canBeSeen

        // todo instead invalidate all file explorers, if they contain that file
        /*val newFile = getReference(file)
        if (newFile !== file) {
            file = newFile
            invalidateDrawing()
        }

        if (!file.exists) {
            explorer.invalidate()
        }*/

        // needs to be disabled in the future, I think
        isVisible = ref1?.isHidden != true

        backgroundColor = when {
            isInFocus || ref1 in rightClickedFiles -> darkerBackgroundColor
            isHovered -> hoverBackgroundColor
            else -> originalBackgroundColor
        }
        updatePlaybackTime()

        // todo only if is animation
        if (isHovered) invalidateDrawing()
    }

    var supportsPlayback = true
    private fun updatePlaybackTime() {
        when (importType) {
            "Video", "Audio" -> {
                val meta = getMeta(path, true)
                this.meta = meta
                if (meta != null) {
                    val w = width
                    val h = height
                    previewFPS = min(meta.videoFPS, 120.0)
                    maxFrameIndex = max(1, (previewFPS * meta.videoDuration).toInt())
                    time = 0.0
                    frameIndex = if (isHovered && supportsPlayback && GFX.activeWindow == GFX.focusedWindow) {
                        invalidateDrawing()
                        if (startTime == 0L) {
                            startTime = Time.nanoTime
                            val file = getReferenceOrTimeout(path)
                            stopAnyPlayback()
                            val maxSize = min(
                                max(meta.videoWidth, meta.videoHeight),
                                max(width, height)
                            )
                            if (meta.hasAudio) startAudioPlayback(file, meta)
                            if (meta.hasVideo) startVideoPlayback(file, meta, maxSize)
                            0
                        } else {
                            time = (Time.nanoTime - startTime) * 1e-9 - hoverPlaybackDelay
                            max(0, (time * previewFPS).toInt())
                        }
                    } else {
                        startTime = 0
                        val file = getReferenceOrTimeout(path)
                        stopPlayback(file)
                        0
                    } % maxFrameIndex
                    scale = max(min(meta.videoWidth / w, meta.videoHeight / h), 1)
                }
            }
        }
    }

    private fun drawDefaultIcon(x0: Int, y0: Int, x1: Int, y1: Int) {
        val image = TextureCache[iconPath, true] ?: whiteTexture
        drawTexture(x0, y0, x1, y1, image)
    }

    private fun drawTexture(x0: Int, y0: Int, x1: Int, y1: Int, image: ITexture2D) {
        val w = x1 - x0
        val h = y1 - y0
        // if aspect ratio is extreme, use a different scale
        var (iw, ih) = scaleMaxPreview(image.width, image.height, abs(w), abs(h), 5)
        iw *= w.sign
        ih *= h.sign
        val isHDR = image.isHDR
        // we can use FSR to upsample the images xD
        val x = x0 + (w - iw) / 2
        val y = y0 + (h - ih) / 2
        if (image is Texture2D) image.filtering = Filtering.LINEAR
        if (iw > image.width && ih > image.height) {// maybe use fsr only, when scaling < 4x
            FSR.upscale(image, x, y, iw, ih, backgroundColor, false, isHDR, true)// ^^
        } else {
            drawTexture(x, y, iw, ih, image, -1, null, isHDR)
        }
    }

    private fun getDefaultIcon() = TextureCache[iconPath, true]

    private fun getImage(): Any? {
        val ref1 = ref1 ?: return null
        val thumb = Thumbs[ref1, width, true]
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
                val frameTime = (Time.nanoTime / 1e9) % animSample.duration
                val frameIndex = (frameTime * animSample.numFrames).toFloat()
                val samples = min(
                    GFX.maxSamples, when (EngineBase.instance?.gfxSettings) {
                        GFXSettings.HIGH -> 8
                        GFXSettings.MEDIUM -> 2
                        else -> 1
                    }
                )
                val aspect = w.toFloat() / h
                if (samples > 1) {
                    val tmp =
                        FBStack["fex", w, h, 4, false, 8, DepthBufferType.INTERNAL] // msaa; probably should depend on gfx settings
                    GFXState.useFrame(0, 0, w, h, tmp, Renderers.simpleNormalRenderer) {
                        val depthMode = if (GFX.supportsClipControl) DepthMode.CLOSE
                        else DepthMode.FORWARD_CLOSE
                        GFXState.depthMode.use(depthMode) {
                            tmp.clearColor(backgroundColor, true)
                            AssetThumbnails.drawAnimatedSkeleton(animSample, frameIndex, aspect)
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
                        val depthMode = if (GFX.supportsClipControl) DepthMode.CLOSE
                        else DepthMode.FORWARD_CLOSE
                        GFXState.depthMode.use(depthMode) {
                            GFXState.currentBuffer.clearDepth()
                            AssetThumbnails.drawAnimatedSkeleton(animSample, frameIndex, aspect)
                        }
                    }
                }
                invalidateDrawing() // make sure to draw it next frame
                return
            }
        }
        val image0 = Thumbs[file, w, true]
        val image1 = image0 ?: getDefaultIcon()
        val image2 = if (image1 == null || (image1 is Texture2D && !image1.wasCreated)) whiteTexture else image1
        val rot = (image2 as? Texture2D)?.rotation
        image2.bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
        if (rot == null || rot.isNull()) {
            drawTexture(x0, y0, x1, y1, image2)
        } else if (rot.angleCW == 0 && rot.mirrorVertical && !rot.mirrorHorizontal) {
            drawTexture(x0, y1, x1, y0, image2)
        } else {
            // todo draw image without overflowing into other things
            // todo maybe use transform for that :)
            val transform = GFXx2D.transform
            transform.pushMatrix()

            rot.apply(transform)

            // transform.rotateY(45f.toRadians())

            drawTexture(x0, y0, x1, y1, image2)

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
            drawLoadingCircle(relativeTime, x0, y0, x1, y1)
        }
    }

    private fun drawVideo(x0: Int, y0: Int, x1: Int, y1: Int, meta: MediaMetadata) {
        val image = video?.getFrame()
        if (image != null) {
            val size = width - 2 * padding
            val (nw, nh) = ImageScale.scaleMin(image.width, image.height, size, size)
            drawTexture((x0 + x1 - nw) / 2, (y0 + y1 - nh) / 2, nw, nh, image)
            drawCircle(x0, y0, x1, y1)
        } else {
            drawDefaultIcon(x0, y0, x1, y1)
        }
        if (hasSpaceForVideoProgress()) {
            drawVideoProgress(x0, y0, x1, y1, meta)
        }
    }

    private fun hasSpaceForVideoProgress(): Boolean {
        return height >= 3 * titlePanel.font.sizeInt
    }

    /**
     * show video progress on playback, e.g. hh:mm:ss/hh:mm:ss
     * */
    private fun drawVideoProgress(x0: Int, y0: Int, x1: Int, y1: Int, meta: MediaMetadata) {
        val totalSeconds = (meta.duration).roundToIntOr()
        val needsHours = totalSeconds >= 3600
        val seconds = max((time % meta.duration).toInt(), 0)

        val format = if (needsHours) charHHMMSS else charMMSS
        val data = format.value
        if (needsHours) {
            setNumber(15, totalSeconds % 60, data)
            setNumber(12, (totalSeconds / 60) % 60, data)
            setNumber(9, totalSeconds / 3600, data)
            setNumber(6, seconds % 60, data)
            setNumber(3, (seconds / 60) % 60, data)
            setNumber(0, seconds / 3600, data)
        } else {
            setNumber(9, totalSeconds % 60, data)
            setNumber(6, (totalSeconds / 60) % 60, data)
            setNumber(3, seconds % 60, data)
            setNumber(0, seconds / 60, data)
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

    private fun drawThumb(
        x0: Int, y0: Int,
        x1: Int, y1: Int
    ) {
        when (importType) {
            // todo audio preview???
            // todo animation preview: draw the animated skeleton
            "Video", "Audio" -> {
                val meta = meta
                if (meta != null) {
                    if (meta.videoWidth > 0) {
                        if (time == 0.0 || !supportsPlayback) { // not playing
                            drawImageOrThumb(x0, y0, x1, y1)
                        } else {
                            drawVideo(x0, y0, x1, y1, meta)
                        }
                    } else {
                        drawDefaultIcon(x0, y0, x1, y1)
                        drawCircle(x0, y0, x1, y1)
                        if (!(time == 0.0 || !supportsPlayback) &&
                            audio != null && hasSpaceForVideoProgress()
                        ) {
                            drawVideoProgress(x0, y0, x1, y1, meta)
                        }
                    }
                } else drawDefaultIcon(x0, y0, x1, y1)
            }
            else -> drawImageOrThumb(x0, y0, x1, y1)
        }
    }

    private fun appendMetaTTT(file: FileReference, ttt: StringBuilder, meta: MediaMetadata) {
        if (meta.hasVideo) {
            ttt.append('\n').append(meta.videoWidth).append(" x ").append(meta.videoHeight)
            if (meta.videoFrameCount > 1) ttt.append(" @").append(meta.videoFPS.f1())
                .append(" fps")
        } else {
            val image = ImageCache.getImageWithoutGenerator(file)
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
            val digits = if (duration < 60) max((1.5 - log10(duration)).roundToIntOr(), 0) else 0
            ttt.append('\n').append(meta.duration.formatTime(digits))
        }
    }

    private fun appendPrefabTTT(ttt: StringBuilder, prefab: Prefab) {
        ttt.append('\n').append(prefab.clazzName)
        if (prefab.prefab != InvalidRef) {
            ttt.append(" : ").append(prefab.prefab.toLocalPath()).append('\n')
        } else ttt.append(", ")
        // if is entity, or mesh, get sample bounds
        if (prefab.clazzName == "Entity" || prefab.clazzName == "Mesh") {
            when (val sample = prefab.getSampleInstance()) {
                is Entity -> ttt.append(AABBf(sample.getBounds())).append('\n')
                is Mesh -> ttt.append(sample.getBounds()).append('\n')
            }
        }
        ttt.append(prefab.adds.size).append("+, ").append(prefab.sets.size).append("*")
    }

    open fun updateTooltip() {

        // todo add created & modified information

        // if is selected, and there are multiple files selected, show group stats
        if (isInFocus && siblings.count2 { (it.isInFocus && it is FileExplorerEntry) || it === this } > 1) {
            val files = siblings
                .filter { it.isInFocus || it === this }
                .mapNotNull { (it as? FileExplorerEntry)?.path }
                .mapNotNull { getReferenceAsync(it) }
            tooltip = "${files.count2 { it.isDirectory }} folders + ${files.count2 { !it.isDirectory }} files\n" +
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
                        val (width, height) = file.readSize()
                        file.name + "\n$width x $height"
                    }
                    else -> {
                        val ttt = StringBuilder()
                        ttt.append(file.name).append('\n')
                        ttt.append(file.length().formatFileSize())
                        val meta = getMeta(path, true)
                        if (meta != null) {
                            appendMetaTTT(file, ttt, meta)
                        } else {
                            val prefab = PrefabCache[file, true]
                            if (prefab != null) {
                                appendPrefabTTT(ttt, prefab)
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
            addEvent {
                // todo execute this asynchronously...
                updateTooltip()
            }
        }

        drawBackground(x0, y0, x1, y1)

        val font0 = titlePanel.font
        val fontSize = font0.size

        val x = x
        val y = y
        val w = width
        val h = height

        padding = h / 16

        // why not twice the padding?????
        // only once centers it...
        val remainingW = w - padding// * 2
        val remainingH = h - padding// * 2

        if (listMode) {

            // todo customize weights
            // todo add/remove columns

            val imgSize = min(width, height) - 2 * padding
            if (imgSize > 1) {

                lines = 1

                clip2Dual(
                    x0, y0, x1, y1,
                    x + padding,
                    y + padding,
                    x + padding + imgSize,
                    y + padding + imgSize,
                    ::drawThumb
                )

                // todo draw lines for separation?
                val spacing = padding
                val available = w - imgSize - spacing * (fileStatColumns.size + 3)
                val weightSum = fileStatColumns.sumOf { it.weight.toDouble() }
                val invW = available / weightSum + spacing
                val xi = x + imgSize + 3 * spacing
                val ref1s = ref1s
                var sumW = 0f
                for (i in fileStatColumns.indices) {
                    val column = fileStatColumns[i]
                    val xi0 = xi + (sumW * invW).toInt()
                    val xi1 = xi + ((sumW + column.weight) * invW).toInt()
                    val text = column.type.getValue(ref1s)
                    val alignment = column.type.alignment
                    drawTextCharByChar(
                        xi0 + alignment.getOffset(xi1 - xi0, 0),
                        y + h, monospaceFont, text,
                        titlePanel.textColor,
                        titlePanel.backgroundColor,
                        -1, -1,
                        alignment, AxisAlignment.MAX,
                        true
                    )
                    sumW += column.weight
                }
            }
        } else {

            val extraHeight = h - w
            lines = if (showTitle) max(ceil(extraHeight / fontSize).toInt(), 1) else 0

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
                ::drawTitle
            )
        }
    }

    private fun drawTitle(x0: Int, y0: Int, x1: Int, y1: Int) {
        titlePanel.width = x1 - x0
        titlePanel.minW = x1 - x0
        titlePanel.calculateSize(x1 - x0, y1 - y0)
        titlePanel.backgroundColor = backgroundColor and 0xffffff
        val deltaX = ((x1 - x0) - titlePanel.minW) / 2 // centering the text
        titlePanel.x = x0 + max(0, deltaX)
        titlePanel.y = max(y0, (y0 + y1 - titlePanel.minH) / 2)
        titlePanel.width = x1 - x0
        titlePanel.height = titlePanel.minH
        // the title might overlap with the image,
        // and this can cause badly-readable text,
        // so use better text rendering, if possible
        val pbb = pushBetterBlending(true)
        titlePanel.drawText()
        popBetterBlending(pbb)
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
                EngineBase.dragged = Draggable(stringContent, "File", original, title, style)
            }
            "Enter" -> {
                if (explorer != null) {
                    val file = getReferenceOrTimeout(path)
                    if (explorer.canSensiblyEnter(file)) {
                        explorer.switchTo(file)
                    } else return false
                } else return false
            }
            "Rename" -> rename(windowStack, explorer, findInFocusReferences())
            "OpenInExplorer" -> openInExplorer(findInFocusReferences())
            "OpenInStandardProgram" -> openInStandardProgram(findInFocusReferences())
            "EditInStandardProgram" -> editInStandardProgram(findInFocusReferences())
            "Delete" -> askToDeleteFiles(windowStack, explorer, findInFocusReferences())
            "OpenOptions" -> explorer?.openOptions(findInFocusReferences()) ?: return false
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun findInFocusReferences(): List<FileReference> {
        val hits = ArrayList<FileReference>()
        hits.add(getReferenceOrTimeout(path))
        for (sibling in siblings) {
            if (sibling !== this && sibling is FileExplorerEntry && sibling.isInFocus) {
                hits.add(getReferenceOrTimeout(sibling.path))
            }
        }
        return hits
    }

    private fun tryEntering(explorer: FileExplorer) {
        val file = getReferenceOrTimeout(path)
        if (explorer.canSensiblyEnter(file)) {
            LOGGER.info("Can enter ${file.name}? Yes!")
            explorer.switchTo(file)
        } else {
            LOGGER.info("Can enter ${file.name}? No :/")
            explorer.onDoubleClick(file)
        }
    }

    override fun onEnterKey(x: Float, y: Float) {
        if (explorer != null) {
            tryEntering(explorer)
        } else super.onEnterKey(x, y)
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        if (button == Key.BUTTON_LEFT && explorer != null) {
            tryEntering(explorer)
        } else super.onDoubleClick(x, y, button)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        askToDeleteFiles(windowStack, explorer, findInFocusReferences())
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        val files = if (isInFocus) {// multiple files maybe
            siblings.filterIsInstance<FileExplorerEntry>()
                .filter { it.isInFocus }
                .map { getReferenceOrTimeout(it.path) }
        } else listOf(getReferenceOrTimeout(path))
        Clipboard.copyFiles(files)
        return null
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val thisFile = ref1s
        val canPasteInto = thisFile.isDirectory
        val canReplace = files.size == 1 && !canPasteInto
        when {
            canReplace -> explorer!!.pasteFiles(
                files, explorer.folder, listOf(
                    MenuOption(NameDesc("Replace")) {
                        thread(name = "replacing file") {
                            val progress = GFX.someWindow.addProgressBar(
                                "Replacing",
                                "Bytes",
                                files.sumOf { it.length() }.toDouble()
                            )
                            thisFile.writeFile(
                                files.first(),
                                { delta, _ -> progress.progress += delta },
                                { it?.printStackTrace() })
                            thisFile.invalidate()
                            invalidateDrawing()
                            progress.finish()
                        }
                    },
                )
            )
            canPasteInto -> explorer!!.pasteFiles(files, thisFile)
            else -> super.onPasteFiles(x, y, files)
        }
    }

    override fun getMultiSelectablePanel() = this

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        println("${Strings.spaces(tabDepth * 2 + 2)} ${getReferenceOrTimeout(path).name}")
    }

    companion object {

        private var lastVideoOrAudioRef: FileReference? = null
        private var audio: AudioFileStreamOpenAL? = null
        private var video: VideoStream? = null
        val hoverPlaybackDelay = 0.5

        fun startAudioPlayback(file: FileReference, meta: MediaMetadata) {
            val audio = AudioFileStreamOpenAL(
                file, LoopingState.PLAY_LOOP,
                -hoverPlaybackDelay, true, meta, 1.0,
                left = true, center = false, right = true
            )
            audio.start()
            this.audio = audio
            lastVideoOrAudioRef = file
        }

        fun startVideoPlayback(file: FileReference, meta: MediaMetadata, maxSize: Int) {
            val video = VideoStream(file, meta, false, LoopingState.PLAY_LOOP, meta.videoFPS, maxSize)
            video.start(-hoverPlaybackDelay)
            this.video = video
            lastVideoOrAudioRef = file
        }

        fun stopPlayback(file: FileReference) {
            if (lastVideoOrAudioRef != file) return
            stopAnyPlayback()
        }

        fun stopAnyPlayback() {
            audio?.stop()
            video?.destroy()
            audio = null
            video = null
            lastVideoOrAudioRef = null
        }

        @JvmField
        var showFileExtensions = false

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FileExplorerEntry::class)

        @JvmStatic
        private val charHHMMSS = ComparableStringBuilder("hh:mm:ss/hh:mm:ss")

        @JvmStatic
        private val charMMSS = ComparableStringBuilder("mm:ss/mm:ss")

        @JvmStatic
        var fileStatColumns = arrayListOf(
            FileStatColumn(FileStatType.FILE_NAME, 3f),
            FileStatColumn(FileStatType.EXTENSION, 1f),
            FileStatColumn(FileStatType.FILE_SIZE, 1f),
            // FileStatColumn(FileStatType.SIGNATURE, 1f),
            FileStatColumn(FileStatType.CREATED, 2f),
            FileStatColumn(FileStatType.MODIFIED, 2f),
        )

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
        fun drawLoadingCircle(relativeTime: Float, x0: Int, y0: Int, x1: Int, y1: Int) {
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

        fun rename(windowStack: WindowStack, explorer: FileExplorer?, files: List<FileReference>) {
            val file = files.firstOrNull() ?: return
            val title = NameDesc("Rename To...", "", "ui.file.rename2")
            askName(windowStack, title, file.name, NameDesc("Rename"), { newName ->
                when (newName.toAllowedFilename()) {
                    newName -> 0x77ff77
                    null -> 0xff7777
                    else -> 0xffff77
                }.withAlpha(255)
            }) { newName ->
                renameTo(windowStack, explorer, file, newName) {
                    if (files.size > 1) { // rename multiple files one after the other for now
                        rename(windowStack, explorer, files.subList(1, files.size))
                    }
                }
            }
        }

        fun renameTo(
            windowStack: WindowStack, explorer: FileExplorer?, file: FileReference, newName: String,
            callback: () -> Unit
        ) {
            val allowed = newName.toAllowedFilename()
            if (allowed != null) {
                val dst = file.getParent().getChild(allowed)
                if (dst.exists && !allowed.equals(file.name, true)) {
                    ask(windowStack, NameDesc("Override existing file?", "", "ui.file.override"), {
                        file.renameTo(dst)
                        explorer?.invalidate()
                    }, callback)
                } else {
                    file.renameTo(dst)
                    explorer?.invalidate()
                    callback()
                }
            } else callback()
        }

        fun askToDeleteFiles(windowStack: WindowStack, explorer: FileExplorer?, files: List<FileReference>) {
            // todo in Rem's Engine, we first should check, whether there are prefabs, which depend on this file
            //  - same for renaming
            // ask, then delete all (or cancel)
            val title = if (files.size == 1) {
                NameDesc(
                    "Delete this file? (${files.first().length().formatFileSize()})",
                    "", "ui.file.delete.ask.one"
                )
            } else {
                NameDesc(
                    "Delete these files? (${files.size}x, ${
                        files.sumOf { it.length() }.formatFileSize()
                    } total)", "", "ui.file.delete.ask.many"
                )
            }
            val moveToTrash = MenuOption(
                NameDesc(
                    "Yes, move to trash",
                    "Move the files to the trash. Better save than sorry.",
                    "ui.file.delete.yes"
                )
            ) {
                moveToTrash(files)
                explorer?.invalidate()
            }
            val deletePermanently = MenuOption(
                NameDesc(
                    "Yes, permanently",
                    "Deletes all selected files; forever; files cannot be recovered",
                    "ui.file.delete.many.permanently"
                )
            ) {
                for (i in files.indices) files[i].deleteRecursively()
                explorer?.invalidate()
            }
            openMenu(windowStack, title, listOf(deletePermanently, moveToTrash, dontDelete))
        }
    }
}