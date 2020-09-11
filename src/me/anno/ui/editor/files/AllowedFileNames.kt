package me.anno.ui.editor.files

import java.io.File

// https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
// could be disabled for Linux only users
fun toAllowedFilename(name: String): String? {
    var name = name.filter { it !in FileExplorer.forbiddenCharacters }.trim()
    while(name.startsWith(".")){
        name = name.substring(1).trim()
    }
    while(name.endsWith(".")){
        name = name.substring(0, name.lastIndex).trim()
    }
    val split = name.split('.')
    when(split[0]){// without extension
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4",
        "COM5", "COM6", "COM7", "COM8",
        "LPT1", "LPT2", "LPT3", "LPT4",
        "LPT5", "LPT6", "LPT7", "LPT8", "LPT9" -> return null
    }
    if(name.isEmpty()) return null
    return name
}

fun File?.hasValidName() = this != null && toString().isNotBlank()