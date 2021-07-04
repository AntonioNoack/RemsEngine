package me.anno.image.tar

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS
import me.anno.utils.files.Files.use
import javax.imageio.ImageIO

// todo try all tar images you can find, whether they are supported by the jMonkey tar plugin
fun main() {

    convert(OS.downloads)

}

fun convert(file: FileReference) {

    if (file.isDirectory) {
        for (file2 in file.listChildren() ?: return) {
            convert(file2)
        }
    } else {
        if (file.extension.equals("tga", true)) {
            println("reading file $file")
            val image = TGAImage.read(file.inputStream(), false)
            val data = image.data
            println("${file.name}: ${image.width} x ${image.height}, ${image.channels}, ${data.size}")
            // println(OS.desktop)
            val dst = getReference(OS.desktop, "tga/${file.name}.png")
            // println("dst: $dst, ${dst.name}")
            // println("dst.parent: ${dst.getParent()}")
            dst.getParent()!!.mkdirs()
            use(dst.outputStream()) {
                ImageIO.write(image.createBufferedImage(), "png", it)
            }
        }
    }

}