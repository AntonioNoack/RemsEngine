package me.anno.objects

import me.anno.audio.AudioManager
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.TextureLib
import me.anno.gpu.TextureLib.colorShowTexture
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import me.anno.image.svg.SVGMesh
import me.anno.input.Input
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.objects.cache.StaticFloatBufferData
import me.anno.objects.cache.VideoData.Companion.framesPerContainer
import me.anno.objects.modes.EditorFPS
import me.anno.objects.modes.LoopingState
import me.anno.objects.modes.UVProjection
import me.anno.studio.Scene
import me.anno.studio.Studio
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.Panel
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.hasValidName
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.*
import me.anno.utils.test.ImageSequenceMeta
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import me.anno.video.MissingFrameException
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.File
import kotlin.math.*

// idea: hovering needs to be used to predict when the user steps forward in time
// -> no, that's too taxing; we'd need to pre-render a smaller version
// todo pre-render small version for scrubbing? can we playback a small version using ffmpeg with no storage overhead?

// todo feature tracking on videos as anchors, e.g. for easy blurry signs, or text above heads (marker on head/eyes)

// todo image sequences using % in the name and number parsing...

/**
 * Images, Cubemaps, Videos, Audios, joint into one
 * */
class Video(file: File = File(""), parent: Transform? = null) : Audio(file, parent) {

    var tiling = AnimatedProperty.tiling()
    var uvProjection = UVProjection.Planar
    var clampMode = ClampMode.MIRRORED_REPEAT

    var startTime = 0.0
    var endTime = 100.0

    var filtering = DefaultConfig["default.video.nearest", FilteringMode.LINEAR]

    var videoScale = DefaultConfig["default.video.scale", 6]

    var lastFile: File? = null
    var imageSequenceMeta: ImageSequenceMeta? = null
    var type = VideoType.AUDIO

    var zoomLevel = 0

    var editorVideoFPS = EditorFPS.F10

    val cgOffset = AnimatedProperty.vec3()
    val cgSlope = AnimatedProperty.color(Vector4f(1f, 1f, 1f, 1f))
    val cgPower = AnimatedProperty.color(Vector4f(1f, 1f, 1f, 1f))
    val cgSaturation = AnimatedProperty.float(1f) // only allow +? only 01?

    override fun isVisible(localTime: Double): Boolean {
        return localTime + startTime >= 0.0 && (isLooping != LoopingState.PLAY_ONCE || localTime < endTime)
    }

    fun calculateSize(matrix: Matrix4f, w: Int, h: Int): Int? {

        /**
        gl_Position = transform * vec4(betterUV, 0.0, 1.0);
         * */

        // clamp points to edges of screens, if outside, clamp on the z edges
        // -> just generally clamp the polygon...
        // the most extreme cases should be on a quad always, because it's linear
        // -> clamp all axis separately

        val avgSize =
            if (w * Studio.targetHeight > h * Studio.targetWidth) w.toFloat() * Studio.targetHeight / Studio.targetWidth else h.toFloat()
        val sx = w / avgSize
        val sy = h / avgSize

        fun getPoint(x: Float, y: Float): Vector4f {
            return matrix.transformProject(Vector4f(x * sx, y * sy, 0f, 1f))
        }

        val v00 = getPoint(-1f, -1f)
        val v01 = getPoint(-1f, +1f)
        val v10 = getPoint(+1f, -1f)
        val v11 = getPoint(+1f, +1f)

        // check these points by drawing them on the screen
        // they were correct as of 12th July 2020, 9:18 am
        /*
        for(pt in listOf(v00, v01, v10, v11)){
            val x = GFX.windowX + (+pt.x * 0.5f + 0.5f) * GFX.windowWidth
            val y = GFX.windowY + (-pt.y * 0.5f + 0.5f) * GFX.windowHeight
            GFX.drawRect(x.toInt()-2, y.toInt()-2, 5, 5, 0xff0000 or black)
        }
        */

        val zRange = Clipping.getZ(v00, v01, v10, v11) ?: return null

        // calculate the depth based on the z value
        fun unmapZ(z: Float): Float {
            val n = Scene.nearZ
            val f = Scene.farZ
            val top = 2 * f * n
            val bottom = (z * (f - n) - (f + n))
            return -top / bottom // the usual z is negative -> invert it :)
        }

        val closestDistance = min(unmapZ(zRange.first), unmapZ(zRange.second))

        // calculate the zoom level based on the distance
        val pixelZoom = GFX.windowHeight * 1f / (closestDistance * h) // e.g. 0.1 for a window far away
        val availableRedundancy = 1f / pixelZoom // 0.1 zoom means that we only need every 10th pixel

        return max(1, availableRedundancy.toInt())

    }

