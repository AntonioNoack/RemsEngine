package me.anno.cache

import me.anno.cache.data.ICacheData

class CacheEntry(var data: ICacheData?, var timeout: Long, var lastUsed: Long){

    fun destroy(){
        data?.destroy()
    }

}