package me.anno.studio.history

import me.anno.io.base.BaseWriter
import kotlin.math.max
import kotlin.math.min

abstract class StringHistory : History<String>() {

    override fun getTitle(v: String): String {
        return "X${v.length}"
    }

    private var deltaStart = 0
    private var deltaEnd = 0

    override fun readInt(name: String, value: Int) {
        when (name) {
            "s0", "a" -> {
                deltaStart = value
                deltaEnd = value
            }
            "s1", "b" -> deltaEnd = value
            "t0", "c" -> {
                deltaStart = value
                deltaEnd = 0
            }
            else -> super.readInt(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        if (name == "ds" || name == "d") {
            val previous = states.last()
            // if deltaEnd == 0, they will have the same length
            if (deltaEnd == 0) deltaEnd = deltaStart + value.length
            states.add(previous.substring(0, deltaStart) + value + previous.substring(deltaEnd))
        } else super.readString(name, value)
    }

    override fun saveCompressed(writer: BaseWriter, instance: String, previousInstance: String?): Boolean {
        if (previousInstance == null) {
            writer.writeString("state", instance)
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
                writer.writeString("state", instance)
            } else {
                val s2 = s1 + deltaLength
                if (s2 > s0) {
                    if (deltaLength == 0) {
                        writer.writeInt("c", s0, true)
                    } else {
                        writer.writeInt("a", s0, true)
                        writer.writeInt("b", s2)
                    }
                } else writer.writeInt("a", s0, true)
                writer.writeString("d", instance.substring(s0, s1), true)
            }
        }
        return true
    }

    override fun filter(v: Any?): String? = v as? String

}