package me.anno.cache.instances

import me.anno.gpu.GFX.gameTime
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object LastModifiedCache {
    var lastChecked = 0L
    var values = ConcurrentHashMap<File, Pair<File, Long>>()
    operator fun get(file: File): Pair<File, Long> {
        val time = gameTime
        if(abs(time - lastChecked)/1_000_000 > timeout){
            lastChecked = time
            values.clear()
        }
        return values.getOrPut(file){ file to file.lastModified() }
    }
    const val timeout = 20_000 // ms
}