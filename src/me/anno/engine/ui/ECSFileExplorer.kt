package me.anno.engine.ui

import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.Prefab.Companion.loadPrefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.style.Style
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.hpc.SyncMaster
import org.apache.logging.log4j.LogManager

class ECSFileExplorer(file0: FileReference?, val syncMaster: SyncMaster, style: Style) : FileExplorer(file0, style) {

    override fun onDoubleClick(file: FileReference) {
        // open the file
        val prefab = loadPrefab(file)
        if (prefab != null) {
            prefab.src = file
            ECSSceneTabs.open(syncMaster, prefab)
        } else msg(NameDesc("Could not open prefab!"))
    }

    override fun getRightClickOptions(): List<FileExplorerOption> {
        return folderOptions
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        println("pasted $type : $data")
        when (type) {
            "PrefabSaveable" -> if (!pastePrefab(data)) {
                LOGGER.warn("Could not parse prefab, $data")
            }
            else -> {
                if (!pastePrefab(data)) {
                    if (data.length < 2048) {
                        val ref = FileReference.getReference(data)
                        if (ref.exists) {
                            switchTo(ref)
                        }// else super.onPaste(x, y, data, type)
                    }// else super.onPaste(x, y, data, type)
                }
            }
        }
    }

    fun pastePrefab(data: String): Boolean {
        val read = TextReader.read(data)
        val saveable = read.getOrNull(0) ?: return false
        when (saveable) {
            is Prefab -> {
                // find the name of the root element
                var name = saveable.changes
                    ?.filterIsInstance<CSet>()
                    ?.firstOrNull { it.path?.size == 0 && it.name == "name" }?.value?.toString()
                name = name?.toAllowedFilename()
                name = name ?: saveable.className
                name = name.toAllowedFilename() ?: "Something"
                // make .json lowercase
                if (name.endsWith(".json", true)) {
                    name = name.substring(0, name.length - 5)
                }
                name += ".json"
                val file = findNextFileName(folder.getChild(name), 1, '-')
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
                val file = findNextFileName(folder.getChild(name), 1, '-')
                file.writeText(data)
                invalidate()
                return true
            }
            else -> throw RuntimeException("Unknown class ${saveable.className}")
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(ECSFileExplorer::class)

        val folderOptions = ArrayList<FileExplorerOption>()

        fun addOptionToCreateFile(name: String, fileContent: String) {
            folderOptions.add(FileExplorerOption(NameDesc("Add $name")) { folder ->
                var file = folder.getChild("$name.json")
                if (file.exists) {
                    file = findNextFileName(file, 1, 0.toChar(), 0)
                }
                if (file == InvalidRef) {
                    msg(NameDesc("Directory is not writable"))
                } else file.writeText(fileContent)
                invalidateFileExplorers()
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
            addOptionToCreateComponent("Prefab", "Entity")
            addOptionToCreateComponent("Scene", "Entity", ScenePrefab)
            addOptionToCreateComponent("Camera", "CameraComponent")
            // addOptionToCreateComponent("Cube", "")
            addOptionToCreateComponent("Material")
        }
    }

}