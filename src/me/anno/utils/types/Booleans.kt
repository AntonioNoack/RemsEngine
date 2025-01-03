package me.anno.utils.types

object Booleans {

    @JvmStatic
    fun Boolean.toInt(ifTrue: Int = 1, ifFalse: Int = 0): Int = if (this) ifTrue else ifFalse

    @JvmStatic
    fun Boolean.toLong(ifTrue: Long = 1L, ifFalse: Long = 0L): Long = if (this) ifTrue else ifFalse

    @JvmStatic
    fun Boolean.toFloat(ifTrue: Float = 1f, ifFalse: Float = 0f): Float = if (this) ifTrue else ifFalse

    @JvmStatic
    fun Boolean.toDouble(ifTrue: Double = 1.0, ifFalse: Double = 0.0): Double = if (this) ifTrue else ifFalse

    @JvmStatic
    fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag

    @JvmStatic
    fun Long.hasFlag(flag: Long): Boolean = (this and flag) == flag

    @JvmStatic
    fun Int.flagDifference(negativeFlag: Int, positiveFlag: Int): Int {
        return hasFlag(positiveFlag).toInt() - hasFlag(negativeFlag).toInt()
    }

    @JvmStatic
    fun Long.flagDifference(negativeFlag: Long, positiveFlag: Long): Int {
        return hasFlag(positiveFlag).toInt() - hasFlag(negativeFlag).toInt()
    }

    @JvmStatic
    fun Int.withFlag(flag: Int): Int {
        return withoutFlag(flag) or flag
    }

    @JvmStatic
    fun Int.withFlag(flag: Int, isSet: Boolean): Int {
        return withoutFlag(flag) or isSet.toInt(flag)
    }

    @JvmStatic
    fun Long.withFlag(flag: Long): Long {
        return withoutFlag(flag) or flag
    }

    @JvmStatic
    fun Long.withFlag(flag: Long, isSet: Boolean): Long {
        return withoutFlag(flag) or isSet.toLong(flag)
    }

    @JvmStatic
    fun Int.withoutFlag(flag: Int): Int {
        return and(flag.inv())
    }

    @JvmStatic
    fun Long.withoutFlag(flag: Long): Long {
        return and(flag.inv())
    }
}