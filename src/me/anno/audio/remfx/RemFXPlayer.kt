package me.anno.audio.remfx

import me.anno.audio.openal.AudioTasks.addAudioTask
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.audio.AttenuationModel.Companion.updateGlobalDistanceModel
import me.anno.ecs.components.audio.AudioCamera
import me.anno.ecs.systems.OnUpdate
import me.anno.audio.remfx.sound.Modulator
import kotlin.math.max

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

    var minAmplitude = 10f
    val sounds = ArrayList<Modulator>()

    private val context = RemFXContext()
    open fun fill(bufferIndex: Long, left: ShortArray, right: ShortArray) {
        val stream = stream ?: return
        val sampleRate = stream.playbackSampleRate

        val bufferSize = left.size
        val context = context
        context.sampleRate = sampleRate
        context.sampleOffset = bufferIndex * bufferSize
        context.bufferSize = bufferSize

        val invSize = 1f / left.size
        for (i in sounds.indices) {
            val sound = sounds[i]
            sound.update(context)

            val effect = sound.sound
            val l = sound.lastLeft
            val r = sound.lastRight
            context.amplitudeL0 = l
            context.amplitudeR0 = r
            context.deltaAmplitudeL = (sound.left - l) * invSize
            context.deltaAmplitudeR = (sound.right - r) * invSize

            val maxAmplitude = max(
                max(sound.left, l),
                max(sound.right, r)
            )
            if (maxAmplitude >= minAmplitude || effect.hasTimeDelay()) {
                effect.add(context, left, right)
            }

            sound.lastLeft = sound.left
            sound.lastRight = sound.right
        }
    }
}