package me.anno.export.platform

import me.anno.io.saveable.AutoSaveable

class MacOSPlatforms : AutoSaveable() {
    var x64 = true
    var arm64 = true
    val any get() = x64 or arm64
}