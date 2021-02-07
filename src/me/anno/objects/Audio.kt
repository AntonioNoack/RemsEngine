package me.anno.objects

import me.anno.audio.AudioStreamOpenAL
import me.anno.audio.effects.SoundPipeline
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.modes.LoopingState
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe
import me.anno.utils.structures.ValueWithDefaultFunc
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File

// flat playback vs 3D playback
// respect scale? nah, rather not xD
// (it becomes pretty complicated, I think)

abstract class Audio(var file: File = File(""), parent: Transform? = null) : GFXTransform(parent) {

    val amplitude = AnimatedProperty.floatPlus(1f)
    var effects = SoundPipeline(this)
    var isLooping = ValueWithDefaultFunc {
        if (file.extension.equals("gif", true)) LoopingState.PLAY_LOOP
        else LoopingState.PLAY_ONCE
    }

    var is3D = false

    val meta get() = getMeta(file, true)
    val forcedMeta get() = getMeta(file, false)

    var needsUpdate = true
    var component: AudioStreamOpenAL? = null

    /**
     * is synchronized with the audio thread
     * */
    fun startPlayback(globalTime: Double, speed: Double, camera: Camera) {
        // why an exception? because I happened to run into this issue
        if (speed == 0.0) throw IllegalArgumentException("Audio speed must not be 0.0, because that's inaudible")
        needsUpdate = false
        component?.stop()
        val meta = forcedMeta
        if (meta?.hasAudio == true) {
            val component = AudioStreamOpenAL(this, speed, globalTime, camera)
            this.component = component
            component.start()
        } else component = null
    }

    fun stopPlayback() {
        component?.stop()
        component = null // for garbage collection
    }

    override fun onDestroy() {
        super.onDestroy()
        GFX.addAudioTask(1) { stopPlayback() }
    }

    // we need a flag, whether we draw in editor mode or not -> GFX.isFinalRendering
    // to do a separate mode, where resource availability is enforced? -> yes, we have that
    // Transforms, which load resources, should load async, and throw an error, if they don't block, while final-rendering

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        // to do ensure, that the correct buffer is being generated -> done
        // to do we need to invalidate buffers, if we touch the custom timeline mode, or accelerate/decelerate audio... -> half done
        // how should we generate left/right audio? -> we need to somehow do this in software, too, for rendering -> started

        getMeta(file, true) // just in case we need it ;)

    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        effects.apply {
            audio = this@Audio
            createInspector(list, style, getGroup)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
        writer.writeObject(this, "amplitude", amplitude)
        writer.writeObject(this, "effects", effects)
        writer.writeMaybe(this, "isLooping", isLooping)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "isLooping" -> isLooping.set(LoopingState.getState(value))
            else -> super.readInt(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "amplitude" -> amplitude.copyFrom(value)
            "effects" -> effects = value as? SoundPipeline ?: return
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "src", "file", "path" -> file = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun onReadingEnded() {
        super.onReadingEnded()
        needsUpdate = true
    }

}