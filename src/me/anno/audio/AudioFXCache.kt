package me.anno.audio

import me.anno.animation.LoopingState
import me.anno.audio.AudioPools.SAPool
import me.anno.audio.AudioStreamRaw.Companion.bufferSize
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.utils.Sleep.acquire
import me.anno.utils.hpc.ProcessingQueue
import me.anno.video.AudioCreator.Companion.playbackSampleRate
import me.anno.video.ffmpeg.FFMPEGMetadata
import java.util.concurrent.Semaphore
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

object AudioFXCache : CacheSection("AudioFX0") {

    // limit the number of requests for performance,
    // and accumulating many for the future is useless,
    // when the user is just scrolling through time
    private const val rangeRequestLimit = 8

    val SPLITS = 256

    data class PipelineKey(
        val file: FileReference,
        val time0: Double,
        val time1: Double,
        val bufferSize: Int,
        val repeat: LoopingState,
    ) {

        val hashCode = calculateHashCode()
        override fun hashCode(): Int {
            return hashCode
        }

        fun withDelta(deltaIndex: Int): PipelineKey {
            if (deltaIndex == 0) return this
            TODO()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is PipelineKey) return false

            if (hashCode != other.hashCode) return false
            if (bufferSize != other.bufferSize) return false
            if (time0 != other.time0) return false
            if (time1 != other.time1) return false
            if (repeat != other.repeat) return false
            if (file != other.file) return false

            return true
        }

