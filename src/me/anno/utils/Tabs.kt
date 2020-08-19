package me.anno.utils

import java.lang.StringBuilder

object Tabs {

    fun spaces(ctr: Int): String {
        return String(CharArray(ctr){ ' ' })
    }

    fun tabs(ctr: Int): String {
        return String(CharArray(ctr){ '\t' })
    }

}