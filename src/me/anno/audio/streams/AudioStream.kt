package me.anno.audio.streams

import me.anno.audio.streams.AudioStreamRaw.Companion.bufferSize
import me.anno.utils.Sleep
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.ByteBufferPool
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicInteger

abstract class AudioStream(
    val speed: Double,
    val playbackSampleRate: Int = 48000,
    val left: Boolean, val center: Boolean, val right: Boolean
) {

    val stereo = left && right && !center

    companion object {

        @JvmField
        val taskQueue = ProcessingGroup("AudioStream", 0.5f)

        @JvmField
        val bufferPool = ByteBufferPool(32)

        @JvmStatic
        fun getIndex(globalTime: Double, speed: Double, playbackSampleRate: Int): Long {
            val progressedSamples = ((globalTime / speed) * playbackSampleRate).toLong()
            return progressedSamples.floorDiv(bufferSize.toLong())
        }

        @JvmStatic
        fun getFraction(globalTime: Double, speed: Double, playbackSampleRate: Int): Long {
            val progressedSamples = ((globalTime / speed) * playbackSampleRate).toLong()
            val bs = bufferSize.toLong()
            return progressedSamples - progressedSamples.floorDiv(bs) * bs
        }
    }

    private val filledBuffers = ArrayList<ShortBuffer?>(8)
    private val gettingIndex = AtomicInteger()

    /**
     * waits until a new buffer is available;
     * whoever calls this function must return the buffer!!
     * */
    fun getNextBuffer(): ShortBuffer {
        val index = gettingIndex.getAndIncrement()
        Sleep.waitUntil(true) { filledBuffers.size > index }
        return filledBuffers.set(index, null)!!
    }

    open fun onBufferFilled(stereoBuffer: ShortBuffer, sb0: ByteBuffer, bufferIndex: Long, session: Int): Boolean {
        filledBuffers.add(stereoBuffer)
        return false
    }

    open fun frameIndexToTime(index: Long): Double = (index * bufferSize * speed) / playbackSampleRate

    var isWaitingForBuffer = false

    var isPlaying = false

    abstract fun getBuffer(bufferIndex: Long): Pair<ShortArray?, ShortArray?>

    fun requestNextBuffer(bufferIndex: Long, session: Int) {

        isWaitingForBuffer = true
        taskQueue += {// load all data async

            val bufferSize = bufferSize
            val size = bufferSize * 2 * (if (stereo) 2 else 1)
            val sb0 = bufferPool[size, false, true]
                .order(ByteOrder.nativeOrder())
            val stereoBuffer = sb0.asShortBuffer()

            when {
                center -> {
                    val (left, right) = getBuffer(bufferIndex)
                    if (left != null && right != null) {
                        for (i in 0 until bufferSize) {
                            stereoBuffer.put(floatToShort((left[i] + right[i]) * 0.5f))
                        }
                    }
                }
                stereo -> {
                    val (left, right) = getBuffer(bufferIndex)
                    if (left != null && right != null) {
                        for (i in 0 until bufferSize) {
                            stereoBuffer.put(left[i])
                            stereoBuffer.put(right[i])
                        }
                    }
                }
                left -> {
                    val (left, _) = getBuffer(bufferIndex)
                    if (left != null) {
                        for (i in 0 until bufferSize) {
                            stereoBuffer.put(left[i])
                        }
                    }
                }
                else -> {
                    val (_, right) = getBuffer(bufferIndex)
                    if (right != null) {
                        for (i in 0 until bufferSize) {
                            stereoBuffer.put(right[i])
                        }
                    }
                }
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
}