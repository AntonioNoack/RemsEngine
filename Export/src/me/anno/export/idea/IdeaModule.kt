package me.anno.export.idea

import me.anno.io.files.FileReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.assertions.assertEquals

class IdeaModule(val project: IdeaProject) {
    val moduleDependencies = ArrayList<String>()
    val libraryDependencies = ArrayList<String>()

    companion object {
        fun loadModule(project: IdeaProject, source: FileReference): IdeaModule {
            val result = IdeaModule(project)
            val root = source.inputStreamSync().use {
                XMLReader().read(it) as XMLNode
            }
            assertEquals("module", root.type)
            val modManager = root.children
                .filterIsInstance<XMLNode>()
                .first { it.type == "component" && it["name"] == "NewModuleRootManager" }
            val orderEntries = modManager.children
                .filterIsInstance<XMLNode>()
                .filter { it.type == "orderEntry" }
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