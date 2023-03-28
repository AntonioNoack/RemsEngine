package me.anno.audio.openal

import me.anno.audio.openal.AudioManager.openALSession
import me.anno.utils.LOGGER
import org.joml.Vector3f
import org.lwjgl.openal.AL10.*

class SoundSource(val loop: Boolean, val relativePositionsToListener: Boolean) {

    var session = openALSession
    var sourcePtr = alGenSources()
    var hasBeenStarted = false

    init {
        alSourcei(sourcePtr, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
        alSourcei(sourcePtr, AL_SOURCE_RELATIVE, if (relativePositionsToListener) AL_TRUE else AL_FALSE)
        ALBase.check()
    }

    fun checkSessionWasReset(): Boolean {
        return if (session != openALSession) {
            session = openALSession
            sourcePtr = alGenSources()
            alSourcei(sourcePtr, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
            alSourcei(sourcePtr, AL_SOURCE_RELATIVE, if (relativePositionsToListener) AL_TRUE else AL_FALSE)
            ALBase.check()
            hasBeenStarted = false
            true
        } else false
    }

    fun setDistanceModel(rollOffFactor: Float = 1f, referenceDistance: Float = 1f, maxDistance: Float = 1e3f) {
        if (sourcePtr < 0) return
        // rollOffFactor = 0 == no attenuation
        LOGGER.debug("setDistanceModel($sourcePtr,$rollOffFactor,$referenceDistance,$maxDistance)")
        alSourcef(sourcePtr, AL_ROLLOFF_FACTOR, rollOffFactor)
        alSourcef(
            sourcePtr,
            AL_REFERENCE_DISTANCE,
            referenceDistance
        ) // until this distance, the volume is constant (in clamped models)
        alSourcef(
            sourcePtr,
            AL_MAX_DISTANCE,
            maxDistance
        ) // after this distance, the model is no longer attenuated, but still playing; except in linear model, there it stops
    }

    fun setBuffer(buffer: Int) {
        if (sourcePtr < 0) return
        stop()
        alSourcei(sourcePtr, AL_BUFFER, buffer)
    }

    fun setPosition(v: Vector3f) = setPosition(v.x, v.y, v.z)
    fun setVelocity(v: Vector3f) = setVelocity(v.x, v.y, v.z)

    fun setPosition(x: Float, y: Float, z: Float) {
        if (sourcePtr < 0) return
        alSource3f(sourcePtr, AL_POSITION, x, y, z)
    }

    fun setVelocity(x: Float, y: Float, z: Float) {
        if (sourcePtr < 0) return
        alSource3f(sourcePtr, AL_VELOCITY, x, y, z)
    }

    fun setGain(value: Float) {
        if (sourcePtr < 0) return
        alSourcef(sourcePtr, AL_GAIN, value)
    }

    fun setSpeed(value: Float) {
        if (sourcePtr < 0) return
        alSourcef(sourcePtr, AL_PITCH, value)
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