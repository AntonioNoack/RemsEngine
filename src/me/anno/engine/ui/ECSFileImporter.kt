package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.prefab.ChangeAddEntity
import me.anno.ecs.prefab.EntityPrefab.Companion.loadPrefab
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.PrefabInspector
import me.anno.io.files.FileReference
import me.anno.ui.editor.files.FileContentImporter
import org.apache.logging.log4j.LogManager

object ECSFileImporter : FileContentImporter<Entity>() {

    private val LOGGER = LogManager.getLogger(ECSFileImporter::class)

    override fun setName(element: Entity, name: String) {
        element.name = name
        // todo if there is a prefab, then add the change as well
    }

    override fun import(
        parent: Entity?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (Entity) -> Unit
    ) {

        parent!!

        val inspector = PrefabInspector.currentInspector!!
        val path = parent.pathInRoot(inspector.root).toIntArray()

        val prefab = loadPrefab(file)

        if (prefab != null) {

            val instance = prefab.createInstance()
            parent.add(instance)
            inspector.changes.add(ChangeAddEntity(Path(path), file))
            callback(instance)
            if(doSelect){
                // todo select it

            }

        } else LOGGER.warn("Failed to import $file")

    }

    override fun createNode(parent: Entity?): Entity {
        return Entity(parent)
    }

}