package me.anno.audio.effects

import me.anno.utils.Maths

class Time(val localTime: Double, val globalTime: Double) {
    override fun toString(): String = "[local: $localTime, global: $globalTime]"
    fun mix(second: Time, factor: Double) = Time(
        Maths.mix(localTime, second.localTime, factor),
        Maths.mix(globalTime, second.globalTime, factor)
    )
}