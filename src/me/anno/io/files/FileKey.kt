package me.anno.io.files

data class FileKey(val file: FileReference, val lastModified: Long) {

    private val absolutePath get() = file.absolutePath
    private val hash = absolutePath.hashCode() * 31 + lastModified.hashCode()

    override fun toString(): String = absolutePath
    override fun hashCode(): Int = hash
    override fun equals(other: Any?): Boolean {
        return other is FileKey &&
                other.absolutePath == absolutePath &&
                other.lastModified == lastModified
    }
}