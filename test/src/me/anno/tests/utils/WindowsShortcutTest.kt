package me.anno.tests.utils

import me.anno.io.files.Reference.getReference
import me.anno.io.links.WindowsShortcut
import me.anno.tests.LOGGER

fun main() {
    @Suppress("SpellCheckingInspection")
    val names = listOf(
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
        WindowsShortcut.get(file) { link, err ->
            err?.printStackTrace()
            if (link != null) LOGGER.info(
                "------ $name -----\n" +
                        "getRealFilename: ${link.absolutePath}\n" +
                        "getDescription: ${link.description}\n" +
                        "getRelativePath: ${link.relativePath}\n" +
                        "getWorkingDirectory: ${link.workingDirectory}\n" +
                        "getCommandLineArguments: ${link.commandLineArguments}\n" +
                        "isLocal: ${link.isLocalResource}\n" +
                        "isDirectory: ${link.isDirectory}\n" +
                        "iconPath: ${link.iconPath}"
            )
        }
    }
}
