package me.anno.audio

import me.anno.objects.Audio
import me.anno.objects.LoopingState
import me.anno.objects.cache.Cache
import me.anno.utils.mix
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import me.anno.video.FFMPEGStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

// only play once, then destroy; it makes things easier
// (on user input and when finally rendering only)

abstract class AudioStream(val file: File, val repeat: LoopingState, val startTime: Double, val meta: FFMPEGMetadata, val playbackSampleRate: Int = 48000){

    constructor(audio: Audio, speed: Double, globalTime: Double, playbackSampleRate: Int):
            this(audio.file, audio.isLooping, 0.0, getMeta(audio.file, false)!!, playbackSampleRate){
        configure(audio, speed, globalTime)
    }

    fun configure(audio: Audio, speed: Double, globalTime: Double){
        globalToLocalTime = { time -> audio.getGlobalTransform(time * speed + globalTime).second }
        val amplitude = audio.amplitude
        localAmplitude = { time -> amplitude[time] }
    }

    val minPerceptibleAmplitude = 1f/32500f

    val ffmpegSampleRate = meta.audioSampleRate

    val maxSampleIndex = meta.audioSampleCount

    val ffmpegSliceSampleDuration = 10.0 // seconds, 10s of music
    val ffmpegSliceSampleCount get() = (ffmpegSampleRate * ffmpegSliceSampleDuration).toInt()

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    companion object {
        val playbackSliceDuration = 1.0
    }

    // map the real time to the correct time xD
    // to do allow skipping and such -> no, too much cleanup ;)

    // var time = 0f

    var isWaitingForBuffer = AtomicBoolean(false)

    var isPlaying = false

    var globalToLocalTime = { globalTime: Double -> globalTime }
    var localAmplitude = { localtime: Double -> 1f }

    val buffers = ArrayList<SoundBuffer>()

    data class AudioSliceKey(val file: File, val slice: Long)

    fun getAmplitudesSync(index: Double): Pair<Float, Float> {
        if(index < 0f) return 0f to 0f
        // multiply by local time dependent amplitude
        val localAmplitude = localAmplitude(index / ffmpegSampleRate)
        if(localAmplitude < minPerceptibleAmplitude) return 0f to 0f
        val i0 = index.toLong()
        val data0 = getMaxAmplitudesSync(i0)
        val data1 = if(index.toInt().toDouble() == index){ // <3, data doesn't need to be interpolated
            return data0.first * localAmplitude to data0.second * localAmplitude
        } else getMaxAmplitudesSync(i0+1)
        val f = (index - i0).toFloat() // sollte ok sein; hohe Präzession ist hier nicht notwendig
        return mix(data0.first, data1.first, f) * localAmplitude to
                mix(data0.second, data1.second, f) * localAmplitude
    }

    fun getMaxAmplitudesSync(index: Long): Pair<Short, Short> {
        if(index < 0 || (repeat == LoopingState.PLAY_ONCE && index >= maxSampleIndex)) return 0.toShort() to 0.toShort()
        val index = repeat[index, maxSampleIndex]
        // val index = if(repeat) index % maxSampleIndex else index
        val sliceIndex = index / ffmpegSliceSampleCount
        val localIndex = (index % ffmpegSliceSampleCount).toInt()
        val arrayIndex0 = localIndex * 2 // for stereo
        val sliceTime = sliceIndex * ffmpegSliceSampleDuration
        val soundBuffer = Cache.getEntry(AudioSliceKey(file, sliceIndex), (ffmpegSliceSampleDuration * 2 * 1000).toLong(), false){
            val sequence = FFMPEGStream.getAudioSequence(file, sliceTime, ffmpegSliceSampleDuration, ffmpegSampleRate)
            var buffer: SoundBuffer?
            while(true){
                buffer = sequence.soundBuffer
                if(buffer != null) break
                // somebody else needs to work on the queue
                Thread.sleep(0, 100_000) // wait 0.1ms
            }
            buffer!!
        } as SoundBuffer
        val data = soundBuffer.pcm!!
        return data[arrayIndex0] to data[arrayIndex0+1]
    }

