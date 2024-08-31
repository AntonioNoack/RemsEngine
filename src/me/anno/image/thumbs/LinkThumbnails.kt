package me.anno.image.thumbs

import me.anno.gpu.texture.ITexture2D
import me.anno.graph.hdb.HDBKey
import me.anno.io.files.FileReference
import me.anno.io.files.Reference
import me.anno.utils.InternalAPI
import me.anno.utils.async.Callback
import me.anno.utils.structures.Iterators.firstOrNull

@InternalAPI
object LinkThumbnails {

    @JvmStatic
    @InternalAPI
    fun register() {
        Thumbs.registerFileExtensions("url") { srcFile, dstFile, size, callback ->
            // try to read the url, and redirect to the icon
            findIconLineInTxtLink(srcFile, dstFile, size, "IconFile=", callback)
        }
        Thumbs.registerFileExtensions("desktop") { srcFile, dstFile, size, callback ->
            // sample data by https://help.ubuntu.com/community/UnityLaunchersAndDesktopFiles:
            //[Desktop Entry]
            //Version=1.0
            //Name=BackMeUp
            //Comment=Back up your data with one click
            //Exec=/home/alex/Documents/backup.sh
            //Icon=/home/alex/Pictures/backup.png
            //Terminal=false
            //Type=Application
            //Categories=Utility;Application;
            findIconLineInTxtLink(srcFile, dstFile, size, "Icon=", callback)
        }
    }

    @JvmStatic
    private fun findIconLineInTxtLink(
        srcFile: FileReference, dstFile: HDBKey, size: Int, prefix: String,
        callback: Callback<ITexture2D>
    ) {
        val lineLengthLimit = 1024
        srcFile.readLines(lineLengthLimit) { lines, exc ->
            if (lines != null) {
                val iconFileLine = lines.firstOrNull { it.startsWith(prefix, true) }
                if (iconFileLine != null) {
                    val iconFile = iconFileLine
                        .substring(prefix.length)
                        .trim() // against \r
                        .replace('\\', '/')
                    Thumbs.generate(Reference.getReference(iconFile), dstFile, size, callback)
                }
                lines.close()
            } else exc?.printStackTrace()
        }
    }
}