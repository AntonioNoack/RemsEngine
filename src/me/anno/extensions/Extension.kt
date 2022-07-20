package me.anno.extensions

import me.anno.extensions.events.Event
import me.anno.extensions.events.EventHandler
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions

abstract class Extension {

    lateinit var name: String
    lateinit var uuid: String
    lateinit var description: String
    lateinit var version: String
    lateinit var authors: String

    lateinit var dependencies: List<String>

    /**
     * extensions with the same priorities can be inited in parallel,
     * default value: 0.0; high priorities get executed first
     * */
    var priority = 0.0

    private val listeners = HashMap<KClass<*>, HashSet<ListenerData>>()

    var isRunning = true

    class ListenerData(val listener: Any, val method: KFunction<*>, val priority: Int) : Comparable<ListenerData> {
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
        priority = info.priority
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
    fun registerListener(listener: Any): Int {
        if (!isRunning) return 0
        var ctr = 0
        listener::class.memberFunctions.forEach { method ->
            if (method.visibility == KVisibility.PUBLIC &&
                !method.isAbstract
            ) {
                val eventHandler = method.annotations
                    .firstOrNull { it is EventHandler } as? EventHandler
                if (eventHandler != null) {
                    val types = method.parameters
                    if (types.size == 1) {
                        val list = listeners.getOrPut(types[0].type as KClass<*>) { HashSet() }
                        list.add(ListenerData(listener, method, eventHandler.priority))
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
        val listeners = listeners[event::class] ?: return
        for (data in listeners) {
            try {
                data.method.call(data.listener, event)
                if (event.isCancelled) {
                    break
                }
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }
    }

}