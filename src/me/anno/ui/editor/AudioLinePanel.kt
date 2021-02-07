package me.anno.ui.editor

import me.anno.audio.SoundBuffer
import me.anno.cache.instances.AudioCache
import me.anno.cache.keys.AudioSliceKey
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFXx2D.drawRect
import me.anno.objects.Audio
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Sleep.sleepShortly
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGStream
import org.apache.logging.log4j.LogManager
import java.lang.IndexOutOfBoundsException
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.abs
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
    fun getRelY(amplitude: Int) = h/2 - h * amplitude / 65536

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = textSize * 3
        minW = w
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        if(audio.file !== file){
            // ("file change noticed")
            // forcedMeta would put the UI to sleep
            meta = audio.meta ?: return
            // ("file was changed")
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

        val y = y
        try {
            for(x in x0 until x1){
                val index = x - x0
                val minY = minValue[index]
                val maxY = maxValue[index]
                drawRect(x, y+minY, 1, maxY-minY+1, black)
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
        val soundBuffer = AudioCache.getEntry(AudioSliceKey(file, sliceIndex), (ffmpegSliceSampleDuration * 2 * 1000).toLong(), false){
            val sequence = FFMPEGStream.getAudioSequence(file, sliceTime, ffmpegSliceSampleDuration, ffmpegSampleRate)
            var buffer: SoundBuffer?
            while(true){
                buffer = sequence.soundBuffer
                if(buffer != null) break
                // somebody else needs to work on the queue
                sleepShortly() // wait 0.1ms
            }
            buffer!!
        } as SoundBuffer
        val data = soundBuffer.data!!
        if(arrayIndex0+1 > data.capacity() || arrayIndex0 < 0) LOGGER.info("$arrayIndex0 for ${data.capacity()}")
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

                    val v0 = getMaxAmplitudesSync(i0)
                    var minValue = abs(v0.second.toInt())
                    var maxValue = abs(v0.first.toInt())

                    for(i in i0 until i1){
                        val v = getMaxAmplitudesSync(i)
                        val l = abs(v.second.toInt())
                        val r = abs(v.first.toInt())
                        minValue = max(minValue, l)
                        maxValue = max(maxValue, r)
                    }

                    minValue = -minValue

                    min[x-x0] = getRelY(maxValue)
                    max[x-x0] = getRelY(minValue)

                    i0 = i1

                }
            }
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

    companion object {
        private val LOGGER = LogManager.getLogger(AudioLinePanel::class.java)
    }

}