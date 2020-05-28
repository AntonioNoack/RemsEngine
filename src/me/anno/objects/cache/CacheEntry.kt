package me.anno.objects.cache

import me.anno.gpu.GFX
import java.io.File
import kotlin.math.abs

class CacheEntry(var data: CacheData?, var lastUsed: Long){

    fun destroy(){
        data?.destroy()
    }

}