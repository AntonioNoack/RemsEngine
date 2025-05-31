package me.anno.audio.streams

import me.anno.audio.streams.AudioStreamRaw.Companion.bufferSize
import me.anno.maths.Maths.posMod
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertTrue
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.structures.lists.PairArrayList
import java.lang.Math.floorDiv
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

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
        val byteBufferPool = ByteBufferPool(32)

        @JvmStatic
        fun getIndex(globalTime: Double, speed: Double, playbackSampleRate: Int): Long {
            val progressedSamples = ((globalTime / speed) * playbackSampleRate).toLong()
            return floorDiv(progressedSamples, bufferSize.toLong())
        }

        @JvmStatic
        fun getFraction(globalTime: Double, speed: Double, playbackSampleRate: Int): Long {
            val progressedSamples = ((globalTime / speed) * playbackSampleRate).toLong()
            return posMod(progressedSamples, bufferSize.toLong())
        }
    }

    private val filledBuffers = PairArrayList<ShortBuffer, ByteBuffer>(8)

    /**
     * Blocks until a new buffer is available;
     * Whoever calls this function must return the ByteBuffer to AudioStream.byteBufferPool!!
     * */
    fun getNextBuffer(): Pair<ShortBuffer, ByteBuffer> {
        Sleep.waitUntil(true) { filledBuffers.isNotEmpty() }
        synchronized(filledBuffers) {
            assertTrue(filledBuffers.isNotEmpty())
            val stereo = filledBuffers.getFirst(0)
            val bytes = filledBuffers.getSecond(0)
            filledBuffers.removeAt(0, keepOrder = true)
            return stereo to bytes
        }
    }

    /**
     * Returns whether sb0 can be freed immediately after.
     * */
    open fun onBufferFilled(
        stereoBuffer: ShortBuffer, byteBuffer: ByteBuffer,
        bufferIndex: Long, session: Int
    ): Boolean {
        assertNotNull(stereoBuffer)
        synchronized(filledBuffers) {
            filledBuffers.add(stereoBuffer, byteBuffer)
        }
        return false
    }

    open fun frameIndexToTime(index: Long): Double = (index * bufferSize * speed) / playbackSampleRate

    var isWaitingForBuffer = false

    var isPlaying = false

    abstract fun getBuffer(bufferIndex: Long): Pair<ShortArray?, ShortArray?>

    fun requestNextBuffer(bufferIndex: Long, session: Int) {
        isWaitingForBuffer = true
        taskQueue += {// load all data async
            requestNextBufferImpl(bufferIndex, session)
        }
    }

    private fun requestNextBufferImpl(bufferIndex: Long, session: Int) {

        val bufferSize = bufferSize
        val size = bufferSize * 2 * (if (stereo) 2 else 1)
        val sb0 = byteBufferPool[size, false, true]
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
            byteBufferPool.returnBuffer(sb0)
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