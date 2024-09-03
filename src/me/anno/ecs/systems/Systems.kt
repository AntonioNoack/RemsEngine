package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.utils.structures.Recursion
import me.anno.utils.structures.lists.Lists.sortedAdd
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Systems manages all registered systems; there can only be one system per name;
 * */
object Systems : PrefabSaveable() {

    private val registeredIDs = HashSet<String>()
    private val systems = ArrayList<System>()
    val readonlySystems: List<System> = systems

    fun registerSystem(instance: System) {
        registerSystem(instance.className, instance)
    }

    // do we need unregistering?
    fun registerSystem(id: String, instance: System) {
        synchronized(registeredIDs) {
            if (registeredIDs.add(id)) {
                systems.sortedAdd(instance, Comparator.comparingInt(System::priority), true)
            }
        }
    }

    init {
        registerSystem(UpdateSystem)
        registerSystem(BeforeDrawSystem)
        registerSystem(UIEventSystem)
    }

    init {
        isCollapsed = false
    }

    @NotSerializedProperty
    var world: PrefabSaveable? = null
        set(value) {
            if (field !== value) {
                for (i in systems.indices) {
                    systems[i].clear()
                }
                if (value != null) {
                    addOrRemoveRecursively(value, true)
                }
                field = value
            }
        }

    override fun listChildTypes(): String = "s"
    override fun getChildListByType(type: Char): List<PrefabSaveable> = systems

    fun onUpdate() {
        forAllSystems { it.onUpdate() }
    }

    fun onBeforeDrawing() {
        forAllSystems { it.onBeforeDrawing() }
    }

    inline fun forAllSystems(callback: (System) -> Unit) {
        val systems = readonlySystems
        for (i in systems.indices) {
            callback(systems[i])
        }
    }

    inline fun <V : Any> forAllSystems(clazz: KClass<V>, callback: (V) -> Unit) {
        val systems = readonlySystems
        for (i in systems.indices) {
            callback(clazz.safeCast(systems[i]) ?: continue)
        }
    }

    fun addOrRemoveRecursively(root: PrefabSaveable, add: Boolean) {
        Recursion.processRecursive(root) { element, remaining ->
            if (element.isEnabled) {
                for (type in element.listChildTypes()) {
                    remaining.addAll(element.getChildListByType(type))
                }
                when (element) {
                    is Entity -> forAllSystems { it.setContains(element, add) }
                    is Component -> forAllSystems { it.setContains(element, add) }
                }
            }
        }
    }
}