package me.anno.utils.test

import com.twelvemonkeys.imageio.plugins.tga.TGAImageReaderSpi
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS
import org.apache.commons.imaging.Imaging
import java.io.File
import javax.imageio.ImageIO


fun main() {

    // todo there is a set of tga files, which still can't be loaded :/
    val file = File("C:\\Users\\Antonio\\Downloads/ogldev-source/Content/guard1_body.tga")
    val image = ImageIO.read(file) ?: try {
        Imaging.getBufferedImage(file)
    } catch (e: Exception) {
        e.printStackTrace();null
    }
    TGAImageReaderSpi()
    if (image != null) {
        ImageIO.write(image, "png", getReference(OS.desktop, "guard1_body.png").unsafeFile)
    } else println("image could not be read")

}