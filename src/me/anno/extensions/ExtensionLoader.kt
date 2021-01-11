package me.anno.extensions

import me.anno.extensions.mods.Mod
import me.anno.extensions.plugins.Plugin
import java.io.BufferedReader
import java.io.File
import java.util.zip.ZipInputStream

object ExtensionLoader {

    val loadedMods = HashSet<Mod>()
    val loadedPlugins = HashSet<Plugin>()

    /**
     * removes all extensions, which have missing dependencies
     * */
    fun checkDependencies(extensions: Collection<ExtensionInfo>): HashSet<ExtensionInfo> {
        val remaining = HashSet(extensions)
        val remainingUUIDs = HashSet(extensions.map { it.uuid })
        val toRemove = ArrayList<ExtensionInfo>()
        while (true){
            for(extension in remaining){
                if(!remainingUUIDs.containsAll(extension.dependencies)){
                    toRemove += extension
                }
            }
            if(toRemove.isEmpty()){
                break
            } else {
                remaining.removeAll(toRemove)
                remainingUUIDs.removeAll(toRemove.map { it.uuid })
                toRemove.clear()
            }
        }
        return remaining
    }

    fun loadInfo(file: File): ExtensionInfo? {
        ZipInputStream(file.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name == "META-INF/MANIFEST.MF") {
                    val reader = BufferedReader(zis.reader())
                    var name = ""
                    var version = ""
                    var description = ""
                    var authors = ""
                    var mainClass = ""
                    var isPluginNotMod = true
                    var dependencies = ""
                    var uuid = ""
                    while (true) {
                        val line = reader.readLine() ?: break
                        val index = line.indexOf(':')
                        if (index > 0) {
                            val key = line.substring(0, index).trim()
                            val value = line.substring(index + 1).trim()
                            when (key.toLowerCase()) {
                                "plugin-name", "pluginname" -> {
                                    name = value
                                    isPluginNotMod = true
                                }
                                "plugin-class", "pluginclass" -> {
                                    mainClass = value
                                    isPluginNotMod = true
                                }
                                "mod-class", "modclass" -> {
                                    mainClass = value
                                    isPluginNotMod = false
                                }
                                "mod-name", "modname" -> {
                                    name = value
                                    isPluginNotMod = false
                                }
                                "plugin-version", "mod-version", "pluginversion", "modversion" -> version = value
                                "desc", "description", "mod-description", "moddescription",
                                "plugin-description", "plugindescription" -> description = value
                                "plugin-author", "plugin-authors",
                                "mod-author", "mod-authors",
                                "author", "authors" -> authors = value
                                "moddependencies", "mod-dependencies",
                                "plugindependencies", "plugin-dependencies",
                                "dependencies" -> dependencies += value
                                "plugin-uuid", "mod-uuid", "plugin-id", "mod-id", "uuid" -> uuid = value
                            }
                        }
                    }
                    uuid = uuid.trim()
                    if(uuid.isEmpty()) uuid = name.trim()
                    uuid = uuid.toLowerCase()
                    if(name.isNotEmpty()){
                        val dependencyList =
                            dependencies.toLowerCase().split(',')
                                .map { it.trim() }.filter { it.isNotEmpty() }
                        return ExtensionInfo(
                            uuid, file,
                            name, description, version, authors,
                            mainClass, isPluginNotMod, dependencyList
                        )
                    }
                }
            }
        }
        return null
    }

}