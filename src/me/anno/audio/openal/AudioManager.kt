package me.anno.audio.openal

import me.anno.Engine
import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Sleep.sleepABit
import org.apache.logging.log4j.LogManager
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.alcCloseDevice
import org.lwjgl.openal.ALC10.alcCreateContext
import org.lwjgl.openal.ALC10.alcDestroyContext
import org.lwjgl.openal.ALC10.alcGetIntegerv
import org.lwjgl.openal.ALC10.alcMakeContextCurrent
import org.lwjgl.openal.ALC10.alcOpenDevice
import org.lwjgl.openal.ALCCapabilities
import org.lwjgl.openal.EXTDisconnect.ALC_CONNECTED
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.concurrent.thread
import kotlin.math.abs

object AudioManager {

    var audioDeviceHash: () -> Int = { 0 }

    // var cameraMatrix = Matrix4f()
    // var soundSourceMap = HashMap<String, SoundSource>()
    // var soundBuffers = ArrayList<SoundBuffer>()

    private var device = 0L
    private var context = 0L

    // which session is the current one;
    // resources from old session shall not be used,
    // as all pointers will be invalid!
    var openALSession = 0

    var needsUpdate = false
    var lastUpdate = 0L
    var ctr = 0
    var runningThread: Thread? = null

    var onUpdate: ((time: Long) -> Unit)? = null

    fun startRunning() {
        runningThread?.interrupt() // kill the old thread
        runningThread = thread(name = "AudioManager") {
            // just in case the other thread is still alive, wait for a bit
            try {
                Thread.sleep(50)
                init()
                while (!Engine.shutdown) {
                    // idk, just in case...
                    ALBase.alThread = Thread.currentThread()
                    ALBase.check()
                    val time = Time.nanoTime
                    try {
                        AudioTasks.workQueue()
                    } catch (e: Exception) {
                        // if(e.message != "ALException: Invalid Name") // why does the error happen???
                        e.printStackTrace()
                    }
                    ALBase.check()
                    onUpdate?.invoke(time)
                    ALBase.check()
                    if (!Engine.shutdown) {
                        // shall be destroyed by OpenAL itself -> false
                        sleepABit(false)
                    }
                    checkIsDestroyed()
                }
                destroy()
            } catch (_: InterruptedException) {
                // ignore
            }
        }
    }

    private var lastCheckedTime = 0L
    private var lastDeviceHash = 0
    private val queryBuffer = intArrayOf(0)
    private fun checkIsDestroyed() {
        // to do detect if the primary audio device was changed by the user?
        // it works nice currently

        // todo possibilities to select/change audio device manually

        val time = Time.nanoTime
        if (abs(time - lastCheckedTime) > 500 * MILLIS_TO_NANOS) {
            lastCheckedTime = time
            // 0.1ms -> it would be fine to even check it every time
            // we could consider only playback devices, but realistically the audio config shouldn't change often
            val hash = audioDeviceHash()
            // val t1 = Time.nanoTime
            if (hash != lastDeviceHash) {
                LOGGER.info("Devices changed -> Reconnecting")
                val needsReconnect = lastDeviceHash != 0
                lastDeviceHash = hash
                if (needsReconnect) reconnect()
                return
            } else {
                // maybe it died anyways?...
                alcGetIntegerv(device, ALC_CONNECTED, queryBuffer)
                if (queryBuffer[0] == 0) {
                    LOGGER.warn("Audio playing device disconnected")
                    reconnect()
                }
            }
        }
    }

    fun reconnect() {
        // stop()
        destroy()
        init()
    }

    fun requestUpdate() {
        needsUpdate = true
    }

    fun init() {

        openALSession++ // new session
        ALBase.alThread = Thread.currentThread()

        // only returns "OpenAL Soft" on Windows -> relatively useless
        // LOGGER.info(alcGetString(0L, ALC_DEFAULT_DEVICE_SPECIFIER))
        var deviceCaps: ALCCapabilities

        var hasWarned = false
        while (true) {
            device = alcOpenDevice(null as ByteBuffer?)
            if (device != 0L) {
                deviceCaps = ALC.createCapabilities(device)
                context = alcCreateContext(device, null as IntBuffer?)
                if (context != 0L) break
            }
            if (!hasWarned) {
                LOGGER.warn(
                    if (device == 0L) "Failed to open default OpenAL device"
                    else "Failed to create OpenAL context"
                )
                hasWarned = true
            }
            Thread.sleep(500)
        }

        alcMakeContextCurrent(context)
        AL.createCapabilities(deviceCaps)
        ALBase.check()

        LOGGER.info("Successfully created audio context")
    }

    fun destroy() {
        ALBase.check()
        alcCloseDevice(device)
        alcDestroyContext(context)
        device = 0L
        context = 0L
    }

    private val LOGGER = LogManager.getLogger(AudioManager::class)
}