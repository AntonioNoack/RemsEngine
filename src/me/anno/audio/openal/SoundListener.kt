package me.anno.audio.openal

import org.joml.Vector3f
import org.lwjgl.openal.AL11.AL_ORIENTATION
import org.lwjgl.openal.AL11.AL_POSITION
import org.lwjgl.openal.AL11.AL_VELOCITY
import org.lwjgl.openal.AL11.alListener3f
import org.lwjgl.openal.AL11.alListenerfv

object SoundListener {

    fun setVelocity(speed: Vector3f) {
        alListener3f(AL_VELOCITY, speed.x, speed.y, speed.z)
    }

    fun setPosition(position: Vector3f) {
        alListener3f(AL_POSITION, position.x, position.y, position.z)
    }

    fun setOrientation(lookAt: Vector3f, up: Vector3f) {
        alListenerfv(
            AL_ORIENTATION, floatArrayOf(
                lookAt.x, lookAt.y, lookAt.z,
                up.x, up.y, up.z
            )
        )
    }
}