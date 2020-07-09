package me.anno.objects

import me.anno.audio.AudioStream
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.AudioInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import java.io.File

// todo openal to ffmpeg?

// todo get audio length for looping

// todo flat playback vs 3D playback
// todo use the align-with-camera param for that? :)
// respect scale? nah, rather not xD
// (it becomes pretty complicated, I think)
open class Audio(var file: File = File(""), parent: Transform? = null): GFXTransform(parent){

    val amplitude = AnimatedProperty.floatPlus().set(1f)

    var needsUpdate = true

    var component: AudioStream? = null

    /**
     * is synchronized with the audio thread
     * */
    fun start(globalTime: Float, speed: Float){
        component?.stop()
        val component = AudioStream(file)
        this.component = component
        component.globalToLocalTime = { time ->
            getGlobalTransform((time - globalTime) * speed + globalTime).second
        }
        component.localAmplitude = { time ->
            amplitude[time]
        }
        component.start(globalTime)
    }

    fun stop(){
        component?.stop()
        component = null // for garbage collection
    }

    override fun onDestroy() {
        super.onDestroy()
        GFX.addAudioTask { stop(); 1 }
    }

    // we need a flag, whether we draw in editor mode or not -> GFX.isFinalRendering
    // to do a separate mode, where resource availability is enforced? -> yes, we have that
    // Transforms, which load resources, should load async, and throw an error, if they don't block, while final-rendering

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        // to do ensure, that the correct buffer is being generated -> done
        // to do we need to invalidate buffers, if we touch the custom timeline mode, or accelerate/decelerate audio... -> half done
        // todo how should we generate left/right audio? -> we need to somehow do this in software, too, for rendering

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += AudioInput(file, style)
        list += VI("Amplitude", "How loud it is", amplitude, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("src", file)
        writer.writeObject(this, "amplitude", amplitude)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "amplitude" -> amplitude.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when(name){
            "src" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun onReadingEnded() {
        super.onReadingEnded()
        needsUpdate = true
    }

    override fun getClassName() = "Audio"

}