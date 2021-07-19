package me.anno.ecs.annotations

annotation class Range(val min: Double, val max: Double) {

    companion object {

        fun Range?.minByte(): Byte =
            if (this == null || min < Byte.MIN_VALUE.toDouble()) Byte.MIN_VALUE
            else min.toInt().toByte()

        fun Range?.maxByte(): Byte =
            if (this == null || max > Byte.MAX_VALUE.toDouble()) Byte.MAX_VALUE
            else max.toInt().toByte()

        fun Range?.minUByte(): UByte =
            if (this == null || min < UByte.MIN_VALUE.toDouble()) UByte.MIN_VALUE
            else min.toInt().toUByte()

        fun Range?.maxUByte(): UByte =
            if (this == null || max > UByte.MAX_VALUE.toDouble()) UByte.MAX_VALUE
            else max.toInt().toUByte()

        fun Range?.minShort(): Short =
            if (this == null || min < Short.MIN_VALUE.toDouble()) Short.MIN_VALUE
            else min.toInt().toShort()

        fun Range?.maxShort(): Short =
            if (this == null || max > Short.MAX_VALUE.toDouble()) Short.MAX_VALUE
            else max.toInt().toShort()

        fun Range?.minUShort(): UShort =
            if (this == null || min < UShort.MIN_VALUE.toDouble()) UShort.MIN_VALUE
            else min.toInt().toUShort()

        fun Range?.maxUShort(): UShort =
            if (this == null || max > UShort.MAX_VALUE.toDouble()) UShort.MAX_VALUE
            else max.toInt().toUShort()

        fun Range?.minInt(): Int =
            if (this == null || min < Int.MIN_VALUE.toDouble()) Int.MIN_VALUE
            else min.toInt()

        fun Range?.maxInt(): Int =
            if (this == null || max > Int.MAX_VALUE.toDouble()) Int.MAX_VALUE
            else max.toInt()

        fun Range?.minUInt(): UInt =
            if (this == null || min < UInt.MIN_VALUE.toDouble()) UInt.MIN_VALUE
            else min.toUInt()

        fun Range?.maxUInt(): UInt =
            if (this == null || max > UInt.MAX_VALUE.toDouble()) UInt.MAX_VALUE
            else max.toUInt()

        fun Range?.minLong(): Long =
            if (this == null || min < Long.MIN_VALUE.toDouble()) Long.MIN_VALUE
            else min.toLong()

        fun Range?.maxLong(): Long =
            if (this == null || max > Long.MAX_VALUE.toDouble()) Long.MAX_VALUE
            else max.toLong()

        fun Range?.minULong(): ULong =
            if (this == null || min < ULong.MIN_VALUE.toDouble()) ULong.MIN_VALUE
            else min.toULong()

        fun Range?.maxULong(): ULong =
            if (this == null || max > ULong.MAX_VALUE.toDouble()) ULong.MAX_VALUE
            else max.toULong()

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
    }


}
