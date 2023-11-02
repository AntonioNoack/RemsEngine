package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.prefab.*
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.files.FileReference
import me.anno.ui.editor.files.FileContentImporter
import org.apache.logging.log4j.LogManager

object ECSFileImporter : FileContentImporter<ISaveable>() {

    private val LOGGER = LogManager.getLogger(ECSFileImporter::class)

    override fun setName(element: ISaveable, name: String) {
        when(element){
            is Prefab -> element["name"] = name
            is PrefabSaveable -> element.name = name
            is NamedSaveable -> element.name = name
        }
    }

    override fun import(
        parent: ISaveable?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (ISaveable) -> Unit
    ) {

        parent!!

        if(parent is PrefabSaveable){
            val inspector = PrefabInspector.currentInspector!!
            val path = parent.prefabPath
            val prefab = PrefabCache[file]
            if (prefab != null) {
                val newPath = Hierarchy.add(prefab, Path.ROOT_PATH, inspector.prefab, path, ' ')
                if (doSelect && newPath != null) {
                    val root = inspector.prefab.getSampleInstance()
                    val instance = Hierarchy.getInstanceAt(root, newPath)
                    ECSSceneTabs.refocus()
                    EditorState.select(instance)
                }
            } else LOGGER.warn("Failed to import $file")
        } else LOGGER.warn("todo implement import of ${parent::class}")

    }

    // what is this used for?
    override fun createNode(parent: ISaveable?): ISaveable {
        return Entity(parent as? Entity)
    }

}