package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ui.editor.files.FileContentImporter
import java.io.File

object ECSFileImporter: FileContentImporter<Entity>() {

    override fun import(
        parent: Entity?,
        file: File,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (Entity) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun createNode(parent: Entity?): Entity {
        return Entity(parent)
    }

}