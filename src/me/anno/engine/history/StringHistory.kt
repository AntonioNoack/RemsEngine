package me.anno.engine.history

import me.anno.io.base.BaseWriter
import kotlin.math.max
import kotlin.math.min

abstract class StringHistory : History<String>() {

    override fun getTitle(v: String): String {
        return "X${v.length}"
    }

    private var deltaStart = 0
    private var deltaEnd = 0

    private fun decodeA(value: Int) {
        deltaStart = value
        deltaEnd = value
    }

    private fun decodeB(value: Int) {
        deltaEnd = value
    }

    private fun decodeC(value: Int) {
        deltaStart = value
        deltaEnd = 0
    }

    private fun decodeD(value: String) {
        val previous = states.last()
        // if deltaEnd == 0, they will have the same length
        if (deltaEnd == 0) deltaEnd = deltaStart + value.length
        states.add(previous.substring(0, deltaStart) + value + previous.substring(deltaEnd))
    }

    override fun setProperty(name: String, value: Any?) {
        if (name == "compressedState") {
            val values = (value as? List<*>)?.filterIsInstance<String>() ?: return
            val letters = values.lastOrNull() ?: return
            loop@ for (i in 0 until min(values.lastIndex, letters.length)) {
                val element = values[i]
                when (letters[i]) {
                    'a' -> decodeA(element.toIntOrNull() ?: break@loop)
                    'b' -> decodeB(element.toIntOrNull() ?: break@loop)
                    'c' -> decodeC(element.toIntOrNull() ?: break@loop)
                    'd' -> decodeD(element)
                    's' -> states.add(element)
                }
            }
        } else {
            val first = name.getOrNull(0) ?: ' '
            when (first) {
                'a' -> decodeA(value as? Int ?: return)
                'b' -> decodeB(value as? Int ?: return)
                'c' -> decodeC(value as? Int ?: return)
                'd' -> decodeD(value.toString())
                else -> super.setProperty(name, value)
            }
        }
    }

    fun saveCompressed(
        instance: String, previousInstance: String?,
        dstLetters: StringBuilder, dstElements: ArrayList<String>
    ) {
        if (previousInstance == null) {
            dstLetters.append('s')
            dstElements.add(instance)
        } else {
            // find first different and last different index
            // save delta state
            var s0 = 0
            val length = min(instance.length, previousInstance.length)
            while (s0 < length && instance[s0] == previousInstance[s0]) {
                s0++
            }
            var s1 = instance.length
            val deltaLength = previousInstance.length - instance.length
            val minS1Index = max(s0, -deltaLength)
            while (s1 > minS1Index && instance[s1 - 1] == previousInstance[s1 - 1 + deltaLength]) {
                s1--
            }
            if (s0 == 0 && s1 == instance.length) {
                dstLetters.append('s')
                dstElements.add(instance)
            } else {
                val s2 = s1 + deltaLength
                if (s2 > s0) {
                    if (deltaLength == 0) {
                        dstLetters.append('c')
                        dstElements.add(s0.toString())
                    } else {
                        dstLetters.append("ab")
                        dstElements.add(s0.toString())
                        dstElements.add(s2.toString())
                    }
                } else {
                    dstLetters.append('a')
                    dstElements.add(s0.toString())
                }
                dstLetters.append('d')
                dstElements.add(instance.substring(s0, s1))
            }
        }
    }

    override fun saveStates(writer: BaseWriter) {
        val dstLetters = StringBuilder()
        val dstElements = ArrayList<String>()
        var previous: String? = null
        for (i in states.indices) {
            val instance = states[i]
            saveCompressed(instance, previous, dstLetters, dstElements)
            previous = instance
        }
        dstElements.add(dstLetters.toString())
        writer.writeStringList("compressedState", dstElements)
    }

    override fun filter(v: Any?): String? = v as? String
}