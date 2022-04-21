package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.ui.editor.files.FileContentImporter
import org.apache.logging.log4j.LogManager

object ECSFileImporter : FileContentImporter<PrefabSaveable>() {

    private val LOGGER = LogManager.getLogger(ECSFileImporter::class)

    override fun setName(element: PrefabSaveable, name: String) {
        element.name = name
        // todo if there is a prefab, then add the change as well
    }

    override fun import(
        parent: PrefabSaveable?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (PrefabSaveable) -> Unit
    ) {

        parent!!

        val inspector = PrefabInspector.currentInspector!!
        val path = parent.prefabPath!!

        val prefab = PrefabCache[file]
        if (prefab != null) {
            val newPath = Hierarchy.add(prefab, Path.ROOT_PATH, inspector.prefab, path)
            if (doSelect && newPath != null) {
                val root = inspector.prefab.getSampleInstance()
                val instance = Hierarchy.getInstanceAt(root, newPath)
                EditorState.select(instance)
            }
        } else LOGGER.warn("Failed to import $file")

    }

    override fun createNode(parent: PrefabSaveable?): PrefabSaveable {
        return Entity(parent as? Entity)
    }

}