package me.anno.io.zip

import org.apache.commons.compress.archivers.zip.ZipFile

fun interface GetStreamCallback {
    fun callback(file: ZipFile?, exception: Exception?)
}