    fun getCacheableZoomLevel(level: Int): Int {
        return when {
            level < 1 -> 1
            level <= 6 || level == 8 || level == 12 || level == 16 -> level
            else -> {
                val stepsIn2 = 3
                val log = log2(level.toFloat())
                val roundedLog = round(stepsIn2 * log) / stepsIn2
                pow(2f, roundedLog).toInt()
            }
        }
    }

    fun drawImageSequence(meta: ImageSequenceMeta, stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        var wasDrawn = false

        if (meta.isValid) {

            val sourceDuration = meta.duration

            if (startTime >= sourceDuration) startTime = sourceDuration
            if (endTime >= sourceDuration) endTime = sourceDuration

            if (time + startTime >= 0.0 && (isLooping != LoopingState.PLAY_ONCE || time <= endTime)) {

                // draw the current texture
                val duration = endTime - startTime
                val localTime = startTime + isLooping[time, duration]

                val frame = Cache.getImage(meta.getImage(localTime), 500L, true)
                if (frame != null) {
                    GFX.draw3DVideo(
                        this, time,
                        stack, frame, color, this@Video.filtering, this@Video.clampMode, tiling[time], uvProjection
                    )
                    wasDrawn = true
                } else {
                    if (GFX.isFinalRendering) {
                        throw MissingFrameException(file)
                    }
                }

            } else wasDrawn = true

        }

        if (!wasDrawn) {
            GFX.draw3D(
                stack, colorShowTexture, 16, 9,
                Vector4f(0.5f, 0.5f, 0.5f, 1f).mul(color),
                FilteringMode.NEAREST, ClampMode.REPEAT, tiling16x9, uvProjection
            )
        }

    }

    fun drawVideoFrames(meta: FFMPEGMetadata, stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        // todo automatic spherical size estimation??
        val zoomLevel = if (videoScale < 1) {
            // calculate reasonable zoom level from canvas size
            if (uvProjection.doScale) {
                val rawZoomLevel = calculateSize(stack, meta.videoWidth, meta.videoHeight) ?: return
                getCacheableZoomLevel(rawZoomLevel)
            } else 1
        } else videoScale
        this.zoomLevel = zoomLevel

        var wasDrawn = false

        val sourceFPS = meta.videoFPS
        val sourceDuration = meta.videoDuration

        if (startTime >= sourceDuration) startTime = sourceDuration
        if (endTime >= sourceDuration) endTime = sourceDuration

        if (sourceFPS > 0.0) {
            if (time + startTime >= 0.0 && (isLooping != LoopingState.PLAY_ONCE || time <= endTime)) {

                // use full fps when rendering to correctly render at max fps with time dilation
                // issues arise, when multiple frames should be interpolated together into one
                // at this time, we chose the center frame only.
                val videoFPS = if (GFX.isFinalRendering) sourceFPS else min(sourceFPS, editorVideoFPS.dValue)

                val frameCount = max(1, (sourceDuration * videoFPS).roundToInt())

                // draw the current texture
                val duration = endTime - startTime
                val localTime = startTime + isLooping[time, duration]
                val frameIndex = (localTime * videoFPS).toInt() % frameCount

                val frame = Cache.getVideoFrame(
                    file, zoomLevel, frameIndex,
                    framesPerContainer, videoFPS, videoFrameTimeout, true
                )

                if (frame != null && frame.isLoaded) {
                    GFX.draw3DVideo(
                        this, time,
                        stack, frame, color, this@Video.filtering, this@Video.clampMode, tiling[time], uvProjection
                    )
                    wasDrawn = true
                } else {
                    if (GFX.isFinalRendering) {
                        throw MissingFrameException(file)
                    }
                }

                // stack.scale(0.1f)
                // GFX.draw3D(stack, FontManager.getString("Verdana",15f, "$frameIndex/$fps/$duration/$frameCount")!!, Vector4f(1f,1f,1f,1f), 0f)
                // stack.scale(10f)

            } else wasDrawn = true
        }

        if (!wasDrawn) {
            GFX.draw3D(
                stack, colorShowTexture, 16, 9,
                Vector4f(0.5f, 0.5f, 0.5f, 1f).mul(color),
                FilteringMode.NEAREST, ClampMode.REPEAT, tiling16x9, uvProjection
            )
        }
    }

