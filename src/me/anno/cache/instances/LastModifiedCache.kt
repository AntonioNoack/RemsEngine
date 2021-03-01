package me.anno.cache.instances

import me.anno.gpu.GFX.gameTime
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object LastModifiedCache {

    data class Result(val file: File, val exists: Boolean, val isDirectory: Boolean, val lastModified: Long){
        constructor(file: File, exists: Boolean): this(file, exists, if(exists) file.isDirectory else false, if(exists) file.lastModified() else 0L)
        constructor(file: File): this(file, file.exists())
        override fun hashCode(): Int = file.hashCode()
        override fun equals(other: Any?): Boolean {
            return other is Result && other.lastModified == lastModified && other.file == file
        }
    }

    var lastChecked = 0L
    var values = ConcurrentHashMap<File, Result>()

    operator fun get(file: File): Result {
        val time = gameTime
        if(abs(time - lastChecked)/1_000_000 > timeout){
            lastChecked = time
            values.clear()
        }
        return values.getOrPut(file){ Result(file) }
    }

    fun clear(){
        values.clear()
    }

    const val timeout = 20_000 // ms

}