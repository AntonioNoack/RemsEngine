package me.anno.ecs

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.utils.structures.Hierarchical
import me.anno.utils.types.Floats.f2
import me.anno.utils.types.Floats.f2s
import me.anno.utils.types.Vectors.print

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

// hide the mutable children list, -> not possible with the general approach
// todo keep track of size of hierarchy

// todo load from file whenever something changes;
//  - other way around: when a file changes, update all nodes

// todo delta settings & control: only saves as values, what was changed from the prefab

open class Entity() : NamedSaveable(), Hierarchical<Entity> {

    constructor(parent: Entity?) : this() {
        parent?.add(this)
    }

    constructor(name: String) : this() {
        this.name = name
    }

    constructor(name: String, vararg cs: Component) : this(name) {
        for (c in cs) {
            addComponent(c)
        }
    }

    constructor(vararg cs: Component) : this() {
        for (c in cs) {
            addComponent(c)
        }
    }

    @SerializedProperty
    val components = ArrayList<Component>()

    @SerializedProperty
    override var parent: Entity? = null

    @NotSerializedProperty
    override val children = ArrayList<Entity>()

    @SerializedProperty
    override var isEnabled = true

    val transform = Transform()

    // for the UI
    override var isCollapsed = false

    fun update() {
        for (component in components) component.onUpdate()
        for (child in children) child.update()
        // todo if rigidbody, calculate interpolated transform
    }

    fun physicsUpdate() {
        for (component in components) component.onPhysicsUpdate()
        for (child in children) child.physicsUpdate()
        // todo if rigidbody, calculate physics (?)
    }

    /*
    * val drawable = children.firstOrNull { it is DrawableComponent } ?: return
        val fragmentEffects = children.filterIsInstance<FragmentShaderComponent>()
        (drawable as DrawableComponent).draw(stack, time, color, fragmentEffects)
    * */

    override val className get() = "Entity"

    override val approxSize get() = 1000

    override fun isDefaultValue(): Boolean = false

    fun setParent(parent: Entity, keepWorldPosition: Boolean) {
        if (parent == this.parent) return
        this.parent?.children?.remove(this)
        if (keepWorldPosition) {
            // todo update transform

        }
        parent.children.add(this)
        this.parent = parent
    }

    override fun destroy() {
        // todo call onDestroy of all components

        // todo some event based system? or just callable functions? idk...
        this.parent?.children?.remove(this)
    }

    fun addComponent(component: Component) {
        components.add(component)
        component.entity = this
    }

    override fun addChild(entity: Entity) {
        entity.setParent(this, false)
    }

    fun removeComponent(component: Component) = components.remove(component)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
        // writer.writeObjectList(this, "children", children)
        // writer.writeObjectList(this, "components", components)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "children" -> {
                if (value is Entity) {
                    addChild(value)
                }
            }
            "components" -> {
                if (value is Component) {
                    components.add(value)
                }
            }
            else -> super.readObject(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "children" -> {
                children.clear()
                val entities = values.filterIsInstance<Entity>()
                children.addAll(entities)
                entities.forEach { it.parent = this }
            }
            "components" -> {
                components.clear()
                components.addAll(values.filterIsInstance<Component>())
            }
        }
    }

    inline fun <reified V> getComponent(): V? {
        return components.firstOrNull { it is V } as V?
    }

    inline fun <reified V> getComponentInChildren(): V? {
        return simpleTraversal(true) { getComponent<V>() != null }?.getComponent<V>()
    }

    inline fun <reified V> getComponents(): List<V> {
        return components.filterIsInstance<V>()
    }

    inline fun <reified V> getComponentsInChildren(): List<V> {
        val result = ArrayList<V>()
        val todo = ArrayList<Entity>()
        todo.add(this)
        while (todo.isNotEmpty()) {
            val entity = todo.removeAt(todo.lastIndex)
            result.addAll(entity.getComponents())
            todo.addAll(entity.children)
        }
        return result
    }

    override fun toString(): String {
        return toString(0).toString().trim()
    }

    fun toString(depth: Int): StringBuilder {
        val text = StringBuilder()
        for (i in 0 until depth) text.append('\t')
        text.append("Entity('$name',$sizeOfHierarchy):\n")
        val nextDepth = depth + 1
        for (child in children)
            text.append(child.toString(nextDepth))
        for (component in components)
            text.append(component.toString(nextDepth))
        return text
    }

    fun toStringWithTransforms(depth: Int): StringBuilder {
        val text = StringBuilder()
        for (i in 0 until depth) text.append('\t')
        val p = transform.localPosition
        val r = transform.localRotation
        val s = transform.localScale
        text.append(
            "Entity((${p.x.f2s()},${p.y.f2s()},${p.z.f2s()})," +
                    "(${r.x.f2s()},${r.y.f2s()},${r.z.f2s()},${r.w.f2s()})," +
                    "(${s.x.f2s()},${s.y.f2s()},${s.z.f2s()}),'$name',$sizeOfHierarchy):\n"
        )
        val nextDepth = depth + 1
        for (child in children)
            text.append(child.toStringWithTransforms(nextDepth))
        for (component in components)
            text.append(component.toString(nextDepth))
        return text
    }

    fun add(entity: Entity) = addChild(entity)
    fun add(component: Component) = addComponent(component)

    fun remove(entity: Entity) {
        children.remove(entity)
        if (entity.parent == this) {
            entity.parent = null
        }
    }

    fun remove(component: Component) {
        components.remove(component)
    }

    val sizeOfHierarchy get(): Int = components.size + children.sumOf { 1 + it.sizeOfHierarchy }

    fun clone() = TextReader.fromText(TextWriter.toText(this, false))[0] as Entity

    override fun onDestroy() {}

    override val symbol: String
        get() = ""

    override val defaultDisplayName: String
        get() = "Entity"

}