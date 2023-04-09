package me.anno.utils.files

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.io.File
import java.util.*

object Files {

    fun FileReference.findNextChild(
        nameWithoutExtension: String,
        extension: String, digitsLength: Int, colonSymbol: Char, startingNumber: Long = 1
    ): FileReference {
        return getChild(
            findNextFileName(
                this, nameWithoutExtension, extension,
                digitsLength, colonSymbol, startingNumber
            )
        )
    }

    fun findNextFileName(
        parent: FileReference,
        nameWithoutExtension: String,
        extension: String,
        @Suppress("unused_parameter")
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): String {
        // format: name-1.json
        // -, because the usual name may contain numbers itself
        // find all files matching the description, and then use the max+1
        val default = if (extension.isEmpty()) nameWithoutExtension else "$nameWithoutExtension.$extension"
        val siblings = parent.listChildren() ?: return default
        if (default !in siblings.map { it.name }) return default
        val prefix = if (colonSymbol.code == 0) nameWithoutExtension else "$nameWithoutExtension$colonSymbol"
        // val nameLength = prefix.length + digitsLength
        val relatedFiles = siblings.filter {
            it.extension == extension && it.nameWithoutExtension.startsWith(prefix)
            // it.nameWithoutExtension.length == nameLength
        }.mapNotNull {
            val name2 = it.nameWithoutExtension
            var i = prefix.length
            while (i + 1 < name2.length && name2[i] == '0') {
                i++
            }
            name2.substring(i).toLongOrNull() // ^^, long for large names
        }
        val maxNumber = relatedFiles.maxOrNull() ?: startingNumber
        val nextNumber = maxNumber + 1
        return if (extension.isEmpty()) "$prefix$nextNumber" else "$prefix$nextNumber.$extension"
    }

    fun findNextFile(
        parent: FileReference,
        name: FileReference,
        extension: String,
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): FileReference {
        val newName = findNextFileName(
            parent, name.nameWithoutExtension, extension,
            digitsLength, colonSymbol, startingNumber
        )
        return parent.getChild(newName)
    }

    fun findNextFile(
        parent: FileReference,
        name: FileReference,
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): FileReference {
        val newName = findNextFileName(
            parent, name.nameWithoutExtension, name.extension,
            digitsLength, colonSymbol, startingNumber
        )
        return parent.getChild(newName)
    }

    fun findNextFile(
        parent: FileReference,
        nameWithoutExtension: String,
        extension: String,
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): FileReference {
        val newName = findNextFileName(
            parent, nameWithoutExtension, extension,
            digitsLength, colonSymbol, startingNumber
        )
        return parent.getChild(newName)
    }

    fun findNextFile(
        reference: FileReference,
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): FileReference {
        // format: name-1.json
        // -, because the usual name may contain numbers itself
        // find all files matching the description, and then use the max+1
        if (!reference.exists) return reference
        val parent = reference.getParent() ?: return InvalidRef
        val name = reference.nameWithoutExtension
        val extension = reference.extension
        val newName = findNextFileName(parent, name, extension, digitsLength, colonSymbol, startingNumber)
        return parent.getChild(newName)
    }

    fun findNextFileName(
        reference: FileReference,
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): String {
        // format: name-1.json
        // -, because the usual name may contain numbers itself
        // find all files matching the description, and then use the max+1
        if (!reference.exists) return reference.name
        val parent = reference.getParent() ?: return reference.name
        val name = reference.nameWithoutExtension
        val extension = reference.extension
        return findNextFileName(parent, name, extension, digitsLength, colonSymbol, startingNumber)
    }

    fun findNextName(name: String, separator: Char, startingNumber: Long = 1): String {
        // find how long the number is
        var numLength = 0
        for (i in name.lastIndex downTo 0) {
            if (name[i] in '0'..'9') {
                numLength++
            } else break
        }
        // create new name
        val newNumber: Long
        var nameEndIndex: Int
        if (numLength == 0) {
            nameEndIndex = name.length
            newNumber = startingNumber
        } else {
            val splitIndex = name.length - numLength
            nameEndIndex = splitIndex
            newNumber = name.substring(splitIndex, name.length).toLong() + 1
        }
        if (nameEndIndex > 0 && name[nameEndIndex - 1] == separator) nameEndIndex--
        val partString = name.substring(0, nameEndIndex)
        return if (separator.code == 0) "$partString$newNumber"
        else "$partString$separator$newNumber"
    }

    fun Long.formatFileSize(): String =
        formatFileSize(if (DefaultConfig["ui.file.showGiB", true]) 1024 else 1000)

    fun Long.formatFileSize(divider: Int): String {
        if (this < 0) return "-" + (-this).formatFileSize()
        val endings = "kMGTPEZY"
        val suffix = if (divider == 1024) "i" else ""
        val halfDivider = divider / 2
        var v = this
        if (v < halfDivider) return "$v Bytes"
        for (prefix in endings) {
            val vSaved = v
            v = (v + halfDivider) / divider
            if (v < divider) {
                return "${
                    when (v) {
                        in 0..9 -> "%.2f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                        in 10..99 -> "%.1f".format(Locale.ENGLISH, (vSaved.toFloat() / divider))
                        else -> v.toString()
                    }
                } ${prefix}${suffix}B"
            }
        }
        return "$v ${endings.last()}${suffix}B"
    }

    fun FileReference.listFiles2(includeHiddenFiles: Boolean = OS.isWindows) = listChildren()?.filter {
        !it.name.equals("desktop.ini", true) && (!name.startsWith('.') || includeHiddenFiles)
    } ?: emptyList()

    fun File.openInExplorer() {
        if (!exists()) {
            parentFile?.openInExplorer() ?: LOGGER.warn("Cannot open file $this, as it does not exist!")
        } else {
            when {
                OS.isWindows -> {// https://stackoverflow.com/questions/2829501/implement-open-containing-folder-and-highlight-file
                    OS.startProcess("explorer.exe", "/select,", absolutePath)
                }
                Desktop.isDesktopSupported() -> {
                    val desktop = Desktop.getDesktop()
                    desktop.open(if (isDirectory) this else this.parentFile ?: this)
                }
                OS.isLinux -> {// https://askubuntu.com/questions/31069/how-to-open-a-file-manager-of-the-current-directory-in-the-terminal
                    OS.startProcess("xdg-open", absolutePath)
                }
                else -> LOGGER.warn("File.openInExplorer() is not implemented on that platform")
            }
        }
    }

    private val LOGGER = LogManager.getLogger(Files::class)

}