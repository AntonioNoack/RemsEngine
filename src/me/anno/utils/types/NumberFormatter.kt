package me.anno.utils.types

import me.anno.utils.InternalAPI
import me.anno.utils.hpc.threadLocal
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.removeRange2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

@InternalAPI
object NumberFormatter {

    private val tmpBuilder = threadLocal { StringBuilder(19) }

    private fun getTmpBuilder(): StringBuilder {
        return tmpBuilder.get().clear()
    }

    /**
     * formats a float;
     * can be replaced with value.toFixed(numDigits) in JavaScript (except for sign-extension);
     * originally was implemented in Java with 'DecimalFormat' and 'Locale.ENGLISH'
     * */
    @JvmStatic
    @InternalAPI
    fun formatFloat(v: Double, numDigits: Int, signExtend: Boolean): String {
        return if (v.isFinite() && fitsInSpace(v, numDigits)) {
            return getTmpBuilder().formatFloat(v, numDigits, signExtend).toString()
        } else v.toString()
    }

    private fun fitsInSpace(v: Double, numDigits: Int): Boolean {
        val absV = abs(v)
        val pow = 10.0.pow(numDigits)
        val base = round(absV * pow)
        return base < Long.MAX_VALUE
    }

    /**
     * formats a float;
     * can be replaced with value.toFixed(numDigits) in JavaScript (except for sign-extension);
     * originally was implemented in Java with 'DecimalFormat' and 'Locale.ENGLISH'
     * */
    @JvmStatic
    @InternalAPI
    fun StringBuilder.formatFloat(v: Double, numDigits: Int, signExtend: Boolean): StringBuilder {
        return if (v.isFinite()) {
            val absV = abs(v)
            val pow = 10.0.pow(numDigits)
            val base = round(absV * pow)
            if (base < Long.MAX_VALUE) {
                val i0 = length
                val digits = formatInt(base.toLong())
                val digitsLength = length - i0
                val isNegative = v < 0.0
                val len = max(numDigits + 1, digitsLength) + 1 + (isNegative || signExtend).toInt()
                ensureCapacity(length + len)
                if (isNegative) {
                    append('-')
                } else if (signExtend) {
                    append(' ')
                }
                var doneDigits = 0
                if (digitsLength <= numDigits) { // 0.
                    append('0')
                } else {
                    // append all higher digits
                    doneDigits = digitsLength - numDigits
                    append(digits, i0, i0 + doneDigits) // start, end
                }
                // append comma
                append('.')
                // append all lower digits
                for (i in 0 until numDigits - digitsLength) {
                    append('0')
                }
                // append remaining digits
                append(digits, i0 + doneDigits, i0 + digitsLength)
                removeRange2(i0, i0 + digitsLength)
            } else append(v)
        } else append(v)
    }

    @JvmStatic
    fun formatIntTriplets(value: Long, signExtend: Boolean = false): String {
        return getTmpBuilder().formatIntTriplets(value, signExtend).toString()
    }

    @JvmStatic
    fun StringBuilder.formatIntTriplets(value: Long, signExtend: Boolean = false): StringBuilder {
        if (value == Long.MIN_VALUE) { // special case
            append("-9,223,372,036,854,775,808")
            return this
        }
        if (value < 0) {
            append('-')
        } else if (signExtend) {
            append(' ')
        }
        val i0 = length
        var v = abs(value)
        while (v >= 1000) {
            val sub = (v % 1000).toInt()
            append('0' + (sub % 10))
            append('0' + ((sub / 10) % 10))
            append('0' + (sub / 100))
            append(',')
            v /= 1000
        }
        val vi = v.toInt()
        append('0' + (vi % 10))
        if (vi >= 10) {
            append('0' + ((vi / 10) % 10))
        }
        if (vi >= 100) {
            append('0' + (vi / 100))
        }
        return reverse(i0, length)
    }

    @JvmStatic
    fun StringBuilder.formatInt(value: Long, signExtend: Boolean = false): StringBuilder {
        if (value == Long.MIN_VALUE) { // special case
            append("-9,223,372,036,854,775,808")
            return this
        }
        if (value < 0) {
            append('-')
        } else if (signExtend) {
            append(' ')
        }
        val i0 = length
        var v = abs(value)
        while (v >= 10) {
            val nextV = v / 10
            val sub = (v - nextV * 10).toInt()
            append('0' + sub)
            v = nextV
        }
        if (v > 0 || length == i0) {
            append('0' + v.toInt())
        }
        return reverse(i0, length)
    }

    fun StringBuilder.reverse(i0: Int, i1: Int): StringBuilder {
        for (di in 0 until (i1 - i0).shr(1)) {
            val i = i0 + di
            val j = i1 - 1 - di
            val tmp = this[i]
            this[i] = this[j]
            this[j] = tmp
        }
        return this
    }
}