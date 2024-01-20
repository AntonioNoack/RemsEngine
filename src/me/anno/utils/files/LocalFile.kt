package me.anno.utils.files

import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.engine.EngineBase
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

    fun String.toLocalPath(workspace: FileReference = EngineBase.workspace): String {
        val fileStr = replace('\\', '/')
        if (fileStr.contains("://")) return fileStr
        return checkIsChild(fileStr, ConfigBasics.configFolder, "\$CONFIG\$")
            ?: checkIsChild(fileStr, ConfigBasics.cacheFolder, "\$CACHE\$")
            // todo if there is a project file somewhere above this current file, use that project
            ?: checkIsChild(fileStr, workspace.nullIfUndefined(), "\$WORKSPACE\$")
            ?: checkIsChild(fileStr, OS.downloads, "\$DOWNLOADS\$")
            ?: checkIsChild(fileStr, OS.documents, "\$DOCUMENTS\$")
            ?: checkIsChild(fileStr, OS.pictures, "\$PICTURES\$")
            ?: checkIsChild(fileStr, OS.videos, "\$VIDEOS\$")
            ?: checkIsChild(fileStr, OS.music, "\$MUSIC\$")
            ?: checkIsChild(fileStr, OS.home, "\$HOME\$")
            ?: fileStr
    }

    fun String.toGlobalFile(workspace: FileReference = EngineBase.workspace): FileReference {
        val fileStr = if ('\\' in this) replace('\\', '/') else this
        val i1 = fileStr.lastIndexOf("$/")
        if (i1 < 0) return getReference(fileStr)
        val i0 = fileStr.lastIndexOf("$", i1 - 1)
        if (i0 < 0) return getReference(fileStr)
        return when (fileStr.substring(i0 + 1, i1)) {
            "CONFIG" -> ConfigBasics.configFolder
            "CACHE" -> ConfigBasics.cacheFolder
            "WORKSPACE" -> workspace.nullIfUndefined() ?: EngineBase.workspace
            "DOWNLOADS" -> OS.downloads
            "DOCUMENTS" -> OS.documents
            "PICTURES" -> OS.pictures
            "VIDEOS" -> OS.videos
            "MUSIC" -> OS.music
            "HOME", "USER" -> OS.home
            else -> null
        }?.getChild(fileStr.substring(i1 + 1)) ?: getReference(fileStr)
    }
}