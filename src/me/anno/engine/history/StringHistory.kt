package me.anno.engine.history

import me.anno.io.base.BaseWriter
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.max
import kotlin.math.min

abstract class StringHistory : History<String>() {

    override fun getTitle(v: String): String {
        return "X${v.length}"
    }

    override fun setProperty(name: String, value: Any?) {
        if (name == "compressedState2") {
            val values = (value as? List<*>)?.filterIsInstance<String>() ?: return
            val indices = values.lastOrNull()?.split(',')?.mapNotNull { it.toIntOrNull() } ?: return
            states.add(values[0])
            for (i in 0 until min(values.size - 1, indices.size.shr(1))) {
                val i0 = indices[i * 2]
                val delta = indices[i * 2 + 1]
                val added = values[i + 1]
                val prev = states.last
                val endI = i0 + added.length - delta
                states.add(prev.substring(0, i0) + added + prev.substring(endI))
            }
        } else super.setProperty(name, value)
    }

    fun saveCompressed(
        currValue: String, prevValue: String?,
        indices: IntArrayList, addedList: ArrayList<String>
    ) {
        if (prevValue == null) {
            addedList.add(currValue)
        } else if (prevValue != currValue) {
            // find first different and last different index
            // save delta state
            var i0 = 0
            val length = min(currValue.length, prevValue.length)
            while (i0 < length && currValue[i0] == prevValue[i0]) {
                i0++
            }
            var i1 = currValue.length - 1
            val delta = currValue.length - prevValue.length
            val minS1Index = max(i0, delta)
            while (i1 >= minS1Index && currValue[i1] == prevValue[i1 - delta]) {
                i1--
            }
            i1++
            indices.add(i0)
            indices.add(delta)
            addedList.add(currValue.substring(i0, i1))
        }
    }

    override fun saveStates(writer: BaseWriter) {
        val dstLetters = IntArrayList(states.size * 2)
        val dstElements = ArrayList<String>()
        var previous: String? = null
        for (i in states.indices) {
            val instance = states[i]
            saveCompressed(instance, previous, dstLetters, dstElements)
            previous = instance
        }
        dstElements.add(dstLetters.toList().joinToString(","))
        writer.writeStringList("compressedState2", dstElements)
    }

    override fun filter(v: Any?): String? = v as? String
}