package me.anno.audio.openal

import me.anno.objects.Audio
import me.anno.objects.Transform
import me.anno.studio.StudioBase.Companion.shallStop
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.editorTimeDilation
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.root
import me.anno.utils.Sleep.sleepShortly
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.EXTDisconnect.ALC_CONNECTED
import java.nio.ByteBuffer
import java.nio.IntBuffer
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread
import kotlin.math.abs

object AudioManager {

    var cameraMatrix = Matrix4f()
    var soundSourceMap = HashMap<String, SoundSource>()
    var soundBuffers = ArrayList<SoundBuffer>()

    private var device = 0L
    private var context = 0L

    var openALSession = 0

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
                    AudioTasks.workQueue()
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
                if (!shallStop) {
                    // shall be destroyed by OpenAL itself -> false
                    sleepShortly(false)
                }
                checkIsDestroyed()
            }
            destroy()
        }
    }

    var lastCheckedTime = 0L
    var lastDeviceConfig = 0
    fun checkIsDestroyed() {
        // todo detect if the primary audio device was changed by the user...
        val time = System.nanoTime()
        if (abs(time - lastCheckedTime) > 500_000_000) {
            lastCheckedTime = time
            // 0.1ms -> it would be fine to even check it every time
            val audioDevices = AudioSystem.getMixerInfo()
            // val t1 = System.nanoTime()
            if (audioDevices.isNotEmpty()) {
                val currentConfig = audioDevices.size
                if (currentConfig != lastDeviceConfig) {
                    LOGGER.info("Devices changed -> Reconnecting")
                    val needsReconnect = lastDeviceConfig != 0
                    lastDeviceConfig = currentConfig
                    if (needsReconnect) reconnect()
                    return
                } else {
                    // maybe it died anyways?...
                    val answer = intArrayOf(0)
                    alcGetIntegerv(device, ALC_CONNECTED, answer)
                    if (answer[0] == 0) {
                        LOGGER.warn("Audio playing device disconnected")
                        reconnect()
                    }
                }
            }
        }
    }

    fun reconnect() {
        stop()
        destroy()
        init()
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
        openALSession++ // new session
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

    private val LOGGER = LogManager.getLogger(AudioManager::class)

}