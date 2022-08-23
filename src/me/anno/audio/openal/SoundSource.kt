package me.anno.audio.openal

import me.anno.audio.openal.AudioManager.openALSession
import org.joml.Vector3f
import org.lwjgl.openal.AL10.*

class SoundSource(val loop: Boolean, val relative: Boolean) {

    var session = openALSession
    var sourcePtr = alGenSources()
    var hasBeenStarted = false

    init {
        alSourcei(sourcePtr, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
        alSourcei(sourcePtr, AL_SOURCE_RELATIVE, if (relative) AL_TRUE else AL_FALSE)
        ALBase.check()
    }

    fun checkSessionWasReset(): Boolean {
        return if (session != openALSession) {
            session = openALSession
            sourcePtr = alGenSources()
            alSourcei(sourcePtr, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
            alSourcei(sourcePtr, AL_SOURCE_RELATIVE, if (relative) AL_TRUE else AL_FALSE)
            ALBase.check()
            hasBeenStarted = false
            true
        } else false
    }

    fun setDistanceModel(rolloffFactor: Float = 1f, referenceDistance: Float = 1f, maxDistance: Float = 1e3f) {
        if (sourcePtr < 0) return
        // max distance = stopped attenuation???
        alSourcef(sourcePtr, AL_ROLLOFF_FACTOR, rolloffFactor)
        alSourcef(sourcePtr, AL_REFERENCE_DISTANCE, referenceDistance)
        alSourcef(sourcePtr, AL_MAX_DISTANCE, maxDistance)
    }

    fun setBuffer(buffer: Int) {
        if (sourcePtr < 0) return
        stop()
        alSourcei(sourcePtr, AL_BUFFER, buffer)
    }

    fun setPosition(position: Vector3f) {
        if (sourcePtr < 0) return
        alSource3f(sourcePtr, AL_POSITION, position.x, position.y, position.z)
    }

    fun setVelocity(speed: Vector3f) {
        if (sourcePtr < 0) return
        alSource3f(sourcePtr, AL_VELOCITY, speed.x, speed.y, speed.z)
    }

    fun setPosition(px: Float, py: Float, pz: Float) {
        if (sourcePtr < 0) return
        alSource3f(sourcePtr, AL_POSITION, px, py, pz)
    }

    fun setVelocity(sx: Float, sy: Float, sz: Float) {
        if (sourcePtr < 0) return
        alSource3f(sourcePtr, AL_VELOCITY, sx, sy, sz)
    }

    fun setGain(gain: Float) {
        if (sourcePtr < 0) return
        alSourcef(sourcePtr, AL_GAIN, gain)
    }

    fun setProperty(param: Int, value: Float) {
        if (sourcePtr < 0) return
        alSourcef(sourcePtr, param, value)
    }

    fun play() {
        if (sourcePtr < 0) return
        if (hasBeenStarted) return
        hasBeenStarted = true
        alSourcePlay(sourcePtr)
    }

    fun pause() {
        if (sourcePtr < 0) return
        hasBeenStarted = false
        alSourcePause(sourcePtr)
    }

    fun stop() {
        if (sourcePtr < 0) return
        hasBeenStarted = false
        alSourceStop(sourcePtr)
    }

    val isPlaying get() = alGetSourcei(sourcePtr, AL_SOURCE_STATE) == AL_PLAYING

    fun destroy() {
        if (sourcePtr < 0) return
        stop()
        alDeleteSources(sourcePtr)
        sourcePtr = -1
    }

}