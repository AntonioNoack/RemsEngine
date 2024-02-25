package me.anno.utils.files

import me.anno.engine.EngineBase
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS

/**
 * Files are usually to be stored relatively to a standard path like Documents.
 * We use special names like $DOCUMENTS$ to represent them.
 * */
object LocalFile {

    private val registeredFiles = HashMap<String, () -> FileReference>()

    private fun String.unifySlashes(): String {
        return if ('\\' in this) replace('\\', '/') else this
    }

    fun register(name: String, location: () -> FileReference) {
        registeredFiles[name] = location
    }

    fun register(name: String, location: FileReference) {
        registeredFiles[name] = { location }
    }

    fun checkIsChild(fileStr: String, parent: FileReference?, pathName: String): String? {
        parent ?: return null
        var parentStr = parent.toString().unifySlashes()
        if (!parentStr.endsWith('/')) {
            parentStr += '/'
        }
        return if (fileStr.startsWith(parentStr)) {
            "$pathName/${fileStr.substring(parentStr.length)}"
        } else null
    }

    fun String.toLocalPath(workspace: FileReference = EngineBase.workspace): String {
        val fileStr = unifySlashes()
        if (fileStr.contains("://")) return fileStr
        // todo if there is a project file somewhere above this current file, use that project
        val match0 = checkIsChild(fileStr, workspace.nullIfUndefined(), "\$WORKSPACE\$")
        if (match0 != null) return match0
        for ((pathName, folderGetter) in registeredFiles) {
            val match = checkIsChild(fileStr, folderGetter(), pathName)
            if (match != null) return match
        }
        return fileStr
    }

    fun String.toGlobalFile(workspace: FileReference = EngineBase.workspace): FileReference {
        val fileStr = unifySlashes()
        val i1 = fileStr.lastIndexOf("$/")
        if (i1 < 0) return getReference(fileStr)
        val i0 = fileStr.lastIndexOf("$", i1 - 1)
        if (i0 < 0) return getReference(fileStr)
        val key = fileStr.substring(i0, i1 + 1)
        val folder = registeredFiles[key]?.invoke()
            ?: if (key == "\$WORKSPACE\$") {
                workspace.ifUndefined(EngineBase.workspace)
            } else null
        return folder?.getChild(fileStr.substring(i1 + 1)) ?: getReference(fileStr)
    }

    init {
        register("\$CONFIG\$") { ConfigBasics.configFolder }
        register("\$CACHE\$") { ConfigBasics.cacheFolder }
        register("\$DOWNLOADS\$", OS.downloads)
        register("\$DOCUMENTS\$", OS.documents)
        register("\$PICTURES\$", OS.pictures)
        register("\$VIDEOS\$", OS.videos)
        register("\$MUSIC\$", OS.music)
        register("\$HOME\$", OS.home)
    }
}