package me.anno.io

import me.anno.utils.files.Files.openInExplorer
import me.anno.utils.types.Strings.isBlank2
import java.io.File
import java.nio.charset.Charset

/**
 * doesn't call toLowerCase() for each comparison,
 * so it's hopefully a lot faster
 *
 * we don't modify files a lot, but we do use them for comparisons a lot
 * because of that, this "performance-wrapper" exists
 * */
class FileReference(val file: File) {

    constructor():this("")
    constructor(parent: File, name: String) : this(File(parent, name))
    constructor(parent: FileReference, name: String) : this(File(parent.file, name))

    constructor(str: String) : this(File(str))

    val nameWithoutExtension = file.nameWithoutExtension
    val extension = file.extension

    val name = file.name
    val absolutePath = file.absolutePath
    val hashCode = absolutePath.hashCode()

    val hasValidName = !toString().isBlank2()
    fun hasValidName() = hasValidName

    fun inputStream() = file.inputStream()
    fun outputStream() = file.outputStream()

    fun readText() = file.readText()
    fun readText(charset: Charset) = file.readText(charset)

    fun readBytes() = file.readBytes()

    fun writeText(text: String) = file.writeText(text)
    fun writeText(text: String, charset: Charset) = file.writeText(text, charset)

    fun writeBytes(bytes: ByteArray) = file.writeBytes(bytes)

    fun length() = file.length()
    fun openInExplorer() = file.openInExplorer()
    fun deleteRecursively() = file.deleteRecursively()
    fun delete() = file.delete()
    fun mkdirs() = file.mkdirs()

    fun listFiles() = file.listFiles()
    fun list() = file.list()

    fun getParent() = if(file.parentFile == null) null else FileReference(file.parentFile)

    fun renameTo(newName: File) = file.renameTo(newName)
    fun renameTo(newName: FileReference) = file.renameTo(newName.file)

    val isDirectory get() = file.isDirectory

    fun lastModified() = file.lastModified()
    fun exists() = file.exists()
    fun toUri() = file.toURI()

    override fun equals(other: Any?): Boolean {
        return other is FileReference && other.hashCode == hashCode && other.absolutePath == absolutePath
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun toString(): String {
        return absolutePath
    }

    companion object {
        val empty = FileReference("")
    }

}