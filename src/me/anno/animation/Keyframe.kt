package me.anno.animation

import me.anno.io.base.BaseWriter
import me.anno.utils.types.AnyToFloat

class Keyframe<V>(
    time: Double, value: V,
    var interpolation: Interpolation
) : TimeValue<V>(time,value), Comparable<Keyframe<V>> {

    @Suppress("UNCHECKED_CAST")
    constructor() : this(0.0, 0f as V, Interpolation.SPLINE)
    constructor(time: Double, value: V) : this(time, value, Interpolation.SPLINE)

    override fun compareTo(other: Keyframe<V>): Int = time.compareTo(other.time)

    override fun isDefaultValue() = false
    override val className get() = "Keyframe"
    override val approxSize get() = 1

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("mode", interpolation.code)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "mode" -> interpolation = Interpolation.getType(value)
            else -> super.readInt(name, value)
        }
    }

    fun setValueUnsafe(value: Any?) {
        this.value = value as V
    }

    fun getChannelAsFloat(index: Int): Float {
        return AnyToFloat.getFloat(value!!, index)
    }

}