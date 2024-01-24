package me.anno.io.find

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

/**
 * a BaseWriter, with the default behaviour of ignoring everything;
 * this is used in Rem's Studio as a way to detect references (whether a pointer needs to be added as such to a file)
 * */
abstract class PartialWriter(canSkipDefaultValues: Boolean) : BaseWriter(canSkipDefaultValues) {

    val writtenObjects = HashSet<Saveable>(64)

    override fun writeSomething(name: String, value: Any?, force: Boolean) {}

    override fun writeNull(name: String?) {}
    override fun writePointer(name: String?, className: String, ptr: Int, value: Saveable) {}

    override fun writeObjectImpl(name: String?, value: Saveable) {
        if (writtenObjects.add(value))
            value.save(this)
    }

    override fun <V : Saveable> writeObjectArray(self: Saveable?, name: String, values: Array<V>?, force: Boolean) {
        values ?: return
        for (value in values) {
            if (writtenObjects.add(value))
                value.save(this)
        }
    }

    override fun <V : Saveable> writeObjectArray2D(
        self: Saveable?,
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

    override fun <V : Saveable?> writeNullableObjectArray(
        self: Saveable?,
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

    override fun <V : Saveable?> writeHomogenousObjectArray(
        self: Saveable?,
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