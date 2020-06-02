package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.objects.cache.Cache
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import me.anno.video.FFMPEGStream
import me.anno.video.MissingFrameException
import org.joml.Matrix4fStack
import org.joml.Vector4f
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

// idea: hovering needs to be used to predict when the user steps forward in time
// -> no, that's too taxing; we'd need to pre-render a smaller version
// todo pre-render small version for scrubbing

class Video(var file: File, parent: Transform?): GFXTransform(parent){

    private var lastFile: Any? = null

    // todo add audio component...

    var startTime = 0f
    var endTime = 100f

    var duration = 0f

    var isLooping = true
    var nearestFiltering = DefaultConfig["default.video.nearest", false]

    // val fps get() = videoCache.fps
    var sourceFPS = -1f

    val frameCount get() =  max(1, FFMPEGStream.frameCountByFile[file] ?: (duration * sourceFPS).toInt())

    // val duration get() = videoCache.duration

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        if(lastFile != file){
            lastFile = file
            sourceFPS = -1f
            duration = -1f
            if(file.exists()){
                // request the metadata :)
                thread {
                    val file = file
                    loop@ while(this.file == file){
                        val frames = Cache.getVideoFrames(file, 0, 1f)
                        if(frames != null){
                            sourceFPS = frames.stream.sourceFPS
                            duration = frames.stream.sourceLength
                            if(sourceFPS > 0f && duration > 0f) break@loop
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

                    val frame = Cache.getVideoFrame(file, frameIndex, frameCount, videoFPS, isLooping)
                    if(frame != null && frame.isLoaded){
                        GFX.draw3D(stack, frame, color, isBillboard.getValueAt(time), nearestFiltering)
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
                true)
        }

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FileInput("File Location", style)
            .setText(file.toString())
            .setChangeListener { text -> file = File(text) }
            .setIsSelectedListener { GFX.selectedProperty = null }
        list += FloatInput("Video Start", startTime, style)
            .setChangeListener { startTime = it }
            .setIsSelectedListener { GFX.selectedProperty = null }
        list += FloatInput("Video End", endTime, style)
            .setChangeListener { endTime = it }
            .setIsSelectedListener { GFX.selectedProperty = null }
        // todo a third mode, where the video is reversed after playing?
        // KISS principle? just allow modules to be created :)
        list += BooleanInput("Looping?", isLooping, style)
            .setChangeListener { isLooping = it }
            .setIsSelectedListener { GFX.selectedProperty = null }
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

}