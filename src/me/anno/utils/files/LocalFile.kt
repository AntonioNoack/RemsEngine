package me.anno.utils.files

import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.studio.StudioBase
import me.anno.utils.OS

object LocalFile {

    fun checkIsChild(fileStr: String, parent: FileReference?, pathName: String): String? {
        parent ?: return null
        var parentStr = parent.toString().replace('\\', '/')
        if (!parentStr.endsWith('/')) {
            parentStr += '/'
        }
        return if (fileStr.startsWith(parentStr)) {
            "$pathName/${fileStr.substring(parentStr.length)}"
        } else null
    }

    fun String.toLocalPath(workspace: FileReference? = StudioBase.workspace): String {
        val fileStr = replace('\\', '/')
        if (fileStr.contains("://")) return fileStr
        return null ?: checkIsChild(fileStr, ConfigBasics.configFolder, "\$CONFIG\$")
        ?: checkIsChild(fileStr, ConfigBasics.cacheFolder, "\$CACHE\$")
        // todo if there is a project file somewhere above this current file, use that project
        ?: checkIsChild(fileStr, workspace, "\$WORKSPACE\$")
        ?: checkIsChild(fileStr, OS.downloads, "\$DOWNLOADS\$")
        ?: checkIsChild(fileStr, OS.documents, "\$DOCUMENTS\$")
        ?: checkIsChild(fileStr, OS.pictures, "\$PICTURES\$")
        ?: checkIsChild(fileStr, OS.videos, "\$VIDEOS\$")
        ?: checkIsChild(fileStr, OS.music, "\$MUSIC\$")
        ?: checkIsChild(fileStr, OS.home, "\$HOME\$")
        ?: fileStr
    }

    fun checkIsChild2(fileStr: String, parent: FileReference?, pathName: String): FileReference? {
        if (parent == null || parent == InvalidRef) return null
        val start = "$pathName/"
        return if (fileStr.startsWith(start, true)) {
            getReference(parent, fileStr.substring(start.length))
        } else null
    }

    fun String.toGlobalFile(workspace: FileReference? = StudioBase.workspace): FileReference {

        val fileStr = replace('\\', '/')
        if (fileStr.contains("://"))
            return getReference(fileStr)

        return null
            ?: checkIsChild2(fileStr, ConfigBasics.configFolder, "\$CONFIG\$")
            ?: checkIsChild2(fileStr, ConfigBasics.cacheFolder, "\$CACHE\$")
            ?: checkIsChild2(fileStr, workspace, "\$WORKSPACE\$")
            ?: checkIsChild2(fileStr, OS.downloads, "\$DOWNLOADS\$")
            ?: checkIsChild2(fileStr, OS.documents, "\$DOCUMENTS\$")
            ?: checkIsChild2(fileStr, OS.pictures, "\$PICTURES\$")
            ?: checkIsChild2(fileStr, OS.videos, "\$VIDEOS\$")
            ?: checkIsChild2(fileStr, OS.music, "\$MUSIC\$")
            ?: checkIsChild2(fileStr, OS.home, "\$HOME\$")
            ?: checkIsChild2(fileStr, OS.home, "\$USER\$")
            ?: getReference(fileStr)

    }

}