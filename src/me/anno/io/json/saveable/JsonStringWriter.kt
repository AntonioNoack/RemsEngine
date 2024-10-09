package me.anno.io.json.saveable

import me.anno.io.files.FileReference
import me.anno.io.saveable.Saveable

open class JsonStringWriter(initialCapacity: Int, workspace: FileReference) : JsonWriterBase(workspace) {

    constructor(workspace: FileReference) : this(32, workspace)

    private val data = StringBuilder(initialCapacity)

    /** you should not use this function
     * if you use it, your file no longer will be readable (probably)
     *
     * Used in Rem's Studio to generate a hash for particle systems.
     * */
    @Suppress("unused")
    fun getFoolishWriteAccess() = data

    override fun append(v: Char) {
        data.append(v)
    }

    override fun append(v: Int) {
        data.append(v)
    }

    override fun append(v: Long) {
        data.append(v)
    }

    override fun append(v: String) {
        data.append(v)
    }

    /**
     * returns the result as a string;
     * only call it, if all writing operations have finished!
     * */
    override fun toString(): String = data.toString()

    companion object {

        fun toText(
            data: Collection<Saveable>, workspace: FileReference,
            resourceMap: Map<FileReference, FileReference> = emptyMap()
        ): String {
            val writer = JsonStringWriter(workspace)
            writer.resourceMap = resourceMap
            for (entry in data) writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toText(entry: Saveable, workspace: FileReference): String {
            val writer = JsonStringWriter(workspace)
            writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun save(entry: Saveable, file: FileReference, workspace: FileReference) {
            save(listOf(entry), file, workspace)
        }

        fun save(data: Collection<Saveable>, file: FileReference, workspace: FileReference) {
            file.outputStream().use { stream ->
                stream.writer().use { writer ->
                    val jsonWriter = JsonStreamWriter(writer, workspace)
                    for (entry in data) jsonWriter.add(entry)
                    jsonWriter.writeAllInList()
                }
            }
        }

        @Suppress("unused")
        fun toBuilder(data: Saveable, workspace: FileReference): StringBuilder {
            val writer = JsonStringWriter(workspace)
            writer.add(data)
            writer.writeAllInList()
            return writer.data
        }
    }
}