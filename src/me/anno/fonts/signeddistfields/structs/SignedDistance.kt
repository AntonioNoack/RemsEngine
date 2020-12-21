package me.anno.fonts.signeddistfields.structs

import kotlin.math.abs

class SignedDistance(var distance: Float, var dot: Float): Comparable<SignedDistance> {

    constructor(): this(-1e38f, 1f)
    constructor(src: SignedDistance): this(src.distance, src.dot)

    companion object { val INFINITE =
        SignedDistance(-1e38f, 1f)
    }

    fun set(other: SignedDistance){
        distance = other.distance
        dot = other.dot
    }

    override operator fun compareTo(other: SignedDistance): Int {
        return if(abs(distance) == abs(other.distance)){
            dot.compareTo(other.dot)
        } else {
            abs(distance).compareTo(abs(other.distance))
        }
    }

    override fun toString() = "($distance * sign($dot))"

}