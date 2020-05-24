package me.anno.utils

import java.lang.StringBuilder

object Tabs {

    fun spaces(ctr: Int): String {
        val builder = StringBuilder(ctr)
        for(i in 0 until ctr){
            builder.append(' ')
        }
        return builder.toString()
    }

}