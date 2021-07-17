package me.anno.ecs.prefab

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.utils.LOGGER

// the first try for prefabs like in Unity
// we need those to work flawlessly for the engine to be usable

// todo the thing, that is actually saved and edited
// todo also do prefab components

// todo this is not the thing, that will be used in-game

// todo static stores of data (libraries/databases...)


// todo load all prefabs into memory, so when something changes, we can just apply the change to all instances instantly

// -> prefab libraries? no, just all descendants of a thing must be placed in the same file
// -> this is not possible, because an entity may contain two children from different files


// todo paste images (partial screenshot, win+shift+s) into file explorer? we could name it like a screenshot, if we have no name

// -- this is theoretically nice, but how do we do it, when in game mode?
// game mode does not change the prefabs -> problem solved
// or we add a list, to which ones shall be saved

open class PrefabProperty : NamedSaveable() {

    // for the editor
    var type = ""
    var oldValue: PrefabProperty? = null
    var customValue: Any? = null
    var isCustom = false

    override val className: String = "PrefabProperty"

}

class PrefabPropertyList : PrefabProperty() {

    class AdditionalElement(var previous: NamedSaveable, var value: NamedSaveable?)

    var disabledElements = HashSet<NamedSaveable>()

    // e.g. when changing child properties in a prefab
    var overriddenElements = ArrayList<AdditionalElement>()

    var additionalElements = ArrayList<AdditionalElement>()

    override val className: String = "PrefabPropertyList"

}

abstract class Prefab0<Type : NamedSaveable>() : NamedSaveable() {

    var prefab: Prefab0<Type>? = null

    val customProperties = HashMap<String, Any?>()

    fun getPropertyOrNull(name: String): Any? {
        return customProperties[name] ?: prefab?.getPropertyOrNull(name)
    }

    fun resetProperty(name: String): Boolean {
        val result = name in customProperties
        customProperties.remove(name)
        return result
    }

    fun setProperty(name: String, value: Any?) {
        customProperties[name] = value
    }

    fun setPropertyInParentPrefab(name: String, value: Any?) {
        val prefab = prefab
        if (prefab != null) {
            customProperties.remove(name)
            prefab.setProperty(name, value)
        } else {
            // mmh...
            setProperty(name, value)
        }
    }

    fun instantiate(): Type {
        val instance = prefab?.instantiate() ?: createInstance()
        // only needs all properties
        // does not need inheritance
        // todo set all properties
        val reflections = instance.getReflections()
        for ((name, value) in customProperties) {
            if (value is PrefabPropertyList) {
                // todo apply these specific changes

            } else {
                // just set the value
                if (!reflections.set(instance, name, value)) {
                    // can happen e.g., when the underlying class is changed
                    LOGGER.warn("Property $name could not be found.")
                }
            }
        }
        return instance
    }

    // to transfer properties from game mode to edit mode
    fun loadProperties(instance: Type) {
        for (name in listProperties()) {
            val newValue = instance.getReflections()[instance, name]
            if (getPropertyOrNull(name) != newValue) {
                // save the value
                setProperty(name, newValue)
            }
        }
    }

    fun listProperties(): Collection<String> {
        return createInstance().getReflections().properties.keys
    }

    fun createChildPrefab() {
        // todo deep clone the thing

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // save all properties that we need
        writer.writeObject(this, "prefab", prefab)
        for ((name, value) in customProperties) {
            writer.writeSomething(this, "custom:$name", value, true)
        }
    }

    abstract fun createInstance(): Type

}

class PrefabEntity0 : Prefab0<Entity>() {

    fun add(child: PrefabEntity0) {
        // todo add thing to mutable list
        // setProperty("children", getPropertyOrNull("children") + child)
    }

    fun add(component: PrefabComponent0) {
        // todo add thing to mutable list
        // setProperty("components", getPropertyOrNull("components") + component)
    }

    fun setPropertyInComponent(component: Component, name: String, value: Any?) {
        // todo if not already custom, make it custom
        // todo
    }

    override fun createInstance(): Entity {
        return Entity()
    }

    override val className: String = "PrefabEntity"

}

class PrefabComponent0 : Prefab0<Component>() {

    var type = ""

    override fun createInstance(): Component {
        val entry = ISaveable.objectTypeRegistry[type] ?: throw RuntimeException("Unknown type '$type'")
        return entry.generator() as? Component ?: throw RuntimeException("Type '$type' must be a Component")
    }

    override val className: String = "PrefabComponent"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "type" -> type = name
            else -> super.readString(name, value)
        }
    }

}

// in a save file, we could include the prefab itself and all its instances
// issue 1: prefabs may be created from meshes, and we cannot write to these files
// issue 2: prefabs can be used in other files -> the correct file would be ambiguous
