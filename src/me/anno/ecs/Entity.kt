package me.anno.ecs

import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.physics.BulletPhysics
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.text.TextReader
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.utils.types.AABBs.reset
import me.anno.utils.types.AABBs.transformUnion
import me.anno.utils.types.Floats.f2s
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.reflect.KClass

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

// todo buttons via annotation, which can be triggered from the editor for debugging
// e.g. @Action

// hide the mutable children list, -> not possible with the general approach
// todo keep track of size of hierarchy

// todo load from file whenever something changes;
//  - other way around: when a file changes, update all nodes

// todo delta settings & control: only saves as values, what was changed from the prefab

class Entity() : PrefabSaveable(), Inspectable {

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

    @NotSerializedProperty
    private val internalComponents = ArrayList<Component>()

    @SerializedProperty
    val components: List<Component>
        get() = internalComponents

    // @SerializedProperty
    // override var parent: Entity? = null

    @NotSerializedProperty
    private val internalChildren = ArrayList<Entity>()

    @NotSerializedProperty
    override val children: List<Entity>
        get() = internalChildren

    override fun listChildTypes(): String = "ec" // entity children, components

    override fun getChildListByType(type: Char): List<PrefabSaveable> {
        return if (type == 'c') components else children
    }

    override fun addChildByType(index: Int, type: Char, instance: PrefabSaveable) {
        if (type == 'c') addComponent(index, instance as Component)
        else addChild(index, instance as Entity)
    }

    override fun getChildListNiceName(type: Char): String {
        return if (type == 'c') "components" else "children"
    }

    override fun getIndexOf(child: PrefabSaveable): Int {
        return if (child is Component) {
            components.indexOf(child)
        } else children.indexOf(child)
    }

    override fun getOptionsByType(type: Char): List<Option>? {
        return if (type == 'c') Component.getComponentOptions(this)
        else null
    }

    @NotSerializedProperty
    var hasValidAABB = false

