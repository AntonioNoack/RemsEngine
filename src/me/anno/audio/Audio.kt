package me.anno.audio

import org.lwjgl.openal.AL10.AL_NO_ERROR
import org.lwjgl.openal.AL10.alGetError
import java.lang.RuntimeException

object Audio {
    fun check(){
        val error = alGetError()
        if(error != AL_NO_ERROR){
            throw RuntimeException("ALException: $error")
        }
    }
}