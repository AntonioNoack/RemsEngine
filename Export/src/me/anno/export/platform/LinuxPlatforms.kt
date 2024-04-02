package me.anno.export.platform

import me.anno.io.AutoSaveable

class LinuxPlatforms : AutoSaveable() {
    var x64 = true
    var arm64 = true
    var arm32 = false
}