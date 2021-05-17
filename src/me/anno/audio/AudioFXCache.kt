package me.anno.audio

import me.anno.audio.AudioPools.FAPool
import me.anno.audio.AudioPools.SAPool
import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundEffect.Companion.copy
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.SoundPipeline.Companion.changeDomain
import me.anno.audio.effects.Time
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.io.FileReference
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.modes.LoopingState
import me.anno.utils.Maths.clamp
import me.anno.utils.hpc.ProcessingQueue
import me.anno.video.AudioCreator.Companion.playbackSampleRate
import me.anno.video.FFMPEGMetadata
import org.apache.logging.log4j.LogManager
import java.util.concurrent.Semaphore
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

object AudioFXCache : CacheSection("AudioFX") {

    // limit the number of requests for performance,
    // and accumulating many for the future is useless,
    // when the user is just scrolling through time
    private const val rangeRequestLimit = 8

    data class EffectKey(val effect: SoundEffect, val data: Any, val previous: EffectKey?)

    data class PipelineKey(
        val file: FileReference,
        val time0: Time, val time1: Time, val bufferSize: Int,
        val is3D: Boolean,
        val audioAlphaSerialized: String, val repeat: LoopingState, val effectKey: EffectKey?
    ) {

        val hashCode = calculateHashCode()
        override fun hashCode(): Int {
            return hashCode
        }

        val previousKey = if (effectKey == null) null
        else PipelineKey(

            file, time0, time1, bufferSize, is3D,
            audioAlphaSerialized, repeat, effectKey.previous
        )

        fun withDelta(deltaIndex: Int): PipelineKey {
            if(deltaIndex == 0) return this
            TODO()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is PipelineKey) return false

            if (hashCode != other.hashCode) return false
            if (bufferSize != other.bufferSize) return false
            if (is3D != other.is3D) return false
            if (time0 != other.time0) return false
            if (time1 != other.time1) return false
            if (audioAlphaSerialized != other.audioAlphaSerialized) return false
            if (repeat != other.repeat) return false
            if (effectKey != other.effectKey) return false
            if (file != other.file) return false

            return true
        }

