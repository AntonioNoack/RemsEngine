package me.anno.audio.openal

import me.anno.Build.isDebug
import org.lwjgl.openal.AL10.*

object ALBase {

    var alThread: Thread? = null

    fun check() {
        // check thread safety
        // can be disabled for final build
        if (isDebug) {
            val currentThread = Thread.currentThread()
            if (alThread !== currentThread) {
                if (alThread == null) {
                    alThread = currentThread
                    currentThread.name = "OpenAL"
                } else {
                    throw RuntimeException("Called from wrong thread! This is not the audio thread!")
                }
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