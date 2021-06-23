package me.anno.ecs

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

class Entity : NamedSaveable() {

    @SerializedProperty
    val components = ArrayList<Component>()

    @SerializedProperty
    var parent: Entity? = null

    @NotSerializedProperty
    val children = ArrayList<Entity>()

    @SerializedProperty
    var isEnabled = true

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
        this.parent?.children?.remove(this)
        if (keepWorldPosition) {
            // todo update transform

        }
        parent.children.add(this)
        this.parent = parent
    }

    fun destroy() {
        // todo call onDestroy of all components

        // todo some event based system? or just callable functions? idk...
        this.parent?.children?.remove(this)
    }

    fun addComponent(component: Component) {
        components.add(component)
        component.entity = this
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
            "children" -> children.add(value as? Entity ?: return)
            "components" -> components.add(value as? Component ?: return)
            else -> super.readObject(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "children" -> {
                children.clear()
                children.addAll(values.filterIsInstance<Entity>())
            }
            "components" -> {
                components.clear()
                components.addAll(values.filterIsInstance<Component>())
            }
        }
    }

}