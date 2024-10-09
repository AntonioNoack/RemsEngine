package me.anno.export.idea

import me.anno.export.idea.IdeaProject.Companion.parseFile
import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.assertions.assertEquals

class IdeaLibrary(val project: IdeaProject, val name: String) {
    val jars = ArrayList<FileReference>()

    companion object {
        fun loadLibrary(project: IdeaProject, source: FileReference): IdeaLibrary {
            val root = source.inputStreamSync().use {
                XMLReader().read(it.reader()) as XMLNode
            }
            assertEquals("component", root.type)
            assertEquals("libraryTable", root["name"])
            val data = root.children
                .filterIsInstance<XMLNode>()
                .first { it.type == "library" }
            val result = IdeaLibrary(project, data["name"]!!)
            val classes = data.children
                .filterIsInstance<XMLNode>()
                .firstOrNull { it.type == "CLASSES" } ?: return result
            val jars = classes.children
                .filterIsInstance<XMLNode>()
                .filter { it.type == "root" }
                .mapNotNull { it["url"] }
                .map { parseFile(it, project.projectDir) }
            result.jars.addAll(jars)
            return result
        }
    }
}