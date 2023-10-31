package me.anno.utils.types

object Ints {

    @JvmStatic
    fun CharSequence?.toIntOrDefault(default: Int): Int {
        val v = toLongOrDefault(default.toLong())
        val w = v.toInt()
        return if (w.toLong() == v) w else default
    }

    @JvmStatic
    fun CharSequence?.toLongOrDefault(default: Long): Long {

        if (this == null) return default
        val length = this.length
        if (length == 0) return default

        val radix = 10

        val start: Int
        val isNegative: Boolean
        val limit: Long

        val firstChar = this[0]
        if (firstChar < '0') {  // Possible leading sign
            if (length == 1) return default  // non-digit (possible sign) only, no digits after

            start = 1

            when (firstChar) {
                '-' -> {
                    isNegative = true
                    limit = Long.MIN_VALUE
                }
                '+' -> {
                    isNegative = false
                    limit = -Long.MAX_VALUE
                }
                else -> return default
            }
        } else {
            start = 0
            isNegative = false
            limit = -Long.MAX_VALUE
        }

        val limitForMaxRadix = (-Long.MAX_VALUE) / 36

        var limitBeforeMul = limitForMaxRadix
        var result = 0L
        for (i in start until length) {
            val digit = Character.digit(this[i].code, radix)
            if (digit < 0) return default
            if (result < limitBeforeMul) {
                if (limitBeforeMul == limitForMaxRadix) {
                    limitBeforeMul = limit / radix
                    if (result < limitBeforeMul) {
                        return default
                    }
                } else {
                    return default
                }
            }
            result *= radix
            if (result < limit + digit) return default
            result -= digit
        }

        return if (isNegative) result else -result
    }
}