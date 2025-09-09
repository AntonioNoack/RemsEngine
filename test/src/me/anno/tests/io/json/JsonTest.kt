package me.anno.tests.io.json

import me.anno.export.ExportSettings
import me.anno.export.platform.LinuxPlatforms
import me.anno.export.platform.MacOSPlatforms
import me.anno.export.platform.WindowsPlatforms
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.OS

fun main() {
    registerCustomClass(ExportSettings::class)
    registerCustomClass(WindowsPlatforms::class)
    registerCustomClass(LinuxPlatforms::class)
    registerCustomClass(MacOSPlatforms::class)
    val source = OS.home.getChild(".config/RemsEngine/Export.json")
    val instances = JsonStringReader.read(source.readTextSync(), InvalidRef, false)
    println(instances)
}