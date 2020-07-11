package me.anno.audio

import me.anno.gpu.GFX
import me.anno.objects.cache.Cache
import me.anno.utils.mix
import me.anno.video.FFMPEGStream
import org.lwjgl.openal.AL10.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

// only play once, then destroy; it makes things easier
// (on user input and when finally rendering only)

class AudioStream(val file: File, val startTime: Float){

    val minPerceptibleAmplitude = 1f/32500f

    // todo use the audio-file sample rate
    // todo get meta information for length xD
    val sampleRate = 48000

    val ffmpegSliceSampleDuration = 10f // seconds, 10s of music
    val ffmpegSliceSampleCount = (sampleRate * ffmpegSliceSampleDuration).toInt()

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    val openALSliceDuration = 1f

    // map the real time to the correct time xD
    // to do allow skipping and such -> no, too much cleanup ;)

    var startTimeNanos = 0L
    val alSource = SoundSource(false, true)
    // var time = 0f

    var queued = AtomicInteger()
    var processed = 0
    var isWaitingForBuffer = AtomicBoolean(false)

    var isPlaying = false

    var globalToLocalTime = { globalTime: Float -> globalTime }
    var localAmplitude = { localTime: Float -> 1f }

    val buffers = ArrayList<SoundBuffer>()
    //val availableBuffers = ArrayList<SoundBuffer>()

    fun checkProcessed(){
        processed = alGetSourcei(alSource.sourcePtr, AL_BUFFERS_PROCESSED)
        ALBase.check()
    }

    fun start(){
        synchronized(this){
            if(!isPlaying){
                isPlaying = true
                startTimeNanos = System.nanoTime()
                waitForRequiredBuffers()
            }
        }
    }

    // not supported ;)
    /*fun unpause(){
        start(pauseTime)
    }

    fun pause(){
        if(!isPlaying) return
        pauseTime = (System.nanoTime() - startTime)*1e-9f
        isPlaying = false
        alSource.pause()
    }*/

    fun stop(){
        if(!isPlaying) return
        isPlaying = false
        alSource.stop()
        alSource.destroy()
        ALBase.check()
        // ALBase.check()
        // somehow crashes..., buffers can't be reused either (without error)
        // buffers.toSet().forEach { it.destroy() }
        // ALBase.check()
    }

    // must be only triggered on start()
    /*private fun seekTo(time: Float){
        this.time = time
    }*/

    data class AudioSliceKey(val file: File, val slice: Int)

    fun getAmplitudesSync(index: Float): Pair<Float, Float> {
        if(index < 0f) return 0f to 0f
        // multiply by local time dependent amplitude
        val localAmplitude = localAmplitude(index / sampleRate)
        if(localAmplitude < minPerceptibleAmplitude) return 0f to 0f
        val i0 = index.toInt()
        val data0 = getMaxAmplitudesSync(i0)
        val data1 = if(index.toInt().toFloat() == index){ // <3, data doesn't need to be interpolated
            return data0.first * localAmplitude to data0.second * localAmplitude
        } else getMaxAmplitudesSync(i0+1)
        val f = index - i0
        return mix(data0.first, data1.first, f) * localAmplitude to
                mix(data0.second, data1.second, f) * localAmplitude
    }

