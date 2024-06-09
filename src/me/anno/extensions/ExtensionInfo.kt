package me.anno.extensions

import me.anno.engine.ui.input.ComponentUI.instanceOf
import me.anno.extensions.plugins.Plugin
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.yaml.generic.SimpleYAMLReader
import me.anno.utils.types.Ints.toIntOrDefault
import kotlin.reflect.KClass

class ExtensionInfo() {

    constructor(clazz: KClass<*>) : this() {
        uuid = clazz.qualifiedName!!
        name = clazz.simpleName!!
        isPluginNotMod = instanceOf(clazz, Plugin::class)
    }

    var uuid = ""
    var file: FileReference = InvalidRef
    var name = ""
    var description = ""
    var version = ""
    var authors = ""
    var minVersion = 0
    var maxVersion = Int.MAX_VALUE
    var mainClass = ""
    var isPluginNotMod = false
    var priority = 0.0
    var dependencies: List<String> = emptyList()
    var clazz: KClass<*>? = null

    fun loadFromTxt(infoFile: FileReference): ExtensionInfo? {
        val properties = SimpleYAMLReader.read(infoFile.readLinesSync(1024))
        val dependencies = ArrayList<String>()
        for ((key, value) in properties) {
            @Suppress("SpellCheckingInspection")
            when (key.lowercase()) {
                "plugin-name", "pluginname", "name" -> {
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
                "mainclass", "main-class" -> {
                    mainClass = value
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
                "dependencies" -> dependencies += value.split(',').filter { it.isNotBlank() }.map { it.trim() }
                "plugin-uuid", "mod-uuid", "plugin-id", "mod-id", "uuid" -> uuid = value
                "minversion", "min-version" -> minVersion = value.toIntOrDefault(minVersion)
                "maxversion", "max-version" -> maxVersion = value.toIntOrDefault(maxVersion)
                "priority" -> priority = value.toDoubleOrNull() ?: priority
            }
        }
        if (uuid.isEmpty()) uuid = name.trim()
        return if(name.isNotEmpty()) this else null
    }

    override fun toString(): String {
        return "Extension { $uuid, '$name', $file, '$description', " +
                "version '$version', authors '$authors', dependencies '$dependencies' }"
    }
}