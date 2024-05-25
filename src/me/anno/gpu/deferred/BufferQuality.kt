package me.anno.gpu.deferred

import kotlin.math.max

enum class BufferQuality(val intBits: Int) {
    UINT_8(8),
    UINT_16(16),
    UINT_32(32),
    FP_16(11),
    FP_32(23);

    val fp get() = ordinal >= 3

    fun isCompatibleWith(other: BufferQuality): Boolean {
        return combineWith(other) != null
    }

    fun combineWith(other: BufferQuality): BufferQuality? {
        if (this === other) return this
        if (fp == other.fp) {// just choose the bigger one
            return if (intBits > other.intBits) this else other
        }
        // we need fp...
        val neededBits = max(intBits, other.intBits)
        if (neededBits <= FP_16.intBits) return FP_16
        if (neededBits <= FP_32.intBits) return FP_32
        return null
    }
}