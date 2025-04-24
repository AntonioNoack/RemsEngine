package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.utils.types.Strings.indexOf2

object FileNames {

    private val forbiddenConfig =
        DefaultConfig["files.forbiddenCharacters", "<>:\"/\\|?*"] + CharArray(32) { it.toChar() }.concatToString()

    private val forbiddenNames = ("CON,PRN,AUX,NUL," +
            "COM¹,COM²,COM³,COM1,COM2,COM3,COM4,COM5,COM6,COM7,COM8,COM9," +
            "LPT¹,LPT²,LPT³,LPT1,LPT2,LPT3,LPT4,LPT5,LPT6,LPT7,LPT8,LPT9")
        .split(',')

    private val forbiddenCharacters = forbiddenConfig.toSet()

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
        if (name == "" || name == "." || name == "..") return null
        val i = name.indexOf2('.')
        if (name.substring(0, i) in forbiddenNames) return null
        return name
    }
}