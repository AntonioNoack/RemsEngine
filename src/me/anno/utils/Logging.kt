package me.anno.utils

import me.anno.utils.Color.hex32
import kotlin.math.max

object Logging {

    @JvmField
    val lastConsoleLines = ArrayDeque<String>()

    @JvmField
    var maxMemorizedLines = 500

    fun push(line: String) {
        val lines = lastConsoleLines
        synchronized(lines) {
            if (lines.size > max(0, maxMemorizedLines)) {
                lines.removeFirst()
            }
            lines.add(line)
        }
    }

    /**
     * returns a short random string with letters and numbers,
     * such that most instances have different codes, and it won't ever change over the lifetime of an instance
     *
     * return "00000000" for null
     * */
    @JvmStatic
    fun hash32(instance: Any?): String {
        return hex32(hash32raw(instance))
    }

    /**
     * returns a random i32,
     * such that most instances have different codes, and it won't ever change over the lifetime of an instance
     *
     * return 0 for null
     * */
    @JvmStatic
    fun hash32raw(instance: Any?): Int {
        if (instance == null) return 0
        return System.identityHashCode(instance)
    }
}