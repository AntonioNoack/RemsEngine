package me.anno.tests

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.utils.WindowsShortcut

fun main() {
    @Suppress("SpellCheckingInspection")
    val names = arrayOf( /* "src/test/fixtures/Local file.lnk",
                "src/test/fixtures/Local folder.lnk",
                "src/test/fixtures/Remote folder.lnk",
                "src/test/fixtures/Remote folder (mapped to X-drive).lnk",
                "src/test/fixtures/Hazarsolve Eduction Tubes II.pdf - Shortcut.lnk",
                "src/test/fixtures/HSBD-10AS Instructions.pdf - Shortcut.lnk",
                "src/test/fixtures/HSBD-50E Instructions.pdf - Shortcut.lnk",
                "src/test/fixtures/test.pdf - Shortcut.lnk",*/
        "C:/Users/Antonio/Desktop/Eclipse.lnk"
    )
    for (name in names) {
        val file = getReference(name)
        val link = WindowsShortcut(file)
        System.out.printf("------ %s -----\n", name)
        System.out.printf("getRealFilename: %s\n", link.absolutePath)
        System.out.printf("getDescription: %s\n", link.description)
        System.out.printf("getRelativePath: %s\n", link.relativePath)
        System.out.printf("getWorkingDirectory: %s\n", link.workingDirectory)
        System.out.printf("getCommandLineArguments: %s\n", link.commandLineArguments)
        System.out.printf("isLocal: %b\n", link.isLocal)
        System.out.printf("isDirectory: %b\n", link.isDirectory)
    }
}
