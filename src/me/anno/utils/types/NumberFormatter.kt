package me.anno.utils.types

import me.anno.utils.InternalAPI
import me.anno.utils.types.Booleans.toInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

@InternalAPI
object NumberFormatter {
    /**
     * formats a float;
     * can be replaced with value.toFixed(numDigits) in JavaScript (except for sign-extension);
     * originally was implemented in Java with 'DecimalFormat' and 'Locale.ENGLISH'
     * */
    @JvmStatic
    @InternalAPI
    fun formatFloat(v: Double, numDigits: Int, signExtend: Boolean): String {
        return if (v.isFinite()) {
            val absV = abs(v)
            val isNegative = v < 0.0
            val pow = 10.0.pow(numDigits)
            val base = round(absV * pow)
            if (base < Long.MAX_VALUE) {
                val digits = base.toLong().toString()
                val len = max(numDigits + 1, digits.length) + 1 + isNegative.toInt()
                val result = StringBuilder(len)
                if (isNegative) {
                    result.append('-')
                } else if (signExtend) {
                    result.append(' ')
                }
                var doneDigits = 0
                if (digits.length <= numDigits) { // 0.
                    result.append('0')
                } else {
                    // append all higher digits
                    doneDigits = digits.length - numDigits
                    result.append(digits, 0, doneDigits) // start, end
                }
                // append comma
                result.append('.')
                // append all lower digits
                for (i in 0 until numDigits - digits.length) {
                    result.append('0')
                }
                // append remaining digits
                result.append(digits, doneDigits, digits.length)
                result.toString()
            } else v.toString()
        } else v.toString()
    }

    @JvmStatic
    fun formatIntTriplets(value: Long, signExtend: Boolean = false): String {
        if (value == Long.MIN_VALUE) { // special case
            return "-9,223,372,036,854,775,808"
        }
        val builder = StringBuilder(19)
        var v = abs(value)
        while (v >= 1000) {
            val sub = (v % 1000).toInt()
            builder.append('0' + (sub % 10))
            builder.append('0' + ((sub / 10) % 10))
            builder.append('0' + (sub / 100))
            builder.append(',')
            v /= 1000
        }
        val vi = v.toInt()
        builder.append('0' + (vi % 10))
        if (vi >= 10) {
            builder.append('0' + ((vi / 10) % 10))
        }
        if (vi >= 100) {
            builder.append('0' + (vi / 100))
        }
        if (value < 0) {
            builder.append('-')
        } else if (signExtend) {
            builder.append(' ')
        }
        builder.reverse()
        return builder.toString()
    }
}