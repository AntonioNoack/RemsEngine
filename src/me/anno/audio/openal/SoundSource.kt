package me.anno.audio.openal

import me.anno.audio.openal.AudioManager.openALSession
import me.anno.maths.Maths.sq
import org.joml.Vector3f
import org.lwjgl.openal.AL10.*
import kotlin.math.max
import kotlin.math.sqrt

class SoundSource(val loop: Boolean, var relativePositionsToListener: Boolean) {

    companion object {
        var maxVelocity = 343f * 0.9f // close to speed of sound
    }

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

    fun setRelative(relative: Boolean) {
        relativePositionsToListener = relative
        alSourcei(sourcePtr, AL_SOURCE_RELATIVE, if (relative) AL_TRUE else AL_FALSE)
    }

    fun setDistanceModel(rollOffFactor: Float = 1f, referenceDistance: Float = 1f, maxDistance: Float = 1e3f) {
        if (sourcePtr < 0) return
        val relative = rollOffFactor <= 0f
        if (relative != relativePositionsToListener) setRelative(relative)
        // rollOffFactor = 0 == no attenuation
        alSourcef(sourcePtr, AL_ROLLOFF_FACTOR, max(rollOffFactor, 0f))
        // until this distance, the volume is constant (in clamped models)
        alSourcef(sourcePtr, AL_REFERENCE_DISTANCE, max(referenceDistance, 1e-38f))
        // after this distance, the model is no longer attenuated, but still playing; except in linear model, there it stops
        alSourcef(sourcePtr, AL_MAX_DISTANCE, max(maxDistance, 1e-38f))
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
        var nx = x
        var ny = y
        var nz = z
        val lenSq = sq(nx, ny, nz)
        val maxVelocity = maxVelocity
        if (lenSq > maxVelocity * maxVelocity) {
            val factor = maxVelocity / sqrt(lenSq)
            nx *= factor
            ny *= factor
            nz *= factor
        }
        alSource3f(sourcePtr, AL_VELOCITY, nx, ny, nz)
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