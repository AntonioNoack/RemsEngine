package me.anno.audio

import me.anno.objects.Audio
import me.anno.objects.Transform
import me.anno.studio.StudioBase.Companion.shallStop
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.editorTimeDilation
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.root
import me.anno.utils.Sleep.sleepShortly
import org.joml.Matrix4f
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
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
    fun startRunning() {
        runningThread = thread {
            init()
            while (!shallStop) {
                ALBase.check()
                val time = System.nanoTime()
                try {
                    me.anno.audio.AudioTasks.workQueue()
                } catch (e: Exception) {
                    // if(e.message != "ALException: Invalid Name") // why does the error happen???
                    e.printStackTrace()
                }
                ALBase.check()
                if (RemsStudio.isPlaying && ctr++ > 15) {
                    ctr = 0; checkTree(root)
                }
                if (RemsStudio.isPlaying && needsUpdate && abs(time - lastUpdate) > 200 * 1_000_000) {
                    // ensure 200 ms delay between changing the time / dilation
                    // for performance reasons
                    lastUpdate = time
                    needsUpdate = false
                    updateTime(editorTime, editorTimeDilation, root)
                }
                ALBase.check()
                if(!shallStop){
                    // shall be destroyed by OpenAL itself -> false
                    sleepShortly(false)
                }
            }
            destroy()
        }
    }

    fun stop(transform: Transform = root) {
        if (transform is Audio) {
            transform.stopPlayback()
        }
        transform.children.forEach {
            stop(it)
        }
    }

    fun requestUpdate() {
        needsUpdate = true
    }

    val camera by lazy { nullCamera!! }
    fun updateTime(time: Double, dilation: Double, transform: Transform) {
        if (transform is Audio) {
            transform.startPlayback(time, dilation, camera)
        }
        transform.children.forEach {
            updateTime(time, dilation, it)
        }
    }

    fun checkTree(transform: Transform) {
        if (transform is Audio && transform.needsUpdate) {
            transform.needsUpdate = false
            transform.startPlayback(editorTime, editorTimeDilation, camera)
        }
        transform.children.forEach {
            checkTree(it)
        }
    }

    fun init() {
        device = alcOpenDevice(null as ByteBuffer?)
        if (device == 0L) throw IllegalStateException("Failed to open default OpenAL device")
        val deviceCaps = ALC.createCapabilities(device)
        context = alcCreateContext(device, null as IntBuffer?)
        if (context == 0L) throw IllegalStateException("Failed to create OpenAL context")
        alcMakeContextCurrent(context)
        AL.createCapabilities(deviceCaps)
        ALBase.check()
    }

    fun destroy() {
        ALBase.check()
        alcCloseDevice(device)
        device = 0L
    }

}