package me.anno.cache

class CacheEntry(var data: ICacheData?, var timeout: Long, var lastUsed: Long){

    fun destroy(){
        data?.destroy()
    }

}