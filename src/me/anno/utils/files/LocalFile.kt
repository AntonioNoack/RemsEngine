package me.anno.utils.files

import me.anno.engine.EngineBase
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS
import me.anno.utils.structures.maps.Maps.removeIf

/**
 * Files are usually to be stored relatively to a standard path like Documents.
 *
 * Originally, I replaced them with $DOCUMENTS$, $HOME$ and such, but there is a much better way:
 * The Linux way, using ~/ and ./
 * */
@Suppress("CanUnescapeDollarLiteral")
object LocalFile {

    private val registeredFiles = HashMap<String, () -> FileReference>()
    private val workspaceCache = HashMap<String, FileReference>()
    private val projectFileName = "Project.json"

    fun invalidateWorkspace(file: FileReference) {
        synchronized(workspaceCache) {
            val prefix = file.absolutePath
            workspaceCache.removeIf { it.key.startsWith(prefix) }
        }
    }

    fun findWorkspace(file: FileReference, fallback: FileReference): FileReference {
        return synchronized(workspaceCache) {
            workspaceCache.getOrPut(file.absolutePath) {
                findWorkspaceImpl(file)
            }
        }.ifUndefined(fallback)
    }

    private fun findWorkspaceImpl(file: FileReference): FileReference {
        var file = file
        while (true) {
            if (file.isDirectory) {
                val marker = file.getChild(projectFileName)
                if (marker.exists) return file
            }

            if (file.name == projectFileName) {
                return file.getParent()
            }

            val parent = file.getParent()
            if (parent == InvalidRef || parent == file) return InvalidRef
            file = parent
        }
    }

    private fun String.unifySlashes(): String {
        return if ('\\' in this) replace('\\', '/') else this
    }

    private fun register(name: String, location: () -> FileReference) {
        registeredFiles[name] = location
    }

    private fun register(name: String, location: FileReference) {
        registeredFiles[name] = { location }
    }

    fun toLocalPath(file: FileReference, workspace: FileReference = EngineBase.workspace): String {
        val fileStr = file.absolutePath
        if (fileStr.contains("://")) return fileStr // URL-syntax

        if (workspace != InvalidRef) {
            // must not use getWorkspace! We sometimes don't want to store our files with a workspace!
            val workspaceFile = file.replacePath(workspace.absolutePath, "./")
            if (workspaceFile != null) return workspaceFile
        }

        val homeFile = file.replacePath(OS.home.absolutePath, "~/")
        if (homeFile != null) return homeFile

        return file.absolutePath
    }

    private fun getWorkspace(workspace: FileReference): FileReference {
        return workspace
            .ifUndefined(EngineBase.workspace)
            .ifUndefined(OS.application)
    }

    fun String.toGlobalFile(workspace: FileReference = EngineBase.workspace): FileReference {
        val fileStr = unifySlashes()
        return when {
            fileStr.startsWith("./") -> {
                getWorkspace(workspace).getChild(fileStr.substring(2))
            }
            fileStr.startsWith("~/") -> {
                OS.home.getChild(fileStr.substring(2))
            }
            fileStr.count { it == '$' } >= 2 && !fileStr.contains("://") -> {
                toGlobalFileLegacy(fileStr, workspace)
            }
            else -> getReference(fileStr)
        }
    }

    private fun toGlobalFileLegacy(fileStr: String, workspace: FileReference): FileReference {
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
        // registration for legacy
        register("\$CONFIG\$") { ConfigBasics.configFolder }
        register("\$CACHE\$") { ConfigBasics.cacheFolder }
        register("\$DOWNLOADS\$", OS.downloads)
        register("\$DOCUMENTS\$", OS.documents)
        register("\$DESKTOP\$", OS.desktop)
        register("\$PICTURES\$", OS.pictures)
        register("\$VIDEOS\$", OS.videos)
        register("\$MUSIC\$", OS.music)
        register("\$HOME\$", OS.home)
    }
}