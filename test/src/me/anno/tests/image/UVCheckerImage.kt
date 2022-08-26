package me.anno.tests.image

import me.anno.utils.OS.desktop
import me.anno.utils.files.UVChecker

fun main() {
    UVChecker.value.image.write(desktop.getChild("checker.png"))
}