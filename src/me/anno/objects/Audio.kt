package me.anno.objects

import me.anno.audio.openal.AudioStreamOpenAL
import me.anno.audio.openal.AudioTasks
import me.anno.audio.effects.SoundPipeline
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.animation.AnimatedProperty
import me.anno.io.files.EmptyRef
import me.anno.objects.modes.LoopingState
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.io.files.FileReference
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe
import me.anno.utils.structures.ValueWithDefaultFunc
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc

// flat playback vs 3D playback
// respect scale? nah, rather not xD
// (it becomes pretty complicated, I think)

abstract class Audio(var file: FileReference = EmptyRef, parent: Transform? = null) : GFXTransform(parent) {

    val amplitude = AnimatedProperty.floatPlus(1f)
    var pipeline = SoundPipeline(this)
    val isLooping = ValueWithDefaultFunc {
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
    open fun startPlayback(globalTime: Double, speed: Double, camera: Camera) {
        // why an exception? because I happened to run into this issue
        if (speed == 0.0) throw IllegalArgumentException("Audio speed must not be 0.0, because that's inaudible")
        stopPlayback()
        val meta = forcedMeta
        if (meta?.hasAudio == true) {
            val component = AudioStreamOpenAL(this, speed, globalTime, camera)
            this.component = component
            component.start()
        } else component = null
    }

    fun stopPlayback() {
        needsUpdate = false
        component?.stop()
        component = null // for garbage collection
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioTasks.addTask(1) { stopPlayback() }
    }

    // we need a flag, whether we draw in editor mode or not -> GFX.isFinalRendering
    // to do a separate mode, where resource availability is enforced? -> yes, we have that
    // Transforms, which load resources, should load async, and throw an error, if they don't block, while final-rendering

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

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
        val pipeline = pipeline
        pipeline.effects.forEach { it.audio = this }
        pipeline.audio = this
        pipeline.createInspector(list, style, getGroup)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
        writer.writeObject(this, "amplitude", amplitude)
        writer.writeObject(this, "effects", pipeline)
        writer.writeMaybe(this, "isLooping", isLooping)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "isLooping" -> isLooping.value = LoopingState.getState(value)
            else -> super.readInt(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "amplitude" -> amplitude.copyFrom(value)
            "effects" -> pipeline = value as? SoundPipeline ?: return
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