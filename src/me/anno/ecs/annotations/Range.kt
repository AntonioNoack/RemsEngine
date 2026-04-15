package me.anno.ecs.annotations

import me.anno.maths.Maths

annotation class Range(val min: Double, val max: Double) {

    companion object {

        fun Range?.minByte(): Byte =
            if (this == null || min < Byte.MIN_VALUE.toDouble()) Byte.MIN_VALUE
            else min.toInt().toByte()

        fun Range?.maxByte(): Byte =
            if (this == null || max > Byte.MAX_VALUE.toDouble()) Byte.MAX_VALUE
            else max.toInt().toByte()

        fun Range?.minShort(): Short =
            if (this == null || min < Short.MIN_VALUE.toDouble()) Short.MIN_VALUE
            else min.toInt().toShort()

        fun Range?.maxShort(): Short =
            if (this == null || max > Short.MAX_VALUE.toDouble()) Short.MAX_VALUE
            else max.toInt().toShort()

        fun Range?.minInt(): Int =
            if (this == null || min < Int.MIN_VALUE.toDouble()) Int.MIN_VALUE
            else min.toInt()

        fun Range?.maxInt(): Int =
            if (this == null || max > Int.MAX_VALUE.toDouble()) Int.MAX_VALUE
            else max.toInt()

        fun Range?.minLong(): Long =
            if (this == null || min < Long.MIN_VALUE.toDouble()) Long.MIN_VALUE
            else min.toLong()

        fun Range?.maxLong(): Long =
            if (this == null || max > Long.MAX_VALUE.toDouble()) Long.MAX_VALUE
            else max.toLong()

        fun Range?.minFloat(): Float =
            if (this == null) Float.NEGATIVE_INFINITY
            else min.toFloat()

        fun Range?.maxFloat(): Float =
            if (this == null) Float.POSITIVE_INFINITY
            else max.toFloat()

        fun Range?.minDouble(): Double =
            this?.min ?: Double.NEGATIVE_INFINITY

        fun Range?.maxDouble(): Double =
            this?.max ?: Double.POSITIVE_INFINITY

        fun Range?.clamp(v: Byte) =
            if (this != null) Maths.clamp(v, minByte(), maxByte()) else v

        fun Range?.clamp(v: Short) =
            if (this != null) Maths.clamp(v, minShort(), maxShort()) else v

        fun Range?.clamp(v: Int) =
            if (this != null) Maths.clamp(v, minInt(), maxInt()) else v

        fun Range?.clamp(v: Long) =
            if (this != null) Maths.clamp(v, minLong(), maxLong()) else v

        fun Range?.clamp(v: Float) =
            if (this != null) Maths.clamp(v, minFloat(), maxFloat()) else v

        fun Range?.clamp(v: Double) =
            if (this != null) Maths.clamp(v, minDouble(), maxDouble()) else v
    }

}
