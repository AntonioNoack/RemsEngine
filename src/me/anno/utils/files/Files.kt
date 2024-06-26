package me.anno.utils.files

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.utils.OS
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.f2

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
        val siblings = parent.listChildren()
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
        val parent = reference.getParent()
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
        val parent = reference.getParent()
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

    fun Int.formatFileSize(): String =
        toLong().formatFileSize()

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
                        in 0..9 -> (vSaved.toFloat() / divider).f2()
                        in 10..99 -> (vSaved.toFloat() / divider).f1()
                        else -> v.toString()
                    }
                } ${prefix}${suffix}B"
            }
        }
        return "$v ${endings.last()}${suffix}B"
    }

    fun FileReference.listFiles2(includeHiddenFiles: Boolean = OS.isWindows) = listChildren().filter {
        !it.name.equals("desktop.ini", true) && (!name.startsWith('.') || includeHiddenFiles)
    }
}