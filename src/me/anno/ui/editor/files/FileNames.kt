package me.anno.ui.editor.files

import me.anno.config.DefaultConfig

object FileNames {

    private val forbiddenConfig =
        DefaultConfig["files.forbiddenCharacters", "<>:\"/\\|?*"] + String(CharArray(32) { it.toChar() })

    private val forbiddenCharacters = forbiddenConfig.toHashSet()

    /**
     * Ensures that a file name is actually allowed to be used.
     * If a file name is generally fine, the same name is returned.
     * If a name only needs slight modifications, that name is returned.
     * If a name is completely forbidden, this method returns null.
     *
     * https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
     * could be disabled for Linux only users
     * */
    fun String.toAllowedFilename(): String? {
        var name = filter { it !in forbiddenCharacters }
        name = name.trim()
        while (name.startsWith(".")) {
            name = name.substring(1).trim()
        }
        while (name.endsWith(".")) {
            name = name.substring(0, name.lastIndex).trim()
        }
        val split = name.split('.')
        when (split[0]) {// without extension
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4",
            "COM5", "COM6", "COM7", "COM8",
            "LPT1", "LPT2", "LPT3", "LPT4",
            "LPT5", "LPT6", "LPT7", "LPT8", "LPT9" -> return null
        }
        if (name.isEmpty()) return null
        return name
    }

}