package me.anno.audio

import org.lwjgl.openal.AL10.*
import java.lang.RuntimeException

object ALBase {

    var thread0: Thread? = null

    fun check(){
        // check thread safety
        // can be disable for final build
        val currentThread = Thread.currentThread()
        if(thread0 !== currentThread){
            if(thread0 == null) thread0 = currentThread
            else {
                throw RuntimeException("Called from wrong thread! This is not the audio thread!")
            }
        }
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