    fun drawImageFrames(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        val name = file.name
        when {
            name.endsWith("svg", true) -> {
                val bufferData = Cache.getEntry(file.absolutePath, "svg", 0, imageTimeout, true) {
                    val svg = SVGMesh()
                    svg.parse(XMLReader.parse(file.inputStream().buffered()) as XMLElement)
                    StaticFloatBufferData(svg.buffer!!)
                } as? StaticFloatBufferData
                if (bufferData == null && GFX.isFinalRendering) throw MissingFrameException(file)
                if (bufferData != null) {
                    // todo apply tiling for svgs...
                    GFX.draw3DSVG(
                        stack,
                        bufferData.buffer,
                        TextureLib.whiteTexture,
                        color,
                        FilteringMode.NEAREST,
                        ClampMode.CLAMP
                    )
                }
            }
            name.endsWith("webp", true) -> {
                val tiling = tiling[time]
                // calculate required scale? no, without animation, we don't need to scale it down ;)
                val texture = Cache.getVideoFrame(file, 1, 0, 1, 1.0, imageTimeout, true)
                if ((texture == null || !texture.isLoaded) && GFX.isFinalRendering) throw MissingFrameException(file)
                if (texture?.isLoaded == true) GFX.draw3DVideo(
                    this,
                    time,
                    stack,
                    texture,
                    color,
                    this@Video.filtering,
                    this@Video.clampMode,
                    tiling,
                    uvProjection
                )
            }
            else -> {// some image
                val tiling = tiling[time]
                val texture = Cache.getImage(file, imageTimeout, true)
                if (texture == null && GFX.isFinalRendering) throw MissingFrameException(file)
                texture?.apply {
                    rotation?.apply(stack)
                    GFX.draw3DVideo(
                        this@Video,
                        time,
                        stack,
                        texture,
                        color,
                        this@Video.filtering,
                        this@Video.clampMode,
                        tiling,
                        uvProjection
                    )
                }
            }
        }
    }

    fun drawSpeakers(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        if (GFX.isFinalRendering) return
        color.w = clamp(color.w * 0.5f * abs(amplitude[time]), 0f, 1f)
        if (is3D) {
            val r = 0.85f
            stack.translate(r, 0f, 0f)
            Grid.drawBuffer(stack, color, speakerModel)
            stack.translate(-2 * r, 0f, 0f)
            Grid.drawBuffer(stack, color, speakerModel)
        } else {
            // mark the speaker with yellow,
            // and let it face upwards (+y) to symbolize, that it's global
            color.z *= 0.8f // yellow
            stack.rotate(-1.5708f, xAxis)
            Grid.drawBuffer(stack, color, speakerModel)
        }
    }

    enum class VideoType {
        IMAGE,
        VIDEO,
        AUDIO,
        IMAGE_SEQUENCE
    }

