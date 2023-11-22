package me.anno.tests.utils

import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    @Suppress("SpellCheckingInspection")
    val names = arrayOf(
        /* "src/test/fixtures/Local file.lnk",
                       "src/test/fixtures/Local folder.lnk",
                       "src/test/fixtures/Remote folder.lnk",
                       "src/test/fixtures/Remote folder (mapped to X-drive).lnk",
                       "src/test/fixtures/Hazarsolve Eduction Tubes II.pdf - Shortcut.lnk",
                       "src/test/fixtures/HSBD-10AS Instructions.pdf - Shortcut.lnk",
                       "src/test/fixtures/HSBD-50E Instructions.pdf - Shortcut.lnk",
                       "src/test/fixtures/test.pdf - Shortcut.lnk",*/
        "C:/Users/Antonio/Desktop/Eclipse.lnk",
        "C:/Users/Antonio/Desktop/Eclipse1.lnk",
    )
    for (name in names) {
        val file = getReference(name)
        val link = file.windowsLnk.value!!
        println("------ $name -----")
        println("getRealFilename: ${link.absolutePath}")
        println("getDescription: ${link.description}")
        println("getRelativePath: ${link.relativePath}")
        println("getWorkingDirectory: ${link.workingDirectory}")
        println("getCommandLineArguments: ${link.commandLineArguments}")
        println("isLocal: ${link.isLocalResource}")
        println("isDirectory: ${link.isDirectory}")
        println("iconPath: ${link.iconPath}")
    }
}
