package me.anno.tests.tools

import me.anno.io.zip.ExeSkipper.getBytesAfterExeSections
import me.anno.utils.OS.desktop

fun main() {
    val src = desktop.getChild("RemsStudio 1.3.2.exe")
    desktop.getChild("Extracted.zip").writeBytes(getBytesAfterExeSections(src))
}