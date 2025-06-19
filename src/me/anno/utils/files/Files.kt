package me.anno.utils.files

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.utils.OS
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.f2
import me.anno.utils.types.Strings.ifBlank2

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
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): String {
        // format: name-1.json
        // -, because the usual name may contain numbers itself
        // find all files matching the description, and then use the max+1
        val siblings = parent.listChildren()
        val prefix = if (colonSymbol.code == 0) nameWithoutExtension else "$nameWithoutExtension$colonSymbol"
        val relatedFiles = siblings.filter {
            it.extension == extension && it.nameWithoutExtension.startsWith(prefix)
        }.mapNotNull {
            val name2 = it.nameWithoutExtension
            name2.substring(prefix.length).toLongOrNull() // ^^, long for large names
        }
        val maxNumber = relatedFiles.maxOrNull() ?: startingNumber
        val nextNumber = (maxNumber + 1).toString().padStart(digitsLength, '0')
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
        sibling: FileReference,
        digitsLength: Int,
        colonSymbol: Char,
        startingNumber: Long = 1
    ): FileReference {
        // format: name-1.json
        // -, because the usual name may contain numbers itself
        // find all files matching the description, and then use the max+1
        if (!sibling.exists) return sibling
        val parent = sibling.getParent()
        val newName = findNextFileName(
            parent, sibling.nameWithoutExtension, sibling.extension,
            digitsLength, colonSymbol, startingNumber
        )
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

    fun findNextName(name: String, separator: Char, startingNumber: Long = 1, digitsLength: Int = 1): String {
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
        val newNumberStr = "$newNumber".padStart(digitsLength, '0')
        return if (separator.code == 0) "$partString$newNumberStr"
        else "$partString$separator$newNumberStr"
    }

    fun nextName(pathName0: String, usedNames: HashSet<String>): String {
        var pathName = pathName0.ifBlank2("Node")
        while (pathName in usedNames) {
            pathName = findNextName(pathName, '-')
        }
        usedNames.add(pathName)
        return pathName
    }

    fun Long.formatFileSize(): String =
        formatFileSize(if (DefaultConfig["ui.file.showGiB", true]) 1024 else 1000)

    fun Int.formatFileSize(): String =
        toLong().formatFileSize()

    fun Long.formatFileSize(divider: Int): String {
        if (this == 1L) return "1 Byte"
        if (this < 0) return "-" + (-this).formatFileSize()
        val halfDivider = divider.ushr(1)
        if (this < halfDivider) return "$this Bytes"

        val endings = "kMGTPEZY"
        val suffix = if (divider == 1024) "i" else ""
        var power = divider.toLong()
        for (prefix in endings) {
            val nextPower = power * divider
            val halfPower = power.ushr(1)
            val v = limited(this + halfPower) / power
            if (v < divider || nextPower < power) {
                return "${
                    when (v) {
                        in 0..9 -> (this.toDouble() / power).f2()
                        in 10..99 -> (this.toDouble() / power).f1()
                        else -> v.toString()
                    }
                } ${prefix}${suffix}B"
            }
            power = nextPower
        }
        return "${toLong()} Bytes" // shouldn't happen
    }

    private fun limited(sum: Long): Long {
        return if (sum < 0) Long.MAX_VALUE else sum
    }
}