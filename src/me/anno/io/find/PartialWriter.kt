package me.anno.io.find

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter

/**
 * a BaseWriter, with the default behaviour of ignoring everything;
 * this is used in Rem's Studio as a way to detect references (whether a pointer needs to be added as such to a file)
 * */
abstract class PartialWriter(canSkipDefaultValues: Boolean) : BaseWriter(canSkipDefaultValues) {

    val writtenObjects = HashSet<ISaveable>(64)

    override fun writeSomething(name: String, value: Any?, force: Boolean) {}

    override fun writeNull(name: String?) {}
    override fun writePointer(name: String?, className: String, ptr: Int, value: ISaveable) {}

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        if (writtenObjects.add(value))
            value.save(this)
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>?, force: Boolean) {
        values ?: return
        for (value in values) {
            if (writtenObjects.add(value))
                value.save(this)
        }
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        for (objects in values) {
            for (value in objects) {
                if (writtenObjects.add(value))
                    value.save(this)
            }
        }
    }

    override fun <V : ISaveable?> writeNullableObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) {
        if (values != null) {
            for (value in values) {
                if (value != null && writtenObjects.add(value))
                    value.save(this)
            }
        }
    }

    override fun <V : ISaveable?> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) {
        for (value in values) {
            if (value != null && writtenObjects.add(value))
                value.save(this)
        }
    }

    override fun writeListStart() {}
    override fun writeListEnd() {}
    override fun writeListSeparator() {}
}