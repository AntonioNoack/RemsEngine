package me.anno.image.jpg

import me.anno.io.files.FileReference
import me.anno.utils.types.InputStreams.readNBytes2

// the standard readers work well and good, but unfortunately,
//  then don't allow me to load the image as a small size;
//  which should definitively be possible
// is there a way to read JPG thumbnails from the primary data?
// writing our own reader is MUCH too complicated; JPEG is insane
object JPGThumbnails {

    fun extractThumbnail(file: FileReference, callback: (ByteArray?) -> Unit) {
        // a small file -> reading the thumbnail is probably not worth it
        if (file.length() < 65536) {
            callback(null)
            return
        }
        // 65k is the max size for an exif section; plus 4k, where we hopefully find the marker
        file.inputStream(65536 + 4096) { it, _ ->
            if (it != null) {
                it.use {
                    val array = it.readNBytes2(65536 + 4096, false)
                    var start = 0
                    var i = 2
                    val li = array.size - 1
                    while (i < li) {
                        if (array[i] == 0xff.toByte() && array[i + 1] == 0xd8.toByte()) {
                            start = i
                            break
                        }
                        i++
                    }
                    while (i < li) {
                        if (array[i] == 0xff.toByte() && array[i + 1] == 0xd9.toByte()) {
                            val end = i + 2
                            val bytes = ByteArray(end - start)
                            System.arraycopy(array, start, bytes, 0, end - start)
                            callback(bytes)
                            return@inputStream
                        }
                        i++
                    }
                    callback(null)
                }
            } else callback(null)
        }
    }

}