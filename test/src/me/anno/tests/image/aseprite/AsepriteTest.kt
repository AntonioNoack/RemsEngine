package me.anno.tests.image.aseprite

import me.anno.image.aseprite.AseSprite
import me.anno.image.aseprite.AsepriteReader
import me.anno.image.aseprite.AsepriteToImage.createImages
import me.anno.utils.OS.desktop

fun main() {
    val source = desktop.getChild("critter_badger.aseprite")
    val bytes = source.readBytesSync()
    val size = AsepriteReader.findSize(bytes.inputStream())
    println("Size: $size")
    val sprite = AsepriteReader.read(bytes.inputStream()) as AseSprite
    println("Data: ${sprite.header.width} x ${sprite.header.height} x ${sprite.header.frames}, speed: ${sprite.header.speed}")
    val images = sprite.createImages()
    val dst = desktop.getChild("critter")
    dst.tryMkdirs()
    for (i in images.indices) {
        images[i].write(dst.getChild("$i.png"))
    }
}