package me.anno.maths

object FastInvSqrt {

    @JvmStatic
    fun fastInvSqrt1(number: Float): Float { // 0.58ns
        val x2 = number * 0.5f
        var y = number
        var bits = y.toRawBits()
        bits = 0x5f3759df - (bits shr 1)
        y = Float.fromBits(bits)
        y = y * (1.5f - (x2 * y * y)) // 1st iteration
        return y
    }

    @JvmStatic
    fun fastInvSqrt2(number: Float): Float { // 0.81ns
        val x2 = number * 0.5f
        var y = number
        var bits = y.toRawBits()
        bits = 0x5f3759df - (bits shr 1)
        y = Float.fromBits(bits)
        y = y * (1.5f - (x2 * y * y)) // 1st iteration
        y = y * (1.5f - (x2 * y * y)) // 2nd iteration
        return y
    }

}