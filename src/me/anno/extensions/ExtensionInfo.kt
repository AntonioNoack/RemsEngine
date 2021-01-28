package me.anno.extensions

import java.io.File

class ExtensionInfo(
    val uuid: String, val file: File,
    val name: String, val description: String,
    val version: String, val authors: String,
    val minVersion: Int, val maxVersion: Int,
    val mainClass: String, val isPluginNotMod: Boolean,
    val dependencies: List<String>
) {
}