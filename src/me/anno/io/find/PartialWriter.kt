package me.anno.io.find

import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable

/**
 * a BaseWriter, with the default behaviour of ignoring everything;
 * this is used in Rem's Studio as a way to detect references (whether a pointer needs to be added as such to a file)
 * */
abstract class PartialWriter(canSkipDefaultValues: Boolean) : BaseWriter(InvalidRef, canSkipDefaultValues) {

    val writtenObjects = HashSet<Saveable>(64)

    override fun writeSomething(name: String, value: Any?, force: Boolean) {}

    override fun writeNull(name: String?) {}
    override fun writePointer(name: String?, className: String, ptr: Int, value: Saveable) {}

    private fun writeObject(value: Saveable) {
        if (writtenObjects.add(value)) {
            value.save(this)
        }
    }

    override fun writeObjectImpl(name: String?, value: Saveable) {
        writeObject(value)
    }

    override fun <V : Saveable> writeObjectList(self: Saveable?, name: String, values: List<V>, force: Boolean) {
        for (value in values) {
            writeObject(value)
        }
    }

    override fun <V : Saveable> writeObjectList2D(
        self: Saveable?, name: String,
        values: List<List<V>>, force: Boolean
    ) {
        for (objects in values) {
            for (value in objects) {
                writeObject(value)
            }
        }
    }

    override fun <V : Saveable?> writeNullableObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean
    ) {
        for (value in values) {
            writeObject(value ?: continue)
        }
    }

    override fun <V : Saveable?> writeHomogenousObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean
    ) {
        for (value in values) {
            writeObject(value ?: continue)
        }
    }

    override fun writeListStart() {}
    override fun writeListEnd() {}
    override fun writeListSeparator() {}
}