    override fun claimLocalResources(lTime0: Double, lTime1: Double) {

        val minT = min(lTime0, lTime1)
        val maxT = max(lTime0, lTime1)

        when (val type = type) {
            VideoType.VIDEO -> {

                val meta = getMeta(file, true)
                if (meta != null) {

                    val sourceFPS = meta.videoFPS
                    val sourceDuration = meta.videoDuration

                    if (sourceFPS > 0.0) {
                        if (maxT + startTime >= 0.0 && (isLooping != LoopingState.PLAY_ONCE || minT < endTime)) {

                            // use full fps when rendering to correctly render at max fps with time dilation
                            // issues arise, when multiple frames should be interpolated together into one
                            // at this time, we chose the center frame only.
                            val videoFPS = if (GFX.isFinalRendering) sourceFPS else min(sourceFPS, editorVideoFPS.dValue)

                            val frameCount = max(1, (sourceDuration * videoFPS).roundToInt())

                            // draw the current texture
                            val duration = endTime - startTime
                            val localTime0 = startTime + isLooping[lTime0, duration]
                            val localTime1 = startTime + isLooping[lTime1, duration]
                            val frameIndex0 = (localTime0 * videoFPS).toInt() % frameCount
                            val frameIndex1 = (localTime1 * videoFPS).toInt() % frameCount

                            if (frameIndex1 >= frameIndex0) {
                                for (frameIndex in frameIndex0..frameIndex1) {
                                    Cache.getVideoFrame(
                                        file, zoomLevel, frameIndex, frameCount,
                                        videoFPS, videoFrameTimeout, true
                                    )
                                }
                            }
                        }
                    }
                }
            }
            VideoType.IMAGE_SEQUENCE -> {

                val meta = imageSequenceMeta ?: return
                if (meta.isValid) {

                    if (maxT + startTime >= 0.0 && (isLooping != LoopingState.PLAY_ONCE || minT <= endTime)) {

                        // draw the current texture
                        val duration = endTime - startTime
                        val localTime0 = startTime + isLooping[minT, duration]
                        val localTime1 = startTime + isLooping[maxT, duration]

                        val index0 = meta.getIndex(localTime0)
                        val index1 = meta.getIndex(localTime1)

                        if (index1 >= index0) {
                            for (i in index0..index1) {
                                Cache.getImage(meta.getImage(i), 500L, true)
                            }
                        } else {
                            for (i in index1 until meta.matches.size) {
                                Cache.getImage(meta.getImage(i), 500L, true)
                            }
                            for (i in 0 until index0) {
                                Cache.getImage(meta.getImage(i), 500L, true)
                            }
                        }

                    }
                }
            }
            // nothing to do for image and audio
            VideoType.IMAGE -> {
            }
            VideoType.AUDIO -> {
            }
            else -> throw RuntimeException("todo implement resource loading for $type")
        }
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val file = file
        if (file.hasValidName()) {

            if (file !== lastFile) {
                lastFile = file
                type = if (file.name.contains(imageSequenceIdentifier)) {
                    VideoType.IMAGE_SEQUENCE
                } else {
                    when (file.extension.getImportType()) {
                        "Video" -> VideoType.VIDEO
                        "Audio" -> VideoType.AUDIO
                        else -> VideoType.IMAGE
                    }
                }
                // async in the future?
                if (type == VideoType.IMAGE_SEQUENCE) {
                    imageSequenceMeta = ImageSequenceMeta(file)
                }
            }

            when (type) {
                VideoType.VIDEO -> {
                    val meta = meta
                    if (meta?.hasVideo == true) {
                        drawVideoFrames(meta, stack, time, color)
                    }
                    // very intrusive :/
                    /*if(meta?.hasAudio == true){
                        drawSpeakers(stack, time, color)
                    }*/
                }
                VideoType.IMAGE_SEQUENCE -> {
                    val meta = imageSequenceMeta!!
                    drawImageSequence(meta, stack, time, color)
                }
                VideoType.IMAGE -> drawImageFrames(stack, time, color)
                VideoType.AUDIO -> drawSpeakers(stack, time, color)
                else -> throw RuntimeException("$type needs visualization")
            }

        } else drawSpeakers(stack, time, color)
        // super.onDraw(stack, time, color) // draw dot

    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)

        // to hide elements, which are not usable / have no effect
        val videoPanels = ArrayList<Panel>()
        val imagePanels = ArrayList<Panel>()
        val audioPanels = ArrayList<Panel>()

        fun vid(panel: Panel): Panel {
            videoPanels += panel
            return panel
        }

        fun img(panel: Panel): Panel {
            imagePanels += panel
            return panel
        }

        fun aud(panel: Panel): Panel {
            audioPanels += panel
            return panel
        }

        list += VI("File Location", "Source file of this video", null, file, style) { file = it }

        val uvMap = getGroup("Texture", "uvs")
        uvMap += img(VI("Tiling", "(tile count x, tile count y, offset x, offset y)", tiling, style))
        uvMap += img(VI("UV-Projection", "Can be used for 360°-Videos", null, uvProjection, style) {
            uvProjection = it
        })
        uvMap += img(VI("Filtering", "Pixelated look?", null, filtering, style) { filtering = it })
        uvMap += img(VI("Clamping", "For tiled images", null, clampMode, style) { clampMode = it })

        val time = getGroup("Time", "time")
        time += VI("Looping Type", "Whether to repeat the song/video", null, isLooping, style) {
            isLooping = it
            AudioManager.requestUpdate()
        }
        time += vid(VI("Video Start", "Timestamp in seconds of the first frames drawn", null, startTime, style) {
            startTime = it
        })
        time += vid(VI("Video End", "Timestamp in seconds of the last frames drawn", null, endTime, style) {
            endTime = it
        })

