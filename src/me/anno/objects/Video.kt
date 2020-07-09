package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.print
import me.anno.utils.toVec3f
import me.anno.video.FFMPEGStream
import me.anno.video.MissingFrameException
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector4f
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

// idea: hovering needs to be used to predict when the user steps forward in time
// -> no, that's too taxing; we'd need to pre-render a smaller version
// todo pre-render small version for scrubbing

// todo get information about full and relative frames, so we get optimal scrubbing performance :)


class Video(file: File = File(""), parent: Transform? = null): Audio(file, parent){

    private var lastFile: Any? = null

    var tiling = AnimatedProperty.tiling()

    var startTime = 0f
    var endTime = 100f

    var duration = 0f

    var isLooping = true
    var nearestFiltering = DefaultConfig["default.video.nearest", false]

    // val fps get() = videoCache.fps
    var sourceFPS = -1f

    val frameCount get() =  max(1, FFMPEGStream.frameCountByFile[file] ?: (duration * sourceFPS).toInt())

    // val duration get() = videoCache.duration

    fun calculateSize(matrix: Matrix4f): Int? {

        // todo we need to apply the full transform before we can do this check correctly
        return 1

        // one edge, knapp: (0.40317687 0.83566207 1.0016097) (0.38428885 -0.73683864 1.0015345) (-1.6112523 -1.5214 0.99689794) (-1.7798866 1.7449334 0.9965732)
        val v00 = matrix.transform(Vector4f(-1f, -1f, 0f, 1f)).toVec3f()
        val v01 = matrix.transform(Vector4f(-1f, +1f, 0f, 1f)).toVec3f()
        val v10 = matrix.transform(Vector4f(+1f, -1f, 0f, 1f)).toVec3f()
        val v11 = matrix.transform(Vector4f(+1f, +1f, 0f, 1f)).toVec3f()
        val minZ = -1f
        val maxZ = 1f
        // is this check good enough?
        if(v00.z < minZ && v01.z < minZ && v10.z < minZ && v11.z < minZ) return null
        if(v00.z > maxZ && v01.z > maxZ && v10.z > maxZ && v11.z > maxZ) return null

        // check the visibility
        // todo a better check:
        // visible if:
        // - contained or
        // - cuts one of the edges

        val fullWidth = GFX.windowWidth
        val fullHeight = GFX.windowHeight

        val minX = min(min(v00.x, v01.x), min(v10.x, v11.x))
        if(minX > 1f) return null

        val maxX = max(max(v00.x, v01.x), max(v10.x, v11.x))
        if(maxX < -1f) return null

        val minY = min(min(v00.y, v01.y), min(v10.y, v11.y))
        if(minY > 1f) return null

        val maxY = max(max(v00.y, v01.y), max(v10.y, v11.y))
        if(maxY < -1f) return null

        // we should transform the values with one axis, by scaling the quad down to match the window (more return null cases; e.g. below left corner)
        // although out of bounds values cannot be seen, they indicate required scale
        val width = (maxX - minX) * fullWidth * 0.5f
        val height = (maxY - minY) * fullHeight * 0.5f

        return max(width, height).toInt()

    }

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        val size = calculateSize(stack) ?: return

        if(lastFile != file){
            val file = file
            lastFile = file
            sourceFPS = sourceFPSCache[file] ?: -1f
            duration = durationCache[file] ?: -1f
            if(file.exists() && (sourceFPS <= 0f || duration <= 0f)){
                // request the metadata :)
                thread {
                    loop@ while(this.file == file){
                        val frames = Cache.getVideoFrames(file, 0, 1f, videoMetaTimeout)
                        if(frames != null){
                            sourceFPS = frames.stream.sourceFPS
                            duration = frames.stream.sourceLength
                            if(sourceFPS > 0f && duration > 0f){
                                sourceFPSCache[file] = sourceFPS
                                durationCache[file] = duration
                                break@loop
                            }
                        } else Thread.sleep(1)
                    }
                }
            }
        }

        var wasDrawn = false

        if((duration <= 0f || sourceFPS <= 0f) && GFX.isFinalRendering) throw MissingFrameException(file)
        if(file.exists() && duration > 0f){

            // todo when the video is loaded the first time using rendering, it won't recognise missing frames

            if(startTime >= duration) startTime = duration
            if(endTime >= duration) endTime = duration

            if(sourceFPS > 0f){
                if(time + startTime >= 0f && (isLooping || time < endTime)){

                    // use full fps when rendering to correctly render at max fps with time dilation
                    // issues arise, when multiple frames should be interpolated together into one
                    // at this time, we chose the center frame only.
                    val videoFPS = if(GFX.isFinalRendering) sourceFPS else min(sourceFPS, GFX.editorVideoFPS)

                    // draw the current texture
                    val duration = endTime - startTime
                    val localTime = startTime + (time % duration)
                    val frameIndex = (localTime*videoFPS).toInt() % frameCount

                    val frame = Cache.getVideoFrame(file, frameIndex, frameCount, videoFPS, videoFrameTimeout, isLooping)
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
        list += FileInput("File Location", style)
            .setText(file.toString())
            .setChangeListener { text -> file = File(text) }
            .setIsSelectedListener { show(null) }
        list += VectorInput(style, "Tiling", tiling[lastLocalTime], AnimatedProperty.Type.TILING)
            .setChangeListener { x, y, z, w -> putValue(tiling, Vector4f(x,y,z,w)) }
            .setIsSelectedListener { show(tiling) }
        list += FloatInput("Video Start", startTime, style)
            .setChangeListener { startTime = it }
            .setIsSelectedListener { show(null) }
        list += FloatInput("Video End", endTime, style)
            .setChangeListener { endTime = it }
            .setIsSelectedListener { show(null) }
        // todo a third mode, where the video is reversed after playing?
        // KISS principle? just allow modules to be created :)
        list += BooleanInput("Looping?", isLooping, style)
            .setChangeListener { isLooping = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Nearest Filtering", nearestFiltering, style)
            .setChangeListener { nearestFiltering = it }
            .setIsSelectedListener { show(null) }
    }

    override fun getClassName(): String = "Video"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("path", file.toString())
        writer.writeFloat("startTime", startTime)
        writer.writeFloat("endTime", endTime)
        writer.writeBool("nearestFiltering", nearestFiltering, true)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "path" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when(name){
            "startTime" -> startTime = value
            "endTime" -> endTime = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "nearestFiltering" -> nearestFiltering = value
            else -> super.readBool(name, value)
        }
    }

    companion object {

        val sourceFPSCache = HashMap<File, Float>()
        val durationCache = HashMap<File, Float>()

        val videoMetaTimeout = 100L
        val videoFrameTimeout = 500L

        val tiling16x9 = Vector4f(8f, 4.5f, 0f, 0f)

        fun clearCache(){
            sourceFPSCache.clear()
            durationCache.clear()
        }
    }

}