    @SerializedProperty
    var position: Vector3d
        get() = transform.localPosition
        set(value) {
            transform.localPosition = value
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @SerializedProperty
    var rotation: Quaterniond
        get() = transform.localRotation
        set(value) {
            transform.localRotation = value
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @SerializedProperty
    var scale: Vector3d
        get() = transform.localScale
        set(value) {
            transform.localScale = value
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @NotSerializedProperty
    val parentEntity
        get() = parent as? Entity

    /**
     * smoothly transitions to the next position
     * */
    fun moveTo(position: Vector3d) {
        transform.globalTransform.m30(position.x)
        transform.globalTransform.m30(position.y)
        transform.globalTransform.m30(position.z)
        transform.calculateLocalTransform(parentEntity?.transform)
        transform.update()
        invalidateAABBsCompletely()
        invalidatePhysics(false)
    }

    /**
     * teleports to the new position without interpolation
     * */
    fun teleportTo(position: Vector3d) {
        transform.globalTransform.m30(position.x)
        transform.globalTransform.m30(position.y)
        transform.globalTransform.m30(position.z)
        transform.calculateLocalTransform(parentEntity?.transform)
        transform.teleportUpdate()
        invalidateAABBsCompletely()
        invalidatePhysics(false)
    }

    @NotSerializedProperty
    val aabb = AABBd()

    @NotSerializedProperty
    var hasRenderables = false

    fun invalidatePhysics(force: Boolean) {
        if (force || hasPhysicsInfluence()) {
            physics?.invalidate(rigidbody ?: return)
        }
    }

    fun invalidateAABBsCompletely() {
        invalidateOwnAABB()
        invalidateChildAABBs()
    }

    private fun invalidateOwnAABB() {
        if (hasValidAABB) {
            hasValidAABB = false
            parentEntity?.invalidateOwnAABB()
        }
    }

    private fun invalidateChildAABBs() {
        hasValidAABB = false
        val children = children
        for (i in children.indices) {
            children[i].invalidateChildAABBs()
        }
    }

    fun validateAABBs() {
        if (hasValidAABB) {
            // to check if all invalidations were applied correctly
            /*val oldAABB = AABBd(aabb)
            hasValidAABB = false
            validateAABBs()
            if (oldAABB != aabb) LOGGER.warn("AABBs differed: $aabb vs $oldAABB, $name")*/
            return
        }
        hasValidAABB = true
        val children = children
        for (i in children.indices) {
            children[i].validateAABBs()
        }
        aabb.reset()
        if (hasRenderables) {
            // todo if has particle system, include
            val globalTransform = transform.globalTransform
            val components = components
            for (i in components.indices) {
                val component = components[i]
                if (component.isEnabled) {
                    when (component) {
                        is MeshComponent -> {
                            // add aabb of that mesh with the transform
                            val mesh = component.mesh ?: continue
                            mesh.ensureBuffer()
                            mesh.aabb.transformUnion(globalTransform, aabb)
                        }
                        is LightComponent -> {
                            val mesh = component.getLightPrimitive()
                            mesh.ensureBuffer()
                            mesh.aabb.transformUnion(globalTransform, aabb)
                        }
                        is AmbientLight -> {
                            // ambient light has influence on everything
                            val n = Double.NEGATIVE_INFINITY
                            val p = Double.POSITIVE_INFINITY
                            aabb.setMin(n, n, n)
                            aabb.setMax(p, p, p)
                        }
                        // todo drawing colliders for GUI -> would need to be included?
                    }
                }
            }
        }
        for (i in children.indices) {
            aabb.union(children[i].aabb)
        }
    }

    @SerializedProperty
    override var isEnabled = true
        set(value) {
            field = value
            invalidatePhysics(false)
        }

    fun hasPhysicsInfluence(): Boolean {
        return isPhysicsControlled || parentEntity?.hasPhysicsInfluence() == true
        // return hasComponent(false, Rigidbody::class) ||
        //        hasComponentInChildren(false, Collider::class)
    }

    val transform = Transform()

    // assigned and tested for click checks
    var clickId = 0

    fun update() {
        for (component in components) component.onUpdate()
        for (child in children) child.update()
    }

    // is set by the engine
    @NotSerializedProperty
    var isPhysicsControlled = false

    fun validateTransforms(time: Long = GFX.gameTime) {
        if (!isPhysicsControlled) {
            transform.update(parentEntity?.transform, time)
            val children = children
            for (i in children.indices) {
                children[i].validateTransforms(time)
            }
        }
    }

    fun invalidate() {

    }

    fun physicsUpdate() {
        // called by physics thread
        // only called for rigidbodies
        // not called for static objects (?), since they should not move
        for (component in components) component.onPhysicsUpdate()
    }

    /*
    * val drawable = children.firstOrNull { it is DrawableComponent } ?: return
        val fragmentEffects = children.filterIsInstance<FragmentShaderComponent>()
        (drawable as DrawableComponent).draw(stack, time, color, fragmentEffects)
    * */

    override val className get() = "Entity"

    override fun isDefaultValue(): Boolean = false

    private fun transformUpdate(parent: Entity, keepWorldTransform: Boolean) {
        if (keepWorldTransform) {
            transform.calculateLocalTransform(parent.transform)
            // global transform theoretically stays the same
            // it will not, if there is an anomaly, e.g. scale 0
        }
        transform.invalidateGlobal()
        invalidateAABBsCompletely()
    }

    override fun add(child: PrefabSaveable) {
        TODO("Not yet implemented")
    }

    override fun add(index: Int, child: PrefabSaveable) {
        TODO("Not yet implemented")
    }

    override fun remove(child: PrefabSaveable) {
        when (child) {
            is Entity -> removeChild(child)
            is Component -> removeComponent(child)
        }
    }

    fun add(index: Int, child: Entity) {
        addChild(index, child)
    }

    // todo don't directly update, rather invalidate this, because there may be more to come
    fun setParent(parent: Entity, index: Int, keepWorldTransform: Boolean) {

        val oldParent = parentEntity
        if (parent === oldParent) return

        // formalities
        if (oldParent != null) {
            oldParent.remove(this)
            oldParent.invalidateOwnAABB()
        }

        parent.internalChildren.add(index, this)
        this.parent = parent

        // transform
        transformUpdate(parent, keepWorldTransform)

        checkNeedsPhysics()

    }

    // todo don't directly update, rather invalidate this, because there may be more to come
    fun setParent(parent: Entity, keepWorldTransform: Boolean) {
        return setParent(parent, parent.children.size, keepWorldTransform)
    }

    private fun checkNeedsPhysics() {
        // physics
        if (listOfHierarchy.all { isEnabled }) {
            // something can change
            val physics = physics
            if (physics != null) {
                // if there is a rigidbody in the hierarchy, update it
                val parentRigidbody = rigidbody
                if (parentRigidbody != null) {
                    // invalidate it
                    physics.invalidate(parentRigidbody)
                } else {
                    // if has collider without rigidbody, add it for click-tests
                    if (hasComponent(Collider::class, false)) {
                        // todo add it for click tests
                    }
                }
            }
        }
    }

    val physics get() = getRoot(Entity::class).getComponent(BulletPhysics::class, false)
    val rigidbody
        get() = listOfHierarchyReversed.firstOrNull {
            it is Entity && it.hasComponent(Rigidbody::class, false)
        } as? Entity

    val rigidbodyComponent: Rigidbody?
        get() {
            for (entry in listOfHierarchyReversed) {
                if (entry is Entity) {
                    return entry.getComponent(Rigidbody::class, false) ?: continue
                }
            }
            return null
        }

    fun invalidateRigidbody() {
        physics?.invalidate(rigidbody ?: return)
    }

    override fun destroy() {
        for (component in components) {
            component.onDestroy()
        }
        // todo some event based system? or just callable functions? idk...
        this.parent?.remove(this)
    }

    fun addComponent(component: Component) {
        internalComponents.add(component)
        onAddComponent(component)
    }

    fun addComponent(index: Int, component: Component) {
        internalComponents.add(index, component)
        onAddComponent(component)
    }

    private fun onAddComponent(component: Component) {
        // if component is Collider or Rigidbody, update the physics
        // todo isEnabled for Colliders and Rigidbody needs to have listeners as well
        onChangeComponent(component)
        component.entity = this
    }

    private fun onChangeComponent(component: Component) {
        if (component.isEnabled) {
            when (component) {
                is Collider -> invalidateRigidbody()
                is Rigidbody -> physics?.invalidate(this)
                is MeshComponent -> invalidateOwnAABB()
                is LightComponent -> invalidateOwnAABB()
            }
        }
        hasRenderables = hasComponent(MeshComponent::class, false) ||
                hasComponent(LightComponent::class, false)
        invalidateOwnAABB()
    }

    fun addChild(child: Entity) {
        child.setParent(this, children.size, false)
    }

    fun addChild(index: Int, child: Entity) {
        child.setParent(this, index, false)
    }

    fun remove(component: Component) {
        removeComponent(component)
    }

    fun removeComponent(component: Component) {
        internalComponents.remove(component)
        onChangeComponent(component)
    }

    fun <V : Component> hasComponent(clazz: KClass<V>, includingDisabled: Boolean): Boolean {
        return getComponent(clazz, includingDisabled) != null
    }

    fun <V : Component> hasComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean): Boolean {
        if (hasComponent(clazz, includingDisabled)) return true
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (includingDisabled || child.isEnabled) {
                if (child.hasComponent(clazz, includingDisabled)) {
                    return true
                }
            }
        }
        return false
    }

    fun <V : Component> getComponent(clazz: KClass<V>, includingDisabled: Boolean): V? {
        // elegant:
        // return components.firstOrNull { clazz.isInstance(it) && (includingDisabled || it.isEnabled) } as V?
        // without damn iterator:
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if ((includingDisabled || component.isEnabled) && clazz.isInstance(component)) {
                return component as V
            }
        }
        return null
    }

