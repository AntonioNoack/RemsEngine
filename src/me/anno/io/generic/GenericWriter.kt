package me.anno.io.generic

/**
 * Interface for implementing JSON, XML and YAML writers.
 *
 * This is also a scanner interface.
 * */
interface GenericWriter {
    companion object {

        inline fun GenericWriter.writeObject(fillObject: () -> Unit) {
            beginObject(null)
            fillObject()
            endObject()
        }

        inline fun GenericWriter.writeObject(tag: String, fillObject: () -> Unit) {
            beginObject(tag)
            fillObject()
            endObject()
        }

        inline fun GenericWriter.writeArray(fillArray: () -> Unit) {
            beginArray()
            fillArray()
            endArray()
        }

        fun <V> GenericWriter.writeArray(elements: List<V>, writeElement: (V) -> Unit) {
            writeArray {
                for (i in elements.indices) {
                    writeElement(elements[i])
                }
            }
        }

        fun <V> GenericWriter.writeArrayIndexed(elements: List<V>, writeElement: (Int, V) -> Unit) {
            writeArray {
                for (i in elements.indices) {
                    writeElement(i, elements[i])
                }
            }
        }

        fun GenericWriter.writeArrayByIndices(i0: Int, i1: Int, writeElement: (Int) -> Unit) {
            writeArray {
                for (i in i0 until i1) {
                    writeElement(i)
                }
            }
        }

        fun GenericWriter.attr(tag: String, value: CharSequence, isString: Boolean = true) {
            attr(tag)
            write(value, isString)
        }

        fun GenericWriter.attr(tag: String, value: Float) {
            attr(tag)
            write(value)
        }

        fun GenericWriter.attr(tag: String, value: Double) {
            attr(tag)
            write(value)
        }

        fun GenericWriter.attr(tag: String, value: Int) {
            attr(tag)
            write(value)
        }

        fun GenericWriter.attr(tag: String, value: Long) {
            attr(tag)
            write(value)
        }

        fun GenericWriter.attr(tag: String, value: Boolean) {
            attr(tag)
            write(value)
        }

        fun GenericWriter.attr(tag: String, value: Nothing?) {
            attr(tag)
            write(value)
        }

    }

    /**
     * Start reading an object.
     * Returns whether it shall be read, not skipped.
     * */
    fun beginObject(tag: CharSequence?): Boolean
    fun endObject()

    /**
     * Start reading an array.
     * Returns whether it shall be read, not skipped.
     * */
    fun beginArray(): Boolean
    fun endArray()

    /**
     * Start reading a property.
     * Returns whether it shall be read, not skipped.
     *
     * Must be followed by an object, array or value.
     * */
    fun attr(tag: CharSequence): Boolean

    fun write(value: CharSequence, isString: Boolean = true)

    fun write(value: Nothing?) {
        write("null", false)
    }

    fun write(value: Float) = write(value.toString(),false)
    fun write(value: Double) {
        write(value.toString(), false)
    }

    fun write(value: Int) = write(value.toLong())
    fun write(value: Long) {
        write(value.toString(), false)
    }

    fun write(value: Boolean) {
        write(if (value) "true" else "false", false)
    }
}