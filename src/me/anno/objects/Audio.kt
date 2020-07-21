package me.anno.objects

import me.anno.audio.AudioManager
import me.anno.audio.AudioStream
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.AudioInput
import me.anno.ui.style.Style
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File

// todo openal to ffmpeg?

// todo flat playback vs 3D playback
// todo use the align-with-camera param for that? :)
// respect scale? nah, rather not xD
// (it becomes pretty complicated, I think)
open class Audio(var file: File = File(""), parent: Transform? = null): GFXTransform(parent){

    val amplitude = AnimatedProperty.floatPlus().set(1f)

    var needsUpdate = true
    var isLooping = file.extension.equals("gif", true)

    var component: AudioStream? = null

    /**
     * is synchronized with the audio thread
     * */
    fun start(globalTime: Double, speed: Double){
        needsUpdate = false
        component?.stop()
        val meta = getMeta(file, false)!!
        if(meta.hasAudio){
            val component = AudioStream(file, isLooping, 0.0, meta)
            this.component = component
            component.globalToLocalTime = { time ->
                getGlobalTransform(time * speed + globalTime).second
            }
            component.localAmplitude = { time ->
                amplitude[time]
            }
            component.start()
        } else component = null
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

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        // to do ensure, that the correct buffer is being generated -> done
        // to do we need to invalidate buffers, if we touch the custom timeline mode, or accelerate/decelerate audio... -> half done
        // todo how should we generate left/right audio? -> we need to somehow do this in software, too, for rendering

        getMeta(file, true) // just in case we need it ;)

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += AudioInput(file, style)
        list += VI("Amplitude", "How loud it is", amplitude, style)
        list += VI("Is Looping", "Whether to repeat the song/video", null, isLooping, style){
            AudioManager.requestUpdate()
            isLooping = it
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("src", file)
        writer.writeObject(this, "amplitude", amplitude)
        writer.writeBool("isLooping", isLooping, true)
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "isLooping" -> isLooping = value
            else -> super.readBool(name, value)
        }
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