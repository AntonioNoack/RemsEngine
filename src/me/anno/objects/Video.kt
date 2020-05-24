package me.anno.objects

import me.anno.config.DefaultConfig.style
import me.anno.fonts.FontManagerV01
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.maths.clamp
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import java.io.File

// todo button and calculation to match video/image/... to screen size

// todo hovering needs to be used to predict when the user steps forward in time
class Video(var file: File, parent: Transform?): GFXTransform(parent){

    private var lastFile: Any? = null
    lateinit var videoCache: VideoCache

    // todo add audio component...

    var startTime = 0f
    var endTime = 100f

    val fps get() = videoCache.fps

    val duration get() = videoCache.duration

    // todo a mode, where drawing frames is forced; for rendering
    override fun draw(stack: Matrix4fStack, parentTime: Float, parentColor: Vector4f, style: Style) {
        super.draw(stack, parentTime, parentColor, style)

        if(lastFile != file){
            videoCache = VideoCache.getVideo(file)
            lastFile = file
        }

        if(file.exists()){
            val time = getLocalTime(parentTime)
            if(time in startTime .. endTime){

                // todo draw the current or last texture
                val frameIndex = ((time-startTime)*fps).toInt()
                val localIndex = frameIndex % videoCache.framesPerContainer

                val frame = videoCache.getFrame(frameIndex)
                if(frame != null){
                    val color = getLocalColor(parentColor, time)
                    GFX.draw3D(stack, frame, color, isBillboard.getValueAt(time))
                }

                stack.scale(0.1f)
                GFX.draw3D(stack, FontManagerV01.getString("Verdana",15f, "$frameIndex:$localIndex")!!, Vector4f(1f,1f,1f,1f), 0f)

            }
        }
    }

    override fun createInspector(list: PanelListY) {
        super.createInspector(list)
        val fileInput = FileInput("Path", style)
            .setText(file.toString())
            .setChangeListener { text -> file = File(text) }
        list += FloatInput(style, "Start Time", startTime, AnimatedProperty.Type.FLOAT)
            .setChangeListener { startTime = it }
        list += FloatInput(style, "End Time", endTime, AnimatedProperty.Type.FLOAT)
            .setChangeListener { endTime = it }
        list += fileInput
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