        val quality = getGroup("Quality", "quality")
        val videoScales = videoScaleNames.entries.sortedBy { it.value }
        quality += vid(EnumInput(
            "Video Scale", true,
            videoScaleNames.reverse[videoScale] ?: "Auto",
            videoScales.map { it.key }, style
        )
            .setChangeListener { _, index, _ -> videoScale = videoScales[index].value }
            .setIsSelectedListener { show(null) }
            .setTooltip("Full resolution isn't always required. Define it yourself, or set it to automatic."))
        quality += vid(EnumInput(
            "Preview FPS", true, editorVideoFPS.displayName,
            EditorFPS.values().map { it.displayName }, style
        )
            .setChangeListener { _, index, _ ->
                editorVideoFPS = EditorFPS.values()[index]
            }
            .setIsSelectedListener { show(null) }
            .setTooltip("Smoother preview, heavier calculation")
        )

        val color = getGroup("Color", "color")
        color += img(VI("Power", "Color Grading, ASC CDL", cgPower, style))
        color += img(
            VI(
                "Saturation",
                "Color Grading, 0 = gray scale, 1 = normal, -1 = inverted colors",
                cgSaturation,
                style
            )
        )
        color += img(VI("Slope", "Color Grading, Intensity", cgSlope, style))
        color += img(VI("Offset", "Color Grading, ASC CDL", cgOffset, style))

        val audio = getGroup("Audio", "audio")
        /*if(meta?.hasAudio == true){
            list += AudioLinePanel(meta, this, style)
        }*/
        audio += aud(VI("Amplitude", "How loud it is", amplitude, style))
        audio += aud(VI("Is 3D Sound", "Sound becomes directional", null, is3D, style) {
            is3D = it
            AudioManager.requestUpdate()
        })
        val audioFX = getGroup("Audio Effects", "audio-fx")
        audioFX += aud(VI("Echo Delay", "", echoDelay, style))
        audioFX += aud(VI("Echo Multiplier", "", echoMultiplier, style))

        val playbackTitles = "Test Playback" to "Stop Playback"
        fun getPlaybackTitle(invert: Boolean) =
            if ((component == null) != invert) playbackTitles.first else playbackTitles.second

        val playbackButton = ButtonPanel(getPlaybackTitle(false), style)
        audio += aud(playbackButton
            .setSimpleClickListener {
                if (Studio.isPaused) {
                    playbackButton.text = getPlaybackTitle(true)
                    if (component == null) {
                        GFX.addAudioTask(5) {
                            val audio = Video(file, null)
                            audio.startPlayback(0.0, 1.0, Studio.nullCamera)
                            component = audio.component
                        }
                    } else GFX.addAudioTask(1) { stopPlayback() }
                } else Studio.warn("Separated playback is only available with paused editor")
            }
            .setTooltip("Listen to the audio separated from the rest"))

