package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.utils.algorithms.Recursion
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.sortedAdd
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Systems manages all registered systems; there can only be one system per name;
 * */
object Systems : PrefabSaveable() {

    private val systemByName = HashMap<String, System>()
    private val systems = ArrayList<System>() // sorted by priority
    val readonlySystems: List<System> = systems

    /**
     * registers a system by its class name;
     * returns whether the system was changed
     * */
    fun registerSystem(instance: System) {
        registerSystem(instance.className, instance)
    }

    /**
     * unregisters a system by its class name;
     * returns which system was removed
     * */
    fun unregisterSystem(instance: System): System? { // todo test this method
        return unregisterSystem(instance.className)
    }

    private val systemSorter = Comparator<System> { o1, o2 ->
        o1.priority.compareTo(o2.priority)
            .ifSame(o1.className.compareTo(o2.className))
    }

    // do we need unregistering?
    /**
     * registers a system by its class name;
     * returns whether the system was changed
     * */
    fun registerSystem(id: String, system: System): Boolean {
        val prevSystem: System?
        synchronized(systemByName) {
            prevSystem = systemByName.put(id, system)
            if (prevSystem !== system) {
                if (prevSystem != null) systems.remove(prevSystem)
                systems.sortedAdd(system, systemSorter, true)
            } else return false // done
        }
        prevSystem?.clear()
        val world = world
        if (world != null) setContainsRecursively(world, true, system)
        return true
    }

    /**
     * unregisters a system by its class name;
     * returns which system was removed
     * */
    fun unregisterSystem(id: String): System? {
        val prevSystem: System
        synchronized(systemByName) {
            prevSystem = systemByName.remove(id) ?: return null
            systems.remove(prevSystem)
        }
        prevSystem.clear()
        return prevSystem
    }

    init {
        registerSystem(UpdateSystem)
        registerSystem(BeforeDrawSystem)
        registerSystem(UIEventSystem)
        registerSystem(MotionVectorSystem)
        registerSystem(GlobalSettings)
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
                    setContainsRecursively(value, true)
                }
                field = value
            }
        }

    override fun listChildTypes(): String = "s"
    override fun getChildListByType(type: Char): List<PrefabSaveable> = systems

    fun onUpdate() {
        forAllSystems(Updatable::class) { system ->
            system.update(listOf(system))
        }
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

    fun setContainsRecursively(root: PrefabSaveable, add: Boolean, system: System) {
        Recursion.processRecursive(root) { element, remaining ->
            if (element.isEnabled or (root === element)) {
                for (type in element.listChildTypes()) {
                    remaining.addAll(element.getChildListByType(type))
                }
                when (element) {
                    is Entity -> system.setContains(element, add)
                    is Component -> system.setContains(element, add)
                }
            }
        }
    }

    fun setContainsRecursively(root: PrefabSaveable, add: Boolean) {
        Recursion.processRecursive(root) { element, remaining ->
            if (element.isEnabled or (root === element)) {
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