    fun getMaxAmplitudesSync(index: Int): Pair<Short, Short> {
        if(index < 0) return 0.toShort() to 0.toShort()
        val sliceIndex = index / ffmpegSliceSampleCount
        val localIndex = index % ffmpegSliceSampleCount
        val arrayIndex0 = localIndex * 2 // for stereo
        val sliceTime = sliceIndex * ffmpegSliceSampleDuration
        val soundBuffer = Cache.getEntry(AudioSliceKey(file, sliceIndex), (ffmpegSliceSampleDuration * 2 * 1000).toLong(), false){
            val sequence = FFMPEGStream.getAudioSequence(file, sliceTime, ffmpegSliceSampleDuration, sampleRate)
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

    fun requestNextBuffer(startTime: Float, bufferIndex: Int){

        isWaitingForBuffer.set(true)
        thread {// load all data async

            // println("[INFO:AudioStream] Working on buffer $queued")

            // todo speed up for 1:1 playback
            // todo cache sound buffer for 1:1 playback
            // (superfluous calculations)

            // time += dt
            val sampleCount = (sampleRate * openALSliceDuration).toInt()

            // todo get higher/lower quality, if it's sped up/slowed down?
            // rare use-case...
            // slow motion may be a use case, for which it's worth to request 96kHz or more
            // sound recorded at 0.01x speed is really rare, and at the edge (10Hz -> 10.000Hz)
            // slower frequencies can't be that easily recorded (besides the song/noise of wind (alias air pressure zones changing))

            val dtx = openALSliceDuration / sampleCount

            val global0 = startTime
            val local0 = globalToLocalTime(global0)
            var index0 = sampleRate * local0

            val byteBuffer = ByteBuffer.allocateDirect(sampleCount * 2 * 2)
                .order(ByteOrder.nativeOrder())
            val stereoBuffer = byteBuffer.asShortBuffer()

            for(sampleIndex in 0 until sampleCount){

                val global1 = startTime + (sampleIndex + 1) * dtx
                val local1 = globalToLocalTime(global1)

                val index1 = sampleRate * local1

                // average values from index0 to index1
                val mni = min(index0, index1)
                val mxi = max(index0, index1)

                val mnI = mni.toInt()
                val mxI = mxi.toInt()

                val a0: Float
                val a1: Float

                when {
                    mni == mxi -> {
                        val data = getAmplitudesSync(mni)
                        a0 = data.first
                        a1 = data.second
                    }
                    mnI == mxI -> {
                        // from the same index, so 50:50
                        val data0 = getAmplitudesSync(mni)
                        val data1 = getAmplitudesSync(mxi)
                        a0 = 0.5f * (data0.first + data1.first)
                        a1 = 0.5f * (data0.second + data1.second)
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
                            val time = index.toFloat() / sampleRate
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

            GFX.addAudioTask {
                val isFirstBuffer = bufferIndex == 0
                ALBase.check()
                val soundBuffer = SoundBuffer()
                ALBase.check()
                if(isFirstBuffer){
                    val dt = max(0f, (System.nanoTime() - startTimeNanos) * 1e-9f)
                    // println("skipping first $dt")
                    // 10s slices -> 2.6s
                    // 1s slices -> 0.55s
                    val samples = dt * sampleRate
                    val currentIndex = samples.toInt() * 2
                    // what if index > sampleCount? add empty buffer???...
                    val minPlayedSamples = 32 // not correct, but who cares ;) (our users care ssshhh)
                    val skipIndex = min(currentIndex, stereoBuffer.capacity() - 2 * minPlayedSamples)
                    if(skipIndex > 0){
                        stereoBuffer.position(skipIndex)
                    }
                }
                soundBuffer.loadRawStereo16(stereoBuffer, sampleRate)
                buffers.add(soundBuffer)
                ALBase.check()
                // println("Invalid Name? alSourceQueueBuffers(${alSource.sourcePtr}, ${soundBuffer.buffer})")
                alSourceQueueBuffers(alSource.sourcePtr, soundBuffer.buffer)
                ALBase.check()
                if(isFirstBuffer){
                    alSource.play()
                    ALBase.check()
                }
                // time += openALSliceDuration
                isWaitingForBuffer.set(false)
                ALBase.check()
                1
            }

        }

    }

    fun waitForRequiredBuffers() {
        if(!isPlaying) return
        val queued = queued.get()
        if(!isWaitingForBuffer.get() && queued > 0) checkProcessed()
        // keep 2 on reserve
        if(queued < processed+5 && !isWaitingForBuffer.get()){
            // request a buffer
            // only one at a time
            val index = this.queued.getAndIncrement()
            requestNextBuffer(startTime + openALSliceDuration * index, index)
        }
        thread {
            Thread.sleep(10)
            GFX.addAudioTask {
                waitForRequiredBuffers()
                ALBase.check()
                1
            }
        }
    }

}