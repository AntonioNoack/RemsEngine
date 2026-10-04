package me.anno.audio.openal

import me.anno.audio.openal.ALBase.isALThread
import me.anno.audio.openal.AudioManager.openALSession
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFX.INVALID_SESSION
import me.anno.gpu.GFX.isPointerValid
import me.anno.maths.Maths.sq
import org.joml.Vector3f
import org.lwjgl.openal.AL11.AL_BUFFER
import org.lwjgl.openal.AL11.AL_FALSE
import org.lwjgl.openal.AL11.AL_GAIN
import org.lwjgl.openal.AL11.AL_LOOPING
import org.lwjgl.openal.AL11.AL_MAX_DISTANCE
import org.lwjgl.openal.AL11.AL_PITCH
import org.lwjgl.openal.AL11.AL_PLAYING
import org.lwjgl.openal.AL11.AL_POSITION
import org.lwjgl.openal.AL11.AL_REFERENCE_DISTANCE
import org.lwjgl.openal.AL11.AL_ROLLOFF_FACTOR
import org.lwjgl.openal.AL11.AL_SOURCE_RELATIVE
import org.lwjgl.openal.AL11.AL_SOURCE_STATE
import org.lwjgl.openal.AL11.AL_TRUE
import org.lwjgl.openal.AL11.AL_VELOCITY
import org.lwjgl.openal.AL11.alDeleteSources
import org.lwjgl.openal.AL11.alGenSources
import org.lwjgl.openal.AL11.alGetSourcei
import org.lwjgl.openal.AL11.alSource3f
import org.lwjgl.openal.AL11.alSourcePause
import org.lwjgl.openal.AL11.alSourcePlay
import org.lwjgl.openal.AL11.alSourceStop
import org.lwjgl.openal.AL11.alSourcef
import org.lwjgl.openal.AL11.alSourcei
import kotlin.math.max
import kotlin.math.sqrt

class SoundSource(val loop: Boolean, var relativePositionsToListener: Boolean) {

    companion object {
        var maxVelocity = 343f * 0.9f // close to speed of sound
    }

    var session = INVALID_SESSION
    var pointer = INVALID_POINTER
    var hasBeenStarted = false

    fun ensurePointer(): Int {
        check(isALThread())
        if (!isPointerValid(pointer) || session != openALSession) {
            hasBeenStarted = false
            session = openALSession
            pointer = alGenSources()
            alSourcei(pointer, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
            alSourcei(pointer, AL_SOURCE_RELATIVE, if (relativePositionsToListener) AL_TRUE else AL_FALSE)
            ALBase.check()
            check(isPointerValid(pointer)) { "alGenSources returned invalid pointer" }
        }
        return pointer
    }

    fun checkSessionWasReset(): Boolean {
        return if (session != openALSession) {
            ensurePointer()
            true
        } else false
    }

    fun setRelative(relative: Boolean) {
        relativePositionsToListener = relative
        alSourcei(pointer, AL_SOURCE_RELATIVE, if (relative) AL_TRUE else AL_FALSE)
    }

    fun setDistanceModel(rollOffFactor: Float = 1f, referenceDistance: Float = 1f, maxDistance: Float = 1e3f) {
        val pointer = ensurePointer()
        val relative = rollOffFactor <= 0f
        if (relative != relativePositionsToListener) setRelative(relative)
        // rollOffFactor = 0 == no attenuation
        alSourcef(pointer, AL_ROLLOFF_FACTOR, max(rollOffFactor, 0f))
        // until this distance, the volume is constant (in clamped models)
        alSourcef(pointer, AL_REFERENCE_DISTANCE, max(referenceDistance, 1e-38f))
        // after this distance, the model is no longer attenuated, but still playing; except in linear model, there it stops
        alSourcef(pointer, AL_MAX_DISTANCE, max(maxDistance, 1e-38f))
    }

    @Suppress("unused")
    fun setBuffer(buffer: Int) {
        if (pointer < 0) return
        stop()
        alSourcei(pointer, AL_BUFFER, buffer)
    }

    fun setPosition(v: Vector3f) = setPosition(v.x, v.y, v.z)
    fun setVelocity(v: Vector3f) = setVelocity(v.x, v.y, v.z)

    fun setPosition(x: Float, y: Float, z: Float) {
        val pointer = ensurePointer()
        alSource3f(pointer, AL_POSITION, x, y, z)
    }

    fun setVelocity(x: Float, y: Float, z: Float) {
        val pointer = ensurePointer()
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
        alSource3f(pointer, AL_VELOCITY, nx, ny, nz)
    }

    fun setVolume(value: Float) {
        val pointer = ensurePointer()
        alSourcef(pointer, AL_GAIN, value)
    }

    fun setSpeed(value: Float) {
        val pointer = ensurePointer()
        alSourcef(pointer, AL_PITCH, value)
    }

    fun setProperty(param: Int, value: Float) {
        if (pointer < 0) return
        alSourcef(pointer, param, value)
    }

    fun play() {
        val pointer = ensurePointer()
        if (hasBeenStarted) return
        hasBeenStarted = true
        alSourcePlay(pointer)
    }

    fun pause() {
        val pointer = ensurePointer()
        hasBeenStarted = false
        alSourcePause(pointer)
    }

    fun stop() {
        val pointer = ensurePointer()
        hasBeenStarted = false
        alSourceStop(pointer)
    }

    @Suppress("unused")
    val isPlaying: Boolean
        get() = alGetSourcei(pointer, AL_SOURCE_STATE) == AL_PLAYING

    fun destroy() {
        if (pointer < 0) return
        stop()
        alDeleteSources(pointer)
        pointer = INVALID_POINTER
    }
}