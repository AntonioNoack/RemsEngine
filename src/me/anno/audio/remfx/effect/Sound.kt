package me.anno.audio.remfx.effect

import me.anno.maths.Maths.clamp
import me.anno.audio.remfx.RemFXContext

abstract class Sound {

    abstract fun add(
        context: RemFXContext,
        left: ShortArray, right: ShortArray
    )

    open fun hasTimeDelay() = false

    fun add(value: Float, i: Int, dst: ShortArray) {
        add(value.toInt(), i, dst)
    }

    fun add(value: Int, i: Int, dst: ShortArray) {
        val newValue = dst[i] + value
        dst[i] = clampS(newValue)
    }

    fun add(
        context: RemFXContext, value: Float, i: Int,
        left: ShortArray, right: ShortArray
    ) {
        left[i] = clampS(left[i] + (value * (context.amplitudeL0 + i * context.deltaAmplitudeL)).toInt())
        right[i] = clampS(right[i] + (value * (context.amplitudeR0 + i * context.deltaAmplitudeR)).toInt())
    }

    fun clampS(v: Int) = clamp(v, Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
}