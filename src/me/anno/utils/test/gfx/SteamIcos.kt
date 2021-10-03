package me.anno.utils.test.gfx

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.LOGGER

fun main() {

    // works :)
    val file = getReference("C:/Program Files (x86)/Steam/steam/games/635743783810874a3a293d01b478280eaef3da18.ico")
    val image = ImageCPUCache.getImage(file, false)
    LOGGER.info(image)

}