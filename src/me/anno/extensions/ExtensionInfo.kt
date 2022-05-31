package me.anno.extensions

import me.anno.engine.ui.ComponentUI.instanceOf
import me.anno.extensions.plugins.Plugin
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import kotlin.reflect.KClass

class ExtensionInfo(
    val uuid: String, val file: FileReference,
    val name: String, val description: String,
    val version: String, val authors: String,
    val minVersion: Int, val maxVersion: Int,
    val mainClass: String, val isPluginNotMod: Boolean,
    val priority: Double,
    val dependencies: List<String>
) {

    constructor(clazz: KClass<*>) :
            this(
                clazz.qualifiedName!!, InvalidRef,
                clazz.simpleName!!, "", "", "",
                0, 0, clazz.qualifiedName!!, instanceOf(clazz, Plugin::class),
                0.0, emptyList()
            )

    var clazz: KClass<*>? = null

}