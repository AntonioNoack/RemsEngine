package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
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
        val path = parent.pathInRoot2(inspector.root, false)

        val prefab = loadPrefab(file)

        if (prefab != null) {

            val instance = prefab.createInstance()
            parent.add(instance)
            inspector.adds.add(CAdd(path, 'e', "Entity", instance.name, file))
            callback(instance as Entity)
            if (doSelect) {
                // todo select it

            }

        } else LOGGER.warn("Failed to import $file")

    }

    override fun createNode(parent: PrefabSaveable?): PrefabSaveable {
        return Entity(parent as? Entity)
    }

}