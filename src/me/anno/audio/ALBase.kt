package me.anno.audio

import org.lwjgl.openal.AL10.*
import java.lang.RuntimeException

object ALBase {
    fun check(){
        val error = alGetError()
        throw RuntimeException(when(error){
            AL_NO_ERROR -> { return }
            AL_INVALID_NAME -> "ALException: Invalid Name"
            AL_INVALID_ENUM -> "ALException: Invalid Enum"
            AL_INVALID_VALUE -> "ALException: Invalid Value"
            AL_INVALID_OPERATION -> "ALException: Invalid Operation"
            AL_OUT_OF_MEMORY -> "ALException: OutOfMemory"
            else -> "ALException: $error (unknown)"
        })
    }
}