package me.anno.ecs

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

// todo hide the mutable list,
// todo keep track of size of hierarchy

open class Entity : NamedSaveable() {

    @SerializedProperty
    val components = ArrayList<Component>()

    @SerializedProperty
    var parent: Entity? = null

    @NotSerializedProperty
    private val childList = ArrayList<Entity>()

    @NotSerializedProperty
    val children: List<Entity> = childList

    @SerializedProperty
    var isEnabled = true

    var transform = Transform()

    // for the UI
    var isCollapsed = false

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

    override fun getClassName(): String = "Entity"

    override fun getApproxSize(): Int = 1000

    override fun isDefaultValue(): Boolean = false

    fun setParent(parent: Entity, keepWorldPosition: Boolean) {
        if (parent == this.parent) return
        this.parent?.childList?.remove(this)
        if (keepWorldPosition) {
            // todo update transform

        }
        parent.childList.add(this)
        this.parent = parent
    }

    fun destroy() {
        // todo call onDestroy of all components

        // todo some event based system? or just callable functions? idk...
        this.parent?.childList?.remove(this)
    }

    fun addComponent(component: Component) {
        components.add(component)
        component.entity = this
    }

    fun addChild(entity: Entity) {
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
                childList.clear()
                val entities = values.filterIsInstance<Entity>()
                childList.addAll(entities)
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

    fun add(entity: Entity) = addChild(entity)
    fun add(component: Component) = addComponent(component)

    fun remove(entity: Entity) {
        childList.remove(entity)
        if (entity.parent == this) {
            entity.parent = null
        }
    }

    fun remove(component: Component) {
        components.remove(component)
    }

    val sizeOfHierarchy get(): Int = components.size + children.sumOf { 1 + it.sizeOfHierarchy }

    fun clone() = TextReader.fromText(TextWriter.toText(this, false))[0] as Entity

}