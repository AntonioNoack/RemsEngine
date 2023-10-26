package me.anno.engine.ui

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.RemsEngine
import me.anno.engine.ScenePrefab
import me.anno.engine.ui.AssetImport.deepCopyImport
import me.anno.engine.ui.AssetImport.shallowCopyImport
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase
import me.anno.ui.Style
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.files.Files.findNextChild
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.all2
import org.apache.logging.log4j.LogManager


// done import mesh/material/... for modifications:
// done create material, mesh, animation etc folder
// done rename Scene.json to mesh file name.json

class ECSFileExplorer(file0: FileReference?, style: Style) : FileExplorer(file0, style) {

    override fun onDoubleClick(file: FileReference) {
        // open the file
        val prefab = PrefabCache[file]
        if (prefab != null) {
            ECSSceneTabs.open(file, PlayMode.EDITING, true)
        } else {
            switchTo(file)
            // msg(NameDesc("Could not open prefab!"))
        }
    }

    override fun getFileOptions(): List<FileExplorerOption> {
        return fileOptions + super.getFileOptions()
    }

    override fun getFolderOptions(): List<FileExplorerOption> {
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
        val entry = content2d.children.firstOrNull { it.contains(x, y) } as? FileExplorerEntry
        val current = if (entry == null) folder else getReference(entry.path)

        val projectFolder = (StudioBase.instance as? RemsEngine)
            ?.currentProject?.location ?: return

        if (current.absolutePath.startsWith(projectFolder.absolutePath) &&
            !files.all { AssetImport.isPureFile(it) }
        ) {
            openMenu(windowStack, listOf(
                MenuOption(NameDesc("Shallow-Copy Import")) {
                    shallowCopyImport(current, files, this)
                },
                MenuOption(NameDesc("Deep-Copy Import")) {
                    deepCopyImport(current, files, this)
                },
                MenuOption(NameDesc("Link To Index")) {
                    val firstParent = files.first().getParent()
                    val name = if (files.size == 1) files.first().nameWithoutExtension
                    else if (files.all2 { it.getParent() == firstParent }) firstParent?.nameWithoutExtension ?: "Root"
                    else files.first().nameWithoutExtension
                    val newFile = current.findNextChild(name, "url", 3, '-')
                    // http://www.lyberty.com/encyc/articles/tech/dot_url_format_-_an_unofficial_guide.html
                    newFile.writeText(
                        "[InternetShortcut]\r\n" +
                                files.joinToString(",") { "URL=file://${it.toLocalPath()}\r\n" }
                    )
                    LOGGER.debug("Created url link file {}", newFile)
                },
                MenuOption(NameDesc("More Options")) {
                    super.onPasteFiles(x, y, files)
                }
            ))
        } else super.onPasteFiles(x, y, files)
    }

    private fun pastePrefab(data: String): Boolean {
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
                    if (!name.endsWith(".json", true)) {
                        name += ".json"
                    }
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
                    if (!name.endsWith(".json", true)) {
                        name += ".json"
                    }
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

        val fileOptions = ArrayList<FileExplorerOption>()
        val folderOptions = ArrayList<FileExplorerOption>()

        @JvmField
        val openAsSceneDesc = NameDesc(
            "Open As Scene",
            "Show the file in a new scene tab",
            "ui.file.openInSceneTab"
        )

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateFile(name: String, fileContent: String) {
            folderOptions.add(FileExplorerOption(NameDesc("Add $name")) { p, files ->
                val first = files.firstOrNull()
                if (first?.isDirectory == true) {
                    val file = findNextFile(first, name, "json", 1, 0.toChar(), 0)
                    if (file == InvalidRef) {
                        msg(p.windowStack, NameDesc("Directory is not writable"))
                    } else file.writeText(fileContent)
                    invalidateFileExplorers(p)
                } else LOGGER.warn("Not a directory")
            })
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateComponent(name: String, clazzName: String = name) {
            addOptionToCreateFile(name, Prefab(clazzName).toString())
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateComponent(name: String, clazzName: String, prefab: FileReference) {
            addOptionToCreateFile(name, Prefab(clazzName, prefab).toString())
        }

        init {
            val openAsScene = FileExplorerOption(openAsSceneDesc) { p, files ->
                for (file in files) {
                    ECSSceneTabs.open(
                        file, PlayMode.EDITING,
                        setActive = (file == files.first())
                    )
                }
                invalidateFileExplorers(p)
            }
            fileOptions.add(openAsScene)
            folderOptions.add(openAsScene)
            // create camera, material, shader, prefab, mesh, script, etc
            addOptionToCreateComponent("Entity")
            addOptionToCreateComponent("Scene", "Entity", ScenePrefab)
            // addOptionToCreateComponent("Camera", "Camera")
            // addOptionToCreateComponent("Cube", "")
            addOptionToCreateComponent("Material")
            addOptionToCreateComponent("Rigidbody")
        }
    }
}