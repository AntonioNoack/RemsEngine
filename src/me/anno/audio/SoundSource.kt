package me.anno.audio

import org.joml.Vector3f
import org.lwjgl.openal.AL10.*

class SoundSource(val loop: Boolean, val relative: Boolean){


    var source = alGenSources()

    init {
        if(loop) alSourcei(source, AL_LOOPING, AL_TRUE)
        if(relative) alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE)
    }

    fun setDistanceModel(){
        // max distance = stopped attenuation???
        alSourcef(source, AL_ROLLOFF_FACTOR, 1f)
        alSourcef(source, AL_REFERENCE_DISTANCE, 1f)
        alSourcef(source, AL_MAX_DISTANCE, 1e3f)
    }

    fun setBuffer(buffer: Int){
        stop()
        alSourcei(source, AL_BUFFER, buffer)
    }

    fun setPosition(position: Vector3f){
        alSource3f(source, AL_POSITION, position.x, position.y, position.z)
    }

    fun setVelocity(speed: Vector3f){
        alSource3f(source, AL_VELOCITY, speed.x, speed.y, speed.z)
    }

    fun setGain(gain: Float){
        alSourcef(source, AL_GAIN, gain)
    }

    fun setProperty(param: Int, value: Float){
        alSourcef(source, param, value)
    }

    fun play(){
        alSourcePlay(source)
    }

    fun pause(){
        alSourcePause(source)
    }

    fun stop(){
        alSourceStop(source)
    }

    val isPlaying get() = alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING

    fun destroy(){
        stop()
        alDeleteSources(source)
    }

}