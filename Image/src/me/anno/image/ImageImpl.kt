package me.anno.image

import me.anno.io.files.FileReference
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.async.Callback
import org.apache.commons.imaging.Imaging
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

object ImageImpl : AsyncImageReader<ByteArray> {

    fun register() {
        ImageCache.registerByteArrayReader("png,jpg,gif,bmp,webp", ImageImpl)
    }

    override fun read(srcFile: FileReference, source: ByteArray, callback: Callback<Image>) {
        tryImageIO(source) { img, _ ->
            if (img != null) callback.ok(img)
            else tryImaging(source, callback)
        }
    }

    private fun tryImageIO(bytes: ByteArray, callback: Callback<Image>) {
        try {
            val img = ImageIO.read(ByteArrayInputStream(bytes))
            callback.call(img?.toImage(), null)
        } catch (e: Exception) {
            callback.err(e)
        }
    }

    private fun tryImaging(bytes: ByteArray, callback: Callback<Image>) {
        try {
            val img = Imaging.getBufferedImage(ByteArrayInputStream(bytes))
            callback.call(img.toImage(), null)
        } catch (e: Exception) {
            callback.err(e)
        }
    }
}