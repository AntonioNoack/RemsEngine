package me.anno.extensions

import me.anno.io.files.FileReference

class ExtensionInfo(
    val uuid: String, val file: FileReference,
    val name: String, val description: String,
    val version: String, val authors: String,
    val minVersion: Int, val maxVersion: Int,
    val mainClass: String, val isPluginNotMod: Boolean,
    val dependencies: List<String>
)