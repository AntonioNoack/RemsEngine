package me.anno.audio.openal

import me.anno.Build.isDebug
import org.lwjgl.openal.AL11.AL_INVALID_ENUM
import org.lwjgl.openal.AL11.AL_INVALID_NAME
import org.lwjgl.openal.AL11.AL_INVALID_OPERATION
import org.lwjgl.openal.AL11.AL_INVALID_VALUE
import org.lwjgl.openal.AL11.AL_NO_ERROR
import org.lwjgl.openal.AL11.AL_OUT_OF_MEMORY
import org.lwjgl.openal.AL11.alGetError

object ALBase {

    var alThread: Thread? = null

    fun isALThread(): Boolean {
        return alThread == Thread.currentThread()
    }

    fun check() {
        // check thread safety
        // can be disabled for final build
        if (isDebug) {
            if (!isALThread()) {
                throw RuntimeException("Called from wrong thread! This is not the audio thread!")
            }
            val errorMessage = when (val error = alGetError()) {
                AL_NO_ERROR -> return
                AL_INVALID_NAME -> "ALException: Invalid Name"
                AL_INVALID_ENUM -> "ALException: Invalid Enum"
                AL_INVALID_VALUE -> "ALException: Invalid Value"
                AL_INVALID_OPERATION -> "ALException: Invalid Operation"
                AL_OUT_OF_MEMORY -> "ALException: OutOfMemory"
                else -> "ALException: $error (unknown)"
            }
            throw RuntimeException(errorMessage)
        }
    }
}