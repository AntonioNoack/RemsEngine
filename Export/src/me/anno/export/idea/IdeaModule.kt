package me.anno.export.idea

import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.assertions.assertEquals
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import java.io.InputStream

class IdeaModule(val project: IdeaProject) {
    val moduleDependencies = ArrayList<String>()
    val libraryDependencies = ArrayList<String>()

    companion object {
        fun loadModule(project: IdeaProject, source: FileReference, callback: Callback<IdeaModule>) {
            source.inputStream(callback.map { stream -> loadModule(project, stream) })
        }

        fun loadModule(project: IdeaProject, source: InputStream): IdeaModule {
            val root = source.use {
                XMLReader(it.reader()).read() as XMLNode
            }
            assertEquals("module", root.type)
            val modManager = root.children
                .filterIsInstance<XMLNode>()
                .first { it.type == "component" && it["name"] == "NewModuleRootManager" }
            val orderEntries = modManager.children
                .filterIsInstance<XMLNode>()
                .filter { it.type == "orderEntry" }
            val result = IdeaModule(project)
            for (entry in orderEntries) {
                when (entry["type"]) {
                    "library" -> result.libraryDependencies.add(entry["name"]!!)
                    "module" -> result.moduleDependencies.add(entry["module-name"]!!)
                }
            }
            return result
        }
    }
}