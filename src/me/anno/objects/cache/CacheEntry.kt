package me.anno.objects.cache

class CacheEntry(var data: CacheData?, var timeout: Long, var lastUsed: Long){

    fun destroy(){
        data?.destroy()
    }

}