package me.anno.audio

import org.joml.Matrix4f
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.IntBuffer

object AudioManager {

    var cameraMatrix = Matrix4f()
    var soundSourceMap = HashMap<String, SoundSource>()
    var soundBuffers = ArrayList<SoundBuffer>()

    private var device = 0L
    private var context = 0L

    fun init(){
        device = alcOpenDevice(null as ByteBuffer?)
        if(device == 0L) throw IllegalStateException("Failed to open default OpenAL device")
        val deviceCaps = ALC.createCapabilities(device)
        context = alcCreateContext(device, null as IntBuffer?)
        if(context == 0L) throw IllegalStateException("Failed to create OpenAL context")
        alcMakeContextCurrent(context)
        AL.createCapabilities(deviceCaps)
        Audio.check()
    }

    fun destroy(){
        Audio.check()
        alcCloseDevice(device)
        device = 0L
    }

}