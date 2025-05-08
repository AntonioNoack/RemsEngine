package me.anno.tests.io

import me.anno.export.ExportSettings
import me.anno.export.platform.LinuxPlatforms
import me.anno.export.platform.MacOSPlatforms
import me.anno.export.platform.WindowsPlatforms
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.saveable.Saveable.Companion.registerCustomClass

fun main() {
    registerCustomClass(ExportSettings::class)
    registerCustomClass(WindowsPlatforms::class)
    registerCustomClass(LinuxPlatforms::class)
    registerCustomClass(MacOSPlatforms::class)
    val source = getReference("C:/Users/Antonio/.config/RemsEngine/Export.json")
    val instances = JsonStringReader.read(source, InvalidRef, false)
    println(instances)
}