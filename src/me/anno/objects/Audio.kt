package me.anno.objects

import me.anno.audio.AudioManager
import me.anno.audio.AudioStreamOpenAL
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.AudioLinePanel
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
    val forcedMeta get() = getMeta(file, false)!!
    val meta get() = getMeta(file, true)

    var needsUpdate = true
    var isLooping =
        if(file.extension.equals("gif", true)) LoopingState.PLAY_LOOP
        else LoopingState.PLAY_ONCE

    var component: AudioStreamOpenAL? = null

    /**
     * is synchronized with the audio thread
     * */
    fun start(globalTime: Double, speed: Double, camera: Camera){
        needsUpdate = false
        component?.stop()
        val meta = forcedMeta
        if(meta.hasAudio){
            val component = AudioStreamOpenAL(this, speed, globalTime, camera)
            this.component = component
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
        list += VI("File Location", "Source file of this video", null, file, style){ file = it }
        val meta = forcedMeta
        if(meta.hasAudio){
            list += AudioLinePanel(forcedMeta, this, style)
        }
        list += VI("Amplitude", "How loud it is", amplitude, style)
        list += VI("Looping Type", "Whether to repeat the song/video", null, isLooping, style){
            AudioManager.requestUpdate()
            isLooping = it
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("src", file)
        writer.writeObject(this, "amplitude", amplitude)
        writer.writeInt("isLooping", isLooping.id, true)
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "isLooping" -> isLooping = LoopingState.getState(value)
            else -> super.readInt(name, value)
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