        list += object : SpyPanel(style) {
            var lastState = -1
            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                val isValid = file.hasValidName()
                val hasAudio = isValid && meta?.hasAudio == true
                val hasImage = isValid && type != VideoType.AUDIO
                val hasVideo = isValid && when (type) {
                    VideoType.IMAGE_SEQUENCE, VideoType.VIDEO -> true
                    else -> false
                } && meta?.hasVideo == true
                val state = hasAudio.toInt(1) + hasImage.toInt(2) + hasVideo.toInt(4)
                if (state != lastState) {
                    lastState = state
                    audioPanels.forEach { it.visibility = if (hasAudio) Visibility.VISIBLE else Visibility.GONE }
                    videoPanels.forEach { it.visibility = if (hasVideo) Visibility.VISIBLE else Visibility.GONE }
                    imagePanels.forEach { it.visibility = if (hasImage) Visibility.VISIBLE else Visibility.GONE }
                    Input.invalidateLayout()
                }
            }
        }

    }

    override fun getClassName(): String = "Video"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("startTime", startTime)
        writer.writeDouble("endTime", endTime)
        writer.writeObject(this, "tiling", tiling)
        writer.writeInt("filtering", filtering.id, true)
        writer.writeInt("clamping", clampMode.id, true)
        writer.writeInt("videoScale", videoScale)
        writer.writeObject(this, "cgSaturation", cgSaturation)
        writer.writeObject(this, "cgOffset", cgOffset)
        writer.writeObject(this, "cgSlope", cgSlope)
        writer.writeObject(this, "cgPower", cgPower)
        writer.writeInt("uvProjection", uvProjection.id, true)
        writer.writeInt("editorVideoFPS", editorVideoFPS.value, true)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "tiling" -> tiling.copyFrom(value)
            "cgSaturation" -> cgSaturation.copyFrom(value)
            "cgOffset" -> cgOffset.copyFrom(value)
            "cgSlope" -> cgSlope.copyFrom(value)
            "cgPower" -> cgPower.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "videoScale" -> videoScale = value
            "filtering" -> filtering = filtering.find(value)
            "clamping" -> clampMode = ClampMode.values().firstOrNull { it.id == value } ?: clampMode
            "uvProjection" -> uvProjection = UVProjection.values().firstOrNull { it.id == value } ?: uvProjection
            "editorVideoFPS" -> editorVideoFPS = EditorFPS.values().firstOrNull { it.value == value } ?: editorVideoFPS
            else -> super.readInt(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "path", "file" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "startTime" -> startTime = value
            "endTime" -> endTime = value
            else -> super.readDouble(name, value)
        }
    }

    companion object {

        val imageSequenceIdentifier = DefaultConfig["video.imageSequence.identifier", "%"]

        val videoScaleNames = BiMap<String, Int>(10)

        init {
            videoScaleNames["Auto"] = 0
            videoScaleNames["Original"] = 1
            videoScaleNames["1/2"] = 2
            videoScaleNames["1/3"] = 3
            videoScaleNames["1/4"] = 4
            videoScaleNames["1/6"] = 6
            videoScaleNames["1/8"] = 8
            videoScaleNames["1/12"] = 12
            videoScaleNames["1/16"] = 16
        }

        val videoFrameTimeout = 500L
        val tiling16x9 = Vector4f(8f, 4.5f, 0f, 0f)

        val imageTimeout = 5000L

        val cubemapModel = StaticFloatBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), 4 * 6)
        val speakerModel: StaticFloatBuffer

        init {

            fun put(v0: Vector3f, dx: Vector3f, dy: Vector3f, x: Float, y: Float, u: Int, v: Int) {
                val pos = v0 + dx * x + dy * y
                cubemapModel.put(pos.x, pos.y, pos.z, u / 4f, v / 3f)
            }

            fun addFace(u: Int, v: Int, v0: Vector3f, dx: Vector3f, dy: Vector3f) {
                put(v0, dx, dy, -1f, -1f, u + 1, v)
                put(v0, dx, dy, -1f, +1f, u + 1, v + 1)
                put(v0, dx, dy, +1f, +1f, u, v + 1)
                put(v0, dx, dy, +1f, -1f, u, v)
            }

            val mxAxis = Vector3f(-1f, 0f, 0f)
            val myAxis = Vector3f(0f, -1f, 0f)
            val mzAxis = Vector3f(0f, 0f, -1f)

            addFace(1, 1, mzAxis, mxAxis, yAxis) // center, front
            addFace(0, 1, mxAxis, zAxis, yAxis) // left, left
            addFace(2, 1, xAxis, mzAxis, yAxis) // right, right
            addFace(3, 1, zAxis, xAxis, yAxis) // 2x right, back
            addFace(1, 0, myAxis, mxAxis, mzAxis) // top
            addFace(1, 2, yAxis, mxAxis, zAxis) // bottom

            cubemapModel.quads()

            val speakerEdges = 64
            speakerModel = StaticFloatBuffer(
                listOf(
                    Attribute("attr0", 3),
                    Attribute("attr1", 2)
                ), speakerEdges * 3 * 2 + 4 * 2 * 2
            )

            fun addLine(r0: Float, d0: Float, r1: Float, d1: Float, dx: Int, dy: Int) {
                speakerModel.put(r0 * dx, r0 * dy, d0, 0f, 0f)
                speakerModel.put(r1 * dx, r1 * dy, d1, 0f, 0f)
            }

            fun addRing(radius: Float, depth: Float, edges: Int) {
                val dr = (Math.PI * 2 / edges).toFloat()
                fun putPoint(i: Int) {
                    val angle1 = dr * i
                    speakerModel.put(sin(angle1) * radius, cos(angle1) * radius, depth, 0f, 0f)
                }
                putPoint(0)
                for (i in 1 until edges) {
                    putPoint(i)
                    putPoint(i)
                }
                putPoint(0)
            }

            val scale = 0.5f

            addRing(0.45f * scale, 0.02f * scale, speakerEdges)
            addRing(0.50f * scale, 0.01f * scale, speakerEdges)
            addRing(0.80f * scale, 0.30f * scale, speakerEdges)

            val dx = listOf(0, 0, 1, -1)
            val dy = listOf(1, -1, 0, 0)
            for (i in 0 until 4) {
                addLine(0.45f * scale, 0.02f * scale, 0.50f * scale, 0.01f * scale, dx[i], dy[i])
                addLine(0.50f * scale, 0.01f * scale, 0.80f * scale, 0.30f * scale, dx[i], dy[i])
            }

            speakerModel.lines()

        }

    }

}