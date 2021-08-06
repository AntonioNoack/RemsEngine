package me.anno.extensions

import me.anno.extensions.events.Event
import me.anno.extensions.events.EventHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier

// todo priorities?...
abstract class Extension {

    lateinit var name: String
    lateinit var uuid: String
    lateinit var description: String
    lateinit var version: String
    lateinit var authors: String

    lateinit var dependencies: List<String>

    private val listeners = HashMap<Class<*>, HashSet<ListenerData>>()

    var isRunning = true

    class ListenerData(val listener: Any, val method: Method, val priority: Int) : Comparable<ListenerData> {
        override fun compareTo(other: ListenerData): Int {
            return priority.compareTo(other.priority)
        }
    }

    fun setInfo(info: ExtensionInfo) {
        uuid = info.uuid
        name = info.name
        description = info.description
        version = info.version
        authors = info.authors
        dependencies = info.dependencies
    }

    fun clearListeners() {
        listeners.clear()
    }

    /**
     * register an object with listener methods
     * this method returns, how many valid listener
     * methods of the type
     * public (non-static) (non-abstract) void <anyName>(EventClass event){}
     * were found
     * */
    fun registerListener(any: Any): Int {
        if (!isRunning) return 0
        var ctr = 0
        any.javaClass.methods.forEach { method ->
            val modifiers = method.modifiers
            if (
                Modifier.isPublic(modifiers) &&
                !Modifier.isAbstract(modifiers) &&
                !Modifier.isStatic(modifiers)
            ) {
                val eventHandler = method.annotations.firstOrNull { it is EventHandler } as? EventHandler
                if (eventHandler != null) {
                    val types = method.parameterTypes
                    if (types.size == 1) {
                        val list = listeners.getOrPut(types[0]) { HashSet() }
                        list.add(ListenerData(any, method, eventHandler.priority))
                        ctr++
                    }
                }
            }
        }
        return ctr
    }

    fun unregisterListener(any: Any) {
        if (!isRunning) return
        for (data in listeners.values) {
            data.removeIf { it.listener == any }
        }
    }

    fun onEvent(event: Event) {
        if (event.isCancelled) return
        val listeners = listeners[event.javaClass] ?: return
        for (data in listeners) {
            data.method.invoke(data.listener, event)
            if (event.isCancelled) {
                break
            }
        }
    }

}