package me.anno.engine.ui

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.io.files.FileReference
import me.anno.io.text.TextReader
import me.anno.objects.Transform
import me.anno.ui.editor.files.FileContentImporter
import me.anno.utils.types.Strings.getImportType
import org.apache.logging.log4j.LogManager

object ECSFileImporter : FileContentImporter<Entity>() {

    private val LOGGER = LogManager.getLogger(ECSFileImporter::class)

    override fun import(
        parent: Entity?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (Entity) -> Unit
    ) {

        parent!!

        // todo undo history, just like for Rem's Studio

        // todo an entity needs to know whether it's environment is 2D or 3D

        // todo depending on the filetype, import the structure
        // todo double mapping for customization? mod-ability?
        when (file.extension.getImportType()) {
            /*"Image", "Video" -> {
                // todo create an image
                // todo 2D or 3D? (ui vs world)
            }
            "Audio" -> {
                // todo place audio source
            }*/
            "Transform", "Entity" -> {
                // try to parse as an entity
                // (or an transform)
                when (val content = TextReader.fromText(file.readText()).first()) {
                    is Component -> {
                        parent.add(content)
                    }
                    is Entity -> {
                        parent.add(content)
                    }
                    is Transform -> {
                        // todo convert it somehow...
                        LOGGER.warn("Converting Rem's Studio to Rem's Engine objects not yet implemented")
                    }
                    else -> {
                        LOGGER.warn("Could not understand element... $content")
                    }
                }
            }
            /*
            "Cubemap-Equ" -> {
                 // todo add cubemap
             }
             "Cubemap-Tiles" -> {
                 // todo add cubemap
             }
             "Mesh" -> {

             }
             "PDF" -> {

             }
             "HTML" -> {

             }
             "Markdown", "Markdeep" -> {

             }
            "Text" -> {
                // todo import text, 2D or 3D
            }*/
            else -> {
                // todo import as text
                // todo 2D or 3D? (ui vs world)
            }
        }
    }

    override fun createNode(parent: Entity?): Entity {
        return Entity(parent)
    }

}