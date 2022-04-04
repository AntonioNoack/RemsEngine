package me.anno.engine.ui

import me.anno.cache.instances.LastModifiedCache
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache.getPrefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.RemsEngine
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.zip.ZipCache
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.style.Style
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.hpc.SyncMaster
import org.apache.logging.log4j.LogManager


// done import mesh/material/... for modifications:
// done create material, mesh, animation etc folder
// done rename Scene.json to mesh file name.json
// todo if the name is a number, try to find another one
// todo or use a different name for that prefab...

// todo drag assets into the scene
// todo drag materials onto mesh components

class ECSFileExplorer(file0: FileReference?, val syncMaster: SyncMaster, style: Style) : FileExplorer(file0, style) {

    override fun onDoubleClick(file: FileReference) {
        // open the file
        val prefab = getPrefab(file)
        if (prefab != null) {
            prefab.source = file
            ECSSceneTabs.open(syncMaster, prefab, PlayMode.EDITING)
        } else {
            switchTo(file)
            // msg(NameDesc("Could not open prefab!"))
        }
    }

    override fun getRightClickOptions(): List<FileExplorerOption> {
        return folderOptions
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        LOGGER.info("Pasted $type : $data")
        when (type) {
            "PrefabSaveable" -> if (!pastePrefab(data)) {
                LOGGER.warn("Could not parse prefab, $data")
            }
            else -> {
                if (!pastePrefab(data)) {
                    if (data.length < 2048) {
                        val ref = data.toGlobalFile()
                        if (ref.exists) {
                            switchTo(ref)
                        }// else super.onPaste(x, y, data, type)
                    }// else super.onPaste(x, y, data, type)
                }
            }
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {

        // if current folder is inside project, then import all these assets


        // when dragging over a current folder, do that operation on that folder
        val entry = content.children.firstOrNull { it.contains(x, y) } as? FileExplorerEntry
        val current = if (entry == null) folder else getReference(entry.path)

        val projectFolder = RemsEngine.instance2!!.currentProject.location

        if (current.absolutePath.startsWith(projectFolder.absolutePath)) {
            openMenu(windowStack, listOf(
                MenuOption(NameDesc("Import")) {
                    import(current, files)
                },
                MenuOption(NameDesc(if (files.size > 1) "Raw-Copy" else "Other")) {
                    super.onPasteFiles(x, y, files)
                }
            ))
        } else {
            LOGGER.info("$current is not in $projectFolder, skipping import")
            super.onPasteFiles(x, y, files)
        }
    }

    private fun import(current: FileReference, files: List<FileReference>) {
        for (src in files) {
            // in the future, we could do this async
            val innerFolder = ZipCache.unzip(src, false)
            if (innerFolder != null) {

                // ask user for destination directory... or do we just use this one? yes :)
                // easier

                // first create the file mapping, then replace all references
                val result = HashMap<FileReference, Prefab>()
                copyAssets(innerFolder, current, true, result)
                replaceReferences(result)

            } else LOGGER.warn("Could not load $src as prefab")
        }
        invalidate()
        // todo update icon of current folder
        LastModifiedCache.invalidate(current)
    }

    fun replaceReferences(prefabs: HashMap<FileReference, Prefab>) {
        // replace all local references, so we can change the properties of everything:
        for ((_, prefab) in prefabs) {
            val original = getPrefab(prefab.prefab)!!
            original.sets.forEach { k1, k2, v ->
                when {
                    v is FileReference && v != InvalidRef -> {
                        val replacement = prefabs[v]
                        if (replacement != null) {
                            prefab.set(k1, k2, replacement.source)
                        }
                    }
                    v is List<*> && v.any { it is FileReference && it != InvalidRef } -> {
                        // e.g. materials
                        val replacement = v.map { prefabs[it]?.source ?: it }
                        prefab.set(k1, k2, replacement)
                    }
                }
            }
            prefab.source.writeText(TextWriter.toText(prefab, StudioBase.workspace))
        }
    }

    fun copyAssets(
        srcFolder: FileReference, dstFolder: FileReference, isMainFolder: Boolean,
        result: HashMap<FileReference, Prefab>
    ) {
        for (srcFile in srcFolder.listChildren() ?: return) {
            if (srcFile.isDirectory) {
                val dst2 = getReference(dstFolder, srcFile.name)
                if (!dst2.exists) dst2.tryMkdirs()
                if (dst2.isDirectory) {
                    copyAssets(srcFile, dst2, false, result)
                } else LOGGER.warn("Could not create directory $dst2 for $srcFile/${srcFile.name}")
            } else {

                // read as inner folder
                // rename its inner "Scene.json" into src.nameWithoutExtension + .json
                // import all other inner files into their respective files
                // if content is identical, we could merge them

                // it may not be necessarily a prefab, it could be a saveable
                val prefab = try {
                    getPrefab(srcFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                if (prefab != null) {
                    /*val possibleNames = ArrayList<String>(8)
                    val name0 = srcFile.nameWithoutExtension.toAllowedFilename()
                    possibleNames.add(name0 ?: "")
                    possibleNames.add(srcFile.getParent()?.nameWithoutExtension ?: "")
                    possibleNames.add(prefab.instanceName ?: "")*/
                    val prefabName = prefab.instanceName?.toAllowedFilename()
                    val fileName = srcFile.nameWithoutExtension.toAllowedFilename()
                    var name = fileName ?: srcFile.getParent()?.nameWithoutExtension ?: prefab.instanceName ?: "Scene"
                    if (name.toIntOrNull() != null) {
                        name = prefabName ?: "Scene"
                    }
                    if (isMainFolder && name == "Scene") {
                        // rename to file name
                        name = srcFile.getParent()!!.nameWithoutExtension
                    }
                    val newPrefab = Prefab(prefab.clazzName, srcFile)
                    var dstFile = getReference(dstFolder, "$name.json")

                    if (dstFile.exists && prefab.clazzName == "Mesh") {
                        // todo compare the contents
                        dstFile = findNextFile(dstFolder, name, "json", 3, '-', 1)
                    }
                    dstFile.getParent()?.tryMkdirs()
                    dstFile.writeText(TextWriter.toText(newPrefab, StudioBase.workspace))
                    newPrefab.source = dstFile
                    result[srcFile] = newPrefab

                } else LOGGER.warn("Skipped $srcFile, because it was not a prefab")
            }
        }
    }

    fun pastePrefab(data: String): Boolean {
        try {
            val read = TextReader.read(data, StudioBase.workspace, true)
            val saveable = read.getOrNull(0) ?: return false
            when (saveable) {
                is Prefab -> {
                    // find the name of the root element
                    var name = saveable.instanceName
                    name = name?.toAllowedFilename()
                    name = name ?: saveable.className
                    name = name.toAllowedFilename() ?: "Something"
                    // make .json lowercase
                    if (name.endsWith(".json", true)) {
                        name = name.substring(0, name.length - 5)
                    }
                    name += ".json"
                    val file = findNextFile(folder.getChild(name), 1, '-')
                    file.writeText(data)
                    invalidate()
                    return true
                }
                is PrefabSaveable -> {
                    var name = saveable.name.toAllowedFilename()
                    name = name ?: saveable.defaultDisplayName.toAllowedFilename()
                    name = name ?: saveable.className
                    name = name.toAllowedFilename() ?: "Something"
                    // make .json lowercase
                    if (name.endsWith(".json", true)) {
                        name = name.substring(0, name.length - 5)
                    }
                    name += ".json"
                    val file = findNextFile(folder.getChild(name), 1, '-')
                    file.writeText(data)
                    invalidate()
                    return true
                }
                else -> throw RuntimeException("Unknown class ${saveable.className}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(ECSFileExplorer::class)

        val folderOptions = ArrayList<FileExplorerOption>()

        fun addOptionToCreateFile(name: String, fileContent: String) {
            folderOptions.add(FileExplorerOption(NameDesc("Add $name")) { p, folder ->
                val file = findNextFile(folder, name, "json", 1, 0.toChar(), 0)
                if (file == InvalidRef) {
                    msg(p.windowStack, NameDesc("Directory is not writable"))
                } else file.writeText(fileContent)
                invalidateFileExplorers(p)
            })
        }

        fun addOptionToCreateComponent(name: String, clazzName: String = name) {
            addOptionToCreateFile(name, Prefab(clazzName).toString())
        }

        fun addOptionToCreateComponent(name: String, clazzName: String, prefab: FileReference) {
            addOptionToCreateFile(name, Prefab(clazzName, prefab).toString())
        }

        init {
            // todo create camera, material, shader, prefab, mesh, script, etc
            addOptionToCreateComponent("Entity")
            addOptionToCreateComponent("Scene", "Entity", ScenePrefab)
            addOptionToCreateComponent("Camera", "CameraComponent")
            // addOptionToCreateComponent("Cube", "")
            addOptionToCreateComponent("Material")
            addOptionToCreateComponent("Rigidbody")
        }
    }

}