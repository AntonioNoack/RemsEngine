package me.anno.io.json.saveable

import me.anno.io.saveable.Saveable
import me.anno.io.files.FileReference

class JsonStringWriter(initialCapacity: Int, workspace: FileReference) : JsonWriterBase(workspace) {

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

        fun toText(data: Collection<Saveable>, workspace: FileReference): String {
            val writer = JsonStringWriter(workspace)
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
            file.outputStream().use {
                val writer = JsonStreamWriter(it, workspace)
                writer.add(entry)
                writer.writeAllInList()
            }
        }

        fun save(data: Collection<Saveable>, file: FileReference, workspace: FileReference) {
            file.outputStream().use {
                val writer = JsonStreamWriter(it, workspace)
                for (entry in data) writer.add(entry)
                writer.writeAllInList()
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