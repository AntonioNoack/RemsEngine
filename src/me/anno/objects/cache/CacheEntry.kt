package me.anno.objects.cache

import me.anno.gpu.GFX
import java.io.File
import kotlin.math.abs

class CacheEntry(val file: File, val index: Int, var data: CacheData?, var lastUsed: Long){

    fun destroy(){

    }

    companion object {


    }


}