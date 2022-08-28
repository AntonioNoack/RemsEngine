package me.anno.animation

import me.anno.io.base.BaseWriter
import me.anno.utils.types.AnyToFloat

@Suppress("unused")
class Keyframe<V>(
    time: Double, value: V,
    var interpolation: Interpolation
) : TimeValue<V>(time,value), Comparable<Keyframe<V>> {

    @Suppress("unchecked_cast")
    constructor() : this(0.0, 0f as V, Interpolation.SPLINE)
    constructor(time: Double, value: V) : this(time, value, Interpolation.SPLINE)

    override fun compareTo(other: Keyframe<V>): Int = time.compareTo(other.time)

    override fun isDefaultValue() = false
    override val className get() = "Keyframe"
    override val approxSize get() = 1

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("mode", interpolation.id)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "mode" -> interpolation = Interpolation.getType(value)
            else -> super.readInt(name, value)
        }
    }

    fun setValueUnsafe(value: Any?) {
        @Suppress("unchecked_cast")
        this.value = value as V
    }

    fun getChannelAsFloat(index: Int): Float {
        return AnyToFloat.getFloat(value!!, index)
    }

}