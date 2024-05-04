package me.anno.extensions

import me.anno.engine.ui.input.ComponentUI.instanceOf
import me.anno.extensions.plugins.Plugin
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
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

    override fun toString(): String {
        return "Extension { $uuid, '$name', $file, '$description', " +
                "version '$version', authors '$authors', dependencies '$dependencies' }"
    }
}