        private fun calculateHashCode(): Int {
            var result = bufferSize.hashCode()
            result = 31 * result + file.hashCode()
            result = 31 * result + time0.hashCode()
            result = 31 * result + time1.hashCode()
            result = 31 * result + is3D.hashCode()
            result = 31 * result + audioAlphaSerialized.hashCode()
            result = 31 * result + repeat.hashCode()
            result = 31 * result + (effectKey?.hashCode() ?: 0)
            result = 31 * result + (previousKey?.hashCode() ?: 0)
            return result
        }

    }

    class AudioData(
        val key: PipelineKey,
        var timeLeft: FloatArray?,
        var freqLeft: FloatArray?,
        var timeRight: FloatArray?,
        var freqRight: FloatArray?
    ) : ICacheData {

        override fun equals(other: Any?): Boolean {
            return other === this || (other is AudioData && other.key == key)
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }

        constructor(key: PipelineKey, left: FloatArray, right: FloatArray, timeDomain: Boolean) : this(
            key,
            if (timeDomain) left else null,
            if (timeDomain) null else left,
            if (timeDomain) right else null,
            if (timeDomain) null else right
        )

        constructor(key: PipelineKey, left: FloatArray, right: FloatArray, domain: Domain) : this(
            key, left, right, domain == Domain.TIME_DOMAIN
        )

        var isDestroyed = 0L
        override fun destroy() {
            // println("Destroying ${hashCode()} $key")
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

        fun getBuffersOfDomain(domain: Domain): Pair<FloatArray, FloatArray> {
            // if (isDestroyed > 0L) throw IllegalAccessException("Value was already destroyed")
            val buffer = this
            var left = if (domain == Domain.FREQUENCY_DOMAIN) buffer.freqLeft else buffer.timeLeft
            var right = if (domain == Domain.FREQUENCY_DOMAIN) buffer.freqRight else buffer.timeRight
            if (left == null) {
                left = if (domain == Domain.TIME_DOMAIN) buffer.freqLeft else buffer.timeLeft
                left!!
                val other = if (domain == Domain.TIME_DOMAIN) Domain.FREQUENCY_DOMAIN else Domain.TIME_DOMAIN
                val left2 = FloatArray(left.size) // FAPool[left.size]
                copy(left, left2)
                changeDomain(domain, other, left2)
                left = left2
                if (domain == Domain.TIME_DOMAIN) buffer.timeLeft = left else buffer.freqLeft = left
            }
            if (right == null) {
                right = if (domain == Domain.TIME_DOMAIN) buffer.freqRight else buffer.timeRight
                right!!
                val other = if (domain == Domain.TIME_DOMAIN) Domain.FREQUENCY_DOMAIN else Domain.TIME_DOMAIN
                val right2 = FloatArray(right.size) // FAPool[right.size]
                copy(right, right2)
                changeDomain(domain, other, right2)
                right = right2
                if (domain == Domain.TIME_DOMAIN) buffer.timeRight = right else buffer.freqRight = right
            }
            return left to right
        }

    }

    fun getBuffer(
        source: Audio,
        destination: Camera,
        pipelineKey: PipelineKey,
        domain: Domain,
        async: Boolean
    ): Pair<FloatArray, FloatArray>? {
        val buffer = getBuffer(source, destination, pipelineKey, async) ?: return null
        return buffer.getBuffersOfDomain(domain)
    }

    // limit the calls to this function, at max 32 simultaneously
    // this fixes the running out of memory issues from 13-15th May 2021
    // I don't know where these problems came from... in Release 1.1.2, they were fine
    private val rawDataLimiter = Semaphore(32)
    fun getRawData(
        source: Audio,
        destination: Camera,
        key: PipelineKey
    ): AudioData = getRawData(source.forcedMeta!!, source, destination, key)

    fun getRawData(
        meta: FFMPEGMetadata,
        source: Audio?,
        destination: Camera?,
        key: PipelineKey
    ): AudioData {
        // we cannot simply return null from this function, so getEntryLimited isn't an option
        rawDataLimiter.acquire()
        val entry = getEntry(key to "", timeout, false) {
            val stream = AudioStreamRaw(
                key.file, key.repeat,
                meta, key.is3D,
                source, destination
            )
            val pair = stream.getBuffer(key.bufferSize, key.time0.globalTime, key.time1.globalTime)
            AudioData(key, pair.first, pair.second, Domain.TIME_DOMAIN)
        } as AudioData
        rawDataLimiter.release()
        return entry
    }

    fun getBuffer0(
        meta: FFMPEGMetadata,
        source: Audio?,
        destination: Camera?,
        pipelineKey: PipelineKey,
        async: Boolean
    ): AudioData? {
        val effectKey = pipelineKey.effectKey
        if (effectKey != null) throw IllegalStateException()
        return getEntry(pipelineKey, timeout, async) { key ->
            getRawData(meta, source, destination, key)
        } as? AudioData
    }

    fun getBuffer0(
        meta: FFMPEGMetadata,
        pipelineKey: PipelineKey,
        async: Boolean
    ): AudioData? = getBuffer0(meta, null, null, pipelineKey, async)

    fun getBuffer(
        source: Audio,
        destination: Camera,
        pipelineKey: PipelineKey,
        async: Boolean
    ): AudioData? {
        return getEntry(pipelineKey, timeout, async) { key ->
            val effectKey = key.effectKey
            if (effectKey == null) {
                // get raw data
                getRawData(source, destination, key)
            } else {
                // get previous data, and process it
                val effect = effectKey.effect
                val previousKey = pipelineKey.previousKey!!
                val left = FAPool[bufferSize, true]
                val right = FAPool[bufferSize, true]
                val cachedSolutions = HashMap<Int, Pair<FloatArray, FloatArray>>()
                effect.apply({ deltaIndex ->
                    cachedSolutions.getOrPut(deltaIndex) {
                        getBuffer(source, destination, previousKey.withDelta(deltaIndex), effect.inputDomain, false)!!
                    }.first
                }, left, source, destination, key.time0, key.time1)
                effect.apply({ deltaIndex ->
                    cachedSolutions.getOrPut(deltaIndex) {
                        getBuffer(source, destination, previousKey.withDelta(deltaIndex), effect.inputDomain, false)!!
                    }.second
                }, right, source, destination, key.time0, key.time1)
                AudioData(key, left, right, effect.outputDomain)
            }
        } as? AudioData
    }

    fun getBuffer(
        source: Audio, destination: Camera,
        time0: Time, time1: Time,
        bufferSize: Int,
        async: Boolean
    ) = getBuffer(source, destination, getKey(source, destination, time0, time1, bufferSize), async)

    fun getBuffer(
        source: Audio, destination: Camera,
        time0: Time, time1: Time, bufferSize: Int,
        domain: Domain,
        async: Boolean
    ) = getBuffer(source, destination, getKey(source, destination, time0, time1, bufferSize), domain, async)

    fun getBuffer(index: Long, stream: AudioStream, async: Boolean): Pair<FloatArray, FloatArray>? {
        val t0 = stream.getTime(index)
        val t1 = stream.getTime(index + 1)
        return getBuffer(stream.source, stream.destination, t0, t1, bufferSize, Domain.TIME_DOMAIN, async)
    }

    val SPLITS = 256

    fun getRange(
        bufferSize: Int,
        t0: Double,
        t1: Double,
        identifier: String,
        audio: Audio,
        destination: Camera,
        async: Boolean = true
    ): ShortArray? {
        val index0 = (t0 * playbackSampleRate).roundToLong()// and (bufferSize-1).inv().toLong()
        var index1 = (t1 * playbackSampleRate).roundToLong()
        index1 = StrictMath.max(index1, index0 + SPLITS)
        // what if dt is too large, because we are viewing it from a distance -> approximate
        return getRange(bufferSize, index0, index1, identifier, audio, destination, async)
    }

    private fun getTime(index: Long, audio: Audio): Time {
        val globalTime = index * bufferSize.toDouble() / playbackSampleRate
        val localTime = audio.getLocalTimeFromRoot(globalTime, false)
        return Time(localTime, globalTime)
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
        bufferSize: Int,
        index0: Long,
        index1: Long,
        identifier: String,
        audio: Audio,
        destination: Camera,
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
                    val speed = 1.0
                    val deltaIndex = index1 - index0
                    val index0i = index0 + deltaIndex * split / splits
                    val index1i = StrictMath.min(index0i + 256, index0 + deltaIndex * (split + 1) / splits)
                    for (i in index0i until index1i) {

                        val bufferIndex = Math.floorDiv(i, bufferSize.toLong())
                        if (i == index0 || lastBufferIndex != bufferIndex) {
                            val time0 = getTime(bufferIndex, audio)
                            val time1 = getTime(bufferIndex + 1, audio)
                            buffer = getBuffer(
                                audio, destination, time0, time1, bufferSize,
                                Domain.TIME_DOMAIN, false
                            )!!
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
        source: Audio, destination: Camera,
        time0: Time, time1: Time, bufferSize: Int
    ): PipelineKey {
        var effectKeyI: EffectKey? = null
        val pipeline = source.pipeline
        for (effect in pipeline.effects) {
            val state = effect.getStateAsImmutableKey(source, destination, time0, time1)
            effectKeyI = EffectKey(effect, state, effectKeyI)
        }
        return PipelineKey(
            source.file, time0, time1, bufferSize, source.is3D,
            "${source.amplitude},${source.color}",
            source.isLooping.value,
            effectKeyI
        )
    }

    fun getIndex(time: Double, bufferSize: Int, sampleRate: Int): Double {
        return time * sampleRate.toDouble() / bufferSize
    }

    fun getTime(index: Long, bufferSize: Int, sampleRate: Int): Double {
        return index * bufferSize.toDouble() / sampleRate
    }

    fun getTime(index: Long, fraction: Double, bufferSize: Int, sampleRate: Int): Double {
        return (index + fraction) * bufferSize.toDouble() / sampleRate
    }

    fun printFloats(floats: FloatArray) {
        var str = "${floats.size}x ["
        if (floats.isNotEmpty()) {
            val first = floats.first()
            val last = floats.last()
            val firstIndex = floats.indexOfFirst { it != first }
            val lastIndex = floats.indexOfLast { it != last }
            str += "$firstIndex * $first, [${lastIndex - firstIndex}, avg: ${floats.average()}], ${floats.size - lastIndex} * $last"
        }
        str += "]"
        println(str)
    }

    private const val timeout = 20_000L // audio needs few memory, so we can keep all recent audio
    private val LOGGER = LogManager.getLogger(AudioFXCache::class)

}