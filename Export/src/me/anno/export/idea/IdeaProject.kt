package me.anno.export.idea

import me.anno.config.ConfigRef
import me.anno.export.idea.IdeaLibrary.Companion.loadLibrary
import me.anno.export.idea.IdeaModule.Companion.loadModule
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.OS
import me.anno.utils.assertions.assertEquals

class IdeaProject(val projectDir: FileReference) {
    val modules = HashMap<String, IdeaModule>()
    val libraries = HashMap<String, IdeaLibrary>()

    companion object {

        var kotlinc by ConfigRef(
            "export.kotlin.stdlibLocation",
            getReference("C:/Program Files/IntelliJ IDEA/plugins/Kotlin/kotlinc")
        )

        fun loadProject(projectDir: FileReference): IdeaProject {
            val result = IdeaProject(projectDir)
            val modules = loadModules(projectDir)
            for (file in modules) {
                result.modules[file.nameWithoutExtension] = loadModule(result, file)
            }
            val libDir = projectDir.getChild(".idea/libraries")
            for (file in libDir.listChildren()) {
                val lib = loadLibrary(result, file)
                result.libraries[lib.name] = lib
            }
            return result
        }

        private val mavenHome = OS.home.getChild(".m2/repository")
        fun parseFile(file: String, projectDir: FileReference): FileReference {
            return when {
                file.startsWith("jar://") && file.endsWith("!/") -> {
                    parseFile(file.substring(6, file.length - 2), projectDir)
                }
                else -> getReference(
                    file // is there more of these?
                        .replace("\$PROJECT_DIR\$", projectDir.absolutePath)
                        .replace("\$USER_HOME\$", OS.home.absolutePath)
                        // do we need to look up the environment variable for that?
                        .replace("\$MAVEN_REPOSITORY\$", mavenHome.absolutePath)
                        .replace("\$KOTLIN_BUNDLED\$", kotlinc.absolutePath)
                )
            }
        }

        fun loadModules(projectDir: FileReference): List<FileReference> {
            val moduleConfig = projectDir.getChild(".idea/modules.xml")
            if (!moduleConfig.exists) return emptyList()
            val node0 = moduleConfig.inputStreamSync().use {
                XMLReader().read(it.reader()) as XMLNode
            }
            assertEquals("project", node0.type)
            val node1 = node0.children.filterIsInstance<XMLNode>()
                .first { it.type == "component" && it["name"] == "ProjectModuleManager" }
            val node2 = node1.children.filterIsInstance<XMLNode>()
                .first { it.type == "modules" }
            return node2.children
                .filterIsInstance<XMLNode>()
                .filter { it.type == "module" }
                .mapNotNull { it["filepath"] }
                .map { parseFile(it, projectDir) }
        }
    }
}