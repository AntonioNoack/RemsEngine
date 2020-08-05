package me.anno.utils

import me.anno.config.DefaultConfig
import java.util.*

fun Long.formatFileSize(): String {
    val endings = "kMGTPEZY"
    val divider = if(DefaultConfig["ui.file.showGiB", true]) 1024 else 1000
    val suffix = if(divider == 1024) "i" else ""
    val halfDivider = divider/2
    var v = this
    if(v < halfDivider) return "$v Bytes"
    for(prefix in endings){
        val vSaved = v
        v = (v + halfDivider)/divider
        if(v < divider){
            return "${when(v){
                in 0 .. 9 -> "%.2f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                in 10 .. 99 -> "%.1f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                else -> v.toString()
            }} ${prefix}B$suffix"
        }
    }
    return "$v ${endings.last()}B$suffix"
}