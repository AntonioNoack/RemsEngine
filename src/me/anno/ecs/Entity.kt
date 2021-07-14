package me.anno.ecs

import me.anno.animation.Type
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.TextInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.strings.StringHelper.splitCamelCase
import me.anno.utils.structures.Hierarchical
import me.anno.utils.structures.lists.UpdatingList
import me.anno.utils.types.Floats.f2s
import org.joml.Matrix4x3d

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

// hide the mutable children list, -> not possible with the general approach
// todo keep track of size of hierarchy

// todo load from file whenever something changes;
//  - other way around: when a file changes, update all nodes

// todo delta settings & control: only saves as values, what was changed from the prefab

open class Entity() : NamedSaveable(), Hierarchical<Entity>, Inspectable {

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

    inline fun <reified V : Component> getComponent(includingDisabled: Boolean): V? {
        return components.firstOrNull { it is V && (includingDisabled || it.isEnabled) } as V?
    }

    inline fun <reified V : Component> getComponentInChildren(includingDisabled: Boolean): V? {
        return simpleTraversal(true) { getComponent<V>(includingDisabled) != null }?.getComponent<V>(includingDisabled)
    }

    inline fun <reified V : Component> getComponents(includingDisabled: Boolean): List<V> {
        return if (includingDisabled) {
            components.filterIsInstance<V>()
        } else {
            components.filterIsInstance<V>().filter { it.isEnabled }
        }
    }

    inline fun <reified V : Component> getComponentsInChildren(includingDisabled: Boolean): List<V> {
        val result = ArrayList<V>()
        val todo = ArrayList<Entity>()
        todo.add(this)
        while (todo.isNotEmpty()) {
            val entity = todo.removeAt(todo.lastIndex)
            result.addAll(entity.getComponents(includingDisabled))
            if (includingDisabled) {
                todo.addAll(entity.children)
            } else {
                todo.addAll(entity.children.filter { it.isEnabled })
            }
        }
        return result
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        // todo implement the properties and stuff
        // todo add history support (undoing stuff)
        list.add(TextInput("Name", "", style, name).setChangeListener { name = it })
        list.add(TextInput("Description", "", style, description).setChangeListener { description = it })
        list.add(VectorInput(style, "Position", "pos", transform.localPosition, Type.POSITION)
            .setChangeListener { x, y, z, _ -> transform.localPosition.set(x, y, z) })
        list.add(VectorInput(style, "Rotation", "rot", transform.localRotation, Type.ROT_YXZ)
            .setChangeListener { x, y, z, _ -> transform.setLocalEulerAngle(x, y, z) })
        list.add(VectorInput(style, "Scale", "scale", transform.localScale, Type.SCALE)
            .setChangeListener { x, y, z, _ -> transform.localScale.set(x, y, z) })
        list.add(
            object : StackPanel(
                "Components",
                "Customize properties and behaviours",
                getComponentOptions(),
                components,
                style
            ) {
                override fun onAddComponent(component: Inspectable, index: Int) {
                    components.add(index, component as Component)
                }

                override fun onRemoveComponent(component: Inspectable) {
                    components.remove(component)
                }

                override fun getOptionFromInspectable(inspectable: Inspectable): Option {
                    inspectable as Component
                    return Option(inspectable.className, "") { inspectable }
                }

            }
        )
    }

    fun getComponentOptions(): List<Option> {
        // registry over all options... / todo search the raw files + search all scripts
        val knownComponents = ISaveable.objectTypeRegistry.filterValues { it.sampleInstance is Component }
        return UpdatingList {
            knownComponents.map {
                Option(splitCamelCase(it.key), "") {
                    it.value.generator() as Component
                }
            }.sortedBy { it.title }
        }
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
    val depthInHierarchy
        get(): Int {
            val parent = parent ?: return 0
            return parent.depthInHierarchy + 1
        }

    fun fromOtherLocalToLocal(other: Entity): Matrix4x3d {
        // converts the point from the local coordinates of the other one to our local coordinates
        return Matrix4x3d(transform.globalTransform).invert().mul(other.transform.globalTransform)
    }

    fun fromLocalToOtherLocal(other: Entity): Matrix4x3d {
        // converts the point from our local coordinates of the local coordinates of the other one
        return Matrix4x3d(other.transform.globalTransform).invert().mul(transform.globalTransform)
    }

    fun clone() = TextReader.fromText(TextWriter.toText(this, false))[0] as Entity

    override fun onDestroy() {}

    override val symbol: String
        get() = ""

    override val defaultDisplayName: String
        get() = "Entity"

}