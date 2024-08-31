package me.anno.jvm.images

import me.anno.gpu.texture.ITexture2D
import me.anno.graph.hdb.HDBKey
import me.anno.jvm.images.BIImage.toImage
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.image.thumbs.Thumbs
import me.anno.utils.async.Callback
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView

object ThumbsImpl {
    fun generateSystemIcon(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        srcFile.useAsFile({
            try {
                val shellFolder = javaClass.classLoader.loadClass("sun.awt.shell.ShellFolder")
                val shellMethod = shellFolder.getMethod("getShellFolder", File::class.java)
                // val sf = ShellFolder.getShellFolder(it)
                val sf = shellMethod.invoke(null, it)
                val iconMethod = shellFolder.getMethod("getIcon", Boolean::class.java)
                // val icon = sf.getIcon(true)
                val icon = iconMethod.invoke(sf, true) as java.awt.Image
                ImageIcon(icon)
            } catch (e: Exception) {
                FileSystemView.getFileSystemView().getSystemIcon(it)
            }
        }, { icon, exc ->
            if (icon != null) {
                val image = BufferedImage(icon.iconWidth + 2, icon.iconHeight + 2, 2)
                val gfx = image.createGraphics()
                icon.paintIcon(null, gfx, 1, 1)
                gfx.dispose()
                // respect the size
                Thumbs.transformNSaveNUpload(srcFile, true, image.toImage(), dstFile, size, callback)
            } else exc?.printStackTrace()
        })
    }

    private fun <V> FileReference.useAsFile(process: (File) -> V, callback: Callback<V>) {
        if (this is FileFileRef) {
            callback.ok(process(file))
        } else {
            val tmp = File.createTempFile(nameWithoutExtension, extension)
            readBytes { bytes, exc ->
                if (bytes != null) {
                    tmp.writeBytes(bytes)
                    val result = process(tmp)
                    tmp.deleteOnExit()
                    callback.ok(result)
                } else {
                    callback.err(exc)
                }
            }
        }
    }
}