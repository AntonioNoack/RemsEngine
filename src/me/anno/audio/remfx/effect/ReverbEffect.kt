package me.anno.audio.remfx.effect

import me.anno.audio.remfx.RemFXContext
import me.anno.utils.structures.arrays.ShortArrayList

class ReverbEffect(val sound: Sound) : Sound() {

    val bufferL = ShortArrayList(512 * 2)
    val bufferR = ShortArrayList(512 * 2)

    var numReverbs = 3
    var falloff = 0.5f
    var timeOffset = 0.1f

    override fun hasTimeDelay(): Boolean = true

    override fun add(context: RemFXContext, left: ShortArray, right: ShortArray) {
        val offsetPerReverb = (timeOffset * context.sampleRate).toInt()
        val totalReverb = offsetPerReverb * numReverbs

        val bufferSize = context.bufferSize
        val capacity = bufferSize + totalReverb
        bufferL.ensureCapacity(capacity)
        bufferR.ensureCapacity(capacity)

        // move data left
        val vl = bufferL.values
        val vr = bufferR.values
        vl.copyInto(vl, 0, bufferSize, capacity)
        vr.copyInto(vr, 0, bufferSize, capacity)
        vl.fill(0, totalReverb)
        vr.fill(0, totalReverb)

        // fill highest data
        val b0 = context.bufferOffset
        context.bufferOffset = totalReverb
        sound.add(context, bufferL.values, bufferR.values)
        context.bufferOffset = b0

        var sum = 0f
        var amplitude = 1f
        val falloff = falloff
        repeat(numReverbs + 1) {
            sum += amplitude
            amplitude *= falloff
        }
        amplitude = 1f / sum

        var readOffset = totalReverb - b0
        repeat(numReverbs + 1) {

            // calculate reverb
            val amplitudeS16 = (amplitude * 65536).toInt()
            for (j in context.indices) {
                add((vl[j + readOffset] * amplitudeS16).shr(16), j, left)
                add((vr[j + readOffset] * amplitudeS16).shr(16), j, right)
            }

            readOffset -= offsetPerReverb
            amplitude *= falloff
        }
    }

}