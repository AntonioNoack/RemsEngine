package me.anno.utils.types

object Booleans {
    @JvmStatic
    fun Boolean.toInt(): Int = if (this) 1 else 0

    @JvmStatic
    fun Boolean.toInt(n: Int): Int = if (this) n else 0

    @JvmStatic
    fun Boolean.toLong(): Long = if (this) 1 else 0

    @JvmStatic
    fun Boolean.toLong(n: Long): Long = if (this) n else 0

    @JvmStatic
    fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag

    @JvmStatic
    fun Long.hasFlag(flag: Long): Boolean = (this and flag) == flag

    @JvmStatic
    fun Int.withFlag(flag: Int, isSet: Boolean): Int {
        return withoutFlag(flag) or isSet.toInt(flag)
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