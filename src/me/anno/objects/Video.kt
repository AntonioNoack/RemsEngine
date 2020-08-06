package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.studio.Scene
import me.anno.studio.Studio
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.*
import me.anno.ui.style.Style
import me.anno.utils.BiMap
import me.anno.utils.Clipping
import me.anno.utils.plus
import me.anno.utils.pow
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

// todo get information about full and relative frames, so we get optimal scrubbing performance :)


class Video(file: File = File(""), parent: Transform? = null): Audio(file, parent){

    var tiling = AnimatedProperty.tiling()

    var startTime = 0.0
    var endTime = 100.0

    var nearestFiltering = DefaultConfig["default.video.nearest", false]

    var videoScale = 1

    fun calculateSize(matrix: Matrix4f, isBillboard: Float, w: Int, h: Int): Int? {



        // todo we would need to apply align-with-camera, too

        /**
            vec4 billboardTransform(vec2 betterUV, float z){
                vec4 pos0 = transform * vec4(0.0,0.0,0.0,1.0);
                pos0.xy += betterUV * billboardSize;
                pos0.z += z;
                return pos0;
            }
            vec4 transform3D(vec2 betterUV){
                return transform * vec4(betterUV, 0.0, 1.0);
            }
            vec4 billboard = billboardTransform(betterUV, 0.0);
            vec4 in3D = transform3D(betterUV);
            gl_Position = mix(in3D, billboard, isBillboard);
         * */

        // clamp points to edges of screens, if outside, clamp on the z edges
        // -> just generally clamp the polygon...
        // the most extreme cases should be on a quad always, because it's linear
        // -> clamp all axis separately

        val avgSize = if(w * Studio.targetHeight > h * Studio.targetWidth) w.toFloat() * Studio.targetHeight / Studio.targetWidth else h.toFloat()
        val sx = w / avgSize
        val sy = h / avgSize

        // we need the w value for the billboard size :)
        val billboardCenter = if(isBillboard > 0f) matrix.transform(Vector4f(0f, 0f, 0f, 1f)) else null
        val billboardW = billboardCenter?.w ?: 1f
        billboardCenter?.mul(1f/billboardW)

        // billboard size
        val scale = matrix.transformDirection(Vector3f(1f, 1f, 1f)).length()
        val bbx = scale * GFX.windowHeight / GFX.windowWidth * w/h / billboardW
        val bby = scale / billboardW

        fun getPoint(x: Float, y: Float): Vector4f {
            val billboardPoint = if(isBillboard > 0f) billboardCenter!! + Vector4f(x*bbx, y*bby, 0f, 0f) else null
            val v00 = if(isBillboard < 1f) matrix.transformProject(Vector4f(x*sx, y*sy, 0f, 1f)) else null
            if(isBillboard <= 0f) return v00!!
            if(isBillboard >= 1f) return billboardPoint!!
            return v00!!.lerp(billboardPoint!!, isBillboard)
        }

        val v00 = getPoint(-1f, -1f)
        val v01 = getPoint(-1f, +1f)
        val v10 = getPoint(+1f, -1f)
        val v11 = getPoint(+1f, +1f)

        // check these points by drawing them on the screen
        // they were correct as of 12th July 2020, 9:18 am
        /*glDisable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)

        for(pt in listOf(v00, v01, v10, v11)){
            val x = GFX.windowX + (+pt.x * 0.5f + 0.5f) * GFX.windowWidth
            val y = GFX.windowY + (-pt.y * 0.5f + 0.5f) * GFX.windowHeight
            GFX.drawRect(x.toInt()-2, y.toInt()-2, 5, 5, 0xff0000 or black)
        }

        glEnable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)*/

        val zRange = Clipping.getZ(v00, v01, v10, v11) ?: return null

        // calculate the depth based on the z value
        fun unmapZ(z: Float): Float {
            val n = Scene.nearZ
            val f = Scene.farZ
            val top = 2 * f * n
            val bottom = (z * (f-n) - (f+n))
            return - top / bottom // the usual z is negative -> invert it :)
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

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val meta = getMeta(file, true)

        val zoomLevel = if(meta != null){
            // calculate reasonable zoom level from canvas size
            val alignWithCamera = isBillboard[time]
            val rawZoomLevel = calculateSize(stack, alignWithCamera, meta.videoWidth, meta.videoHeight) ?: return
            if(videoScale < 1) getCacheableZoomLevel(rawZoomLevel)
            else videoScale
        } else 1

        var wasDrawn = false

        if(meta == null && GFX.isFinalRendering) throw MissingFrameException(file)
        if(meta != null){

            val sourceFPS = meta.videoFPS
            val sourceDuration = meta.videoDuration

            if(startTime >= sourceDuration) startTime = sourceDuration
            if(endTime >= sourceDuration) endTime = sourceDuration

            if(sourceFPS > 0f){
                if(time + startTime >= 0f && (isLooping != LoopingState.PLAY_ONCE || time < endTime)){

                    // use full fps when rendering to correctly render at max fps with time dilation
                    // issues arise, when multiple frames should be interpolated together into one
                    // at this time, we chose the center frame only.
                    val videoFPS = if(GFX.isFinalRendering) sourceFPS else min(sourceFPS, GFX.editorVideoFPS)

                    val frameCount = (sourceDuration * videoFPS).roundToInt()

                    // draw the current texture
                    val duration = endTime - startTime
                    val localTime = startTime + isLooping[time, duration]
                    val frameIndex = (localTime*videoFPS).toInt() % frameCount

                    val frame = Cache.getVideoFrame(file, zoomLevel, frameIndex, frameCount, videoFPS, videoFrameTimeout, isLooping)
                    if(frame != null && frame.isLoaded){
                        GFX.draw3D(stack, frame, color, isBillboard.getValueAt(time), nearestFiltering, tiling[time])
                        wasDrawn = true
                    } else {
                        if(GFX.isFinalRendering){
                            throw MissingFrameException(file)
                        }
                    }

                    // stack.scale(0.1f)
                    // GFX.draw3D(stack, FontManager.getString("Verdana",15f, "$frameIndex/$fps/$duration/$frameCount")!!, Vector4f(1f,1f,1f,1f), 0f)
                    // stack.scale(10f)

                } else wasDrawn = true
            }
        }

        if(!wasDrawn){
            GFX.draw3D(stack, GFX.flat01, GFX.colorShowTexture, 16, 9,
                Vector4f(0.5f, 0.5f, 0.5f, 1f).mul(color), isBillboard.getValueAt(time),
                true, tiling16x9
            )
        }

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Tiling", "(tile count x, tile count y, offset x, offset y)", tiling, style)
        list += VI("Video Start", "Timestamp in seconds of the first frames drawn", null, startTime, style){ startTime = it }
        list += VI("Video End", "Timestamp in seconds of the last frames drawn", null, endTime, style) { endTime = it }
        // todo a third mode, where the video is reversed after playing?
        // KISS principle? just allow modules to be created :)
        // list += VI("Looping?", "Should the video start after it ended? (useful for Gifs)", null, isLooping, style){ isLooping = it }
        // todo more interpolation modes? (cubic?)
        list += VI("Nearest Filtering", "Pixelated look; linear interpolation otherwise", null, nearestFiltering, style){ nearestFiltering = it }
        list += EnumInput("Video Scale", true,
            videoScaleNames.reverse[videoScale] ?: "Auto",
            videoScaleNames.entries.sortedBy { it.value }.map { it.key }, style)
            .setChangeListener { videoScale = videoScaleNames[it]!! }
            .setIsSelectedListener { show(null) }
            .setTooltip("Full resolution isn't always required. Define it yourself, or set it to automatic.")
    }

    override fun getClassName(): String = "Video"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("path", file.toString())
        writer.writeDouble("startTime", startTime)
        writer.writeDouble("endTime", endTime)
        writer.writeBool("nearestFiltering", nearestFiltering, true)
        writer.writeInt("videoScale", videoScale)
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "videoScale" -> videoScale = value
            else -> super.readInt(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when(name){
            "path" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when(name){
            "startTime" -> startTime = value
            "endTime" -> endTime = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "nearestFiltering" -> nearestFiltering = value
            else -> super.readBool(name, value)
        }
    }

    companion object {


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

    }

}