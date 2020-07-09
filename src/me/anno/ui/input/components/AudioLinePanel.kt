package me.anno.ui.input.components

import me.anno.audio.SoundBuffer
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.pow
import java.io.File
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min

// always load all the audio?
// worst case: 24h * 3600s/h * 44kHz * 2 Channels * 16 Bit = 30 GB -> no
// todo we need to split the audio if it is too long :/
class AudioLinePanel(style: Style): Panel(style){

    val textSize = style.getSize("textSize", 10)

    val buffer = ShortBuffer.allocate(0)//SoundBuffer(File("C:\\Users\\Antonio\\Music\\test.ogg")).pcm!!

    var isStereo = true
    var frequency = 44100f

    var mod = 0 // left/right/avg
    var dt = 10f
    var time0 = 0f

    val lengthSeconds get() = (buffer.capacity()) * (if(isStereo) 0.5f else 1f) / frequency

    fun getBufferIndex(time: Float): Int {
        val index0 = ((time*frequency) * (if(isStereo) 2f else 1f)).toInt()
        return if(isStereo){
            when(mod){
                0 -> index0 and (-1 xor 1)
                1 -> index0 or 1
                else -> index0
            }
        } else index0
    }

    fun getTimeAt(x: Float) = time0 + dt * (x-this.x) / w
    fun getTimeAt(x: Int) = time0 + dt * (x-this.x) / w

    fun getY(amplitude: Int) = y + h/2 - h * amplitude / 65536

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = textSize * 3
        minW = w
    }

    fun requestBuffer(): ShortBuffer? {
        // todo find the buffer...
        return null
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        val buffer = requestBuffer() ?: return

        val length = buffer.limit()

        // todo two windows for stereo? average as default?
        val step = if(isStereo && mod in 0 .. 1) 2 else 1

        val t0 = getTimeAt(x0)
        var i0 = getBufferIndex(t0)

        for(x in x0 until x1){

            val t1 = getTimeAt(x+1)
            val i1 = min(max(getBufferIndex(t1), i0+step), length)

            var minValue = buffer[i0].toInt()
            var maxValue = minValue

            for(i in i0+step until i1 step step){// step depends on stereo/avg/...
                val value = buffer[i].toInt()
                if(value < minValue) minValue = value
                else if(value > maxValue) maxValue = value
            }

            val minY = getY(maxValue)
            val maxY = getY(minValue)

            GFX.drawRect(x, minY, 1, maxY-minY+1, black)
            // println("$t0-$t1: $minValue-$maxValue")

            if(i1 >= length) break

            i0 = i1

        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        val buffer = requestBuffer() ?: return
        val size = (if(Input.isShiftDown) 4f else 20f) / max(GFX.width, GFX.height)
        val dx0 = dx*size
        val dy0 = dy*size
        val delta = dx0-dy0
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
    }

    fun clampDt(){
        dt = clamp(dt, w/frequency, lengthSeconds)
    }

    fun clampTime(){
        time0 = clamp(time0, 0f, lengthSeconds - dt)
    }

}