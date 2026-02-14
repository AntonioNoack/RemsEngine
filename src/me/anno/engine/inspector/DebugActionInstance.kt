package me.anno.engine.inspector

import me.anno.utils.structures.Compare.ifSame
import java.lang.reflect.Method

class DebugActionInstance(
    val method: Method, val title: String,
    val parameterNames: List<String>, val order: Int
) : Comparable<DebugActionInstance> {
    override fun compareTo(other: DebugActionInstance): Int {
        return order.compareTo(other.order).ifSame { title.compareTo(other.title) }
    }
}