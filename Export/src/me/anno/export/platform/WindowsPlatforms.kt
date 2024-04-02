package me.anno.export.platform

import me.anno.io.AutoSaveable

class WindowsPlatforms : AutoSaveable() {
    var x64 = true
    var x86 = false // 32-bit windows
    var arm64 = true
}