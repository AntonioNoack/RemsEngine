package me.anno.io.files

class FileKey(val file: FileReference, val lastModified: Long) {

    private val absolutePath get() = file.absolutePath
    private val hash = absolutePath.hashCode() * 31 + lastModified.hashCode()

    operator fun component1() = file
    operator fun component2() = lastModified

    override fun hashCode(): Int {
        return hash
    }

    override fun equals(other: Any?): Boolean {
        return other is FileKey &&
                other.absolutePath == absolutePath &&
                other.lastModified == lastModified
    }
}