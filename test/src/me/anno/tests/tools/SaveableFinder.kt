package me.anno.tests.tools

import me.anno.config.DefaultConfig.style
import me.anno.io.saveable.Saveable
import me.anno.io.files.FileReference
import me.anno.ui.Style
import me.anno.utils.OS.documents
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.isName
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Modifier

class SaveableFinder {

    val dst = HashMap<String, String>()
    val clazzMap = HashMap<Class<*>, Boolean>()

    val saveableClass = Saveable::class.java

    fun isChildClass(clazz: Class<*>?): Boolean {
        clazz ?: return false
        return clazzMap.getOrPut(clazz) {
            clazz == saveableClass || isChildClass(clazz.superclass)
        }
    }

    private fun put(sample: Saveable, classPath: String) {
        dst[sample.className] = classPath
        val name = classPath.substring(classPath.lastIndexOf('.') + 1)
        if (name != sample.className) LOGGER.warn("Non-trivial: $name -> ${sample.className}")
    }

    fun index(file: FileReference) {
        if (file.isDirectory) {
            for (child in file.listChildren()) {
                index(child)
            }
        } else if (file.lcExtension == "kt") {
            // todo find out if any class in this file is
            //  - Saveable,
            //  - has easy constructor
            val lines = file.readLinesSync(256)
            var packagePath = ""
            for (line in lines) {
                if (line.startsWith("package ")) {
                    packagePath = line.substring(8)
                }
                var i = 0
                while (true) {
                    i = line.indexOf("class ", i) + 6
                    if (i < 6) break
                    val j = " (<:".minOf { line.indexOf2(it, i) }
                    if (j == line.length) break
                    val name = line.substring(i, j)
                    if (!isName(name)) continue
                    val classPath = "$packagePath.$name"
                    try {
                        val clazz = Class.forName(classPath)
                        if (Modifier.isAbstract(clazz.modifiers) || !isChildClass(clazz)) continue
                        try {
                            val sample = clazz.newInstance() as Saveable
                            put(sample, classPath)
                        } catch (ignored: IllegalAccessException) {
                        } catch (ignored: NoSuchMethodException) {
                        } catch (ignored: InstantiationException) {
                            try {
                                val sample = clazz.getConstructor(Style::class.java).newInstance(style) as Saveable
                                put(sample, classPath)
                            } catch (ignored: InstantiationException) {
                            } catch (e: IllegalAccessException) {
                                e.printStackTrace()
                            } catch (ignored: NoSuchMethodException) {
                            }
                        }
                    } catch (e: ClassNotFoundException) {
                        LOGGER.info("Ignored $classPath")
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SaveableFinder::class)

        @JvmStatic
        fun main(args: Array<String>) {
            val instance = SaveableFinder()
            val project = documents.getChild("IdeaProjects/RemsEngine")
            val sources = listOf(
                "src",
                "Bullet/src",
                "Box2d/src",
                "Lua/src",
                "Mesh/src",
                "SDF/src",
                "Video/src",
                "Unpack/src",
                "Network/src",
                "OpenXR/src",
                "PDF/src",
                "Recast/src",
                "Export/src",
            )
            for (source in sources) {
                instance.index(project.getChild(source))
            }
            println(instance.dst.toList()
                .sortedBy { it.first }
                .joinToString("\n") { (k, v) -> "$k: $v" })
        }
    }
}