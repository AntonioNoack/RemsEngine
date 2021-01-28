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

    private val listeners = HashMap<Class<*>, HashSet<Pair<Any, Method>>>()

    var isRunning = true

    fun setInfo(info: ExtensionInfo){
        uuid = info.uuid
        name = info.name
        description = info.description
        version = info.version
        authors = info.authors
        dependencies = info.dependencies
    }

    fun clearListeners(){
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
        var ctr = 0
        if(isRunning){
            any.javaClass.methods.forEach { method ->
                val modifiers = method.modifiers
                if (
                    Modifier.isPublic(modifiers) &&
                    !Modifier.isAbstract(modifiers) &&
                    !Modifier.isStatic(modifiers) &&
                    method.annotations.any { it is EventHandler }
                ) {
                    val types = method.parameterTypes
                    if (types.size == 2) {
                        // first is object, second is argument
                        listeners.getOrPut(types[1]) { HashSet() }.add(any to method)
                        ctr++
                    }
                }
            }
        }
        return ctr
    }

    fun unregisterListener(any: Any) {
        if(isRunning){
            listeners.values.forEach {
                it.removeIf { (listener, _) ->
                    listener == any
                }
            }
        }
    }

    fun onEvent(event: Event) {
        if (event.isCancelled) return
        for ((listener, method) in listeners[event.javaClass] ?: return) {
            method.invoke(listener, event)
            if (event.isCancelled) {
                break
            }
        }
    }

}