package me.anno.audio

import org.joml.Vector3f
import org.lwjgl.openal.AL10.*

class SoundSource(val loop: Boolean, val relative: Boolean){


    var sourcePtr = alGenSources()

    init {
        if(loop) alSourcei(sourcePtr, AL_LOOPING, AL_TRUE)
        if(relative) alSourcei(sourcePtr, AL_SOURCE_RELATIVE, AL_TRUE)
    }

    fun setDistanceModel(){
        // max distance = stopped attenuation???
        alSourcef(sourcePtr, AL_ROLLOFF_FACTOR, 1f)
        alSourcef(sourcePtr, AL_REFERENCE_DISTANCE, 1f)
        alSourcef(sourcePtr, AL_MAX_DISTANCE, 1e3f)
    }

    fun setBuffer(buffer: Int){
        stop()
        alSourcei(sourcePtr, AL_BUFFER, buffer)
    }

    fun setPosition(position: Vector3f){
        alSource3f(sourcePtr, AL_POSITION, position.x, position.y, position.z)
    }

    fun setVelocity(speed: Vector3f){
        alSource3f(sourcePtr, AL_VELOCITY, speed.x, speed.y, speed.z)
    }

    fun setGain(gain: Float){
        alSourcef(sourcePtr, AL_GAIN, gain)
    }

    fun setProperty(param: Int, value: Float){
        alSourcef(sourcePtr, param, value)
    }

    fun play(){
        alSourcePlay(sourcePtr)
    }

    fun pause(){
        alSourcePause(sourcePtr)
    }

    fun stop(){
        alSourceStop(sourcePtr)
    }

    val isPlaying get() = alGetSourcei(sourcePtr, AL_SOURCE_STATE) == AL_PLAYING

    fun destroy(){
        stop()
        alDeleteSources(sourcePtr)
    }

}