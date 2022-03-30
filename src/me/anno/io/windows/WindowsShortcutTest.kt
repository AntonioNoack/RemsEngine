package me.anno.io.windows

import java.io.File

fun main() {
    val filenames = arrayOf( /* "src/test/fixtures/Local file.lnk",
                "src/test/fixtures/Local folder.lnk",
                "src/test/fixtures/Remote folder.lnk",
                "src/test/fixtures/Remote folder (mapped to X-drive).lnk",
                "src/test/fixtures/Hazarsolve Eduction Tubes II.pdf - Shortcut.lnk",
                "src/test/fixtures/HSBD-10AS Instructions.pdf - Shortcut.lnk",
                "src/test/fixtures/HSBD-50E Instructions.pdf - Shortcut.lnk",
                "src/test/fixtures/test.pdf - Shortcut.lnk",*/
        "C:/Users/Antonio/Desktop/Eclipse.lnk"
    )
    for (filename in filenames) {
        val file = File(filename)
        val link = WindowsShortcut(file)
        System.out.printf("-------%s------ \n", filename)
        System.out.printf("getRealFilename: %s \n", link.absolutePath)
        System.out.printf("getDescription: %s \n", link.description)
        System.out.printf("getRelativePath: %s \n", link.relativePath)
        System.out.printf("getWorkingDirectory: %s \n", link.workingDirectory)
        System.out.printf("getCommandLineArguments: %s \n", link.commandLineArguments)
        System.out.printf("isLocal: %b \n", link.isLocal)
        System.out.printf("isDirectory: %b \n", link.isDirectory)
    }
}