    fun <V : Component> getComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean): V? {
        val e = simpleTraversal(true) { getComponent(clazz, includingDisabled) != null }
        return (e as? Entity)?.getComponent(clazz, includingDisabled)
    }

    fun <V : Component> getComponents(clazz: KClass<V>, includingDisabled: Boolean): List<V> {
        return components.filter { (includingDisabled || it.isEnabled) && clazz.isInstance(it) } as List<V>
    }

    fun <V : Component> getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean): List<V> {
        val result = ArrayList<V>()
        val todo = ArrayList<Entity>()
        todo.add(this)
        while (todo.isNotEmpty()) {
            val entity = todo.removeAt(todo.lastIndex)
            result.addAll(entity.getComponents(clazz, includingDisabled))
            todo.addAll(if (includingDisabled) entity.children
            else entity.children.filter { it.isEnabled })
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

    fun add(child: Entity) = addChild(child)
    fun add(component: Component) = addComponent(component)

    fun remove(child: Entity) {
        if (child.parent !== this) return
        // todo invalidate physics
        internalChildren.remove(child)
        if (child.parent == this) {
            child.parent = null
        }
    }

    val sizeOfHierarchy
        get(): Int {
            val children = children
            var sum = children.size + components.size
            for (i in children.indices) {
                sum += children[i].sizeOfHierarchy
            }
            return sum
        }

    val depthInHierarchy
        get(): Int {
            val parent = parentEntity ?: return 0
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

    fun clone() = TextReader.clone(this) as Entity

    override fun onDestroy() {}

    override val symbol: String
        get() = ""

    override val defaultDisplayName: String
        get() = "Entity"

    // which properties were changed
    // options:
    // - child/index, child/name
    // - component/index, component/name
    // - position, rotation, scale
    // - name, description,
    // - isEnabled

    // var prefabPath: FileReference = InvalidRef

    // var prefab: Entity? = null
    // var ownPath: FileReference = InvalidRef // where our file is located

    // get root somehow? how can we detect it?
    // not possible in the whole scene for sub-scenes
    // however, when we are only editing prefabs, it would be possible :)

    /*fun pathInRoot(root: Entity): ArrayList<Int> {
        if (this == root) return arrayListOf()
        val parent = parent
        return if (parent != null) {
            val ownIndex = parent.children.indexOf(this@Entity)
            parent.pathInRoot().apply {
                add(ownIndex)
            }
        } else arrayListOf()
    }

    fun pathInRoot(): ArrayList<Int> {
        val parent = parent
        return if (parent != null) {
            val ownIndex = parent.children.indexOf(this@Entity)
            parent.pathInRoot().apply {
                add(ownIndex)
            }
        } else arrayListOf()
    }*/

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        // interpolation tests
        /*list += UpdatingTextPanel(50, style) {
            val t = transform
            "1x/${(t.lastUpdateDt * 1e-9).f3()}s, ${((GFX.gameTime - t.lastUpdateTime) * 1e-9).f3()}s ago"
        }*/
        PrefabInspector.currentInspector!!.inspect(this, list, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "children", children)
        writer.writeObjectList(this, "components", components)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "children" -> {
                internalChildren.clear()
                internalChildren.ensureCapacity(values.size)
                for (value in values) {
                    if (value is Entity) {
                        addChild(value)
                    }
                }
            }
            "components" -> {
                internalComponents.clear()
                internalComponents.ensureCapacity(values.size)
                for (value in values) {
                    if (value is Component) {
                        addComponent(value)
                    }
                }
            }
        }
    }

}