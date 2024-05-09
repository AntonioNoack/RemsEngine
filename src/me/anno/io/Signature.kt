package me.anno.io

import me.anno.utils.Color.argb
import me.anno.utils.assertions.assertEquals

object Signature {
    @JvmStatic
    fun le32Signature(str: String): Int {
        assertEquals(4, str.length)
        return argb(str[3].code, str[2].code, str[1].code, str[0].code)
    }

    @JvmStatic
    fun be32Signature(str: String): Int {
        return when (str.length) {
            0 -> 0
            1 -> argb(str[0].code, 0, 0, 0)
            2 -> argb(str[0].code, str[1].code, 0, 0)
            3 -> argb(str[0].code, str[1].code, str[2].code, 0)
            else -> argb(str[0].code, str[1].code, str[2].code, str[3].code)
        }
    }
}