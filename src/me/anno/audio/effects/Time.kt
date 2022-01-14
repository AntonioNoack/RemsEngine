package me.anno.audio.effects

import me.anno.maths.Maths

data class Time(val localTime: Double, val globalTime: Double) {

    constructor(time: Double): this(time, time)

    override fun toString(): String = "[local: $localTime, global: $globalTime]"
    fun mix(second: Time, factor: Double) = Time(
        Maths.mix(localTime, second.localTime, factor),
        Maths.mix(globalTime, second.globalTime, factor)
    )

}