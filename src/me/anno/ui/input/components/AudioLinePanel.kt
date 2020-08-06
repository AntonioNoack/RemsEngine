package me.anno.ui.input.components

import me.anno.audio.AudioStream
import me.anno.audio.SoundBuffer
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.objects.Audio
import me.anno.objects.cache.Cache
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.pow
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGStream
import java.lang.IndexOutOfBoundsException
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.max

// todo show current audio level in real time?

// always load all the audio?
// worst case: 24h * 3600s/h * 44kHz * 2 Channels * 16 Bit = 30 GB -> no
// -> we need to split the audio if it is too long :/
class AudioLinePanel(var meta: FFMPEGMetadata, val audio: Audio, style: Style): Panel(style){

    val textSize = style.getSize("textSize", 10)

    var isStereo = true

    var mod = 0 // left/right/avg
    var dt = 3.0
    var time0 = 0.0

    var lengthSeconds  = meta.duration
    var frequency = meta.audioSampleRate.toDouble()

    var minValue = IntArray(0)
    var maxValue = minValue
    var isValid = false

    val ffmpegSliceSampleDuration = 10.0
    var ffmpegSampleRate = meta.audioSampleRate
    var ffmpegSliceSampleCount = (ffmpegSampleRate * ffmpegSliceSampleDuration).toLong()
    var file = audio.file
    var lastW = 0

    fun invalidate(){
        isValid = false
    }

    init {
        clampDt()
        clampTime()
    }

    fun getBufferIndex(time: Double): Long {
        val index0 = (time*frequency).toLong()
        return if(isStereo){
            when(mod){
                0 -> index0 and (-1 xor 1)
                1 -> index0 or 1
                else -> index0
            }
        } else index0
    }

    fun getTimeAt(x: Int) = time0 + dt * (x-this.x) / w
    fun getY(amplitude: Int) = y + h/2 - h * amplitude / 65536

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = textSize * 3
        minW = w
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        if(audio.file !== file){
            // println("file change noticed")
            // forcedMeta would put the UI to sleep
            meta = audio.meta ?: return
            // println("file was changed")
            lengthSeconds  = meta.duration
            frequency = meta.audioSampleRate.toDouble()
            file = audio.file
            ffmpegSampleRate = meta.audioSampleRate
            ffmpegSliceSampleCount = (ffmpegSampleRate * ffmpegSliceSampleDuration).toLong()
            clampDt()
            clampTime()
            invalidate()
        }

        if(!isValid || lastW != w) fillBuffer()

        try {
            for(x in x0 until x1){
                val index = x - x0
                val minY = minValue[index]
                val maxY = maxValue[index]
                GFX.drawRect(x, minY, 1, maxY-minY+1, black)
            }
        } catch (e: IndexOutOfBoundsException){
            // buffer was changed -> we don't really care;
            // will be fixed next frame
        }

    }

    fun getMaxAmplitudesSync(index: Long): Pair<Short, Short> {
        // val index = repeat[index, maxSampleIndex]
        // val index = if(repeat) index % maxSampleIndex else index
        if(index < 0) return 0.toShort() to 0.toShort()
        val sliceIndex = index / ffmpegSliceSampleCount
        val localIndex = (index % ffmpegSliceSampleCount).toInt()
        val arrayIndex0 = localIndex * 2 // for stereo
        val sliceTime = sliceIndex * ffmpegSliceSampleDuration
        val soundBuffer = Cache.getEntry(AudioStream.AudioSliceKey(file, sliceIndex), (ffmpegSliceSampleDuration * 2 * 1000).toLong(), false){
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
        if(arrayIndex0+1 > data.capacity() || arrayIndex0 < 0) println("$arrayIndex0 for ${data.capacity()}")
        return data[arrayIndex0] to data[arrayIndex0+1]
    }

    // todo reuse old data for left/right scrolling to improve performance
    fun fillBuffer(){
        isValid = true
        lastW = w
        val w = w
        val x0 = x
        if(w > 0){
            val min = IntArray(w)
            val max = IntArray(w)
            this.minValue = min
            this.maxValue = max
            thread {

                // load async
                // todo prevent user from crashing the engine by looking at too much audio? lol

                val t0 = getTimeAt(x0)
                var i0 = getBufferIndex(t0)

                if(t0.isNaN()) throw RuntimeException(
                    "$time0 $dt $ffmpegSampleRate $ffmpegSliceSampleCount $ffmpegSliceSampleDuration")

                for(x in x0 until x0+w){

                    if(x.and(15) == 0 && minValue !== min) break

                    val t1 = getTimeAt(x)
                    val i1 = getBufferIndex(t1)

                    var minValue = getMaxAmplitudesSync(i0).first.toInt()
                    var maxValue = minValue

                    for(i in i0 .. i1){
                        val value = getMaxAmplitudesSync(i).first.toInt()
                        if(value < minValue) minValue = value
                        else if(value > maxValue) maxValue = value
                    }

                    min[x-x0] = getY(maxValue)
                    max[x-x0] = getY(minValue)

                    i0 = i1

                }
            }
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        // requestBuffer() ?: return
        val size = (if(Input.isShiftDown) 4f else 20f) / max(GFX.width, GFX.height)
        val dx0 = dx*size
        val dy0 = dy*size
        val delta = dx0-dy0
        val oldTime = time0
        val oldDt = dt
        if(Input.isControlDown){
            val fraction = (x-this.x)/w
            time0 += dt * fraction
            dt *= pow(5f, delta)
            clampDt()
            time0 -= dt * fraction
        } else {
            time0 += dt * delta
        }
        clampTime()
        if(time0 != oldTime || oldDt != dt){
            invalidate()
        }
    }

    fun clampDt(){
        dt = clamp(dt, w/frequency, lengthSeconds)
        if(dt.isNaN()) dt = 1.0
    }

    fun clampTime(){
        time0 = clamp(time0, 0.0, lengthSeconds - dt)
        if(time0.isNaN()) time0 = 0.0
    }

}