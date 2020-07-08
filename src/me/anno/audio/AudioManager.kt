package me.anno.audio

import me.anno.gpu.GFX
import me.anno.objects.Audio
import me.anno.objects.Transform
import me.anno.studio.Studio
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.editorTimeDilation
import me.anno.studio.Studio.root
import org.joml.Matrix4f
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import java.lang.Exception
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.concurrent.thread
import kotlin.math.abs

object AudioManager {

    var cameraMatrix = Matrix4f()
    var soundSourceMap = HashMap<String, SoundSource>()
    var soundBuffers = ArrayList<SoundBuffer>()

    private var device = 0L
    private var context = 0L

    var needsUpdate = false
    var lastUpdate = 0L
    var ctr = 0
    var runningThread: Thread? = null
    fun startRunning(){
        runningThread = thread {
            init()
            while(true){
                ALBase.check()
                val time = System.nanoTime()
                try {
                    GFX.workQueue(GFX.audioTasks)
                } catch (e: Exception){
                    e.printStackTrace()
                }
                ALBase.check()
                if(Studio.isPlaying && ctr++ > 15){ ctr = 0; checkTree(root) }
                if(Studio.isPlaying && needsUpdate && abs(time-lastUpdate) > 200 * 1_000_000){
                    // ensure 200 ms delay between changing the time / dilation
                    // for performance reasons
                    lastUpdate = time
                    needsUpdate = false
                    updateTime(editorTime, editorTimeDilation, root)
                }
                ALBase.check()
                Thread.sleep(1)
            }
        }
    }

    fun stop(transform: Transform = root){
        if(transform is Audio){
            transform.stop()
        }
        transform.children.forEach {
            stop(it)
        }
    }

    fun requestTimeUpdate(){
        needsUpdate = true
    }

    fun updateTime(time: Float, dilation: Float, transform: Transform){
        // if(transform == root) println("$time += t * $dilation")
        if(transform is Audio){
            transform.start(time, dilation)
        }
        transform.children.forEach {
            updateTime(time, dilation, it)
        }
    }

    fun checkTree(transform: Transform){
        if(transform is Audio && transform.needsUpdate){
            transform.needsUpdate = false
            transform.start(editorTime, editorTimeDilation)
        }
        transform.children.forEach {
            checkTree(it)
        }
    }

    fun init(){
        device = alcOpenDevice(null as ByteBuffer?)
        if(device == 0L) throw IllegalStateException("Failed to open default OpenAL device")
        val deviceCaps = ALC.createCapabilities(device)
        context = alcCreateContext(device, null as IntBuffer?)
        if(context == 0L) throw IllegalStateException("Failed to create OpenAL context")
        alcMakeContextCurrent(context)
        AL.createCapabilities(deviceCaps)
        ALBase.check()
    }

    fun destroy(){
        runningThread?.stop()
        ALBase.check()
        alcCloseDevice(device)
        device = 0L
    }

}