package me.anno.export.platform

import me.anno.io.saveable.AutoSaveable
import me.anno.utils.OS.documents

class WindowsPlatforms : AutoSaveable() {
    var x64 = true
    var x86 = false // 32-bit windows
    var arm64 = true
    val any get() = x64 or x86 or arm64
    var exeBaseLocation = documents.getChild("IdeaProjects/RemsEngine/Export/cpp/JarToExe/x64/Release/JarToExe.exe")
}