        private fun calculateHashCode(): Int {
            var result = bufferSize.hashCode()
            result = 31 * result + file.hashCode()
            result = 31 * result + time0.hashCode()
            result = 31 * result + time1.hashCode()
            result = 31 * result + repeat.hashCode()
            return result
        }

    }

    class AudioData(
        val key: PipelineKey,
        var timeLeft: FloatArray,
        var timeRight: FloatArray
    ) : ICacheData {

        override fun equals(other: Any?): Boolean {
            return other === this || (other is AudioData && other.key == key)
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }

        var isDestroyed = 0L
        override fun destroy() {
            // LOGGER.info("Destroying ${hashCode()} $key")
            // printStackTrace()
            GFX.checkIsGFXThread()
            // todo why is it being destroyed twice????
            /*if (isDestroyed > 0L){
                Engine.shutdown()
                throw IllegalStateException("Cannot destroy twice, now $gameTime, then: $isDestroyed!")
            }*/
            isDestroyed = gameTime
            /*FAPool.returnBuffer(timeLeft)
            FAPool.returnBuffer(freqLeft)
            FAPool.returnBuffer(timeRight)
            FAPool.returnBuffer(freqRight)*/
        }

    }

    fun getBuffer(
        pipelineKey: PipelineKey,
        async: Boolean
    ): Pair<FloatArray, FloatArray>? {
        val buffer = getBuffer1(pipelineKey, async) ?: return null
        return Pair(buffer.timeLeft, buffer.timeRight)
    }

    // limit the calls to this function, at max 32 simultaneously
    // this fixes the running out of memory issues from 13-15th May 2021
    // I don't know where these problems came from... in Release 1.1.2, they were fine
    private val rawDataLimiter = Semaphore(32)

    fun getRawData(
        meta: FFMPEGMetadata,
        key: PipelineKey
    ): AudioData {
        // we cannot simply return null from this function, so getEntryLimited isn't an option
        acquire(true, rawDataLimiter)
        val entry = getEntry(key to "", timeout, false) {
            val stream = AudioStreamRaw(
                key.file, key.repeat,
                meta
            )
            val pair = stream.getBuffer(key.bufferSize, key.time0, key.time1)
            AudioData(key, pair.first, pair.second)
        } as AudioData
        rawDataLimiter.release()
        return entry
    }

    fun getBuffer0(
        meta: FFMPEGMetadata,
        pipelineKey: PipelineKey,
        async: Boolean
    ): AudioData? {
        return getEntry(pipelineKey, timeout, async) { key ->
            getRawData(meta, key)
        } as? AudioData
    }

    fun getBuffer1(
        pipelineKey: PipelineKey,
        async: Boolean
    ): AudioData? {
        return getEntry(pipelineKey, timeout, async) { key ->
            getRawData(FFMPEGMetadata.getMeta(key.file, false)!!, key)
        } as? AudioData
    }

    fun getBuffer(
        file: FileReference,
        time0: Double,
        time1: Double,
        bufferSize: Int,
        repeat: LoopingState,
        async: Boolean
    ) = getBuffer(getKey(file, time0, time1, bufferSize, repeat), async)

    fun getBuffer(
        index: Long,
        stream: AudioStream,
        async: Boolean
    ): Pair<FloatArray, FloatArray>? {
        val t0 = stream.getTimeD(index)
        val t1 = stream.getTimeD(index + 1)
        return getBuffer(stream.file, t0, t1, bufferSize, stream.repeat, async)
    }

    fun getRange(
        file: FileReference,
        bufferSize: Int,
        t0: Double,
        t1: Double,
        repeat: LoopingState,
        identifier: String,
        async: Boolean = true
    ): ShortArray? {
        val index0 = (t0 * playbackSampleRate).roundToLong()// and (bufferSize-1).inv().toLong()
        var index1 = (t1 * playbackSampleRate).roundToLong()
        index1 = StrictMath.max(index1, index0 + SPLITS)
        // what if dt is too large, because we are viewing it from a distance -> approximate
        return getRange(file, bufferSize, index0, index1, repeat, identifier, async)
    }

    private fun getTime(index: Long): Double {
        return index * bufferSize.toDouble() / playbackSampleRate
    }

    private val rangingProcessing = ProcessingQueue("AudioFX")
    private val rangingProcessing2 = ProcessingQueue("AudioFX-2")

    data class RangeKey(val i0: Long, val i1: Long, val identifier: String) {

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is RangeKey) return false
            return other.i0 == i0 && other.i1 == i1 && other.identifier == identifier
        }

        val hashCode = 31 * (31 * i0.hashCode() + i1.hashCode()) + identifier.hashCode()
        override fun hashCode(): Int {
            return hashCode
        }

    }

    class ShortData : CacheData<ShortArray?>(null) {
        override fun destroy() {
            SAPool.returnBuffer(value)
        }
    }

    fun getRange(
        file: FileReference,
        bufferSize: Int,
        index0: Long,
        index1: Long,
        repeat: LoopingState,
        identifier: String,
        async: Boolean = true
    ): ShortArray? {
        val queue = if (async) rangingProcessing2 else null
        val entry = getEntryLimited(RangeKey(index0, index1, identifier), 10000, queue, rangeRequestLimit) {
            val data = ShortData()
            rangingProcessing += {
                val splits = SPLITS
                val values = SAPool[splits * 2, false]
                var lastBufferIndex = 0L
                lateinit var buffer: Pair<FloatArray, FloatArray>
                val bufferSizeM1 = bufferSize - 1
                for (split in 0 until splits) {

                    var min = +1e5f
                    var max = -1e5f

                    val deltaIndex = index1 - index0
                    val index0i = index0 + deltaIndex * split / splits
                    val index1i = StrictMath.min(index0i + 256, index0 + deltaIndex * (split + 1) / splits)
                    for (i in index0i until index1i) {

                        val bufferIndex = Math.floorDiv(i, bufferSize.toLong())
                        if (i == index0 || lastBufferIndex != bufferIndex) {
                            val time0 = getTime(bufferIndex)
                            val time1 = getTime(bufferIndex + 1)
                            buffer = getBuffer(file, time0, time1, bufferSize, repeat, false)!!
                            lastBufferIndex = bufferIndex
                        }

                        val localIndex = i.toInt() and bufferSizeM1
                        val v0 = buffer.first[localIndex]
                        val v1 = buffer.second[localIndex]

                        min = StrictMath.min(min, v0)
                        min = StrictMath.min(min, v1)
                        max = StrictMath.max(max, v0)
                        max = StrictMath.max(max, v1)

                    }

                    val minInt = floor(min).toInt()
                    val maxInt = ceil(max).toInt()
                    values[split * 2 + 0] = clamp(minInt, -32768, 32767).toShort()
                    values[split * 2 + 1] = clamp(maxInt, -32768, 32767).toShort()

                }
                data.value = values
            }
            data
        } as? ShortData ?: return null
        return entry.value
    }

    fun getKey(
        file: FileReference,
        time0: Double, time1: Double, bufferSize: Int,
        repeat: LoopingState
    ) = PipelineKey(file, time0, time1, bufferSize, repeat)

    fun getIndex(time: Double, bufferSize: Int, sampleRate: Int): Double {
        return time * sampleRate.toDouble() / bufferSize
    }

    fun getTime(index: Long, bufferSize: Int, sampleRate: Int): Double {
        return index * bufferSize.toDouble() / sampleRate
    }

    fun getTime(index: Long, fraction: Double, bufferSize: Int, sampleRate: Int): Double {
        return (index + fraction) * bufferSize.toDouble() / sampleRate
    }

    private const val timeout = 20_000L // audio needs few memory, so we can keep all recent audio

}