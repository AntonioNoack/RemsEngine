package me.anno.fonts.signeddistfields.structs

import kotlin.math.abs

class SignedDistance(var distance: Double, var dot: Double): Comparable<SignedDistance> {

    constructor(): this(-1e240, 1.0)
    constructor(src: SignedDistance): this(src.distance, src.dot)

    companion object { val INFINITE =
        SignedDistance(-1e240, 1.0)
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