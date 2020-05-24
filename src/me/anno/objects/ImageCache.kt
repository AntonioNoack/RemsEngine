package me.anno.objects

import java.io.File

class ImageCache(val file: File){

    var lastUsed = 0L

    companion object {
        val cache = HashMap<File, ImageCache>()
        fun getImage(file: File): ImageCache {
            synchronized(cache){
                val cached = cache[file]
                if(cached != null) return cached
                val image = ImageCache(file)
                cache[file] = image
                return image
            }
        }
    }

}