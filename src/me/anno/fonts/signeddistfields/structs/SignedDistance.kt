package me.anno.fonts.signeddistfields.structs

import kotlin.math.abs

class SignedDistance(var distance: Float, var dot: Float) : Comparable<SignedDistance> {

    constructor() : this(-1e38f, 1f)
    constructor(src: SignedDistance) : this(src.distance, src.dot)

    fun clear() {
        distance = -1e38f
        dot = 1f
    }

    fun set(other: SignedDistance) {
        distance = other.distance
        dot = other.dot
    }

    fun set(distance: Float, dot: Float): SignedDistance {
        this.distance = distance
        this.dot = dot
        return this
    }

    override operator fun compareTo(other: SignedDistance): Int {
        val absDist0 = abs(distance)
        val absDist1 = abs(other.distance)
        return if (absDist0 == absDist1) {
            dot.compareTo(other.dot)
        } else {
            absDist0.compareTo(absDist1)
        }
    }

    override fun toString() = "($distance * sign($dot))"

}