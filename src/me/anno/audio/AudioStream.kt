package me.anno.audio

import me.anno.animation.LoopingState
import me.anno.audio.AudioStreamRaw.Companion.bufferSize
import me.anno.io.files.FileReference
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.ByteBufferPool
import me.anno.video.ffmpeg.FFMPEGMetadata
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean

// only play once, then destroy; it makes things easier
// (on user input and when finally rendering only)

// done viewing the audio levels is more important than effects
// done especially editing the audio levels is important (amplitude)


// idk does not work, if the buffers aren't filled fast enough -> always fill them fast enough...
// idk or restart playing...

/**
 * todo audio effects:
 * done better echoing ;)
 * todo velocity frequency change
 * done pitch
 * todo losing high frequencies in the distance
 * done audio becoming quiet in the distance
 * */
abstract class AudioStream(
    val file: FileReference,
    val repeat: LoopingState,
    var startIndex: Long,
    val meta: FFMPEGMetadata,
    val speed: Double,
    val playbackSampleRate: Int = 48000
) {

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    companion object {

        val taskQueue = ProcessingGroup("AudioStream", 0.5f)

        val bufferPool = ByteBufferPool(32, true)

        fun getIndex(globalTime: Double, speed: Double, playbackSampleRate: Int): Long {
            val progressedSamples = ((globalTime / speed) * playbackSampleRate).toLong()
            return Math.floorDiv(progressedSamples, bufferSize.toLong())
        }

        fun getFraction(globalTime: Double, speed: Double, playbackSampleRate: Int): Long {
            val progressedSamples = ((globalTime / speed) * playbackSampleRate).toLong()
            return Math.floorMod(progressedSamples, bufferSize.toLong())
        }

    }

    open fun getTimeD(index: Long): Double = (index * bufferSize * speed) / playbackSampleRate

    var isWaitingForBuffer = AtomicBoolean(false)

    var isPlaying = false

    open fun getBuffer(bufferIndex: Long): Pair<FloatArray, FloatArray> {
        return AudioFXCache.getBuffer(bufferIndex, this, false)!!
    }

    fun requestNextBuffer(bufferIndex: Long, session: Int) {

        isWaitingForBuffer.set(true)
        taskQueue += {// load all data async

            val sb0 = bufferPool[bufferSize * 2 * 2, false]
                .order(ByteOrder.nativeOrder())
            val stereoBuffer = sb0.asShortBuffer()

            val (left, right) = getBuffer(bufferIndex)
            for (i in 0 until bufferSize) {
                stereoBuffer.put(floatToShort(left[i]))
                stereoBuffer.put(floatToShort(right[i]))
            }

            stereoBuffer.position(0)

            if (onBufferFilled(stereoBuffer, sb0, bufferIndex, session)) {
                bufferPool.returnBuffer(sb0)
            }

        }

    }

    /**
     * the usual function calls d.toInt().toShort(),
     * which causes breaking from max to -max, which ruins audio quality (cracking)
     * this fixes that :)
     * */
    private fun floatToShort(d: Float): Short {
        return when {
            d >= 32767f -> 32767
            d >= -32768f -> d.toInt().toShort()
            else -> -32768
        }
    }

    /**
     * has to return, whether the buffer can be freed
     * */
    abstract fun onBufferFilled(stereoBuffer: ShortBuffer, sb0: ByteBuffer, bufferIndex: Long, session: Int): Boolean

}