package me.anno.objects

import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import me.anno.video.FFMPEGStream
import org.joml.Matrix4fStack
import org.joml.Vector4f
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max

// todo button and calculation to match video/image/... to screen size

// todo hovering needs to be used to predict when the user steps forward in time
class Video(var file: File, parent: Transform?): GFXTransform(parent){

    private var lastFile: Any? = null

    // todo add audio component...

    var startTime = 0f
    var endTime = 100f

    var duration = 0f

    var isLooping = true

    // val fps get() = videoCache.fps
    var fps = -1f

    val frameCount get() =  max(1, FFMPEGStream.frameCountByFile[file] ?: (duration * fps).toInt())

    // val duration get() = videoCache.duration

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        if(lastFile != file){
            lastFile = file
            fps = -1f
            duration = -1f
            if(file.exists()){
                // request the metadata :)
                thread {
                    val file = file
                    loop@ while(this.file == file){
                        val frames = Cache.getVideoFrames(file, 0)
                        if(frames != null){
                            fps = frames.fps
                            duration = frames.stream.sourceLength
                            if(fps > 0f && duration > 0f) break@loop
                        } else Thread.sleep(1)
                    }
                }
            }
        }

        var wasDrawn = false

        if(file.exists() && duration > 0f){

            if(startTime >= duration) startTime = duration
            if(endTime >= duration) endTime = duration

            if(fps > 0f){
                if(time + startTime >= 0f && (isLooping || time < endTime)){

                    // todo draw the current or last texture
                    val duration = endTime - startTime
                    val localTime = startTime + (time % duration)
                    val frameIndex = (localTime*fps).toInt() % frameCount

                    val frame = Cache.getVideoFrame(file, frameIndex, frameCount, isLooping)
                    if(frame != null){
                        GFX.draw3D(stack, frame, color, isBillboard.getValueAt(time))
                        wasDrawn = true
                    }

                    // stack.scale(0.1f)
                    // GFX.draw3D(stack, FontManager.getString("Verdana",15f, "$frameIndex/$fps/$duration/$frameCount")!!, Vector4f(1f,1f,1f,1f), 0f)
                    // stack.scale(10f)

                } else wasDrawn = true
            }


        }

        if(!wasDrawn){
            GFX.draw3D(stack, GFX.flat01, GFX.colorShowTexture, 16, 9, Vector4f(0.5f, 0.5f, 0.5f, 1f).mul(color), isBillboard.getValueAt(time))
        }

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FileInput("Path", style)
            .setText(file.toString())
            .setChangeListener { text -> file = File(text) }
            .setIsSelectedListener { GFX.selectedProperty = null }
        list += FloatInput(style, "Video Start", startTime, AnimatedProperty.Type.FLOAT)
            .setChangeListener { startTime = it }
            .setIsSelectedListener { GFX.selectedProperty = null }
        list += FloatInput(style, "Video End", endTime, AnimatedProperty.Type.FLOAT)
            .setChangeListener { endTime = it }
            .setIsSelectedListener { GFX.selectedProperty = null }
        // todo a third mode, where the video is reversed after playing?
        list += BooleanInput("Looping?", style, isLooping)
            .setChangeListener { isLooping = it }
            .setIsSelectedListener { GFX.selectedProperty = null }
    }

    override fun getClassName(): String = "Video"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("path", file.toString())
        writer.writeFloat("startTime", startTime)
        writer.writeFloat("endTime", endTime)
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

}