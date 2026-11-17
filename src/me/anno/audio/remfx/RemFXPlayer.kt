package me.anno.audio.remfx

import me.anno.audio.openal.AudioTasks.addAudioTask
import me.anno.audio.remfx.effect.Sound
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.audio.AttenuationModel.Companion.updateGlobalDistanceModel
import me.anno.ecs.components.audio.AudioCamera
import me.anno.ecs.systems.OnUpdate

open class RemFXPlayer : Component(), OnUpdate {

    private var stream: RemFXAudioStream? = null

    private fun start() {
        val stream1 = RemFXAudioStream(this)
        addAudioTask("start", 1) {
            stream?.stop()
            updateGlobalDistanceModel()

            println("starting audio")

            stream = stream1
            stream1.alSource.setVolume(1f)
            stream1.alSource.setSpeed(1f)
            stream1.alSource.setPosition(0f, 0f, 0f)
            stream1.alSource.setVelocity(0f, 0f, 0f)
            stream1.start()
        }
    }

    @DebugProperty
    val isPlaying get() = stream?.isPlaying == true

    @DebugAction
    fun stop() {
        if (stream == null) return
        addAudioTask("stop", 1) {
            stream?.stop()
            stream = null
        }
    }

    override fun destroy() {
        super.destroy()
        stop()
    }

    override fun onUpdate() {
        if (stream == null) start()
        AudioCamera.updateCameraAsync()
    }

    val sounds = ArrayList<Sound>()

    // todo bug: there is a 10% chance that it doesn't play anything...
    //  isPlaying returns true though...

    val context = RemFXContext()
    open fun fill(bufferIndex: Long, left: ShortArray, right: ShortArray) {
        val stream = stream ?: return
        val sampleRate = stream.playbackSampleRate

        val bufferSize = left.size
        val context = context
        context.sampleRate = sampleRate
        context.sampleOffset = bufferIndex * bufferSize
        context.bufferSize = bufferSize

        for (i in sounds.indices) {
            val sound = sounds[i]
            sound.update(context)
            sound.add(context, left, right)
        }
    }
}