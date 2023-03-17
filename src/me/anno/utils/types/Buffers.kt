package me.anno.utils.types

import java.nio.Buffer

object Buffers {

    @JvmStatic
    fun Buffer.skip(n: Int) {
        position(position() + n)
    }

}