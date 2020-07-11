package me.anno.audio

import org.joml.Vector3f
import org.lwjgl.openal.AL10.*

class SoundSource(val loop: Boolean, val relative: Boolean){


    var sourcePtr = alGenSources()

    init {
        if(loop) alSourcei(sourcePtr, AL_LOOPING, AL_TRUE)
        if(relative) alSourcei(sourcePtr, AL_SOURCE_RELATIVE, AL_TRUE)
        ALBase.check()
    }

    fun setDistanceModel(){
        if(sourcePtr < 0) return
        // max distance = stopped attenuation???
        alSourcef(sourcePtr, AL_ROLLOFF_FACTOR, 1f)
        alSourcef(sourcePtr, AL_REFERENCE_DISTANCE, 1f)
        alSourcef(sourcePtr, AL_MAX_DISTANCE, 1e3f)
    }

    fun setBuffer(buffer: Int){
        if(sourcePtr < 0) return
        stop()
        alSourcei(sourcePtr, AL_BUFFER, buffer)
    }

    fun setPosition(position: Vector3f){
        if(sourcePtr < 0) return
        alSource3f(sourcePtr, AL_POSITION, position.x, position.y, position.z)
    }

    fun setVelocity(speed: Vector3f){
        if(sourcePtr < 0) return
        alSource3f(sourcePtr, AL_VELOCITY, speed.x, speed.y, speed.z)
    }

    fun setGain(gain: Float){
        if(sourcePtr < 0) return
        alSourcef(sourcePtr, AL_GAIN, gain)
    }

    fun setProperty(param: Int, value: Float){
        if(sourcePtr < 0) return
        alSourcef(sourcePtr, param, value)
    }

    fun play(){
        if(sourcePtr < 0) return
        alSourcePlay(sourcePtr)
    }

    fun pause(){
        if(sourcePtr < 0) return
        alSourcePause(sourcePtr)
    }

    fun stop(){
        if(sourcePtr < 0) return
        alSourceStop(sourcePtr)
    }

    val isPlaying get() = alGetSourcei(sourcePtr, AL_SOURCE_STATE) == AL_PLAYING

    fun destroy(){
        if(sourcePtr < 0) return
        stop()
        alDeleteSources(sourcePtr)
        sourcePtr = -1
    }

}