    fun requestNextBuffer(startTime: Double, bufferIndex: Long){

        isWaitingForBuffer.set(true)
        thread {// load all data async

            // println("[INFO:AudioStream] Working on buffer $queued")

            // todo speed up for 1:1 playback
            // todo cache sound buffer for 1:1 playback
            // (superfluous calculations)

            // time += dt
            val sampleCount = (playbackSampleRate * playbackSliceDuration).toInt()

            // todo get higher/lower quality, if it's sped up/slowed down?
            // rare use-case...
            // slow motion may be a use case, for which it's worth to request 96kHz or more
            // sound recorded at 0.01x speed is really rare, and at the edge (10Hz -> 10.000Hz)
            // slower frequencies can't be that easily recorded (besides the song/noise of wind (alias air pressure zones changing))

            val dtx = playbackSliceDuration / sampleCount
            val ffmpegSampleRate = ffmpegSampleRate
            val globalToLocalTime = globalToLocalTime
            val localAmplitude = localAmplitude

            val global0 = startTime
            val local0 = globalToLocalTime(global0)
            var index0 = ffmpegSampleRate * local0

            val byteBuffer = ByteBuffer.allocateDirect(sampleCount * 2 * 2)
                .order(ByteOrder.nativeOrder())
            val stereoBuffer = byteBuffer.asShortBuffer()

            for(sampleIndex in 0 until sampleCount){

                val global1 = startTime + (sampleIndex + 1) * dtx
                val local1 = globalToLocalTime(global1)

                val index1 = ffmpegSampleRate * local1

                // average values from index0 to index1
                val mni = min(index0, index1)
                val mxi = max(index0, index1)

                val mnI = mni.toLong()
                val mxI = mxi.toLong()

                val a0: Double
                val a1: Double

                when {
                    mni == mxi -> {
                        val data = getAmplitudesSync(mni)
                        a0 = data.first.toDouble()
                        a1 = data.second.toDouble()
                    }
                    mnI == mxI -> {
                        // from the same index, so 50:50
                        val data0 = getAmplitudesSync(mni)
                        val data1 = getAmplitudesSync(mxi)
                        a0 = 0.5 * (data0.first + data1.first)
                        a1 = 0.5 * (data0.second + data1.second)
                    }
                    else -> {
                        // sampling from all values
                        // (slow motion sound effects)
                        val data0 = getAmplitudesSync(mni)
                        val data1 = getAmplitudesSync(mxi)
                        val f0 = 1f - (mni - mnI) // x.2f -> 0.8f
                        val f1 = mxi - mxI // x.2f -> 0.2f
                        var b0 = data0.first * f0 + data1.first * f1
                        var b1 = data0.second * f0 + data1.second * f1
                        for(index in mnI+1 until mxI){
                            val time = index.toDouble() / ffmpegSampleRate
                            val data = getMaxAmplitudesSync(index)
                            val amplitude = localAmplitude(time)
                            b0 += amplitude * data.first
                            b1 += amplitude * data.second
                        }
                        val dt = mxi - mni
                        // average the values over the time span
                        a0 = b0 / dt
                        a1 = b1 / dt
                    }
                }

                // write the data
                stereoBuffer.put(a0.toShort())
                stereoBuffer.put(a1.toShort())

                // global0 = global1
                // local0 = local1
                index0 = index1

            }

            stereoBuffer.position(0)

            // 1:1 playback
            /*val soundBuffer = Cache.getEntry(AudioSliceKey(file, (time/dt).roundToInt()), 1000){
                val sequence = FFMPEGStream.getAudioSequence(file, time, dt, sampleRate)
                var buffer: SoundBuffer?
                while(true){
                    buffer = sequence.soundBuffer
                    if(buffer != null) break
                    // somebody else needs to work on the queue
                    Thread.sleep(10)
                }
                buffer!!
            } as SoundBuffer*/

            onBufferFilled(stereoBuffer, bufferIndex)

        }

    }

    abstract fun onBufferFilled(stereoBuffer: ShortBuffer, bufferIndex: Long)

}