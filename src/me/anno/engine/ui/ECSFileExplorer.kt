package me.anno.engine.ui

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.Prefab.Companion.loadPrefab
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.style.Style
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.hpc.SyncMaster

class ECSFileExplorer(file0: FileReference?, val syncMaster: SyncMaster, style: Style) : FileExplorer(file0, style) {

    override fun onDoubleClick(file: FileReference) {
        // open the file
        val prefab = loadPrefab(file)
        if (prefab != null) {
            prefab.ownFile = file
            ECSSceneTabs.open(syncMaster, prefab)
        } else msg(NameDesc("Could not open prefab!"))
    }

    override fun getRightClickOptions(): List<FileExplorerOption> {
        return folderOptions
    }

    companion object {

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