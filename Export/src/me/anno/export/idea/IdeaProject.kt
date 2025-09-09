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
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.mapAsync
import me.anno.utils.async.Callback.Companion.mapCallback
import java.io.InputStream

class IdeaProject(val projectDir: FileReference) {
    val modules = HashMap<String, IdeaModule>()
    val libraries = HashMap<String, IdeaLibrary>()

    companion object {

        var kotlinc by ConfigRef(
            "export.kotlin.stdlibLocation",
            getReference("C:/Program Files/IntelliJ IDEA/plugins/Kotlin/kotlinc")
        )

        fun loadProject(projectDir: FileReference, callback: Callback<IdeaProject>) {
            val result = IdeaProject(projectDir)
            loadProjectModules(projectDir, result, callback.mapAsync { result1, callback1 ->
                loadProjectLibraries(projectDir, result1, callback1)
            })
        }

        private fun loadProjectLibraries(
            projectDir: FileReference,
            result: IdeaProject,
            callback: Callback<IdeaProject>
        ) {
            val libDir = projectDir.getChild(".idea/libraries")
            val children = libDir.listChildren()
            children.mapCallback({ _, file, cb ->
                loadLibrary(result, file, cb)
            }, callback.map { libraries ->
                for (i in libraries.indices) {
                    val lib = libraries[i]
                    result.libraries[lib.name] = lib
                }
                result
            })
        }

        private fun loadProjectModules(
            projectDir: FileReference, result: IdeaProject,
            callback: Callback<IdeaProject>
        ) {
            loadModules(projectDir, callback.mapAsync { moduleFiles, then ->
                moduleFiles.mapCallback({ _, file, cb1 ->
                    loadModule(result, file, cb1)
                }, then.map { loadedModules ->
                    for (i in moduleFiles.indices) {
                        val moduleFile = moduleFiles[i]
                        val module = loadedModules[i]
                        result.modules[moduleFile.nameWithoutExtension] = module
                    }
                    result
                })
            })
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

        fun loadModules(projectDir: FileReference, callback: Callback<List<FileReference>>) {
            val moduleConfig = projectDir.getChild(".idea/modules.xml")
            if (!moduleConfig.exists) return callback.ok(emptyList())
            moduleConfig.inputStream(callback.map { loadModules(projectDir, it) })
        }

        private fun loadModules(projectDir: FileReference, moduleConfig: InputStream): List<FileReference> {
            val node0 = moduleConfig.use {
                XMLReader(it.reader()).readXMLNode()!!
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