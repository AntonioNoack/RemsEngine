package me.anno.utils.files

import me.anno.io.config.ConfigBasics
import me.anno.studio.StudioBase
import me.anno.utils.OS
import java.io.File

object LocalFile {

    fun File.toLocalPath(workspace: File? = StudioBase.workspace): String {
        val fileStr = toString().replace('\\', '/')
        fun checkIsChild(parent: File?, pathName: String): String? {
            parent ?: return null
            var parentStr = parent.toString().replace('\\', '/')
            if (!parentStr.endsWith('/')) {
                parentStr += '/'
            }
            return if (fileStr.startsWith(parentStr)) {
                "$pathName/${fileStr.substring(parentStr.length)}"
            } else null
        }
        return null ?: checkIsChild(ConfigBasics.configFolder, "\$CONFIG\$")
        ?: checkIsChild(ConfigBasics.cacheFolder, "\$CACHE\$")
        ?: checkIsChild(workspace, "\$WORKSPACE\$")
        ?: checkIsChild(OS.downloads, "\$DOWNLOADS\$")
        ?: checkIsChild(OS.documents, "\$DOCUMENTS\$")
        ?: checkIsChild(OS.pictures, "\$PICTURES\$")
        ?: checkIsChild(OS.videos, "\$VIDEOS\$")
        ?: checkIsChild(OS.home, "\$HOME\$")
        ?: fileStr
    }

    fun String.toGlobalFile(workspace: File? = StudioBase.workspace): File {
        val fileStr = replace('\\', '/')
        fun checkIsChild(parent: File?, pathName: String): File? {
            parent ?: return null
            val start = "$pathName/"
            return if (fileStr.startsWith(start)) {
                File(parent, fileStr.substring(start.length))
            } else null
        }
        return null
            ?: checkIsChild(ConfigBasics.configFolder, "\$CONFIG\$")
            ?: checkIsChild(ConfigBasics.cacheFolder, "\$CACHE\$")
            ?: checkIsChild(workspace, "\$WORKSPACE\$")
            ?: checkIsChild(OS.downloads, "\$DOWNLOADS\$")
            ?: checkIsChild(OS.documents, "\$DOCUMENTS\$")
            ?: checkIsChild(OS.pictures, "\$PICTURES\$")
            ?: checkIsChild(OS.videos, "\$VIDEOS\$")
            ?: checkIsChild(OS.home, "\$HOME\$")
            ?: File(this)
    }

}