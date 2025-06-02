package me.anno.image

import me.anno.io.files.FileReference
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.async.orElse
import me.anno.utils.async.pack
import org.apache.commons.imaging.Imaging
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

object ImageImpl : AsyncImageReader<ByteArray> {

    fun register() {
        ImageCache.registerByteArrayReader("png,jpg,gif,bmp,webp", ImageImpl)
    }

    override suspend fun read(srcFile: FileReference, source: ByteArray): Result<Image> {
        return tryImageIO(source).orElse {
            tryImaging(source)
        }
    }

    private fun tryImageIO(bytes: ByteArray): Result<Image> {
        return try {
            val img = ImageIO.read(ByteArrayInputStream(bytes))
            pack(img?.toImage(), null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryImaging(bytes: ByteArray): Result<Image> {
        return try {
            val img = Imaging.getBufferedImage(ByteArrayInputStream(bytes))
            Result.success(img.toImage())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}